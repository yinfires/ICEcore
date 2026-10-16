package com.yinfires.icecore.workstation;

import net.minecraft.world.level.block.entity.BlockEntity;
import java.lang.reflect.*;
import java.util.concurrent.ConcurrentHashMap;

/** Cached, on-demand access to verified optional-mod members. Never used for class probing. */
public final class ExternalWorkstation {
    private ExternalWorkstation() {}
    private static final java.util.Map<String, Method> STATIC_METHODS = new ConcurrentHashMap<>();
    public static Object invokeStatic(String owner, String name, Class<?>[] types, Object... arguments) {
        Method method = STATIC_METHODS.computeIfAbsent(owner + ":" + name, ignored -> {
            try { return Class.forName(owner).getMethod(name, types); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        });
        try { return method.invoke(null, arguments); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static final ClassValue<ConcurrentHashMap<String, AccessibleObject>> MEMBERS = new ClassValue<>() {
        protected ConcurrentHashMap<String, AccessibleObject> computeValue(Class<?> type) { return new ConcurrentHashMap<>(); }
    };
    private static AccessibleObject member(Class<?> type, String name, boolean method) {
        return MEMBERS.get(type).computeIfAbsent((method ? "m:" : "f:") + name, key -> {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                if (method) {
                    for (Method candidate : current.getDeclaredMethods()) if (candidate.getName().equals(name) && candidate.getParameterCount() == 0) {
                        candidate.setAccessible(true); return candidate;
                    }
                } else {
                    try { Field field = current.getDeclaredField(name); field.setAccessible(true); return field; }
                    catch (NoSuchFieldException ignored) { /* Verified member may belong to an upstream base class. */ }
                }
            }
            throw new IllegalStateException("Missing workstation member " + type.getName() + "." + name);
        });
    }
    public static Object get(Object target, String name) {
        try { return ((Field)member(target.getClass(), name, false)).get(target); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    public static int number(Object target, String name) { return ((Number)get(target, name)).intValue(); }
    public static Field field(Class<?> type, String name) { return (Field)member(type, name, false); }
    public static void set(Object target, String name, Object value) {
        try { ((Field)member(target.getClass(), name, false)).set(target, value); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    public static Object call(Object target, String name) {
        try { return ((Method)member(target.getClass(), name, true)).invoke(target); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    public static Object invoke(Object target, String name, Class<?>[] types, Object... arguments) {
        String key = "i:" + name + java.util.Arrays.toString(types);
        Method method = (Method)MEMBERS.get(target.getClass()).computeIfAbsent(key, ignored -> {
            try { Method found = target.getClass().getMethod(name, types); found.setAccessible(true); return found; }
            catch (NoSuchMethodException e) { throw new IllegalStateException(e); }
        });
        try { return method.invoke(target, arguments); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    public static void changed(BlockEntity entity) {
        entity.setChanged();
        var level = entity.getLevel();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(entity.getBlockPos(), entity.getBlockState(), entity.getBlockState(), 3);
    }
}
