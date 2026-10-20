package com.yinfires.icecore.shelf.client;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.nio.file.*;
import java.util.jar.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

final class RackUpstreamTest {
    private static ClassNode read(byte[] bytes) {
        var node=new ClassNode();new ClassReader(bytes).accept(node,0);return node;
    }
    private static MethodNode method(ClassNode node,String name) {
        return node.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();
    }
    @Test void grillingContextRequiresPreservingRenderStaticEntry() throws Exception {
        try(var zip=new ZipFile("libs/kaleidoscope_grilling-1.1.1-forge1.20.1.jar")) {
            var node=read(zip.getInputStream(zip.getEntry("cn/breezeth/kaleidoscope_grilling/mixin/ItemRendererContextMixin.class")).readAllBytes());
            boolean push=false,pop=false;
            for(var m:node.methods) for(var i:m.instructions) if(i instanceof MethodInsnNode call && call.owner.endsWith("SkewerItemRenderContext")) {
                push|=call.name.equals("push");pop|=call.name.equals("pop");
            }
            assertTrue(push);assertTrue(pop);
        }
        String renderer=Files.readString(Path.of("src/main/java/com/yinfires/icecore/shelf/client/ShelfRenderer.java"));
        assertTrue(renderer.contains("if(STATIC_CONTEXT ||"));
        assertTrue(renderer.contains("RenderUtils.renderItem(stack,light,pose,buffers,shelf.getLevel())"));
    }
    @Test void actualRackRendererUsesOnlyTheInterceptedItemEntryAndFixedContext() throws Exception {
        try(var zip=new ZipFile("libs/youkaisfeasts-0.5.0.jar")) {
            var node=read(zip.getInputStream(zip.getEntry("dev/xkmc/youkaishomecoming/content/pot/storage/ingredient/IngredientRackRenderer.class")).readAllBytes());
            var render=node.methods.stream().filter(m->m.name.equals("render") && m.desc.contains("IngredientRackBlockEntity;")).findFirst().orElseThrow();
            int calls=0;boolean fixed=false;
            for(var insn:render.instructions) {
                if(insn instanceof MethodInsnNode call && call.owner.equals("net/minecraft/client/renderer/entity/ItemRenderer")) {
                    calls++;assertEquals("m_269128_",call.name);
                    assertEquals("(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;IILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;I)V",call.desc);
                }
                if(insn instanceof FieldInsnNode f && f.name.equals("FIXED")) fixed=true;
            }
            assertEquals(1,calls);assertTrue(fixed);
            var entity=read(zip.getInputStream(zip.getEntry("dev/xkmc/youkaishomecoming/content/pot/storage/ingredient/IngredientRackBlockEntity.class")).readAllBytes());
            assertEquals("()V",method(entity,"notifyTile").desc);
            String nested=zip.stream().map(e->e.getName()).filter(n->n.contains("l2library-") && n.endsWith(".jar")).findFirst().orElseThrow();
            try(var jar=new JarInputStream(zip.getInputStream(zip.getEntry(nested)))) {
                boolean found=false;
                for(JarEntry entry;(entry=jar.getNextJarEntry())!=null;) if(entry.getName().equals("dev/xkmc/l2library/base/tile/BaseBlockEntity.class")) {
                    var base=read(jar.readAllBytes());
                    assertEquals("(Lnet/minecraft/nbt/CompoundTag;)V",method(base,"m_142466_").desc);found=true;break;
                }
                assertTrue(found);
            }
        }
        String mixins=Files.readString(Path.of("src/main/resources/icecore.mixins.json"));
        for(String name:new String[]{"DisplayItemColorsAccessor","RackDisplayRendererMixin","RackDisplayLoadMixin","RackDisplayChangeMixin"}) assertTrue(mixins.contains(name));
    }
}
