package com.yinfires.icecore.mixing.client;

import com.yinfires.icecore.mixing.MixingBowlBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class MixingBowlStirringSound extends AbstractTickableSoundInstance {
    private static final ResourceLocation SOUND_ID =
            ResourceLocation.fromNamespaceAndPath("farm_and_charm", "crafting_bowl_stirring");
    private static final Map<MixingBowlBlockEntity, List<MixingBowlStirringSound>> ACTIVE = new IdentityHashMap<>();

    private final MixingBowlBlockEntity bowl;

    private MixingBowlStirringSound(SoundEvent sound, MixingBowlBlockEntity bowl) {
        super(sound, SoundSource.BLOCKS, RandomSource.create());
        this.bowl = bowl;
        BlockPos pos = bowl.getBlockPos();
        x = pos.getX() + 0.5D;
        y = pos.getY() + 0.5D;
        z = pos.getZ() + 0.5D;
        volume = 1.0F;
        pitch = 1.0F;
        looping = false;
        delay = 0;
        attenuation = SoundInstance.Attenuation.LINEAR;
    }

    public static void update(MixingBowlBlockEntity bowl, boolean stirring) {
        if (!stirring) {
            stopAll(bowl);
            return;
        }
        if (bowl.getLevel() == null) return;
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(SOUND_ID);
        if (sound == null) return;
        MixingBowlStirringSound created = new MixingBowlStirringSound(sound, bowl);
        ACTIVE.computeIfAbsent(bowl, ignored -> new ArrayList<>()).add(created);
        Minecraft.getInstance().getSoundManager().play(created);
    }

    private static void stopAll(MixingBowlBlockEntity bowl) {
        List<MixingBowlStirringSound> sounds = ACTIVE.remove(bowl);
        if (sounds != null) sounds.forEach(MixingBowlStirringSound::stop);
    }

    private void stopAndForget() {
        List<MixingBowlStirringSound> sounds = ACTIVE.get(bowl);
        if (sounds != null) {
            sounds.remove(this);
            if (sounds.isEmpty()) ACTIVE.remove(bowl);
        }
        stop();
    }

    @Override public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean stillStirring = minecraft.level == bowl.getLevel()
                && !bowl.isRemoved()
                && bowl.stage() == MixingBowlBlockEntity.Stage.STIRRING;
        if (!stillStirring) stopAndForget();
    }
}
