package com.yinfires.icecore.npc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class NpcCommands {
    private static final double REMOVE_RANGE = 8.0D;
    private NpcCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var definition = argument("definition", ResourceLocationArgument.id()).suggests(NpcCommands::suggestDefinitions)
                .executes(context -> summon(context, context.getSource().getPosition()))
                .then(argument("position", Vec3Argument.vec3())
                        .executes(context -> summon(context, Vec3Argument.getVec3(context, "position"))));
        dispatcher.register(literal("icecore").requires(source -> source.hasPermission(2))
                .then(literal("npc").then(literal("summon").then(definition))
                        .then(literal("remove").executes(NpcCommands::remove))));
    }

    private static int summon(CommandContext<CommandSourceStack> context, Vec3 position) {
        CommandSourceStack source = context.getSource();
        ResourceLocation id = ResourceLocationArgument.getId(context, "definition");
        NpcDefinition definition = id == null ? null : NpcDefinitionManager.INSTANCE.get(id);
        if (definition == null) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.npc.definition.missing", String.valueOf(id)));
            return 0;
        }
        ServerLevel level = source.getLevel();
        NpcEntity npc = ModEntities.NPC.get().create(level);
        if (npc == null) return 0;
        float yaw = source.getEntity() == null ? 0.0F : source.getRotation().y;
        npc.initialize(definition, position, yaw);
        if (!level.addFreshEntity(npc)) return 0;
        PlayerFeedback.showSuccess(source, () -> Component.translatable("icecore.npc.summon.success", npc.getName()), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;
        try { player = source.getPlayerOrException(); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.npc.player_only"));
            return 0;
        }
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(REMOVE_RANGE));
        HitResult blockHit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end,
                player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D),
                entity -> entity instanceof NpcEntity && entity.isPickable(), REMOVE_RANGE * REMOVE_RANGE);
        if (hit == null || !(hit.getEntity() instanceof NpcEntity npc)) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.npc.remove.invalid_target"));
            return 0;
        }
        Component name = npc.getName();
        npc.remove(Entity.RemovalReason.DISCARDED);
        PlayerFeedback.showSuccess(source, () -> Component.translatable("icecore.npc.remove.success", name), true);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestDefinitions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(NpcDefinitionManager.INSTANCE.ids(), builder);
    }
}
