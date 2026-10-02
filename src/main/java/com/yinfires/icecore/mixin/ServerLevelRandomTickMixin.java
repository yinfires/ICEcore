package com.yinfires.icecore.mixin;

import com.yinfires.icecore.time.RandomTickScaling;
import com.yinfires.icecore.time.TimeConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelRandomTickMixin {
    @Inject(method = "m_8714_(Lnet/minecraft/world/level/chunk/LevelChunk;I)V", at = @At("TAIL"))
    private void icecore$compensateRandomTicks(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (level.dimension() != Level.OVERWORLD
                || !level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) return;

        double multiplier = TimeConfigManager.get().data().dayDurationMultiplier();
        int remaining = RandomTickScaling.compensationAttempts(randomTickSpeed, multiplier);
        if (remaining <= 0) return;

        LevelChunkSection[] sections = chunk.getSections();
        int minSection = level.getMinSection();
        RandomSource random = level.random;
        while (remaining > 0) {
            int batch = Math.min(remaining, RandomTickScaling.MAX_COMPENSATION_PER_CHUNK_TICK);
            for (int i = 0; i < batch; i++) {
                int sectionIndex = random.nextInt(sections.length);
                LevelChunkSection section = sections[sectionIndex];
                if (!section.isRandomlyTicking()) continue;

                int sectionY = (sectionIndex + minSection) << 4;
                BlockPos pos = level.getBlockRandomPos(chunk.getPos().getMinBlockX(), sectionY,
                        chunk.getPos().getMinBlockZ(), 15);
                BlockState state = section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
                if (state.isRandomlyTicking()) state.randomTick(level, pos, random);
                var fluid = state.getFluidState();
                if (fluid.isRandomlyTicking()) fluid.randomTick(level, pos, random);
            }
            remaining -= batch;
        }
    }
}
