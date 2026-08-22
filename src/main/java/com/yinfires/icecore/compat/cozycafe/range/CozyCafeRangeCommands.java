package com.yinfires.icecore.compat.cozycafe.range;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.ModList;

import static net.minecraft.commands.Commands.literal;

/** Operator command for selecting one targeted CozyCafe computer's custom range. */
public final class CozyCafeRangeCommands {
    private CozyCafeRangeCommands() {
    }

    public static void attach(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(literal("cozycafe")
                .then(literal("range")
                        .then(literal("set").executes(CozyCafeRangeCommands::enterAdjustment))));
    }

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
        player.sendSystemMessage(Component.translatable("icecore.cozycafe.range.enter",
                blockHit.getBlockPos().toShortString()));
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> context, String key) {
        context.getSource().sendFailure(Component.translatable(key));
        return 0;
    }
}
