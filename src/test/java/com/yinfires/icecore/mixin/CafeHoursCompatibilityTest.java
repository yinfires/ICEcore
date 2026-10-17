package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class CafeHoursCompatibilityTest {
    private ClassNode upstream(String jarName, String name) throws Exception {
        ClassNode node = new ClassNode();
        try (JarFile jar = new JarFile("libs/" + jarName); var input = jar.getInputStream(jar.getJarEntry(name + ".class"))) {
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }
    private String source(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/" + name + ".java"));
    }
    @Test void declarationCallbackMatchesRealJoinMessageProducer() throws Exception {
        var type = upstream("yuushya-1.20.1-forge-2.3.0.jar", "com/yuushya/Yuushya");
        var method = type.methods.stream().filter(m -> m.name.equals("lambda$init$4")).findFirst().orElseThrow();
        assertTrue(source("YuushyaJoinMessageMixin").contains(method.name + method.desc));
        boolean sends = false;
        for (var i : method.instructions) if (i instanceof MethodInsnNode call && call.name.equals("m_213846_")) sends = true;
        assertTrue(sends);
    }
    @Test void placeableSettingIsExactlySecondSupplierRead() throws Exception {
        var type = upstream("supplementaries-1.20-3.1.43-forge.jar", "net/mehvahdjukaar/supplementaries/common/events/ClientEvents");
        var method = type.methods.stream().filter(m -> m.name.equals("onItemTooltip")).findFirst().orElseThrow();
        assertTrue(source("SupplementariesPlaceableTooltipMixin").contains(method.name + method.desc));
        int reads = 0; String field = null;
        for (var i : method.instructions) {
            if (i instanceof FieldInsnNode f && f.owner.endsWith("ClientConfigs$General")) field = f.name;
            if (i instanceof MethodInsnNode m && m.owner.equals("java/util/function/Supplier") && m.name.equals("get")) {
                if (reads++ == 1) { assertEquals("PLACEABLE_TOOLTIP", field); break; }
            }
        }
        assertEquals(2, reads);
        assertTrue(source("SupplementariesPlaceableTooltipMixin").contains("ordinal = 1"));
        assertTrue(source("SupplementariesPlaceableTooltipMixin").contains("Boolean.FALSE : setting.get()"));
    }
    @Test void managerAndFinalizationHooksMatchRealDescriptors() throws Exception {
        var type = upstream("cozycafe-1.10-all.jar", "io/github/chakyl/cozycafe/blockentities/CafeManagerBlockEntity");
        String code = source("CozyCafeHoursMixin");
        for (String name : new String[]{"tick", "setOpen", "canBeOpened", "assignCustomersInArea"}) {
            var m = type.methods.stream().filter(x -> x.name.equals(name)).findFirst().orElseThrow();
            assertTrue(code.contains(m.name + m.desc));
        }
        assertTrue(code.contains("manager.setOpen(false, false)"));
        assertFalse(code.contains("sendCloseCommandToMenus"));
        var utils = upstream("cozycafe-1.10-all.jar", "io/github/chakyl/cozycafe/util/CustomerEntityUtils");
        for (String name : new String[]{"spawnCustomerAndTarget", "finalizeSpawn"}) {
            var m = utils.methods.stream().filter(x -> x.name.equals(name)).findFirst().orElseThrow();
            assertTrue(source("CozyCafePendingSpawnMixin").contains(m.name + m.desc));
        }
        String spawn = source("CozyCafePendingSpawnMixin");
        assertTrue(spawn.contains("type != CustomerTarget.MENU"));
        assertTrue(spawn.contains("icecore$admissionGeneration() == pending.icecore$capturedGeneration()"));
        assertTrue(spawn.contains("customer.getUUID().equals"));
        assertTrue(spawn.contains("CozyCafeSeatingService.release(menu)"));
        String seating = Files.readString(Path.of("src/main/java/com/yinfires/icecore/compat/cozycafe/seating/CozyCafeSeatingService.java"));
        assertTrue(seating.contains("customer == null && \"pending_spawn\".equals"));
        assertTrue(seating.contains("!manager.isOpen() || com.yinfires.icecore.compat.cozycafe.CozyCafeHours.isClosed"));
    }
    @Test void optionalTargetsAndClientRegistrationAreGated() throws Exception {
        String plugin = source("ICECoreMixinPlugin");
        assertTrue(plugin.contains("YuushyaJoinMessage"));
        assertTrue(plugin.contains("SupplementariesPlaceableTooltip"));
        assertFalse(plugin.contains("Class.forName"));
        String config = Files.readString(Path.of("src/main/resources/icecore.mixins.json"));
        assertTrue(config.indexOf("SupplementariesPlaceableTooltipMixin") > config.indexOf("\"client\""));
        assertTrue(config.indexOf("YuushyaJoinMessageMixin") < config.indexOf("\"client\""));
    }

    @Test void modernUiInterceptsNativePreAndUsesProvidedPositioner() throws Exception {
        var forge = upstream("ModernUI-Forge-1.20.1-3.12.0.1-universal.jar", "icyllis/modernui/mc/forge/UIManagerForge");
        var method = forge.methods.stream().filter(m -> m.name.equals("onRenderTooltipH")).findFirst().orElseThrow();
        assertEquals("(Lnet/minecraftforge/client/event/RenderTooltipEvent$Pre;)V", method.desc);
        assertTrue(java.util.stream.StreamSupport.stream(method.instructions.spliterator(), false)
                .anyMatch(i -> i instanceof MethodInsnNode call && call.name.equals("getTooltipPositioner")));
        var renderer = upstream("ModernUI-Forge-1.20.1-3.12.0.1-universal.jar", "icyllis/modernui/mc/TooltipRenderer");
        var draw = renderer.methods.stream().filter(m -> m.name.equals("drawTooltip")).findFirst().orElseThrow();
        assertTrue(java.util.stream.StreamSupport.stream(draw.instructions.spliterator(), false)
                .anyMatch(i -> i instanceof MethodInsnNode call && call.owner.endsWith("ClientTooltipPositioner")
                        && call.name.equals("m_262814_")));
    }

    @Test void computerPageFailuresUseNativePacketAndNeverPlayerOverlay() throws Exception {
        var screen = upstream("cozycafe-1.10-all.jar", "io/github/chakyl/cozycafe/gui/CafeManagerScreen");
        var setter = screen.methods.stream().filter(m -> m.name.equals("setErrorMessage")).findFirst().orElseThrow();
        assertTrue(source("CozyCafeManagerScreenErrorMixin").contains(setter.name + setter.desc));
        var table = java.util.stream.StreamSupport.stream(setter.instructions.spliterator(), false)
                .filter(i -> i instanceof TableSwitchInsnNode).map(i -> (TableSwitchInsnNode) i).findFirst().orElseThrow();
        assertEquals(4, table.max, "New ICEcore code 6 must not collide with upstream reasons");
        var packet = upstream("cozycafe-1.10-all.jar", "io/github/chakyl/cozycafe/network/ClientBoundCafeCannotOpenPacket");
        assertTrue(packet.methods.stream().anyMatch(m -> m.name.equals("<init>") && m.desc.equals("(Ljava/lang/Byte;)V")));
        assertTrue(packet.methods.stream().anyMatch(m -> java.util.stream.StreamSupport.stream(m.instructions.spliterator(), false)
                .anyMatch(i -> i instanceof MethodInsnNode call && call.owner.equals(screen.name)
                        && call.name.equals(setter.name) && call.desc.equals(setter.desc))));
        String hours = source("CozyCafeHoursMixin");
        assertTrue(hours.contains("new io.github.chakyl.cozycafe.network.ClientBoundCafeCannotOpenPacket((byte) 6)"));
        String mapping = source("CozyCafeManagerScreenErrorMixin");
        assertTrue(mapping.contains("code.byteValue() == 6"));
        assertTrue(mapping.contains("icecore.cozycafe.closed_hours"));
        assertTrue(mapping.contains("errorDisplayTicks = 120"));
        var render = screen.methods.stream().filter(m -> m.name.equals("m_88315_")).findFirst().orElseThrow();
        assertTrue(mapping.contains(render.name + render.desc));
        assertTrue(java.util.stream.StreamSupport.stream(render.instructions.spliterator(), false)
                .anyMatch(i -> i instanceof MethodInsnNode call && call.name.equals("m_280666_")
                        && call.desc.equals("(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V")));
        var wrapper = screen.methods.stream().filter(m -> m.name.equals("wrapErrorText")).findFirst().orElseThrow();
        assertTrue(mapping.contains(wrapper.name + wrapper.desc));
        assertTrue(screen.fields.stream().anyMatch(f -> f.name.equals("toggleOpenButton")
                && f.desc.equals("Lnet/minecraft/client/gui/components/ImageButton;")));
        assertTrue(mapping.contains("toggleOpenButton.getX() + toggleOpenButton.getWidth() / 2"));
        assertTrue(mapping.contains("icecore$positioner, center, toggleOpenButton.getY()"));
        assertTrue(mapping.contains("System.nanoTime() - icecore$errorStarted"));
        assertTrue(mapping.indexOf("icecore$errorStarted = System.nanoTime()") < mapping.indexOf("code.byteValue() == 5"),
                "Every original or extended message restarts its lifetime");
        assertTrue(mapping.contains("icecore$errorLines = List.of()"));
        assertTrue(mapping.contains("errorMessage = null"));
        assertTrue(mapping.contains("graphics.renderTooltip(font, icecore$errorLines, icecore$positioner"));
        assertTrue(mapping.contains("icecore$wrappedMessage = wrapErrorText(font, message)"));
        assertTrue(mapping.contains("map(Component::getVisualOrderText)"));
        assertFalse(mapping.contains("TooltipRenderUtil"));
        assertFalse(mapping.contains("drawString("));
        assertFalse(mapping.contains("0xF0100010"));
        assertTrue(mapping.contains("destroyBuffers()"));
        assertTrue(mapping.contains("main.bindWrite(false)"));

        for (String file : new String[]{"CozyCafeHoursMixin", "CozyCafeManagerSeatingMixin", "CozyCafeManagerScreenErrorMixin",
                "CozyCafeManagerNameMixin", "CozyCafeClearButtonMixin"}) {
            String code = source(file);
            for (String forbidden : new String[]{"PlayerFeedback", "displayClientMessage", "sendSystemMessage",
                    "setOverlayMessage", "ClientboundSetTitles", "setTitle", "setSubtitle"}) {
                assertFalse(code.contains(forbidden), file + " must use cafe page feedback, not " + forbidden);
            }
        }
        assertTrue(Files.readString(Path.of("AGENTS.md")).contains("CozyCafe 咖啡馆电脑页面内"));
    }
}
