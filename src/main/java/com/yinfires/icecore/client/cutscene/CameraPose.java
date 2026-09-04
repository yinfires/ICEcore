package com.yinfires.icecore.client.cutscene;

/**
 * A fixed camera viewpoint for a cutscene: the recorded eye position and look
 * angles. Reused by any cutscene that takes over the view (time skip, island
 * entrance, ...). {@code yaw}/{@code pitch} are Minecraft view angles in degrees;
 * {@code y} is the eye height (not the feet), matching what the recorder captures
 * from a player.
 */
public record CameraPose(double x, double y, double z, float yaw, float pitch) {
}
