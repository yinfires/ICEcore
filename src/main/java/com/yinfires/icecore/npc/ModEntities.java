package com.yinfires.icecore.npc;

import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatAnchorEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ICECore.MOD_ID);
    public static final RegistryObject<EntityType<NpcEntity>> NPC = ENTITY_TYPES.register("npc", () ->
            EntityType.Builder.of(NpcEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).updateInterval(2).fireImmune()
                    .build(ICECore.MOD_ID + ":npc"));
    public static final RegistryObject<EntityType<CozyCafeSeatAnchorEntity>> COZY_CAFE_SEAT =
            ENTITY_TYPES.register("cozycafe_seat", () -> EntityType.Builder
                    .<CozyCafeSeatAnchorEntity>of(CozyCafeSeatAnchorEntity::new, MobCategory.MISC)
                    .sized(0.01F, 0.01F).clientTrackingRange(8).updateInterval(10)
                    .build(ICECore.MOD_ID + ":cozycafe_seat"));

    private ModEntities() {}
}
