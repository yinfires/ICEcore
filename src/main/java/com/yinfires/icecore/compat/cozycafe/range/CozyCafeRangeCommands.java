package com.yinfires.icecore.compat.cozycafe.range;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yinfires.icecore.building.BuildingDataManager;
import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.ModList;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

/** Operator command for selecting one targeted CozyCafe computer's custom range. */
public final class CozyCafeRangeCommands {
    private CozyCafeRangeCommands() {
    }

    public static void attach(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(literal("cozycafe")
                .then(literal("range")
                        .then(literal("set").executes(CozyCafeRangeCommands::enterAdjustment)))
                .then(literal("spawn-region")
                        .then(literal("set").then(argument("region", StringArgumentType.word())
                                .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        BuildingDataManager.get().data().regions().keySet(), builder))
                                .executes(CozyCafeRangeCommands::setSpawnRegion)))
                        .then(literal("clear").executes(CozyCafeRangeCommands::clearSpawnRegion))));
    }

    private static int setSpawnRegion(CommandContext<CommandSourceStack> context) {
        Target target = target(context);
        if (target == null) return 0;
        String region = StringArgumentType.getString(context, "region");
        if (!CozyCafeRangeDataManager.get().setSpawnRegion(target.player(), target.position(), region)) return 0;
        PlayerFeedback.show(target.player(), Component.translatable("icecore.cozycafe.spawn_region.set", region));
        return 1;
    }

    private static int clearSpawnRegion(CommandContext<CommandSourceStack> context) {
        Target target = target(context);
        if (target == null) return 0;
        if (!CozyCafeRangeDataManager.get().clearSpawnRegion(target.player(), target.position())) return 0;
        PlayerFeedback.show(target.player(), Component.translatable("icecore.cozycafe.spawn_region.cleared"));
        return 1;
    }

    private static Target target(CommandContext<CommandSourceStack> context) {
        if (!ModList.get().isLoaded(CozyCafeRangeCompat.MOD_ID)) {
            fail(context, "icecore.cozycafe.range.mod_missing"); return null;
        }
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) { fail(context, "icecore.cozycafe.range.player_only"); return null; }
        if (!CozyCafeRangeAdjustmentManager.canEnter(player)) {
            fail(context, "icecore.cozycafe.range.invalid_stick"); return null;
        }
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        HitResult hit = player.pick(reach, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                || !player.canReach(blockHit.getBlockPos(), 1.5D)
                || !CozyCafeRangeCompat.isCafeManager(player.level(), blockHit.getBlockPos())) {
            fail(context, "icecore.cozycafe.range.invalid_target"); return null;
        }
        return new Target(player, blockHit.getBlockPos());
    }

    private record Target(ServerPlayer player, net.minecraft.core.BlockPos position) {}

    private static int enterAdjustment(CommandContext<CommandSourceStack> context) {
        if (!ModList.get().isLoaded(CozyCafeRangeCompat.MOD_ID)) {
            return fail(context, "icecore.cozycafe.range.mod_missing");
        }
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return fail(context, "icecore.cozycafe.range.player_only");
        }
        if (!CozyCafeRangeAdjustmentManager.canEnter(player)) {
            return fail(context, "icecore.cozycafe.range.invalid_stick");
        }

        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        HitResult target = player.pick(reach, 1.0F, false);
        if (!(target instanceof BlockHitResult blockHit)
                || target.getType() != HitResult.Type.BLOCK
                || !player.canReach(blockHit.getBlockPos(), 1.5D)
                || !CozyCafeRangeCompat.isCafeManager(player.level(), blockHit.getBlockPos())) {
            return fail(context, "icecore.cozycafe.range.invalid_target");
        }
        if (!CozyCafeRangeAdjustmentManager.enter(player, blockHit.getBlockPos())) {
            return 0;
        }
        PlayerFeedback.show(player, Component.translatable("icecore.cozycafe.range.enter",
                blockHit.getBlockPos().toShortString()));
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> context, String key) {
        PlayerFeedback.showFailure(context.getSource(), Component.translatable(key));
        return 0;
    }
}
