package com.yinfires.icecore.compat.fragrantorchard;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Repairs upstream client-only sound references before the dedicated server defines the class. */
public final class JuicerServerClassPatch {
    private JuicerServerClassPatch() {}

    public static void apply(ClassNode target) {
        if (!"com/bmt/kaleidoscope_fragrantorchard/block/entity/JuicerBlockEntity".equals(target.name)) {
            throw new IllegalArgumentException("Unexpected juicer target: " + target.name);
        }
        MethodNode removed = target.methods.stream()
                .filter(method -> method.name.equals("m_7651_") && method.desc.equals("()V"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing juicer setRemoved()"));
        // Only the physical client owns a playing sound. Preserve BlockEntity's server removal lifecycle.
        removed.instructions.clear();
        removed.tryCatchBlocks.clear();
        if (removed.localVariables != null) removed.localVariables.clear();
        removed.visibleLocalVariableAnnotations = null;
        removed.invisibleLocalVariableAnnotations = null;
        removed.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        removed.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, target.superName, "m_7651_", "()V", false));
        removed.instructions.add(new InsnNode(Opcodes.RETURN));
        removed.maxStack = 1;
        removed.maxLocals = 1;

        // DistCleaner removes these annotated members, but leaves callers and constructor assignments.
        target.fields.removeIf(field -> field.name.equals("workingSoundInstance") || field.name.equals("soundTickCounter"));
        target.methods.removeIf(method -> method.name.equals("tickClientSound") && method.desc.equals("()V"));
        for (MethodNode method : target.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof FieldInsnNode field && field.owner.equals(target.name)
                        && field.name.equals("soundTickCounter") && field.desc.equals("I")
                        && field.getOpcode() == Opcodes.PUTFIELD && method.name.equals("<init>")) {
                    method.instructions.set(field, new InsnNode(Opcodes.POP2));
                } else if (instruction instanceof MethodInsnNode call && call.owner.equals(target.name)
                        && call.name.equals("tickClientSound") && call.desc.equals("()V")
                        && call.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    method.instructions.set(call, new InsnNode(Opcodes.POP));
                }
            }
        }
    }
}
