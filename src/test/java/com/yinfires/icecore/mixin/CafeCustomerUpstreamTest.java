package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

/** Check production descriptors and direct members in both supported upstream releases. */
class CafeCustomerUpstreamTest {
    private ClassNode read(JarFile jar,String name)throws Exception{
        var node=new ClassNode();try(var in=jar.getInputStream(jar.getJarEntry(name+".class"))){new ClassReader(in).accept(node,0);}return node;
    }
    private Object value(AnnotationNode a,String key){for(int i=0;i<a.values.size();i+=2)if(a.values.get(i).equals(key))return a.values.get(i+1);return null;}
    private List<AnnotationNode> annotations(MethodNode m){List<AnnotationNode> list=new ArrayList<>();if(m.visibleAnnotations!=null)list.addAll(m.visibleAnnotations);if(m.invisibleAnnotations!=null)list.addAll(m.invisibleAnnotations);return list;}
    private ClassNode local(String name)throws Exception{
        var node=new ClassNode();try(var in=getClass().getClassLoader().getResourceAsStream(name+".class")){assertNotNull(in);new ClassReader(in).accept(node,0);}return node;
    }
    @Test void registeredCafeRedirectsNeverClaimTheSameCall()throws Exception{
        var config=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/icecore.mixins.json"))).getAsJsonObject();
        var claims=new HashMap<String,String>();
        for(String section:List.of("mixins","client"))for(var entry:config.getAsJsonArray(section)){
            String name=entry.getAsString();if(!name.startsWith("CozyCafe"))continue;
            var node=local("com/yinfires/icecore/mixin/"+name);
            var target=node.invisibleAnnotations.stream().filter(a->a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
            Object owners=value(target,"targets");if(!(owners instanceof List<?>))continue;
            for(var method:node.methods)for(var annotation:annotations(method)){
                if(!annotation.desc.endsWith("/Redirect;"))continue;
                Object atValue=value(annotation,"at");
                var points=atValue instanceof List<?> list?list:List.of(atValue);
                for(var owner:(List<?>)owners)for(var selector:(List<?>)value(annotation,"method"))for(var point:points){
                    var at=(AnnotationNode)point;
                    String key=owner+"."+selector+":"+value(at,"value")+":"+value(at,"target")+":"+value(at,"ordinal");
                    String previous=claims.putIfAbsent(key,name+"."+method.name);
                    assertNull(previous,"Conflicting redirects: "+previous+" and "+name+"."+method.name+" at "+key);
                }
            }
        }
        assertFalse(claims.isEmpty());
    }
    @Test void menuTooltipsKeepTheItemStackContext()throws Exception{
        for(String name:List.of("CozyCafeManagerScreenErrorMixin","CozyCafeNormalFoodTooltipMixin")){
            var node=local("com/yinfires/icecore/mixin/"+name);
            var method=node.methods.stream().filter(m->m.name.equals("icecore$itemTooltip")||m.name.equals("icecore$normal")).findFirst().orElseThrow();
            boolean stackTooltip=false;
            for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call
                    &&call.owner.equals("net/minecraft/client/gui/GuiGraphics")&&call.name.equals("renderTooltip")
                    &&call.desc.equals("(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))stackTooltip=true;
            assertTrue(stackTooltip,name+" must use the native stack tooltip, preserving Forge/Modern UI context");
            assertFalse(node.methods.stream().anyMatch(m->m.name.equals("icecore$sharedReputation")));
        }
    }
    @Test void customerNameplatesAreDeniedWithoutChangingEntityNames()throws Exception{
        var node=local("com/yinfires/icecore/compat/cozycafe/customer/client/CafeCustomerKeys$Events");
        var method=node.methods.stream().filter(m->m.name.equals("nameplate")).findFirst().orElseThrow();
        boolean customerOnly=false,deny=false,setsResult=false;
        for(var instruction:method.instructions){
            if(instruction instanceof TypeInsnNode type&&type.getOpcode()==org.objectweb.asm.Opcodes.INSTANCEOF
                    &&type.desc.equals("io/github/chakyl/cozycafe/entities/CustomerEntity"))customerOnly=true;
            if(instruction instanceof FieldInsnNode field&&field.name.equals("DENY"))deny=true;
            if(instruction instanceof MethodInsnNode call){
                if(call.name.equals("setResult"))setsResult=true;
                assertFalse(call.name.equals("setCustomName")||call.name.equals("setCustomNameVisible"));
            }
        }
        assertTrue(customerOnly&&deny&&setsResult);
    }
    @Test void allNewCustomerHooksMatchBothJars()throws Exception{
        String[] mixins={"CozyCafeCustomerManagerMixin","CozyCafeCustomerMenuMixin","CozyCafeCustomerIdentityMixin","CozyCafeCustomerRendererMixin","CozyCafeNativeScrollerMixin","CozyCafeCustomerItemMixin","CozyCafeNormalFoodTooltipMixin","CozyCafeManagerScreenErrorMixin"};
        for(String version:new String[]{"1.9","1.10"})try(var jar=new JarFile("libs/cozycafe-"+version+"-all.jar")){
            for(String name:mixins){
                var mixin=new ClassNode();try(var in=getClass().getClassLoader().getResourceAsStream("com/yinfires/icecore/mixin/"+name+".class")){assertNotNull(in);new ClassReader(in).accept(mixin,0);}
                AnnotationNode target=mixin.invisibleAnnotations.stream().filter(a->a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
                String owner=((List<String>)value(target,"targets")).get(0).replace('.','/');ClassNode upstream=read(jar,owner);
                for(MethodNode method:mixin.methods)for(AnnotationNode a:annotations(method)){
                    if(a.desc.endsWith("/Shadow;"))assertTrue(upstream.methods.stream().anyMatch(m->m.name.equals(method.name)&&m.desc.equals(method.desc)),version+" shadow "+method.name);
                    if(!a.desc.contains("/injection/"))continue;
                    Object selectors=value(a,"method");if(!(selectors instanceof List<?>))continue;
                    for(Object selector:(List<?>)selectors){
                        String signature=selector.toString();MethodNode original=upstream.methods.stream().filter(m->signature.equals(m.name+m.desc)).findFirst().orElseThrow(()->new AssertionError(version+" missing "+owner+"."+signature));
                        Object ats=value(a,"at");List<?> points=ats instanceof List<?> list?list:ats==null?List.of():List.of(ats);
                        for(Object point:points)if(point instanceof AnnotationNode at&&"INVOKE".equals(value(at,"value"))){
                            String invocation=(String)value(at,"target");boolean found=false;
                            for(var instruction:original.instructions)if(instruction instanceof MethodInsnNode call&&invocation.equals("L"+call.owner+";"+call.name+call.desc))found=true;
                            assertTrue(found,version+" missing call "+invocation+" in "+signature);
                        }
                        for(Object point:points)if(point instanceof AnnotationNode at&&"FIELD".equals(value(at,"value"))){
                            String targetField=(String)value(at,"target");boolean found=false;
                            for(var instruction:original.instructions)if(instruction instanceof FieldInsnNode field&&targetField.equals("L"+field.owner+";"+field.name+":"+field.desc)
                                    &&(value(at,"opcode")==null||value(at,"opcode").equals(field.getOpcode())))found=true;
                            assertTrue(found,version+" missing field access "+targetField+" in "+signature);
                        }
                    }
                }
                for(FieldNode field:mixin.fields)if(field.visibleAnnotations!=null||field.invisibleAnnotations!=null){
                    var annotations=new ArrayList<AnnotationNode>();if(field.visibleAnnotations!=null)annotations.addAll(field.visibleAnnotations);if(field.invisibleAnnotations!=null)annotations.addAll(field.invisibleAnnotations);
                    if(annotations.stream().anyMatch(a->a.desc.endsWith("/Shadow;")))assertTrue(upstream.fields.stream().anyMatch(f->f.name.equals(field.name)&&f.desc.equals(field.desc)),version+" missing field "+field.name);
                }
            }
        }
    }
    @Test void jadeUsesTheInheritedEntityNameWithoutAnExtraProvider()throws Exception{
        try(var jade=new JarFile("libs/Jade-1.20.1-Forge-11.13.3.jar")){
            var provider=read(jade,"snownee/jade/addon/core/ObjectNameProvider");
            var method=provider.methods.stream().filter(m->m.name.equals("getEntityName")).findFirst().orElseThrow();
            boolean callsName=false;for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/world/entity/Entity")&&call.name.equals("m_7755_")&&call.desc.equals("()Lnet/minecraft/network/chat/Component;"))callsName=true;
            assertTrue(callsName);
        }
        for(String version:new String[]{"1.9","1.10"})try(var jar=new JarFile("libs/cozycafe-"+version+"-all.jar")){
            assertFalse(read(jar,"io/github/chakyl/cozycafe/entities/CustomerEntity").methods.stream().anyMatch(m->m.name.equals("m_7755_")));
        }
    }
}
