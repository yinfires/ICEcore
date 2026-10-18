package com.yinfires.icecore.tooltip;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class TeapotContentsTooltipTest {
    @Test void overridesAndHideSwitchPreserveFluidAndFinishedTeaWithoutRestoringFlavor() {
        for (boolean hide : new boolean[]{false, true}) {
            for (List<Component> replacement : Arrays.<List<Component>>asList(null, List.of(), List.of(Component.literal("description")))) {
                Component name = Component.literal("teapot");
                Component nativeTea = Component.literal("tea").append(Component.literal(" x4"));
                Component content = PreservedTooltipContents.wrap(nativeTea);
                var lines = new ArrayList<>(List.of(name, Component.literal("flavor"), content));
                TooltipRules.applyOriginal(lines, replacement, hide, true);
                assertSame(name, lines.get(0));
                assertSame(content, lines.get(lines.size()-1));
                assertEquals(nativeTea.getString(), content.copy().getString());
                if (hide || replacement != null) assertFalse(lines.stream().anyMatch(c -> c.getString().equals("flavor")));
                lines.add(Component.literal("price"));
                TooltipRules.removeFoodEffects(lines);
                assertTrue(lines.contains(content));
                assertEquals("price", lines.get(lines.size()-1).getString());
            }
        }
    }
    @Test void bothActualCookeryVersionsExposeExactlyTwoNativeContentsSites() throws Exception {
        for (String file : List.of("libs/[森罗物语：厨房] kaleidoscopecookery-1.4.1-forge+mc1.20.1.jar",
                "libs/kaleidoscopecookery-1.5.1-forge+mc1.20.1.jar")) {
            try (var jar = new ZipFile(file)) {
                var node = new ClassNode();
                try (var stream = jar.getInputStream(jar.getEntry("com/github/ysbbbbbb/kaleidoscopecookery/item/TeapotItem.class"))) {
                    new ClassReader(stream).accept(node, 0);
                }
                var method = node.methods.stream().filter(m -> m.name.equals("m_7373_") && m.desc.equals(
                    "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V"))
                    .findFirst().orElseThrow();
                int additions = 0;
                Set<String> nbt = new HashSet<>();
                for (var instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call && call.owner.equals("java/util/List") && call.name.equals("add")) additions++;
                    if (instruction instanceof LdcInsnNode ldc && ldc.cst instanceof String text) nbt.add(text);
                }
                assertEquals(2, additions);
                assertTrue(nbt.containsAll(List.of("Status", "Result", "TeaFluidId")));
            }
        }
        String mixins = Files.readString(Path.of("src/main/resources/icecore.mixins.json"));
        assertTrue(mixins.indexOf("TeapotContentsTooltipMixin") > mixins.indexOf("\"client\""));
        assertTrue(Files.readString(Path.of("build/createMcpToSrg/output.tsrg")).contains(
            "appendHoverText (Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V m_7373_"));
    }
}
