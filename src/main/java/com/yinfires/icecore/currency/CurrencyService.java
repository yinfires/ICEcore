package com.yinfires.icecore.currency;

import com.yinfires.icecore.ICECore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

public final class CurrencyService {
    private static final ResourceLocation STORAGE_ID = ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "currency");
    private static final ResourceLocation CHANGED_TAG = ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "currency_changed");
    private static final int MAX_CALLBACKS_PER_TICK = 64;
    private static final Deque<CurrencyChange> CALLBACKS = new ArrayDeque<>();

    private static MinecraftServer server;
    private static CurrencySavedData data;

    private CurrencyService() {
    }

    public static void start(MinecraftServer minecraftServer) {
        server = minecraftServer;
        data = minecraftServer.overworld().getDataStorage().computeIfAbsent(
                CurrencySavedData::load, CurrencySavedData::new, CurrencySavedData.FILE_ID);
        CALLBACKS.clear();
        writeSnapshotStorage();
    }

    public static void stop() {
        CALLBACKS.clear();
        data = null;
        server = null;
    }

    public static long balance() {
        return data == null ? 0L : data.balance();
    }

    public static boolean hudEnabled() {
        return data == null || data.hudEnabled();
    }

    public static Result set(long amount, String operation, String source, CommandSourceStack commandSource) {
        if (amount < 0L || data == null) {
            return Result.INVALID;
        }
        return submit(new Request(Mode.SET, amount, operation, source, commandSource));
    }

    public static Result add(long amount, String operation, String source, CommandSourceStack commandSource) {
        if (amount <= 0L || data == null) {
            return Result.INVALID;
        }
        return submit(new Request(Mode.ADD, amount, operation, source, commandSource));
    }

    public static Result remove(long amount, String operation, String source, CommandSourceStack commandSource) {
        if (amount <= 0L || data == null) {
            return Result.INVALID;
        }
        return submit(new Request(Mode.REMOVE, amount, operation, source, commandSource));
    }

    private static Result submit(Request request) {
        return apply(request);
    }

    private static Result apply(Request request) {
        long previous = data.balance();
        long current;
        if (request.mode == Mode.SET) {
            current = request.amount;
        } else if (request.mode == Mode.ADD) {
            if (request.amount > Long.MAX_VALUE - previous) {
                return Result.OVERFLOW;
            }
            current = previous + request.amount;
        } else {
            if (request.amount > previous) {
                return Result.INSUFFICIENT;
            }
            current = previous - request.amount;
        }
        if (current == previous) {
            return Result.UNCHANGED;
        }

        data.setBalance(current);
        com.yinfires.icecore.time.TimeService.recordIncome(current - previous);
        CurrencyChange change = new CurrencyChange(previous, current, current - previous, request.amount,
                request.operation, request.source, normalizeSource(request.commandSource));
        writeChangeStorage(change);
        CurrencyNetworking.broadcastChange(change);
        CurrencySoundHooks.onBalanceChanged(change.delta());
        CALLBACKS.addLast(change);
        return Result.CHANGED;
    }

    public static boolean setHudEnabled(boolean enabled) {
        if (data == null || data.hudEnabled() == enabled) {
            return false;
        }
        data.setHudEnabled(enabled);
        writeSnapshotStorage();
        CurrencyNetworking.broadcastSnapshot();
        return true;
    }

    public static void tick() {
        if (server == null || data == null) {
            return;
        }
        int processed = 0;
        while (processed < MAX_CALLBACKS_PER_TICK && !CALLBACKS.isEmpty()) {
            CurrencyChange change = CALLBACKS.removeFirst();
            // A later change may already have updated command storage. Restore this event's
            // context immediately before its functions run so every callback sees its own data.
            writeChangeStorage(change);
            for (var function : server.getFunctions().getTag(CHANGED_TAG)) {
                server.getFunctions().execute(function, change.commandSource().withPermission(2));
            }
            processed++;
        }
    }

    public static void sendSnapshot(ServerPlayer player) {
        CurrencyNetworking.sendSnapshot(player, balance(), hudEnabled());
    }

    private static CommandSourceStack normalizeSource(CommandSourceStack source) {
        return source == null ? server.createCommandSourceStack().withSuppressedOutput() : source.withSuppressedOutput();
    }

    public static CommandSourceStack sourceAt(ServerLevel level, Vec3 position, Entity entity) {
        CommandSourceStack source = server.createCommandSourceStack().withLevel(level).withPosition(position);
        return entity == null ? source : source.withEntity(entity);
    }

    private static void writeSnapshotStorage() {
        if (server == null || data == null) return;
        CompoundTag tag = server.getCommandStorage().get(STORAGE_ID);
        tag.putLong("balance", data.balance());
        tag.putBoolean("hud_enabled", data.hudEnabled());
        server.getCommandStorage().set(STORAGE_ID, tag);
    }

    private static void writeChangeStorage(CurrencyChange change) {
        CompoundTag root = new CompoundTag();
        root.putLong("balance", balance());
        root.putBoolean("hud_enabled", hudEnabled());
        CompoundTag tag = new CompoundTag();
        tag.putLong("previous", change.previous());
        tag.putLong("current", change.current());
        tag.putLong("delta", change.delta());
        tag.putLong("requested", change.requested());
        tag.putString("operation", change.operation());
        tag.putString("source", change.source());
        CommandSourceStack source = change.commandSource();
        tag.putString("name", source.getTextName());
        tag.putString("dimension", source.getLevel().dimension().location().toString());
        tag.putDouble("x", source.getPosition().x);
        tag.putDouble("y", source.getPosition().y);
        tag.putDouble("z", source.getPosition().z);
        if (source.getEntity() != null) {
            tag.putUUID("uuid", source.getEntity().getUUID());
        }
        root.put("change", tag);
        server.getCommandStorage().set(STORAGE_ID, root);
    }

    public enum Result {
        CHANGED, UNCHANGED, INVALID, OVERFLOW, INSUFFICIENT;

        public boolean success() {
            return this == CHANGED || this == UNCHANGED;
        }
    }

    private enum Mode { SET, ADD, REMOVE }

    private record Request(Mode mode, long amount, String operation, String source,
                           CommandSourceStack commandSource) {
    }
}
