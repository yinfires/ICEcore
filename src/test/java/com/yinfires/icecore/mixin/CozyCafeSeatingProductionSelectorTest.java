package com.yinfires.icecore.mixin;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CozyCafeSeatingProductionSelectorTest {
    private static String source(String relative) throws Exception {
        return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
    }

    @Test void productionHooksCoverTheFullCustomerLifecycle() throws Exception {
        String menu = source("src/main/java/com/yinfires/icecore/mixin/CozyCafeMenuSeatingMixin.java");
        assertTrue(menu.contains("canReceiveNewCustomer()Z"));
        assertTrue(menu.contains("onCustomerArrived(Lnet/minecraft/world/entity/PathfinderMob;)V"));
        assertTrue(menu.contains("closeMenu(Z)V"));
        assertTrue(menu.contains("m_183515_(Lnet/minecraft/nbt/CompoundTag;)V"));
        assertTrue(menu.contains("m_142466_(Lnet/minecraft/nbt/CompoundTag;)V"));
        assertTrue(menu.contains("icecore$pauseWaitUntilRequestExists"));
        assertTrue(menu.contains("requestedItem.isEmpty()"));

        String manager = source("src/main/java/com/yinfires/icecore/mixin/CozyCafeManagerSeatingMixin.java");
        assertTrue(manager.contains("canBeOpened(Lnet/minecraft/server/level/ServerPlayer;)Z"));
        assertTrue(manager.contains("assignCustomersInArea(Lnet/minecraft/world/level/Level;"));
        assertTrue(manager.contains("routeEntrance(menu, serverLevel.random)"));
        assertTrue(manager.contains("reserveIfReachable(menu, customer, routeSpawn)"));
        assertTrue(manager.contains("restoreRoutesAfterLoad"));
        assertTrue(manager.contains("ClientBoundCafeCannotOpenPacket((byte) 5)"));
        assertTrue(!manager.contains("PlayerFeedback"));

        String hud = source("src/main/java/com/yinfires/icecore/mixin/CozyCafeManagerScreenErrorMixin.java");
        assertTrue(hud.contains("code.byteValue() == 5"));
        assertTrue(hud.contains("errorDisplayTicks = 240"));

        String renderer = source("src/main/java/com/yinfires/icecore/mixin/CozyCafeMenuRendererSeatingMixin.java");
        assertTrue(renderer.contains("CafeMenuBlockEntity;getHasCustomer()Z"));
    }

    @Test void seatValidationUsesExactGeometryAndReachability() throws Exception {
        String service = source("src/main/java/com/yinfires/icecore/compat/cozycafe/seating/CozyCafeSeatingService.java");
        assertTrue(service.contains("menuPos.relative(menuFacing).below()"));
        assertTrue(service.contains("seatFacing != null && seatFacing != menuFacing.getOpposite()"));
        assertTrue(service.contains("case \"pfm\", \"refurbished_furniture\" -> facing.getOpposite()"));
        assertTrue(service.contains("enumPropertyEquals(state, \"half\", \"lower\")"));
        assertTrue(service.contains("com.berksire.furniture.block.SofaBlock"));
        assertTrue(service.contains("com.unlikepaladin.pfm.blocks.BasicChairBlock"));
        assertTrue(service.contains("booleanProperty(state, \"tucked\")"));
        assertTrue(service.contains("seatHeadroomClear(level, block, seatState, approach)"));
        assertTrue(service.contains("enumPropertyEquals(feetState, \"half\", \"upper\")"));
        assertTrue(service.contains("seat.relative(direction), ApproachType.ORTHOGONAL"));
        assertTrue(service.contains("seat.offset(x, 0, z), ApproachType.DIAGONAL"));
        assertTrue(!service.contains("seat.relative(direction).above()"));
        assertTrue(service.contains("getBlockState(feet.above()).getCollisionShape"));
        assertTrue(service.contains("customer.getNavigation().createPath(candidate, 0)"));
        assertTrue(service.contains("path != null && path.canReach()"));
        assertTrue(service.contains(".sorted(java.util.Comparator.comparingDouble((BlockPos pos) -> customer.distanceToSqr(pos.getCenter())))"));
        assertTrue(!service.matches("(?s).*\\.toList\\(\\);\\s*candidates\\.sort\\(java\\.util\\.Comparator\\.comparingDouble\\(\\(BlockPos pos\\) -> customer\\.distanceToSqr.*"));
        assertTrue(service.contains("buildRouteSnapshot"));
        assertTrue(service.contains("MAX_ROUTES_PER_MENU = 4"));
        assertTrue(service.contains("probe.setOnGround(true)"));
        assertTrue(service.contains("pending_spawn"));
        assertTrue(service.contains("hasStandingSpace(level, spawn)"));
        assertTrue(service.contains("snapshotApproach(menu, spawn)"));
        assertTrue(service.contains("Do not touch an entity navigation object before the entity is in the world"));
        assertTrue(service.contains("getBlockState(spawn.below()).getCollisionShape"));
        assertTrue(service.contains("getBlockState(feet.above()).getCollisionShape"));
        assertTrue(!service.contains("findApproach(walking, storedSeat)"));
        assertTrue(service.contains("reservation.approach != null"));
        assertTrue(service.contains("isFurnitureSeatEntity(entity.getClass())"));
        assertTrue(service.contains("tickWarmups(MinecraftServer server)"));
        assertTrue(service.contains("for (Map.Entry<ManagerKey, RouteWarmup> entry"));
        assertTrue(service.contains("Round-robin menus"));
        assertTrue(service.contains("finalRiderPose"));
        assertTrue(service.contains("vehicleY - 0.35D"));
        assertTrue(!service.contains("anchor.moveTo(info.x, info.y + 0.35D"));
        assertTrue(service.contains("MAX_FAILED_PATHS = 3"));
        assertTrue(!service.contains("ApproachType.MENU_SIDE"));
        assertTrue(!service.contains("getField(\"height\")"));
        assertTrue(service.contains("icecore$setReceptionPos(approach)"));
        assertTrue(service.contains("#icecore") == false); // tag is a typed key, not a name heuristic

        String navigation = source("src/main/java/com/yinfires/icecore/mixin/CozyCafeNavigateToMenuMixin.java");
        assertTrue(navigation.contains("startNavigation(customer, speed)"));
        assertTrue(navigation.contains("tickNavigation(customer, speed)"));
        assertTrue(!navigation.contains("10 + customer.getRandom()"));

        String collision = source("src/main/java/com/yinfires/icecore/mixin/CozyCafeCustomerCollisionMixin.java");
        assertTrue(collision.contains("m_6094_()Z"));
        assertTrue(collision.contains("m_5829_()Z"));
        assertTrue(collision.contains("m_6087_()Z"));
        assertTrue(collision.contains("callback.setReturnValue(true)"));
    }
}
