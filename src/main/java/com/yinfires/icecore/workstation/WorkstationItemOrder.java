package com.yinfires.icecore.workstation;

import java.util.function.IntPredicate;

/** Slot order is independent of the item or container held by the player. */
public final class WorkstationItemOrder {
    private WorkstationItemOrder() {}

    public static int lastSlot(long[] sequence, IntPredicate occupied) {
        int last = -1;
        for (int i = 0; i < sequence.length; i++) {
            if (occupied.test(i) && (last < 0 || sequence[i] > sequence[last])) last = i;
        }
        return last;
    }

    public static boolean canTake(boolean requiresContainer, boolean emptyHand, boolean matchesContainer) {
        return requiresContainer ? !emptyHand && matchesContainer : emptyHand;
    }

    /** Keep valid saved order; legacy or malformed order falls back to slot order. */
    public static long restore(long[] sequence, long[] saved, long savedNext, IntPredicate occupied) {
        boolean valid = saved.length == sequence.length;
        long max = 0;
        for (int i = 0; valid && i < saved.length; i++) {
            if (!occupied.test(i)) continue;
            if (saved[i] <= 0 || saved[i] == Long.MAX_VALUE) { valid = false; break; }
            for (int j = 0; j < i; j++) {
                if (occupied.test(j) && saved[j] == saved[i]) { valid = false; break; }
            }
            max = Math.max(max, saved[i]);
        }
        long next = 1;
        for (int i = 0; i < sequence.length; i++) {
            sequence[i] = occupied.test(i) ? (valid ? saved[i] : next++) : 0;
        }
        return valid ? Math.max(max + 1, savedNext > 0 && savedNext < Long.MAX_VALUE ? savedNext : 1) : next;
    }
}
