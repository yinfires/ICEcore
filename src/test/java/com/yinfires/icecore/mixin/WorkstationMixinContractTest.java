package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

final class WorkstationMixinContractTest {
    @Test void everyProductionSelectorAndShadowExistsInActualTarget() throws Exception {
        Map<String, ClassNode> targets = new HashMap<>();
        List<Path> jars = new ArrayList<>();
        try (var paths = Files.list(Path.of("libs"))) { paths.filter(p -> p.toString().endsWith(".jar")).forEach(jars::add); }
        for (Path path : jars) try (ZipFile jar = new ZipFile(path.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                ClassNode node = new ClassNode(); new ClassReader(jar.getInputStream(entry)).accept(node, 0);
                targets.put(node.name, node);
            }
        }
        int checked = 0;
        try (var files = Files.list(Path.of("build/classes/java/main/com/yinfires/icecore/mixin"))) {
            for (Path path : files.filter(p -> p.getFileName().toString().startsWith("Workstation") && p.toString().endsWith(".class")).toList()) {
                ClassNode mixin = new ClassNode(); new ClassReader(Files.readAllBytes(path)).accept(mixin, 0);
                AnnotationNode annotation = annotations(mixin.visibleAnnotations, mixin.invisibleAnnotations).stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
                List<String> names = (List<String>)value(annotation, "targets");
                if (names == null) continue; // Minecraft targets are covered separately by mapping checks.
                for (String name : names) {
                    ClassNode target = targets.get(name.replace('.', '/')); assertNotNull(target, name);
                    for (FieldNode field : mixin.fields) if (annotations(field.visibleAnnotations, field.invisibleAnnotations).stream().anyMatch(a -> a.desc.endsWith("/Shadow;")))
                        assertTrue(target.fields.stream().anyMatch(f -> f.name.equals(field.name) && f.desc.equals(field.desc)), name + " shadow " + field.name);
                    for (MethodNode method : mixin.methods) for (AnnotationNode injection : annotations(method.visibleAnnotations, method.invisibleAnnotations)) {
                        if (!injection.desc.contains("/injection/")) continue;
                        Object selection = value(injection, "method"); if (!(selection instanceof List<?> selectors)) continue;
                        for (Object raw : selectors) {
                            String selector = raw.toString(); int bracket = selector.indexOf('('); assertTrue(bracket > 0, selector);
                            assertTrue(target.methods.stream().anyMatch(m -> m.name.equals(selector.substring(0, bracket)) && m.desc.equals(selector.substring(bracket))), name + " " + selector); checked++;
                            MethodNode selected = target.methods.stream().filter(m -> m.name.equals(selector.substring(0, bracket)) && m.desc.equals(selector.substring(bracket))).findFirst().orElseThrow();
                            Object locations = value(injection, "at");
                            if (locations instanceof List<?> points) for (Object point : points) {
                                String invoked = (String)value((AnnotationNode)point, "target");
                                if (invoked == null) continue;
                                assertTrue(java.util.stream.StreamSupport.stream(selected.instructions.spliterator(), false).anyMatch(i -> i instanceof MethodInsnNode call
                                    && invoked.equals("L" + call.owner + ";" + call.name + call.desc)), name + " missing invocation " + invoked);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(checked >= 18, "All registered optional selectors must be checked");
    }
    @Test void inputOnlyAndManualCookingBoundariesRemainExplicit() throws Exception {
        String cuisine = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/WorkstationCuisineMixin.java"));
        assertTrue(cuisine.contains("if (held.isEmpty()) return"));
        assertFalse(cuisine.contains("addToPlayer("));
        String steamer = Files.readString(Path.of("src/main/java/com/yinfires/icecore/workstation/SteamerControl.java"));
        assertTrue(steamer.contains("time[i] = 600"));
        String plugin = Files.readString(Path.of("src/main/java/com/yinfires/icecore/mixin/ICECoreMixinPlugin.java"));
        assertFalse(plugin.contains("Class.forName"));
    }
    @Test void standardStorageAndFeedbackBoundaries() throws Exception {
        Path root = Path.of("src/main/java/com/yinfires/icecore");
        String jade = Files.readString(root.resolve("compat/jade/WorkstationJadeProvider.java"));
        assertFalse(jade.contains("showItems"));
        assertFalse(jade.contains("helper.progress"));
        assertFalse(jade.contains("no_tea_base"));
        assertTrue(jade.contains("data.remove(\"Capacity\")"));
        assertTrue(jade.contains("PotBlock.HAS_OIL"));
        String item = Files.readString(root.resolve("compat/jade/WorkstationItemStorage.java"));
        assertTrue(item.contains("ClientViewGroup.map(groups, ItemView::new, null)"));
        assertTrue(item.contains("else if (contents instanceof ItemStackHandler"));
        String fluid = Files.readString(root.resolve("compat/jade/WorkstationFluidStorage.java"));
        assertTrue(fluid.contains("FluidView::readDefault"));
        assertTrue(fluid.contains("FluidView.writeDefault"));
        assertFalse(fluid.contains("overrideText"));
        String transfer = Files.readString(root.resolve("workstation/WorkstationInteractionService.java"));
        assertFalse(transfer.contains("taken("));
        assertTrue(transfer.contains("need_container"));
        String start = Files.readString(root.resolve("mixin/WorkstationTeapotStartMixin.java"));
        assertTrue(start.contains("icecore$starting = false"));
        assertTrue(start.contains("self.hasHeatSource(level)"));
        assertTrue(start.contains("ExternalWorkstation.set(self, \"currentTick\", -1)"));
        String render = Files.readString(root.resolve("mixin/WorkstationStockpotRenderMixin.java"));
        assertTrue(render.contains("ItemRenderReplacer.getModel"));
        assertTrue(render.contains("icecore$needsCarrier"));
    }
    private static List<AnnotationNode> annotations(List<AnnotationNode> a, List<AnnotationNode> b) { List<AnnotationNode> result = new ArrayList<>(); if(a != null)result.addAll(a);if(b != null)result.addAll(b);return result; }
    private static Object value(AnnotationNode node, String key) { if(node.values != null)for(int i=0;i<node.values.size();i+=2)if(node.values.get(i).equals(key))return node.values.get(i+1);return null; }
}
