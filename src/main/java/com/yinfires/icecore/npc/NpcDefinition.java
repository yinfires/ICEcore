package com.yinfires.icecore.npc;

import net.minecraft.resources.ResourceLocation;

public record NpcDefinition(ResourceLocation id, String nameKey, ResourceLocation texture,
                            boolean slim, ResourceLocation dialogue, String dialogueGroup) {
}
