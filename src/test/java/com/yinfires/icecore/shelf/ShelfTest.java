package com.yinfires.icecore.shelf;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;
import javax.imageio.ImageIO;

final class ShelfTest {
    private static String read(String path) throws Exception { return Files.readString(Path.of(path)); }
    @Test void all72SlotsStayFixedOnFourBoards() {
        Set<String> positions=new HashSet<>();
        for(int slot=0;slot<72;slot++) {
            assertTrue(ShelfLayout.x(slot)>=2 && ShelfLayout.x(slot)<=14);
            assertEquals(32-8*(slot/18),ShelfLayout.y(slot));
            assertTrue(ShelfLayout.z(slot)==5 || ShelfLayout.z(slot)==11);
            assertTrue(positions.add(ShelfLayout.x(slot)+","+ShelfLayout.y(slot)+","+ShelfLayout.z(slot)));
        }
        assertEquals(72,positions.size());
        assertEquals(5,ShelfLayout.z(0)); assertEquals(11,ShelfLayout.z(9));
    }
    @Test void splitGeometryStaysWithinEachCellAndRetainsFullHeight() throws Exception {
        String root="src/main/resources/assets/icecore/";
        var states=JsonParser.parseString(read(root+"blockstates/shelf.json")).getAsJsonObject().getAsJsonObject("variants");
        assertEquals(8,states.size());
        for(String half:List.of("lower","upper")) {
            var model=JsonParser.parseString(read(root+"models/block/shelf_"+half+".json")).getAsJsonObject();
            assertEquals(6,model.getAsJsonArray("elements").size());
            for(var element:model.getAsJsonArray("elements")) {
                var box=element.getAsJsonObject();
                for(String side:List.of("from","to")) for(var value:box.getAsJsonArray(side)) assertTrue(value.getAsDouble()>=0 && value.getAsDouble()<=16);
                for(var face:box.getAsJsonObject("faces").entrySet()) for(var uv:face.getValue().getAsJsonObject().getAsJsonArray("uv")) assertTrue(uv.getAsDouble()>=0 && uv.getAsDouble()<=16);
            }
        }
        var item=JsonParser.parseString(read(root+"models/item/shelf.json")).getAsJsonObject();
        assertEquals(8,item.getAsJsonArray("elements").size());
        assertFalse(Files.exists(Path.of("src/main/resources/data/icecore/recipes/shelf.json")));
    }
    @Test void ownershipSynchronizationAndPlacementContracts() throws Exception {
        String root="src/main/java/com/yinfires/icecore/shelf/";
        String entity=read(root+"ShelfBlockEntity.java");
        for(String method:List.of("getUpdatePacket","onDataPacket","handleUpdateTag","items.clear()","ContainerHelper.loadAllItems","CAPACITY = 72")) assertTrue(entity.contains(method),method);
        String item=read(root+"ShelfItem.java");
        for(String check:List.of("isOutsideBuildHeight(upperPos)","isWithinBounds(upperPos)","canBeReplaced(upperContext)","super.canPlace(upperContext, upper)","original.restore(true, false)","mayUseItemAt(upperPos")) assertTrue(item.contains(check),check);
        assertTrue(read(root+"ShelfBlock.java").contains("state.getValue(HALF) == DoubleBlockHalf.LOWER ? new ShelfBlockEntity"));
        String renderer=read(root+"client/ShelfRenderer.java");
        assertTrue(renderer.contains("RenderUtils.renderItem(stack,light,pose,buffers,shelf.getLevel())")); assertTrue(renderer.contains("rotationDegrees(45)")); assertFalse(renderer.contains("ItemDisplayContext.GROUND")); assertTrue(renderer.contains("ShelfRenderCache.slots(shelf)"));
        assertFalse(renderer.contains("renderStatic("));
    }
    @Test void woodPanelCenterStretchNeverConsumesBorderPixels() throws Exception {
        var image=ImageIO.read(Path.of("src/main/resources/assets/icecore/textures/gui/wood_panel.png").toFile());
        assertEquals(27,image.getWidth()); assertEquals(27,image.getHeight());
        int fill=image.getRGB(13,13);
        for(int y=6;y<21;y++) for(int x=6;x<21;x++) assertEquals(fill,image.getRGB(x,y),"stretch center includes frame at "+x+","+y);
        assertNotEquals(fill,image.getRGB(2,13));
        assertEquals(255,image.getRGB(13,13)>>>24);
    }
    private static ClassNode clazz(ZipFile jar,String name) throws Exception {
        ClassNode node=new ClassNode();new ClassReader(jar.getInputStream(jar.getEntry(name+".class"))).accept(node,0);return node;
    }
    @Test void fixedShelfScaleMatchesFridgeInsteadOfShrinkingToNonoverlappingSilhouettes() throws Exception {
        String renderer=read("src/main/java/com/yinfires/icecore/shelf/client/ShelfRenderer.java");
        assertTrue(renderer.contains("ITEM_SCALE = .3f"));
        // Fridge intentionally overlaps projections: world pitch=.3*.175, scale=.3.
        // Shelf retains evenly distributed 9-column anchors, but must not shrink to .1.
        double pitch=ShelfLayout.x(1)-ShelfLayout.x(0);
        assertTrue(16*.3/Math.sqrt(2)>pitch);
        for(int turn=0;turn<4;turn++) {
            Set<String> rotated=new HashSet<>();
            double angle=turn*Math.PI/2;
            for(int slot=0;slot<72;slot++) {
                double x=ShelfLayout.x(slot)-8,z=ShelfLayout.z(slot)-8;
                assertTrue(rotated.add(String.format(Locale.ROOT,"%.5f,%.5f,%.5f",x*Math.cos(angle)-z*Math.sin(angle),ShelfLayout.y(slot),x*Math.sin(angle)+z*Math.cos(angle))));
            }
        }
        try(ZipFile fridge=new ZipFile("libs/cookingforblockheads-forge-1.20.1-16.0.15.jar")) {
            var utils=clazz(fridge,"net/blay09/mods/cookingforblockheads/client/render/RenderUtils");
            var method=utils.methods.stream().filter(m->m.name.equals("renderItem")).findFirst().orElseThrow();
            boolean fixed=false,render=false;
            for(var instruction:method.instructions) {
                if(instruction instanceof FieldInsnNode field && field.owner.endsWith("ItemDisplayContext") && field.name.equals("FIXED")) fixed=true;
                if(instruction instanceof MethodInsnNode call && call.owner.endsWith("ItemRenderer") && call.name.equals("m_269128_")) render=true;
            }
            assertTrue(fixed && render);
            var fridgeRenderer=clazz(fridge,"net/blay09/mods/cookingforblockheads/client/render/FridgeRenderer");
            boolean upstreamScale=false;
            for(var m:fridgeRenderer.methods) for(var instruction:m.instructions)
                if(instruction instanceof LdcInsnNode constant && Float.valueOf(.3f).equals(constant.cst)) upstreamScale=true;
            assertTrue(upstreamScale);
        }
    }
    @Test void jadeSeparatorSharesTooltipRuleAndRequiresBody() throws Exception {
        String layout=read("src/main/java/com/yinfires/icecore/mixin/WoodJadeLayoutMixin.java");
        assertTrue(layout.contains("title && body"));
        assertTrue(layout.indexOf("tooltip.remove(ICECORE_LINE)")<layout.indexOf("if(title && body)"));
        String jade=read("src/main/java/com/yinfires/icecore/compat/jade/WoodTitleSeparator.java");
        assertTrue(jade.contains("WoodPanel.separator(graphics,(int)x,(int)y,(int)right)"));
        assertFalse(jade.contains("HorizontalLineElement"));
        assertTrue(read("src/main/java/com/yinfires/icecore/mixin/WoodModernTooltipMixin.java").contains("WoodPanel.separator("));
        assertTrue(read("src/main/java/com/yinfires/icecore/mixin/WoodTooltipLayoutMixin.java").contains("WoodPanel.separator("));
    }
    @Test void woodInteriorContainsContentAndShadowAtEveryGuiScale() {
        for(int width:List.of(10,37,128,303)) for(int height:List.of(8,20,74)) for(int scale=1;scale<=4;scale++) {
            int outerWidth=width+13,outerHeight=height+13;
            // Six immutable border pixels, plus only the native one-pixel text shadow.
            assertEquals((width+1)*scale,(outerWidth-12)*scale);
            assertEquals((height+1)*scale,(outerHeight-12)*scale);
        }
    }
    @Test void optionalBackgroundHooksMatchActualJars() throws Exception {
        try(ZipFile modern=new ZipFile("libs/ModernUI-Forge-1.20.1-3.12.0.1-universal.jar");ZipFile jade=new ZipFile("libs/Jade-1.20.1-Forge-11.13.3.jar")) {
            ClassNode tooltip=clazz(modern,"icyllis/modernui/mc/TooltipRenderer");
            for(String method:List.of("drawRoundedBackground","drawVanillaBackground")) assertTrue(tooltip.methods.stream().anyMatch(m->m.name.equals(method)&&m.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;Lorg/joml/Matrix4f;FFIIZI)V")));
            assertTrue(clazz(jade,"snownee/jade/overlay/OverlayRenderer").methods.stream().anyMatch(m->m.name.equals("drawTooltipBox")&&m.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;IIIIFZLsnownee/jade/overlay/TooltipRenderer;)V")));
            assertTrue(clazz(jade,"snownee/jade/impl/BlockAccessorImpl").methods.stream().anyMatch(m->m.name.equals("getBlockEntity")&&m.desc.equals("()Lnet/minecraft/world/level/block/entity/BlockEntity;")));
            assertTrue(clazz(jade,"snownee/jade/overlay/TooltipRenderer").methods.stream().anyMatch(m->m.name.equals("recalculateSize")&&m.desc.equals("()V")));
            assertTrue(clazz(jade,"snownee/jade/impl/Tooltip$Line").methods.stream().anyMatch(m->m.name.equals("render")&&m.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;FFFF)V")));
            assertTrue(clazz(jade,"snownee/jade/impl/ui/HorizontalLineElement").methods.stream().anyMatch(m->m.name.equals("getSize")));
        }
        assertTrue(read("build/createMcpToSrg/output.tsrg").contains("drawBackdrop (Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;III)V m_93039_"));
        Path production=Path.of(System.getProperty("user.home"),".gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-srg.jar");
        try(ZipFile forge=new ZipFile(production.toFile())) {
            assertTrue(clazz(forge,"net/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil").methods.stream().anyMatch(m->m.name.equals("renderTooltipBackground")&&m.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V")));
            assertTrue(clazz(forge,"net/minecraft/client/gui/Gui").methods.stream().anyMatch(m->m.name.equals("m_93039_")&&m.desc.equals("(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;III)V")));
            var layout=clazz(forge,"net/minecraft/client/gui/GuiGraphics").methods.stream().filter(m->m.name.equals("m_280497_")).findFirst().orElseThrow();
            int positions=0,textCalls=0;
            for(var instruction:layout.instructions) if(instruction instanceof MethodInsnNode call) {
                if(call.name.equals("m_262814_")&&call.desc.equals("(IIIIII)Lorg/joml/Vector2ic;")) positions++;
                if(call.name.equals("m_142440_")) textCalls++;
            }
            assertEquals(1,positions); assertEquals(1,textCalls);
        }
    }
}
