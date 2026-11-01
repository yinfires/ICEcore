package com.yinfires.icecore.worldmap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import java.util.*;
import java.nio.file.*;
import java.util.jar.JarFile;
import org.objectweb.asm.*;

class MapIntegrationTest {
    @Test void snapshotRoundTripPreservesSharedUnlocksRevisionAndEditorPermission(){
        var packet=new MapNetwork.Snapshot(123,MapDefinition.JSON.toJson(new MapDefinition()),Set.of("first","tiny"),false,false,List.of());
        var buffer=new FriendlyByteBuf(Unpooled.buffer());try{packet.encode(buffer);var read=MapNetwork.Snapshot.decode(buffer);assertEquals(packet,read);assertFalse(read.operator());assertFalse(read.openEditor());}finally{buffer.release();}
    }
    @Test void editorRequestPreservesStaleRevisionForServerValidation(){
        var packet=new MapNetwork.Request("save","","{\"regions\":[]}",12);var buffer=new FriendlyByteBuf(Unpooled.buffer());try{packet.encode(buffer);assertEquals(packet,MapNetwork.Request.decode(buffer));}finally{buffer.release();}
    }
    @Test void excessiveSnapshotCountsAreRejectedBeforeAllocation(){var b=new FriendlyByteBuf(Unpooled.buffer());try{b.writeLong(1);b.writeUtf("{}");b.writeVarInt(Integer.MAX_VALUE);assertThrows(IllegalArgumentException.class,()->MapNetwork.Snapshot.decode(b));}finally{b.release();}}
    @Test void xaeroNativeExportApiAndEmbeddedLibraryMatchPinnedJar() throws Exception {
        try(var jar=new JarFile("libs/xaeroworldmap-forge-1.20.1-1.47.0.jar")){
            assertNotNull(jar.getEntry("META-INF/jarjar/xaerolib-forge-1.20.1-1.7.3.jar"));
            Set<String> methods=new HashSet<>();try(var in=jar.getInputStream(jar.getEntry("xaero/map/file/export/PNGExporter.class"))){new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9){@Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){methods.add(name+desc);return null;}},ClassReader.SKIP_CODE);}
            assertTrue(methods.contains("export(Lxaero/map/MapProcessor;Lnet/minecraft/core/Registry;Lnet/minecraft/core/Registry;Lxaero/map/gui/MapTileSelection;Lxaero/map/file/OldFormatSupport;)Lxaero/map/file/export/PNGExportResult;"));
        }
    }
    @Test void finalFogImplementationProducesSoftIslandPreview() throws Exception {
        var d=MapRulesTest.map();d.regions.add(MapRulesTest.square("main",-180,-140,180));var tiny=MapRulesTest.square("tiny",100,-20,8);d.regions.add(tiny);d.regions.add(MapRulesTest.square("locked",110,100,40));
        var fog=MapFog.build(d,Set.of("main","tiny"));var image=new java.awt.image.BufferedImage(768,768,java.awt.image.BufferedImage.TYPE_INT_RGB);
        for(int z=0;z<768;z++)for(int x=0;x<768;x++){double wx=x-384,wz=z-384;int a=fog.alpha(wx,wz);boolean land=d.regions.stream().anyMatch(r->r.contains(wx,wz));int color=land?0x648342:0x365c82;int r=((color>>16&255)*(255-a)+128*a)/255,g=((color>>8&255)*(255-a)+128*a)/255,b=((color&255)*(255-a)+128*a)/255;image.setRGB(x,z,r<<16|g<<8|b);}
        Path file=Path.of("tmp/map-inspect/fog-preview.png");Files.createDirectories(file.getParent());javax.imageio.ImageIO.write(image,"png",file.toFile());
        assertEquals(0x808080,image.getRGB(514,504)&0xffffff);assertTrue(fog.alpha(-170,-50)>0&&fog.alpha(-170,-50)<255);
    }
}
