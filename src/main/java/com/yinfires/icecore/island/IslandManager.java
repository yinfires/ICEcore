package com.yinfires.icecore.island;

import com.yinfires.icecore.building.BuildingDataManager;
import com.yinfires.icecore.building.RegionDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Server-side registry of captured islands: their block snapshot, origin
 * (dimension + minimum corner) and visibility state. Each island persists to
 * its own NBT file under {@code data/icecore_islands/<name>.nbt} so a large
 * snapshot never bloats a shared JSON file. Prototype: whole-file load/save.
 */
public final class IslandManager {
    public enum State {
        CAPTURED,
        HIDDEN,
        SHOWN
    }

    private static final IslandManager INSTANCE = new IslandManager();

    private final Map<String, Entry> islands = new HashMap<>();
    private MinecraftServer server;

    private IslandManager() {
    }

    public static IslandManager get() {
        return INSTANCE;
    }

    public void start(MinecraftServer server) {
        this.server = server;
        islands.clear();
        loadAll();
    }

    public void stop() {
        server = null;
        islands.clear();
        IslandPlacer.clear();
    }

    public MinecraftServer server() {
        return server;
    }

    public Set<String> names() {
        return new TreeSet<>(islands.keySet());
    }

    public Entry entry(String name) {
        return islands.get(normalize(name));
    }

    /** Captures the region volume into a named island, binding the source region, and persists it. */
    public boolean capture(String name, String sourceRegion, ServerLevel level, BlockPos min, BlockPos max) {
        String key = normalize(name);
        IslandData data = IslandCaptureService.capture(level, min, max);
        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        Entry entry = new Entry(key, level.dimension(), new BlockPos(minX, minY, minZ), data, State.CAPTURED);
        entry.setSourceRegion(sourceRegion);
        islands.put(key, entry);
        return save(entry);
    }

    /**
     * Re-scans the island from the <em>current</em> bounds of its source region — so a
     * region that was moved or resized is picked up — replacing its dimension, origin and
     * block/entity snapshot while keeping the name, state and entrance camera. Falls back
     * to the island's stored origin + size when it has no bound region or that region is
     * gone/incomplete (legacy islands), preserving the old rescan-in-place behaviour.
     * Returns false if the island is unknown or the target dimension is not loaded.
     * Callers must ensure the blocks are present (not a HIDDEN island) and no placement runs.
     */
    public boolean recapture(String name) {
        Entry entry = islands.get(normalize(name));
        if (entry == null) {
            return false;
        }
        RegionDefinition region = entry.sourceRegion.isEmpty() ? null
                : BuildingDataManager.get().data().regions().get(entry.sourceRegion);
        if (region != null && region.isComplete()) {
            ResourceLocation dimensionId = ResourceLocation.tryParse(region.dimension());
            ServerLevel level = dimensionId == null ? null
                    : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
            if (level == null) {
                return false;
            }
            int[] p1 = region.pos1();
            int[] p2 = region.pos2();
            BlockPos min = new BlockPos(Math.min(p1[0], p2[0]), Math.min(p1[1], p2[1]), Math.min(p1[2], p2[2]));
            BlockPos max = new BlockPos(Math.max(p1[0], p2[0]), Math.max(p1[1], p2[1]), Math.max(p1[2], p2[2]));
            entry.relocate(level.dimension(), min, IslandCaptureService.capture(level, min, max));
            return save(entry);
        }
        // Legacy / unbound island: rescan the stored volume in place.
        ServerLevel level = levelOf(entry);
        if (level == null) {
            return false;
        }
        IslandData old = entry.data;
        BlockPos max = entry.min.offset(old.sizeX() - 1, old.sizeY() - 1, old.sizeZ() - 1);
        entry.setData(IslandCaptureService.capture(level, entry.min, max));
        return save(entry);
    }

    /** Removes a named island from the registry and deletes its data file. */
    public boolean remove(String name) {
        String key = normalize(name);
        if (islands.remove(key) == null) {
            return false;
        }
        if (server != null) {
            try {
                Files.deleteIfExists(directory().resolve(key + ".nbt"));
            } catch (IOException ignored) {
                // The in-memory entry is already gone; a leftover file is reloaded
                // next start only if it still parses, which is harmless here.
            }
        }
        return true;
    }

    public void setState(String name, State state) {
        Entry entry = islands.get(normalize(name));
        if (entry != null) {
            entry.state = state;
            save(entry);
        }
    }

    /** Records the entrance camera pose (player eye + look) for an island and persists it. */
    public boolean setEntrance(String name, double eyeX, double eyeY, double eyeZ, float yaw, float pitch) {
        Entry entry = islands.get(normalize(name));
        if (entry == null) {
            return false;
        }
        entry.setEntrance(eyeX, eyeY, eyeZ, yaw, pitch);
        return save(entry);
    }

    /** Clears an island's recorded entrance camera and persists it. */
    public boolean clearEntrance(String name) {
        Entry entry = islands.get(normalize(name));
        if (entry == null) {
            return false;
        }
        entry.clearEntrance();
        return save(entry);
    }

