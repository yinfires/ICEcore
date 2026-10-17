package com.yinfires.icecore.interaction;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class InteractionManager {
    public static final InteractionManager INSTANCE = new InteractionManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private MinecraftServer server;
    private InteractionConfig config = InteractionConfig.empty();
    private volatile InteractionSnapshot snapshot = InteractionSnapshot.empty();
    private long revision;
    public InteractionConfig config() { return config; }
    public InteractionSnapshot snapshot() { return snapshot; }
    public void start(MinecraftServer value) {
        server = value;
        try { reload(); } catch (Exception error) { LogUtils.getLogger().error("Cannot load adventure interaction rules; keeping previous snapshot", error); }
    }
    public void stop() { server = null; config = InteractionConfig.empty(); snapshot = InteractionSnapshot.empty(); revision++; }
    public Path rulesFile() { return FMLPaths.CONFIGDIR.get().resolve("icecore/adventure_interaction.json"); }
    public Path exceptionsFile() { return server.getWorldPath(LevelResource.ROOT).resolve("data/icecore_interaction_exceptions.json"); }
    public void reload() throws IOException {
        Path rules = rulesFile(), exceptions = exceptionsFile();
        InteractionConfig next = Files.exists(rules) ? InteractionConfig.parse(read(rules)) : InteractionConfig.empty();
        InteractionExceptions nextExceptions = Files.exists(exceptions) ? InteractionExceptions.parse(read(exceptions)) : InteractionExceptions.empty();
        InteractionSnapshot nextSnapshot = compile(next, nextExceptions);
        if (Files.notExists(rules)) write(rules, next.json());
        if (Files.notExists(exceptions)) write(exceptions, nextExceptions.json());
        publish(next, nextSnapshot);
    }
    private static JsonElement read(Path path) throws IOException { return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)); }
    public static void write(Path path, JsonElement json) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temp, GSON.toJson(json) + "\n", StandardCharsets.UTF_8);
            try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException error) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    public boolean edit(boolean allow, boolean add, InteractionConfig.Rule rule) throws IOException {
        InteractionConfig next = config.edit(allow, add, rule);
        if (next == config) return false;
        InteractionSnapshot compiled = compile(next, snapshot.exceptions());
        write(rulesFile(), next.json()); publish(next, compiled); return true;
    }
    public boolean exception(InteractionExceptions.Location location, String block) throws IOException {
        InteractionExceptions next = snapshot.exceptions().edit(location, block);
        if (next.equals(snapshot.exceptions())) return false;
        validateExceptions(next);
        write(exceptionsFile(), next.json());
        publish(config, new InteractionSnapshot(snapshot.denied(), snapshot.allowed(), next)); return true;
    }
    public void tagsChanged() {
        MinecraftServer owner = server;
        if (owner != null) owner.execute(() -> { if (server == owner) publish(config, compile(config, snapshot.exceptions())); });
    }
    private void publish(InteractionConfig next, InteractionSnapshot compiled) {
        config = next; snapshot = compiled; revision++;
        if (server != null) for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player);
    }
    public void send(ServerPlayer player) { ICECoreNetwork.sendToPlayer(new ClientBoundInteractionPacket(snapshot, revision), player); }
    private static InteractionSnapshot compile(InteractionConfig config, InteractionExceptions exceptions) {
        validateExceptions(exceptions);
        Set<Block> deny = expand(config.blacklist()), allow = expand(config.whitelist());
        return new InteractionSnapshot(deny, allow, exceptions);
    }
    private static void validateExceptions(InteractionExceptions exceptions) {
        if (exceptions.json().toString().length() > 1_000_000) throw new IllegalArgumentException("Exception snapshot exceeds network limit");
    }
    private static Set<Block> expand(List<InteractionConfig.Rule> rules) {
        Set<Block> result = new HashSet<>(); List<String> unknown = new ArrayList<>();
        for (InteractionConfig.Rule rule : rules) {
            TagKey<Block> tag = rule.tag() == null ? null : TagKey.create(Registries.BLOCK, ResourceLocation.parse(rule.tag()));
            boolean known = false;
            for (Block block : BuiltInRegistries.BLOCK) {
                if (rule.matches(BuiltInRegistries.BLOCK.getKey(block).toString(), ignored -> block.builtInRegistryHolder().is(tag))) {
                    result.add(block); known = true;
                }
            }
            if (!known) unknown.add(rule.toString());
        }
        if (!unknown.isEmpty()) LogUtils.getLogger().warn("Adventure interaction selectors without current members ({}): {}", unknown.size(), unknown.stream().limit(20).toList());
        return result;
    }
}
