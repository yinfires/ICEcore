package com.yinfires.icecore.time;

public final class TimeTimings {
    private int fadeToCameraTicks = 10;
    private int revealCameraTicks = 10;
    private int fastForwardTicks = 20;
    private int typewriterTicksPerCharacter = 2;
    private int summaryHoldTicks = 40;
    private int summaryFadeTicks = 10;
    private int fadeToPlayerTicks = 10;
    private int revealPlayerTicks = 10;

    public int fadeToCameraTicks() { return fadeToCameraTicks; }
    public int revealCameraTicks() { return revealCameraTicks; }
    public int fastForwardTicks() { return fastForwardTicks; }
    public int typewriterTicksPerCharacter() { return typewriterTicksPerCharacter; }
    public int summaryHoldTicks() { return summaryHoldTicks; }
    public int summaryFadeTicks() { return summaryFadeTicks; }
    public int fadeToPlayerTicks() { return fadeToPlayerTicks; }
    public int revealPlayerTicks() { return revealPlayerTicks; }

    public int get(String name) {
        return switch (name) {
            case "fadeToCamera" -> fadeToCameraTicks;
            case "revealCamera" -> revealCameraTicks;
            case "fastForward" -> fastForwardTicks;
            case "typewriterPerCharacter" -> typewriterTicksPerCharacter;
            case "summaryHold" -> summaryHoldTicks;
            case "summaryFade" -> summaryFadeTicks;
            case "fadeToPlayer" -> fadeToPlayerTicks;
            case "revealPlayer" -> revealPlayerTicks;
            default -> throw new IllegalArgumentException("unknown timing");
        };
    }

    public void set(String name, int ticks) {
        int maximum = name.equals("fastForward") ? 28_800 : 1_200;
        if (ticks < 1 || ticks > maximum) throw new IllegalArgumentException("timing out of range");
        switch (name) {
            case "fadeToCamera" -> fadeToCameraTicks = ticks;
            case "revealCamera" -> revealCameraTicks = ticks;
            case "fastForward" -> fastForwardTicks = ticks;
            case "typewriterPerCharacter" -> typewriterTicksPerCharacter = ticks;
            case "summaryHold" -> summaryHoldTicks = ticks;
            case "summaryFade" -> summaryFadeTicks = ticks;
            case "fadeToPlayer" -> fadeToPlayerTicks = ticks;
            case "revealPlayer" -> revealPlayerTicks = ticks;
            default -> throw new IllegalArgumentException("unknown timing");
        }
    }

    public void validate() {
        for (String name : new String[]{"fadeToCamera", "revealCamera", "fastForward",
                "typewriterPerCharacter", "summaryHold", "summaryFade", "fadeToPlayer", "revealPlayer"}) get(name);
        if (fadeToCameraTicks < 1 || revealCameraTicks < 1 || fastForwardTicks < 1
                || typewriterTicksPerCharacter < 1 || summaryHoldTicks < 1 || summaryFadeTicks < 1
                || fadeToPlayerTicks < 1 || revealPlayerTicks < 1
                || fadeToCameraTicks > 1200 || revealCameraTicks > 1200 || fastForwardTicks > 28_800
                || typewriterTicksPerCharacter > 1200 || summaryHoldTicks > 1200 || summaryFadeTicks > 1200
                || fadeToPlayerTicks > 1200 || revealPlayerTicks > 1200) {
            throw new IllegalArgumentException("timings must be positive");
        }
    }

    void validateConfiguration() {
        validate();
        if (fastForwardTicks > 1_200) throw new IllegalArgumentException("fastForward timing must be 1..1200");
    }
}
