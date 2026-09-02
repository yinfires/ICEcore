package com.yinfires.icecore.npc;

import com.zhenshiz.chatbox.utils.chatbox.ChatBoxCommandUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class NpcEntity extends Mob {
    private static final EntityDataAccessor<String> DEFINITION = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> NAME_KEY = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SLIM = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DIALOGUE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DIALOGUE_GROUP = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> ANCHOR_YAW = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.FLOAT);
    private static final String DEFAULT_DEFINITION = "icecore:missing";
    private static final double LOOK_RANGE = 8.0D;
    private Vec3 anchor = Vec3.ZERO;
    private ServerPlayer lookTarget;
    private long checkedDefinitionRevision = Long.MIN_VALUE;

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override protected void registerGoals() {}

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DEFINITION, DEFAULT_DEFINITION);
        entityData.define(NAME_KEY, "");
        entityData.define(TEXTURE, NpcDefinitionManager.missingTexture().toString());
        entityData.define(SLIM, false);
        entityData.define(DIALOGUE, "");
        entityData.define(DIALOGUE_GROUP, "");
        entityData.define(ANCHOR_YAW, 0.0F);
    }

    public void initialize(NpcDefinition definition, Vec3 position, float yaw) {
        anchor = position;
        float snapped = NpcRotation.snapToEightDirections(yaw);
        entityData.set(DEFINITION, definition.id().toString());
        entityData.set(NAME_KEY, definition.nameKey());
        entityData.set(TEXTURE, definition.texture().toString());
        entityData.set(SLIM, definition.slim());
        entityData.set(DIALOGUE, definition.dialogue().toString());
        entityData.set(DIALOGUE_GROUP, definition.dialogueGroup());
        entityData.set(ANCHOR_YAW, snapped);
        setPos(position);
        setAllRotations(snapped);
        setCustomNameVisible(false);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        if (!level().isClientSide) {
            if (distanceToSqr(anchor) > 1.0E-8D) setPos(anchor.x, anchor.y, anchor.z);
        }
    }

    @Override
    protected void customServerAiStep() {
        // Mob invokes this method from its server AI phase. Refreshing the target
        // here lets vanilla LookControl consume it in the same AI tick, exactly as
        // it does for villagers, instead of racing the superclass tick afterward.
        if (tickCount % 10 == 0 && NpcDefinitionManager.INSTANCE.isLoaded()
                && checkedDefinitionRevision != NpcDefinitionManager.INSTANCE.revision()) {
            checkedDefinitionRevision = NpcDefinitionManager.INSTANCE.revision();
            ResourceLocation definitionId = getDefinitionId();
            NpcDefinition definition = NpcDefinitionManager.INSTANCE.get(definitionId);
            if (definition == null) {
                NpcDefinitionManager.INSTANCE.warnMissing(definitionId);
                entityData.set(NAME_KEY, "");
                entityData.set(TEXTURE, NpcDefinitionManager.missingTexture().toString());
            } else {
                entityData.set(NAME_KEY, definition.nameKey());
                entityData.set(TEXTURE, definition.texture().toString());
                entityData.set(SLIM, definition.slim());
                entityData.set(DIALOGUE, definition.dialogue().toString());
                entityData.set(DIALOGUE_GROUP, definition.dialogueGroup());
            }
        }
        if (tickCount % 10 == 0) {
            lookTarget = ((ServerLevel) level()).getEntitiesOfClass(ServerPlayer.class,
                            getBoundingBox().inflate(LOOK_RANGE), this::isValidTarget).stream()
                    .min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        }
        // LookControl expires its internal target after a few ticks. The player
        // search remains every 10 ticks, but the selected target must be renewed
        // every AI tick; otherwise vanilla immediately turns the mob back toward
        // its idle direction, producing the visible oscillation.
        if (lookTarget != null && isValidTarget(lookTarget)) {
            getLookControl().setLookAt(lookTarget, 30.0F, 30.0F);
        } else {
            lookTarget = null;
            float yaw = getAnchorYaw() * Mth.DEG_TO_RAD;
            getLookControl().setLookAt(anchor.x - Mth.sin(yaw), getEyeY(), anchor.z + Mth.cos(yaw), 10.0F, 10.0F);
        }
        super.customServerAiStep();
    }

    private boolean isValidTarget(Player player) {
        return player.isAlive() && !player.isSpectator() && player.level() == level()
                && distanceToSqr(player) <= LOOK_RANGE * LOOK_RANGE;
    }

    private void setAllRotations(float yaw) {
        setYRot(yaw); setYHeadRot(yaw); yBodyRot = yaw;
        yRotO = yaw; yHeadRotO = yaw; yBodyRotO = yaw; setXRot(0.0F); xRotO = 0.0F;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.CONSUME;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer) || !isValidTarget(player)) return InteractionResult.FAIL;
        ResourceLocation definitionId = getDefinitionId();
        NpcDefinition definition = NpcDefinitionManager.INSTANCE.get(definitionId);
        if (definition == null) {
            NpcDefinitionManager.INSTANCE.warnMissing(definitionId);
            return InteractionResult.FAIL;
        }
        lookTarget = serverPlayer;
        ChatBoxCommandUtil.serverSkipDialogues(serverPlayer, definition.dialogue(), definition.dialogueGroup());
        return InteractionResult.CONSUME;
    }

    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity entity) {}
    @Override public void push(Entity entity) {}
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isPickable() { return true; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean canChangeDimensions() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public Component getName() {
        String key = entityData.get(NAME_KEY);
        return key.isBlank() ? Component.literal(getDefinitionId().toString()) : Component.translatable(key);
    }

    public ResourceLocation getDefinitionId() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(DEFINITION));
        return id == null ? ResourceLocation.parse(DEFAULT_DEFINITION) : id;
    }
    public ResourceLocation getTexture() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(TEXTURE));
        return id == null ? NpcDefinitionManager.missingTexture() : id;
    }
    public boolean isSlim() { return entityData.get(SLIM); }
    public float getAnchorYaw() { return entityData.get(ANCHOR_YAW); }
    public Vec3 getAnchor() { return anchor; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Definition", entityData.get(DEFINITION));
        tag.putString("NameKey", entityData.get(NAME_KEY));
        tag.putString("Texture", entityData.get(TEXTURE));
        tag.putBoolean("Slim", entityData.get(SLIM));
        tag.putString("Dialogue", entityData.get(DIALOGUE));
        tag.putString("DialogueGroup", entityData.get(DIALOGUE_GROUP));
        tag.putDouble("AnchorX", anchor.x); tag.putDouble("AnchorY", anchor.y); tag.putDouble("AnchorZ", anchor.z);
        tag.putFloat("AnchorYaw", getAnchorYaw());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DEFINITION, tag.getString("Definition"));
        entityData.set(NAME_KEY, tag.getString("NameKey"));
        entityData.set(TEXTURE, tag.contains("Texture") ? tag.getString("Texture") : NpcDefinitionManager.missingTexture().toString());
        entityData.set(SLIM, tag.getBoolean("Slim"));
        entityData.set(DIALOGUE, tag.getString("Dialogue"));
        entityData.set(DIALOGUE_GROUP, tag.getString("DialogueGroup"));
        anchor = new Vec3(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ"));
        float yaw = NpcRotation.snapToEightDirections(tag.getFloat("AnchorYaw"));
        entityData.set(ANCHOR_YAW, yaw);
        setAllRotations(yaw);
        checkedDefinitionRevision = Long.MIN_VALUE;
    }
}
