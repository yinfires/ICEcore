package com.yinfires.icecore.mixin;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;
class GrillingRenderCacheContractTest {
 private ClassNode upstream(ZipFile zip,String path) throws Exception {
  ClassNode node=new ClassNode();new ClassReader(zip.getInputStream(zip.getEntry(path+".class"))).accept(node,0);return node;
 }
 @Test void upstreamQueryIsItemOnlyAndDataGenerationsReplaceMaps() throws Exception {
  try(var zip=new ZipFile("libs/kaleidoscope_grilling-1.1.1-forge1.20.1.jar")) {
   String root="cn/breezeth/kaleidoscope_grilling/";
   var recipes=upstream(zip,root+"skewer/SkewerRecipes");
   var query=recipes.methods.stream().filter(m->m.name.equals("usesGeneratedModel")).findFirst().orElseThrow();
   assertEquals("(Lnet/minecraft/world/item/ItemStack;)Z",query.desc);
   for(var insn:query.instructions) if(insn instanceof MethodInsnNode call && call.owner.equals("net/minecraft/world/item/ItemStack")) assertEquals("m_41720_",call.name);
   var data=upstream(zip,root+"data/GrillingDataManager");
   for(String name:new String[]{"SKEWERS","SCRIPT_SKEWERS"}) {
    var f=data.fields.stream().filter(v->v.name.equals(name)).findFirst().orElseThrow();assertEquals("Ljava/util/Map;",f.desc);assertNotEquals(0,f.access&Opcodes.ACC_VOLATILE);
   }
   for(String name:new String[]{"apply","commitScriptThreadingRecipes","replaceSyncedSkewerRecipes"}) {
    var method=data.methods.stream().filter(m->m.name.equals(name)&&!m.desc.startsWith("(Ljava/lang/Object;")).findFirst().orElseThrow();
    assertTrue(java.util.stream.StreamSupport.stream(method.instructions.spliterator(),false).anyMatch(i->i instanceof FieldInsnNode f && f.getOpcode()==Opcodes.PUTSTATIC && (f.name.equals("SKEWERS")||f.name.equals("SCRIPT_SKEWERS"))),name);
   }
  }
 }
 @Test void optionalTargetsAndSelectorsStayProductionSafe() throws Exception {
  for(String name:new String[]{"GrillingSkewerDisplayMapMixin","GrillingSkewerModelQueryMixin"}) {
   assertFalse(new ICECoreMixinPlugin().shouldApplyMixin("missing.Target","com.yinfires.icecore.mixin."+name));
   var node=new ClassNode();new ClassReader(Files.readAllBytes(Path.of("build/classes/java/main/com/yinfires/icecore/mixin/"+name+".class"))).accept(node,0);
   assertTrue(Files.readString(Path.of("src/main/resources/icecore.mixins.json")).contains("\""+name+"\""));
   try(var zip=new ZipFile("libs/kaleidoscope_grilling-1.1.1-forge1.20.1.jar")) {
    String target=name.contains("DisplayMap")?"cn/breezeth/kaleidoscope_grilling/data/GrillingDataManager":"cn/breezeth/kaleidoscope_grilling/skewer/SkewerRecipes";
    var actual=upstream(zip,target);
    for(var method:node.methods) if(method.visibleAnnotations!=null) for(var annotation:method.visibleAnnotations) {
     if(!annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) continue;
     java.util.Map<String,Object> attrs=new java.util.HashMap<>();
     for(int i=0;i<annotation.values.size();i+=2) attrs.put((String)annotation.values.get(i),annotation.values.get(i+1));
     assertEquals(Boolean.FALSE,attrs.get("remap"));
     for(Object selector:(java.util.List<?>)attrs.get("method"))
      assertTrue(actual.methods.stream().anyMatch(m->(m.name+m.desc).equals(selector)),selector.toString());
    }
    for(var field:node.fields) if(field.visibleAnnotations!=null && field.visibleAnnotations.stream().anyMatch(a->a.desc.equals("Lorg/spongepowered/asm/mixin/Shadow;")))
     assertTrue(actual.fields.stream().anyMatch(f->f.name.equals(field.name)&&f.desc.equals(field.desc)),field.name);
   }
  }
 }
}
