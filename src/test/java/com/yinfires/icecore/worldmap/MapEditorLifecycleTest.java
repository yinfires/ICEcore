package com.yinfires.icecore.worldmap;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class MapEditorLifecycleTest {
    @Test void realXaeroLibInitializesEscapeButEmbeddedMapHasNoOwnerOrEscape() throws Exception {
        var upstream=new ClassNode();
        try(var jar=new JarFile("libs/xaerolib-forge-1.20.1-1.7.3.jar");var in=jar.getInputStream(jar.getEntry("xaero/lib/client/gui/ScreenBase.class"))){new ClassReader(in).accept(upstream,0);}
        boolean initializesEscape=false;
        for(var method:upstream.methods)if(method.name.equals("m_7856_")){
            boolean readsEscape=false;
            for(var instruction:method.instructions){
                if(instruction instanceof FieldInsnNode field&&field.name.equals("escape"))readsEscape=true;
                if(readsEscape&&instruction instanceof MethodInsnNode call&&call.name.equals("m_6575_")&&call.desc.equals("(Lnet/minecraft/client/Minecraft;II)V"))initializesEscape=true;
            }
        }
        assertTrue(initializesEscape,"Pinned XaeroLib recursively initializes the escape screen");
        var embedded=new ClassNode();
        try(var in=Files.newInputStream(Path.of("build/classes/java/main/com/yinfires/icecore/worldmap/client/XaeroEditorMap.class"))){new ClassReader(in).accept(embedded,0);}
        var constructor=embedded.methods.stream().filter(m->m.name.equals("<init>")).findFirst().orElseThrow();
        assertEquals("()V",constructor.desc,"Embedded map must not receive an owning Screen");
        AbstractInsnNode creation=null;
        for(var instruction:constructor.instructions)if(instruction instanceof TypeInsnNode type&&type.getOpcode()==Opcodes.NEW&&type.desc.equals("xaero/map/gui/GuiMap"))creation=type;
        assertNotNull(creation);
        var next=nextCode(creation);assertEquals(Opcodes.DUP,next.getOpcode());
        next=nextCode(next);assertEquals(Opcodes.ACONST_NULL,next.getOpcode(),"No parent screen");
        next=nextCode(next);assertEquals(Opcodes.ACONST_NULL,next.getOpcode(),"No escape screen: prevents first-open recursive widget registration");
    }
    private static AbstractInsnNode nextCode(AbstractInsnNode node){do{node=node.getNext();}while(node!=null&&node.getOpcode()<0);return node;}
    @Test void directionalPanHasSameSpeedAcrossFrameRatesAndDiagonalDirections(){
        double a=0,b=0;for(int i=0;i<60;i++)a+=MapKeyboardPan.pixels(1,0,1.0/60);for(int i=0;i<144;i++)b+=MapKeyboardPan.pixels(1,0,1.0/144);
        assertEquals(240,a,1e-8);assertEquals(a,b,1e-8);
        assertEquals(240*.02,Math.hypot(MapKeyboardPan.pixels(1,1,.02),MapKeyboardPan.pixels(1,1,.02)),1e-8);
        assertEquals(-MapKeyboardPan.pixels(1,0,.02),MapKeyboardPan.pixels(-1,0,.02),1e-8);
    }
    @Test void zeroAxisAndWindowPauseDoNotCauseCameraJump(){
        assertEquals(0,MapKeyboardPan.pixels(0,1,.02));assertEquals(0,MapKeyboardPan.pixels(1,0,-1));
        assertEquals(0,MapKeyboardPan.pixels(1,0,Double.NaN));assertEquals(12,MapKeyboardPan.pixels(1,0,10));
    }
}
