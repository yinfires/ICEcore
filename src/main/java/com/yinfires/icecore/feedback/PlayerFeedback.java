package com.yinfires.icecore.feedback;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

import java.util.function.Supplier;

/** Routes transient player feedback through Minecraft's vanilla overlay message. */
public final class PlayerFeedback {
    private PlayerFeedback() {
    }

    /**
     * In 1.20.1 the vanilla overlay keeps a message for 60 GUI ticks: 40 opaque
     * ticks followed by 20 ticks of the built-in fade-out.
     */
    public static void show(ServerPlayer player, Component message) {
        if (player != null) {
            player.displayClientMessage(message, true);
        }
    }

    /** Keeps command output available for non-player sources such as the console. */
    public static void show(CommandSourceStack source, Component message) {
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            show(player, message);
        } else {
            source.sendSystemMessage(message);
        }
    }

    /** Preserves vanilla command failure styling while using the overlay for players. */
    public static void showFailure(CommandSourceStack source, Component message) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(message);
            return;
        }
        show(player, Component.empty().append(message).withStyle(ChatFormatting.RED));
    }

    /** Uses the overlay for the command source and keeps optional admin feedback intact. */
    public static void showSuccess(CommandSourceStack source, Supplier<Component> message,
                                   boolean broadcastToAdmins) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSuccess(message, broadcastToAdmins);
            return;
        }

        Component resolved = message.get();
        show(player, resolved);
        if (broadcastToAdmins && source.source.shouldInformAdmins()) {
            broadcastToAdmins(source, resolved, player);
        }
    }

    private static void broadcastToAdmins(CommandSourceStack source, Component message, ServerPlayer sourcePlayer) {
        MinecraftServer server = source.getServer();
        if (!server.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
            return;
        }

        Component adminMessage = Component.translatable("chat.type.admin", source.getDisplayName(), message)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player != sourcePlayer && server.getPlayerList().isOp(player.getGameProfile())) {
                show(player, adminMessage);
            }
        }
        if (source.source != server
                && server.getGameRules().getBoolean(GameRules.RULE_LOGADMINCOMMANDS)) {
            server.sendSystemMessage(adminMessage);
        }
    }
}
