package com.yinfires.icecore.currency;

import net.minecraft.commands.CommandSourceStack;

public record CurrencyChange(long previous, long current, long delta, long requested,
                             String operation, String source, CommandSourceStack commandSource) {
}
