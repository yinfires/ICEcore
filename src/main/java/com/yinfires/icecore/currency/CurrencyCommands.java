package com.yinfires.icecore.currency;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.Locale;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class CurrencyCommands {
    private CurrencyCommands() {
    }

    public static void attach(LiteralArgumentBuilder<CommandSourceStack> root) {
        var currency = literal("currency")
                .then(literal("get").executes(CurrencyCommands::get))
                .then(literal("set").then(argument("amount", LongArgumentType.longArg(0L))
                        .executes(context -> set(context, LongArgumentType.getLong(context, "amount")))))
                .then(literal("add").then(argument("amount", LongArgumentType.longArg(1L))
                        .executes(context -> add(context, LongArgumentType.getLong(context, "amount")))))
                .then(literal("remove").then(argument("amount", LongArgumentType.longArg(1L))
                        .executes(context -> remove(context, LongArgumentType.getLong(context, "amount")))));
        currency.then(literal("hud")
                .then(literal("get").executes(CurrencyCommands::getHud))
                .then(literal("on").executes(context -> setHud(context, true)))
                .then(literal("off").executes(context -> setHud(context, false)))
                .then(literal("toggle").executes(context -> setHud(context, !CurrencyService.hudEnabled()))));
        root.then(currency);
    }

    private static int get(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.translatable("icecore.currency.get",
                format(CurrencyService.balance())), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> context, long amount) {
        return result(CurrencyService.set(amount, "set", "command", context.getSource()));
    }

    private static int add(CommandContext<CommandSourceStack> context, long amount) {
        return result(CurrencyService.add(amount, "add", "command", context.getSource()));
    }

    private static int remove(CommandContext<CommandSourceStack> context, long amount) {
        return result(CurrencyService.remove(amount, "remove", "command", context.getSource()));
    }

    private static int result(CurrencyService.Result result) {
        if (result.success()) {
            return 1;
        }
        return 0;
    }

    private static int getHud(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.translatable(
                CurrencyService.hudEnabled() ? "icecore.currency.hud.enabled" : "icecore.currency.hud.disabled"), false);
        return 1;
    }

    private static int setHud(CommandContext<CommandSourceStack> context, boolean enabled) {
        CurrencyService.setHudEnabled(enabled);
        context.getSource().sendSuccess(() -> Component.translatable(
                enabled ? "icecore.currency.hud.enabled" : "icecore.currency.hud.disabled"), true);
        return 1;
    }

    private static String format(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }
}