    public ServerLevel levelOf(Entry entry) {
        return server == null ? null : server.getLevel(entry.dimension);
    }

    private void loadAll() {
        Path dir = directory();
        if (!Files.isDirectory(dir)) {
            return;
        }
        var blocks = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".nbt")).forEach(p -> {
                try {
                    CompoundTag tag = NbtIo.readCompressed(p.toFile());
                    Entry entry = Entry.load(tag, blocks);
                    islands.put(entry.name, entry);
                } catch (Exception exception) {
                    // A corrupt island file must not abort loading the rest.
                }
            });
        } catch (IOException ignored) {
        }
    }

    private boolean save(Entry entry) {
        if (server == null) {
            return false;
        }
        Path file = directory().resolve(entry.name + ".nbt");
        try {
            Files.createDirectories(file.getParent());
            NbtIo.writeCompressed(entry.save(), file.toFile());
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private Path directory() {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("icecore_islands");
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static final class Entry {
        private final String name;
        private ResourceKey<Level> dimension;
        private BlockPos min;
        private IslandData data;
        private State state;
        /** Region this island was captured from; recapture rescans that region's current bounds. Empty if unbound. */
        private String sourceRegion = "";
        private boolean hasEntrance;
        private double entranceX;
        private double entranceY;
        private double entranceZ;
        private float entranceYaw;
        private float entrancePitch;

        Entry(String name, ResourceKey<Level> dimension, BlockPos min, IslandData data, State state) {
            this.name = name;
            this.dimension = dimension;
            this.min = min;
            this.data = data;
            this.state = state;
        }

        public String sourceRegion() {
            return sourceRegion;
        }

        void setSourceRegion(String region) {
            this.sourceRegion = region == null ? "" : region;
        }

        /** Repoints the island at a new dimension/origin/snapshot (a recapture from moved region bounds). */
        void relocate(ResourceKey<Level> dimension, BlockPos min, IslandData data) {
            this.dimension = dimension;
            this.min = min;
            this.data = data;
        }

        public String name() {
            return name;
        }

        public ResourceKey<Level> dimension() {
            return dimension;
        }

        public BlockPos min() {
            return min;
        }

        public IslandData data() {
            return data;
        }

        void setData(IslandData data) {
            this.data = data;
        }

        public State state() {
            return state;
        }

        public boolean hasEntrance() {
            return hasEntrance;
        }

        public double entranceX() {
            return entranceX;
        }

        public double entranceY() {
            return entranceY;
        }

        public double entranceZ() {
            return entranceZ;
        }

        public float entranceYaw() {
            return entranceYaw;
        }

        public float entrancePitch() {
            return entrancePitch;
        }

        void setEntrance(double x, double y, double z, float yaw, float pitch) {
            this.hasEntrance = true;
            this.entranceX = x;
            this.entranceY = y;
            this.entranceZ = z;
            this.entranceYaw = yaw;
            this.entrancePitch = pitch;
        }

        void clearEntrance() {
            this.hasEntrance = false;
            this.entranceX = 0D;
            this.entranceY = 0D;
            this.entranceZ = 0D;
            this.entranceYaw = 0F;
            this.entrancePitch = 0F;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", name);
            tag.putString("dimension", dimension.location().toString());
            tag.putInt("minX", min.getX());
            tag.putInt("minY", min.getY());
            tag.putInt("minZ", min.getZ());
            tag.putString("state", state.name());
            if (!sourceRegion.isEmpty()) {
                tag.putString("sourceRegion", sourceRegion);
            }
            tag.put("data", data.save());
            if (hasEntrance) {
                CompoundTag entrance = new CompoundTag();
                entrance.putDouble("x", entranceX);
                entrance.putDouble("y", entranceY);
                entrance.putDouble("z", entranceZ);
                entrance.putFloat("yaw", entranceYaw);
                entrance.putFloat("pitch", entrancePitch);
                tag.put("entrance", entrance);
            }
            return tag;
        }

        static Entry load(CompoundTag tag, net.minecraft.core.HolderGetter<net.minecraft.world.level.block.Block> blocks) {
            ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("dimension"));
            if (dimensionId == null) {
                dimensionId = Level.OVERWORLD.location();
            }
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            BlockPos min = new BlockPos(tag.getInt("minX"), tag.getInt("minY"), tag.getInt("minZ"));
            IslandData data = IslandData.load(tag.getCompound("data"), blocks);
            State state = State.valueOf(tag.getString("state"));
            Entry entry = new Entry(tag.getString("name"), dimension, min, data, state);
            entry.setSourceRegion(tag.getString("sourceRegion"));
            if (tag.contains("entrance")) {
                CompoundTag entrance = tag.getCompound("entrance");
                entry.setEntrance(entrance.getDouble("x"), entrance.getDouble("y"), entrance.getDouble("z"),
                        entrance.getFloat("yaw"), entrance.getFloat("pitch"));
            }
            return entry;
        }
    }
}
