package com.yinfires.icecore.compat.cozycafe.seating;

import com.yinfires.icecore.ICECore;
import io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity;
import io.github.chakyl.cozycafe.blocks.CafeMenuBlock;
import io.github.chakyl.cozycafe.entities.CustomerEntity;
import io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity;
import com.yinfires.icecore.compat.cozycafe.spawn.CozyCafeSpawnRegionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;
import io.github.chakyl.cozycafe.registry.CozyRegistry;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.Deque;

public final class CozyCafeSeatingService {
    public static final TagKey<Block> SEATS = BlockTags.create(ResourceLocation.fromNamespaceAndPath(ICECore.MOD_ID, "cozycafe_seats"));
    private static final Map<SeatKey, Reservation> RESERVATIONS = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_PATHS = 3;
    private static final Map<UUID, Integer> FAILED_PATHS = new ConcurrentHashMap<>();
    private static final Map<ManagerKey, RouteSnapshot> ROUTE_SNAPSHOTS = new ConcurrentHashMap<>();
    private static final Map<ManagerKey, RouteWarmup> ROUTE_WARMUPS = new ConcurrentHashMap<>();
    private static final Map<UUID, NavigationProgress> NAVIGATION = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Set<BlockPos>> MENU_INDEX = new ConcurrentHashMap<>();
    private static final int MAX_ROUTES_PER_MENU = 4;

    private CozyCafeSeatingService() {}

    public static BlockPos seatPosition(CafeMenuBlockEntity menu) {
        BlockState state = menu.getBlockState();
        return menu.getBlockPos().relative(state.getValue(CafeMenuBlock.FACING)).below();
    }

    public static boolean isValidMenu(CafeMenuBlockEntity menu, boolean requireFree) {
        // Structural opening validation must not call canReceiveNewCustomer(): that method is
        // intentionally route-aware and would make an un-warmed route pool look like a bad seat.
        if (!(menu.getLevel() instanceof ServerLevel level)) return false;
        return inspect(level, menu.getBlockPos(), menu.getBlockState(), requireFree) != null;
    }

    /** Variant used by the canReceiveNewCustomer mixin to avoid recursion. */
    public static boolean hasValidSeat(CafeMenuBlockEntity menu, boolean requireFree) {
        if (!(menu.getLevel() instanceof ServerLevel level)) return false;
        if (inspect(level, menu.getBlockPos(), menu.getBlockState(), requireFree) == null) return false;
        if (menu.getCafeManager() == null) return true;
        RouteSnapshot snapshot = ROUTE_SNAPSHOTS.get(new ManagerKey(level.dimension().location(), menu.getCafeManager()));
        if (snapshot == null) return true;
        MenuRoutes routes = snapshot.menus.get(menu.getBlockPos());
        return routes != null && !routes.routes.isEmpty();
    }

    public static boolean hasAnyValidMenu(ServerLevel level, BlockPos first, BlockPos second) {
        return BlockPos.betweenClosedStream(first, second).anyMatch(pos -> {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            return blockEntity instanceof CafeMenuBlockEntity menu && isValidMenu(menu, true);
        });
    }

    public static boolean buildRouteSnapshot(CafeManagerBlockEntity manager, BlockPos first, BlockPos second,
                                             List<BlockPos> entrances) {
        if (!(manager.getLevel() instanceof ServerLevel level)) return false;
        // Opening validation is structural only.  A missing/temporarily unavailable entrance
        // must not turn an otherwise valid menu+seat pair into the "no valid seat" HUD reason.
        // Route warmup handles reachability after the cafe has opened.
        List<BlockPos> safeEntrances = entrances.stream().filter(pos -> isSafeStandingPoint(level, pos)).distinct().toList();
        Map<BlockPos, MenuRoutes> menus = new ConcurrentHashMap<>();
        indexedMenus(level, first, second).forEach(pos -> {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof CafeMenuBlockEntity menu)) return;
            SeatInfo info = inspect(level, menu.getBlockPos(), menu.getBlockState(), true);
            if (info == null) return;
            List<Approach> approaches = approachCandidates(level, menu, info.position);
            // Keep structurally valid menus in the snapshot even when no safe approach or
            // entrance is currently routable.  Reachability is a warmup/dispatch concern and
            // must not turn the structural opening check into a false "no valid seat" result.
            menus.put(menu.getBlockPos().immutable(), new MenuRoutes(info, List.of()));
        });
        ManagerKey key = new ManagerKey(level.dimension().location(), manager.getBlockPos());
        if (menus.isEmpty()) {
            ROUTE_SNAPSHOTS.remove(key);
            return false;
        }
        RouteSnapshot snapshot = new RouteSnapshot(UUID.randomUUID(), menus);
        ROUTE_SNAPSHOTS.put(key, snapshot);
        ROUTE_WARMUPS.put(key, new RouteWarmup(level.dimension().location(), snapshot,
                new ArrayDeque<>(menus.entrySet().stream()
                        .map(entry -> new MenuWarmup(entry.getKey(), entry.getValue().seat,
                                approachCandidates(level, (CafeMenuBlockEntity) level.getBlockEntity(entry.getKey()),
                                        entry.getValue().seat.position), safeEntrances))
                        .toList())));
        return true;
    }

    private static List<BlockPos> indexedMenus(ServerLevel level, BlockPos first, BlockPos second) {
        Set<BlockPos> indexed = MENU_INDEX.get(level.dimension().location());
        if (indexed == null || indexed.isEmpty()) {
            List<BlockPos> fallback = BlockPos.betweenClosedStream(first, second).filter(pos ->
                    level.getBlockEntity(pos) instanceof CafeMenuBlockEntity).map(BlockPos::immutable).toList();
            MENU_INDEX.computeIfAbsent(level.dimension().location(), unused -> ConcurrentHashMap.newKeySet()).addAll(fallback);
            return fallback;
        }
        int minX = Math.min(first.getX(), second.getX()), maxX = Math.max(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY()), maxY = Math.max(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ()), maxZ = Math.max(first.getZ(), second.getZ());
        return indexed.stream().filter(pos -> pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY && pos.getZ() >= minZ && pos.getZ() <= maxZ).toList();
    }

    public static void indexChunk(ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk, boolean add) {
        Set<BlockPos> positions = MENU_INDEX.computeIfAbsent(level.dimension().location(), unused -> ConcurrentHashMap.newKeySet());
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            if (entry.getValue() instanceof CafeMenuBlockEntity) {
                if (add) positions.add(entry.getKey().immutable()); else positions.remove(entry.getKey());
            }
        }
    }

    public static void indexBlock(ServerLevel level, BlockPos pos, BlockState state) {
        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id != null && id.getNamespace().equals("cozycafe") && id.getPath().equals("cafe_menu")) {
            MENU_INDEX.computeIfAbsent(level.dimension().location(), unused -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
        }
    }

    public static void removeIndexedBlock(ServerLevel level, BlockPos pos) {
        Set<BlockPos> positions = MENU_INDEX.get(level.dimension().location());
        if (positions != null) positions.remove(pos);
    }

    /** Performs at most one complete pathfinder invocation per server tick. */
    public static void tickWarmups(MinecraftServer server) {
        for (Map.Entry<ManagerKey, RouteWarmup> entry : List.copyOf(ROUTE_WARMUPS.entrySet())) {
            ServerLevel level = server.getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, entry.getValue().dimension));
            if (level == null || entry.getValue().tick(level)) ROUTE_WARMUPS.remove(entry.getKey(), entry.getValue());
            break;
        }
    }

    public static void clearRouteSnapshot(ServerLevel level, BlockPos manager) {
        ROUTE_SNAPSHOTS.remove(new ManagerKey(level.dimension().location(), manager));
        ROUTE_WARMUPS.remove(new ManagerKey(level.dimension().location(), manager));
    }

    public static boolean hasRouteSnapshot(ServerLevel level, BlockPos manager) {
        return ROUTE_SNAPSHOTS.containsKey(new ManagerKey(level.dimension().location(), manager));
    }

    public static List<BlockPos> defaultEntrance(CafeManagerBlockEntity manager) {
        BlockPos sign = manager.getLinkedSign();
        return sign == null ? List.of() : List.of(sign.below());
    }

    public static BlockPos snapshotEntrance(ServerLevel level, BlockPos manager, net.minecraft.util.RandomSource random) {
        RouteSnapshot snapshot = ROUTE_SNAPSHOTS.get(new ManagerKey(level.dimension().location(), manager));
        if (snapshot == null) return null;
        List<BlockPos> entrances = snapshot.menus.values().stream().flatMap(routes -> routes.routes.stream())
                .map(Route::entrance).distinct().toList();
        return entrances.isEmpty() ? null : entrances.get(random.nextInt(entrances.size()));
    }

    public static BlockPos routeEntrance(CafeMenuBlockEntity menu, net.minecraft.util.RandomSource random) {
        if (!(menu.getLevel() instanceof ServerLevel level) || menu.getCafeManager() == null) return null;
        RouteSnapshot snapshot = ROUTE_SNAPSHOTS.get(new ManagerKey(level.dimension().location(), menu.getCafeManager()));
        MenuRoutes routes = snapshot == null ? null : snapshot.menus.get(menu.getBlockPos());
        if (routes == null || routes.routes.isEmpty()) return null;
        return routes.routes.get(random.nextInt(routes.routes.size())).entrance;
    }

    public static Path cachedPath(CustomerEntity customer) {
        CafeMenuBlockEntity menu = menu(customer);
        if (menu == null || !(menu.getLevel() instanceof ServerLevel level) || menu.getCafeManager() == null) return null;
        RouteSnapshot snapshot = ROUTE_SNAPSHOTS.get(new ManagerKey(level.dimension().location(), menu.getCafeManager()));
        MenuRoutes menuRoutes = snapshot == null ? null : snapshot.menus.get(menu.getBlockPos());
        Reservation reservation = RESERVATIONS.get(new SeatKey(level.dimension().location(), seatPosition(menu)));
        if (menuRoutes == null || reservation == null) return null;
        Route route = menuRoutes.routes.stream().filter(candidate -> candidate.approach.equals(reservation.approach))
                .min(Comparator.comparingDouble(candidate -> candidate.entrance.distSqr(customer.blockPosition())))
                .orElse(null);
        if (route == null || route.nodes.isEmpty()) return null;
        List<Node> nodes = route.nodes.stream().map(pos -> new Node(pos.getX(), pos.getY(), pos.getZ())).toList();
        return new Path(nodes, route.approach, true);
    }

    public static void startNavigation(CustomerEntity customer, double speed) {
        if (trySeat(customer)) return;
        Path path = cachedPath(customer);
        BlockPos target = navigationTarget(customer);
        // A spawned entity may begin a fraction away from the cached entrance block. Reusing a
        // node-zero route in that case causes visible backtracking, so only use it at that entrance.
        if (path != null && path.getNodeCount() > 0) {
            Node first = path.getNode(0);
            if (customer.blockPosition().distSqr(new BlockPos(first.x, first.y, first.z)) > 2.0D) path = null;
        }
        if (path == null && target != null) path = customer.getNavigation().createPath(target, 0);
        if (path != null && customer.getNavigation().moveTo(path, speed)) {
            NAVIGATION.put(customer.getUUID(), new NavigationProgress(customer.position(), customer.tickCount, 0, false));
        } else pathFailed(customer);
    }

    public static void tickNavigation(CustomerEntity customer, double speed) {
        NavigationProgress progress = NAVIGATION.get(customer.getUUID());
        if (progress == null) { startNavigation(customer, speed); return; }
        if (customer.tickCount - progress.checkedAt < 20 && !progress.recoveryRequested) return;
        double moved = customer.position().distanceToSqr(progress.position);
        boolean stuck = customer.getNavigation().isDone() || (customer.tickCount - progress.checkedAt >= 60 && moved < 0.04D);
        if (!stuck && !progress.recoveryRequested) {
            NAVIGATION.put(customer.getUUID(), new NavigationProgress(customer.position(), customer.tickCount,
                    progress.failures, false));
            return;
        }
        BlockPos target = navigationTarget(customer);
        Path path = target == null ? null : customer.getNavigation().createPath(target, 0);
        int failures = path != null && customer.getNavigation().moveTo(path, speed) ? 0 : progress.failures + 1;
        if (failures >= MAX_FAILED_PATHS) { cancelWalking(customer); return; }
        NAVIGATION.put(customer.getUUID(), new NavigationProgress(customer.position(), customer.tickCount, failures, false));
    }

    public static void requestPathRecovery(CustomerEntity customer) {
        NavigationProgress old = NAVIGATION.get(customer.getUUID());
        if (old != null) NAVIGATION.put(customer.getUUID(), new NavigationProgress(old.position, old.checkedAt,
                old.failures, true));
    }

    public static boolean reserve(CafeMenuBlockEntity menu, CustomerEntity customer) {
        ServerLevel level = (ServerLevel) menu.getLevel();
        SeatInfo info = inspect(level, menu.getBlockPos(), menu.getBlockState(), true);
        if (info == null) return false;
        SeatKey key = new SeatKey(level.dimension().location(), info.position);
        BlockPos approach = findApproach(customer, info.position);
        if (approach == null) return false;
        return reserveAt(menu, customer, info, approach);
    }

    private static boolean reserveAt(CafeMenuBlockEntity menu, CustomerEntity customer, SeatInfo info, BlockPos approach) {
        if (!(menu.getLevel() instanceof ServerLevel level)) return false;
        SeatKey key = new SeatKey(level.dimension().location(), info.position);
        Reservation value = new Reservation(menu.getBlockPos(), customer.getUUID(), approach, level.getGameTime());
        Reservation existing = RESERVATIONS.putIfAbsent(key, value);
        if (existing != null && !existing.customer.equals(customer.getUUID())) return false;
        CozyCafeMenuSeatAccess access = (CozyCafeMenuSeatAccess) menu;
        access.icecore$setCustomerUuid(customer.getUUID());
        access.icecore$setSeatPos(info.position);
        access.icecore$setReceptionPos(approach);
        access.icecore$setSeatingStage("pending_spawn");
        menu.setCustomerTravelTime(-1);
        ejectOccupants(level, info.position, customer);
        menu.setChanged();
        return true;
    }

    /** Checks the actual entrance-to-seat route before the async skin lookup is allowed to spawn the customer. */
    public static boolean reserveIfReachable(CafeMenuBlockEntity menu, CustomerEntity customer, BlockPos spawn) {
        if (!(menu.getLevel() instanceof ServerLevel level)) return false;
        SeatInfo info = inspect(level, menu.getBlockPos(), menu.getBlockState(), true);
        if (info == null) return false;
        if (!hasStandingSpace(level, spawn)
                || level.getBlockState(spawn.below()).getCollisionShape(level, spawn.below()).isEmpty()) return false;
        BlockPos approach = snapshotApproach(menu, spawn);
        if (approach == null) return false;
        return approach != null && reserveAt(menu, customer, info, approach);
    }

    public static boolean hasReadyRoute(CafeMenuBlockEntity menu) {
        if (!(menu.getLevel() instanceof ServerLevel level) || menu.getCafeManager() == null) return false;
        RouteSnapshot snapshot = ROUTE_SNAPSHOTS.get(new ManagerKey(level.dimension().location(), menu.getCafeManager()));
        MenuRoutes routes = snapshot == null ? null : snapshot.menus.get(menu.getBlockPos());
        return routes != null && !routes.routes.isEmpty();
    }

    public static BlockPos navigationTarget(CustomerEntity customer) {
        CafeMenuBlockEntity menu = menu(customer);
        if (menu == null || !(menu.getLevel() instanceof ServerLevel level)) return null;
        SeatKey key = new SeatKey(level.dimension().location(), seatPosition(menu));
        Reservation reservation = RESERVATIONS.get(key);
        if (reservation == null || !reservation.customer.equals(customer.getUUID())) return null;
        CozyCafeMenuSeatAccess access = (CozyCafeMenuSeatAccess) menu;
        if ("pending_spawn".equals(access.icecore$getSeatingStage())) {
            access.icecore$setSeatingStage("walking");
            menu.setChanged();
        }
        if (reservation.approach != null) return reservation.approach;
        BlockPos persisted = access.icecore$getReceptionPos();
        if (persisted != null && isAdjacentToSeat(persisted, key.position)) return persisted;
        BlockPos approach = findApproach(customer, key.position);
        if (approach == null) return null;
        RESERVATIONS.replace(key, reservation,
                new Reservation(reservation.menu, reservation.customer, approach, reservation.createdAt));
        return approach;
    }

    public static boolean trySeat(CustomerEntity customer) {
        CafeMenuBlockEntity menu = menu(customer);
        if (menu == null || !(customer.level() instanceof ServerLevel level)) return false;
        SeatInfo info = inspect(level, menu.getBlockPos(), menu.getBlockState(), false);
        CozyCafeMenuSeatAccess access = (CozyCafeMenuSeatAccess) menu;
        if (info == null || !customer.getUUID().equals(access.icecore$getCustomerUuid())) {
            cancelWalking(customer);
            return false;
        }
        BlockPos approach = navigationTarget(customer);
        if (approach == null || !isAtApproach(customer, approach)) return false;
        ejectOccupants(level, info.position, customer);
        CozyCafeSeatAnchorEntity anchor = new CozyCafeSeatAnchorEntity(level, customer.getUUID());
        // LivingEntity contributes -0.35 to vanilla passenger placement. Put the synchronized
        // vehicle position above the desired feet coordinate so both sides resolve to SeatPose.y.
        // SeatInfo.y is already the final rider foot coordinate.  Do not apply the vanilla
        // living-entity passenger offset a second time (Valhelsia's 0.35 is vehicle Y only).
        anchor.moveTo(info.x, info.y, info.z, info.yaw, 0.0F);
        level.addFreshEntity(anchor);
        customer.getNavigation().stop();
        customer.setYRot(info.yaw); customer.setYBodyRot(info.yaw); customer.setYHeadRot(info.yaw);
        if (!customer.startRiding(anchor, true)) {
            anchor.discard();
            return false;
        }
        access.icecore$setSeatingStage("seated");
        customer.setTargetMenuPos(null);
        FAILED_PATHS.remove(customer.getUUID());
        NAVIGATION.remove(customer.getUUID());
        menu.onCustomerArrived(customer);
        menu.setChanged();
        return true;
    }

    public static void pathFailed(CustomerEntity customer) {
        int failures = FAILED_PATHS.merge(customer.getUUID(), 1, Integer::sum);
        if (failures >= MAX_FAILED_PATHS) cancelWalking(customer);
    }

    public static BlockPos menuSidePosition(CafeMenuBlockEntity menu) {
        if (menu == null || !(menu.getLevel() instanceof ServerLevel level)) return null;
        BlockPos counter = menu.getBlockPos().below();
        return Direction.Plane.HORIZONTAL.stream().map(counter::relative)
                .filter(pos -> !pos.equals(seatPosition(menu)))
                .filter(pos -> isSafeStandingPoint(level, pos)).findFirst().orElse(null);
    }

    public static void pathSucceeded(CustomerEntity customer) { FAILED_PATHS.remove(customer.getUUID()); }

    public static void validate(CafeMenuBlockEntity menu) {
        CozyCafeMenuSeatAccess access = (CozyCafeMenuSeatAccess) menu;
        UUID uuid = access.icecore$getCustomerUuid();
        if (uuid == null) {
            if (menu.getHasCustomer()) clearLegacy(menu);
            return;
        }
        if (!(menu.getLevel() instanceof ServerLevel level)) return;
        BlockPos storedSeat = access.icecore$getSeatPos();
        Entity customer = level.getEntity(uuid);
        SeatInfo info = inspect(level, menu.getBlockPos(), menu.getBlockState(), false);
        if (info == null) {
            cancelService(menu);
            return;
        }
        if (storedSeat != null) {
            BlockPos approach = null;
            Reservation current = RESERVATIONS.get(new SeatKey(level.dimension().location(), storedSeat));
            if (current != null && current.customer.equals(uuid)) approach = current.approach;
            if (approach == null) approach = access.icecore$getReceptionPos();
            RESERVATIONS.putIfAbsent(new SeatKey(level.dimension().location(), storedSeat),
                    new Reservation(menu.getBlockPos(), uuid, approach, level.getGameTime()));
        }
        if (customer instanceof CustomerEntity seated && menu.getHasCustomer() && !seated.isPassenger()) {
            cancelService(menu);
        } else if (customer == null && "walking".equals(access.icecore$getSeatingStage())) {
            Reservation reservation = RESERVATIONS.get(new SeatKey(level.dimension().location(), info.position));
            if (reservation != null && level.getGameTime() - reservation.createdAt > 1200L) {
                release(menu);
            }
        }
    }

    public static CustomerEntity boundCustomer(CafeMenuBlockEntity menu) {
        if (!(menu.getLevel() instanceof ServerLevel level)) return null;
        UUID uuid = ((CozyCafeMenuSeatAccess) menu).icecore$getCustomerUuid();
        Entity entity = uuid == null ? null : level.getEntity(uuid);
        return entity instanceof CustomerEntity customer ? customer : null;
    }

    public static void release(CafeMenuBlockEntity menu) {
        CozyCafeMenuSeatAccess access = (CozyCafeMenuSeatAccess) menu;
        if (menu.getLevel() instanceof ServerLevel level && access.icecore$getSeatPos() != null) {
            RESERVATIONS.remove(new SeatKey(level.dimension().location(), access.icecore$getSeatPos()));
        }
        UUID uuid = access.icecore$getCustomerUuid();
        if (uuid != null) FAILED_PATHS.remove(uuid);
        access.icecore$setCustomerUuid(null);
        access.icecore$setSeatPos(null);
        access.icecore$setReceptionPos(null);
        access.icecore$setSeatingStage("idle");
        menu.setChanged();
    }

    public static boolean isReserved(ServerLevel level, BlockPos seat) {
        return RESERVATIONS.containsKey(new SeatKey(level.dimension().location(), seat));
    }

    public static boolean isReservedInteraction(ServerLevel level, BlockPos pos) {
        if (isReserved(level, pos)) return true;
        BlockState state = level.getBlockState(pos);
        return enumPropertyEquals(state, "half", "upper") && isReserved(level, pos.below());
    }

    public static void clearRuntimeState() {
        RESERVATIONS.clear();
        FAILED_PATHS.clear();
        ROUTE_SNAPSHOTS.clear();
        ROUTE_WARMUPS.clear();
        NAVIGATION.clear();
        MENU_INDEX.clear();
    }

    private static void cancelWalking(CustomerEntity customer) {
        CafeMenuBlockEntity menu = menu(customer);
        BlockPos exit = menu == null ? null : exitPosition(menu);
        if (menu != null) {
            release(menu);
        }
        customer.setTargetMenuPos(null);
        customer.getNavigation().stop();
        if (exit != null) customer.setTargetSignPos(exit);
        else customer.discard();
    }

    private static CafeMenuBlockEntity menu(CustomerEntity customer) {
        BlockPos pos = customer.getTargetMenuPos();
        if (pos == null) {
            if (!(customer.level() instanceof ServerLevel level)) return null;
            for (Reservation reservation : RESERVATIONS.values()) {
                if (reservation.customer.equals(customer.getUUID())) {
                    BlockEntity be = level.getBlockEntity(reservation.menu);
                    return be instanceof CafeMenuBlockEntity menu ? menu : null;
                }
            }
            return null;
        }
        BlockEntity be = customer.level().getBlockEntity(pos);
        return be instanceof CafeMenuBlockEntity menu ? menu : null;
    }

    private static SeatInfo inspect(ServerLevel level, BlockPos menuPos, BlockState menuState, boolean requireFree) {
        Direction menuFacing = menuState.getValue(CafeMenuBlock.FACING);
        BlockPos seatPos = menuPos.relative(menuFacing).below();
        if (!level.isLoaded(seatPos) || !level.isLoaded(seatPos.above())) return null;
        BlockState seatState = level.getBlockState(seatPos);
        Block block = seatState.getBlock();
        if (!isSupportedSeat(seatState, block)) return null;
        if (!isActuallySittable(block, seatState)) return null;
        Direction seatFacing = riderFacing(block, seatState);
        if (seatFacing != null && seatFacing != menuFacing.getOpposite()) return null;
        BlockPos approach = seatPos.above();
        if (!seatHeadroomClear(level, block, seatState, approach)) return null;
        SeatKey key = new SeatKey(level.dimension().location(), seatPos);
        if (requireFree && RESERVATIONS.containsKey(key)) return null;
        Direction facing = seatFacing == null ? menuFacing.getOpposite() : seatFacing;
        SeatPose pose = seatPose(block, seatState, seatPos, facing.toYRot());
        return new SeatInfo(seatPos, pose.x, pose.y, pose.z, pose.yaw);
    }

    private static boolean isSupportedSeat(BlockState state, Block block) {
        if (state.is(SEATS)) return true;
        String namespace = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block).getNamespace();
        return switch (namespace) {
            case "another_furniture" -> hasType(block.getClass(), "com.starfish_studios.another_furniture.block.SeatBlock");
            case "furniture" -> hasType(block.getClass(), "com.berksire.furniture.block.DeskChairBlock")
                    || hasType(block.getClass(), "com.berksire.furniture.block.BenchBlock")
                    || hasType(block.getClass(), "com.berksire.furniture.block.SofaBlock");
            case "handcrafted" -> hasType(block.getClass(), "earth.terrarium.handcrafted.common.blocks.base.SittableBlock");
            case "refurbished_furniture" -> hasType(block.getClass(), "com.mrcrayfish.furniture.refurbished.block.ChairBlock")
                    || hasType(block.getClass(), "com.mrcrayfish.furniture.refurbished.block.StoolBlock")
                    || hasType(block.getClass(), "com.mrcrayfish.furniture.refurbished.block.SofaBlock");
            case "pfm" -> hasType(block.getClass(), "com.unlikepaladin.pfm.blocks.BasicChairBlock")
                    || hasType(block.getClass(), "com.unlikepaladin.pfm.blocks.ArmChairBlock")
                    || hasType(block.getClass(), "com.unlikepaladin.pfm.blocks.DinnerChairBlock")
                    || hasType(block.getClass(), "com.unlikepaladin.pfm.blocks.FroggyChairBlock");
            case "valhelsia_furniture" -> hasType(block.getClass(), "net.valhelsia.valhelsia_furniture.common.block.SeatableBlock");
            default -> false;
        };
    }

    private static boolean hasType(Class<?> type, String name) {
        if (type == null) return false;
        if (type.getName().equals(name)) return true;
        for (Class<?> iface : type.getInterfaces()) if (hasType(iface, name)) return true;
        return hasType(type.getSuperclass(), name);
    }

    private static Direction horizontalFacing(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof DirectionProperty direction && property.getName().equals("facing")) {
                Direction value = state.getValue(direction);
                return value.getAxis().isHorizontal() ? value : null;
            }
        }
        return null;
    }

    /** Converts each supported mod's block-facing convention to the direction its rider faces. */
    private static Direction riderFacing(Block block, BlockState state) {
        Direction facing = horizontalFacing(state);
        if (facing == null) return null;
        String namespace = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block).getNamespace();
        return switch (namespace) {
            case "pfm", "refurbished_furniture" -> facing.getOpposite();
            // Another Furniture and Handcrafted pass block facing to their seat entity. Let's Do and
            // Valhelsia do not clamp riders, so their visible chair-facing convention is authoritative.
            default -> facing;
        };
    }

    private static boolean isActuallySittable(Block block, BlockState state) {
        String namespace = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block).getNamespace();
        if ("furniture".equals(namespace) && hasType(block.getClass(), "com.berksire.furniture.block.DeskChairBlock")
                && !enumPropertyEquals(state, "half", "lower")) return false;
        if (("another_furniture".equals(namespace) || "refurbished_furniture".equals(namespace)
                || "pfm".equals(namespace)) && booleanProperty(state, "tucked")) return false;
        if ("another_furniture".equals(namespace)) {
            try {
                Method method = block.getClass().getMethod("isSittable", BlockState.class);
                return Boolean.TRUE.equals(method.invoke(block, state));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }
        return true;
    }

    private static boolean seatHeadroomClear(ServerLevel level, Block block, BlockState seatState, BlockPos feet) {
        BlockState feetState = level.getBlockState(feet);
        boolean letsDoUpper = hasType(block.getClass(), "com.berksire.furniture.block.DeskChairBlock")
                && enumPropertyEquals(seatState, "half", "lower")
                && feetState.getBlock() == block
                && enumPropertyEquals(feetState, "half", "upper");
        return (letsDoUpper || feetState.getCollisionShape(level, feet).isEmpty())
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    private static boolean booleanProperty(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name) && property.getValueClass() == Boolean.class) {
                @SuppressWarnings("unchecked") Property<Boolean> booleanProperty = (Property<Boolean>) property;
                return state.getValue(booleanProperty);
            }
        }
        return false;
    }

    private static boolean enumPropertyEquals(BlockState state, String name, String expected) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof EnumProperty<?> enumProperty && property.getName().equals(name)) {
                return enumValueName(state, enumProperty).equals(expected);
            }
        }
        return false;
    }

    private static <T extends Enum<T> & net.minecraft.util.StringRepresentable> String enumValueName(
            BlockState state, EnumProperty<T> property) {
        return property.getName(state.getValue(property));
    }

    private static SeatPose seatPose(Block block, BlockState state, BlockPos pos, float yaw) {
        String namespace = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block).getNamespace();
        double vehicleY = pos.getY();
        double ridingOffset = 0.5D;
        try {
            Method method = block.getClass().getMethod("seatHeight", BlockState.class);
            vehicleY += 0.001D;
            ridingOffset = ((Number) method.invoke(block, state)).doubleValue();
            return finalRiderPose(pos, vehicleY + ridingOffset, yaw);
        } catch (ReflectiveOperationException | RuntimeException ignored) {}
        if ("handcrafted".equals(namespace)) {
            try {
                Method method = block.getClass().getMethod("getSeatSize", BlockState.class);
                AABB seat = (AABB) method.invoke(block, state);
                ridingOffset = seat.getYsize() * 0.75D;
            } catch (ReflectiveOperationException | RuntimeException ignored) {}
        }
        if ("valhelsia_furniture".equals(namespace)) {
            try {
                Method method = block.getClass().getMethod("getRidingOffset");
                vehicleY += ((Number) method.invoke(block)).doubleValue();
                ridingOffset = 0.0D;
            } catch (ReflectiveOperationException | RuntimeException ignored) {}
        }
        if ("refurbished_furniture".equals(namespace)) {
            vehicleY += hasType(block.getClass(), "com.mrcrayfish.furniture.refurbished.block.StoolBlock") ? 0.2D : 0.375D;
            ridingOffset = 0.0D;
        }
        if ("furniture".equals(namespace)) {
            vehicleY += hasType(block.getClass(), "com.berksire.furniture.block.BenchBlock") ? 0.15D : 0.25D;
            ridingOffset = 0.0D;
        }
        return finalRiderPose(pos, vehicleY + ridingOffset, yaw);
    }

    /**
     * Forge's LivingEntity passenger placement contributes the player's riding offset (about
     * -0.35 blocks).  The anchor bypasses vanilla placement, so apply that offset exactly once
     * here to obtain the same final feet coordinate a player receives when mounting the real
     * furniture seat.  Furniture-specific values above describe the vehicle's world Y only.
     */
    private static SeatPose finalRiderPose(BlockPos pos, double vehicleY, float yaw) {
        return new SeatPose(pos.getX() + 0.5D, vehicleY - 0.35D, pos.getZ() + 0.5D, yaw);
    }

    private static BlockPos findApproach(CustomerEntity customer, BlockPos seat) {
        CafeMenuBlockEntity menu = menu(customer);
        if (menu == null || !(customer.level() instanceof ServerLevel level)) return null;
        List<BlockPos> candidates = approachCandidates(level, menu, seat).stream()
                .map(Approach::position)
                .sorted(java.util.Comparator.comparingDouble((BlockPos pos) -> customer.distanceToSqr(pos.getCenter())))
                .toList();
        for (BlockPos candidate : candidates) {
            Path path = customer.getNavigation().createPath(candidate, 0);
            if (path != null && path.canReach()) return candidate.immutable();
        }
        return null;
    }

    private static BlockPos findApproachFrom(ServerLevel level, BlockPos spawn, CafeMenuBlockEntity menu, BlockPos seat) {
        List<BlockPos> candidates = new ArrayList<>(approachCandidates(level, menu, seat).stream().map(Approach::position).toList());
        candidates.sort(java.util.Comparator.comparingDouble((BlockPos pos) -> pos.distSqr(spawn)));
        // Do not touch an entity navigation object before the entity is in the world.
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static boolean hasStandingSpace(Level level, BlockPos feet) {
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    private static boolean isSafeStandingPoint(Level level, BlockPos feet) {
        return level.hasChunkAt(feet) && hasStandingSpace(level, feet)
                && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty();
    }

    private static List<Approach> approachCandidates(ServerLevel level, CafeMenuBlockEntity menu, BlockPos seat) {
        List<Approach> result = new ArrayList<>(12);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            addApproach(level, result, seat.relative(direction), ApproachType.ORTHOGONAL);
        }
        int[] offsets = {-1, 1};
        for (int x : offsets) for (int z : offsets) {
            addApproach(level, result, seat.offset(x, 0, z), ApproachType.DIAGONAL);
        }
        return List.copyOf(result);
    }

    private static void addApproach(ServerLevel level, List<Approach> result, BlockPos pos, ApproachType type) {
        if (!isSafeStandingPoint(level, pos)) return;
        if (result.stream().noneMatch(existing -> existing.position.equals(pos))) result.add(new Approach(pos.immutable(), type));
    }

    private static List<Route> selectRoutes(ServerLevel level, List<BlockPos> entrances, List<Approach> approaches) {
        List<Route> candidates = new ArrayList<>();
        for (BlockPos entrance : entrances) {
            CustomerEntity probe = CozyRegistry.EntityRegistry.CUSTOMER.get().create(level);
            if (probe == null) continue;
            probe.moveTo(entrance.getX() + 0.5D, entrance.getY(), entrance.getZ() + 0.5D, 0.0F, 0.0F);
            // GroundPathNavigation refuses to create a path for a newly-created entity until its
            // first world tick establishes this state. The opening probe is intentionally never
            // added to the world, so mirror the already-validated entrance floor explicitly.
            probe.setOnGround(true);
            for (Approach approach : approaches) {
                Path path = probe.getNavigation().createPath(approach.position, 0);
                if (path == null || !path.canReach()) continue;
                List<BlockPos> nodes = new ArrayList<>(path.getNodeCount());
                for (int index = 0; index < path.getNodeCount(); index++) {
                    Node node = path.getNode(index);
                    nodes.add(new BlockPos(node.x, node.y, node.z));
                }
                candidates.add(new Route(entrance.immutable(), approach.position, approach.type, List.copyOf(nodes)));
            }
        }
        candidates.sort(Comparator.comparingInt(route -> route.type.ordinal()));
        List<Route> selected = new ArrayList<>(MAX_ROUTES_PER_MENU);
        Set<BlockPos> usedEntrances = new LinkedHashSet<>();
        for (Route route : candidates) {
            if (selected.size() >= MAX_ROUTES_PER_MENU) break;
            if (usedEntrances.add(route.entrance)) selected.add(route);
        }
        for (Route route : candidates) {
            if (selected.size() >= MAX_ROUTES_PER_MENU) break;
            if (!selected.contains(route)) selected.add(route);
        }
        return List.copyOf(selected);
    }

    private static BlockPos snapshotApproach(CafeMenuBlockEntity menu, BlockPos spawn) {
        if (!(menu.getLevel() instanceof ServerLevel level) || menu.getCafeManager() == null) return null;
        RouteSnapshot snapshot = ROUTE_SNAPSHOTS.get(new ManagerKey(level.dimension().location(), menu.getCafeManager()));
        MenuRoutes menuRoutes = snapshot == null ? null : snapshot.menus.get(menu.getBlockPos());
        if (menuRoutes == null) return null;
        return menuRoutes.routes.stream().filter(route -> route.entrance.equals(spawn))
                .findFirst().or(() -> menuRoutes.routes.stream().findFirst()).map(Route::approach).orElse(null);
    }

    private static boolean isAtApproach(CustomerEntity customer, BlockPos approach) {
        double dx = customer.getX() - (approach.getX() + 0.5D);
        double dz = customer.getZ() - (approach.getZ() + 0.5D);
        return dx * dx + dz * dz <= 1.0D && Math.abs(customer.getY() - approach.getY()) <= 0.75D;
    }

    private static void ejectOccupants(ServerLevel level, BlockPos seat, CustomerEntity keep) {
        for (Entity entity : level.getEntities(null, new AABB(seat).inflate(0.15D))) {
            if (entity == keep || entity.getPassengers().contains(keep)) continue;
            if (!entity.getPassengers().isEmpty()) entity.ejectPassengers();
            if (isFurnitureSeatEntity(entity.getClass())) entity.discard();
        }
    }

    private static boolean isFurnitureSeatEntity(Class<?> type) {
        return hasType(type, "com.starfish_studios.another_furniture.entity.SeatEntity")
                || hasType(type, "earth.terrarium.handcrafted.common.entities.Seat")
                || hasType(type, "com.berksire.furniture.client.entity.ChairEntity")
                || hasType(type, "com.mrcrayfish.furniture.refurbished.entity.Seat")
                || hasType(type, "com.unlikepaladin.pfm.entity.ChairEntity")
                || hasType(type, "net.valhelsia.valhelsia_furniture.common.entity.SeatEntity");
    }

    private static void clearLegacy(CafeMenuBlockEntity menu) { closeWithoutPenalty(menu); }
    public static void closeWithoutPenalty(CafeMenuBlockEntity menu) { menu.closeMenu(true); }

    public static void prepareClose(CafeMenuBlockEntity menu) {
        if (menu.getHasCustomer()) return;
        CustomerEntity customer = boundCustomer(menu);
        if (customer == null) return;
        BlockPos exit = exitPosition(menu);
        customer.setTargetMenuPos(null);
        customer.getNavigation().stop();
        if (exit != null) customer.setTargetSignPos(exit);
        else customer.discard();
    }

    public static void prepareDeparture(CafeMenuBlockEntity menu, CustomerEntity customer, BlockPos target) {
        BlockPos reception = ((CozyCafeMenuSeatAccess) menu).icecore$getReceptionPos();
        customer.stopRiding();
        if (reception != null && isSafeStandingPoint(customer.level(), reception)) {
            customer.moveTo(reception.getX() + 0.5D, reception.getY(), reception.getZ() + 0.5D,
                    customer.getYRot(), customer.getXRot());
        }
        customer.setTargetMenuPos(null);
        customer.setTargetSignPos(target);
        Path path = customer.getNavigation().createPath(target, 0);
        if (path != null) customer.getNavigation().moveTo(path, 0.9D);
    }

    private static boolean isAdjacentToSeat(BlockPos approach, BlockPos seat) {
        int dx = Math.abs(approach.getX() - seat.getX());
        int dz = Math.abs(approach.getZ() - seat.getZ());
        return approach.getY() == seat.getY() && dx <= 1 && dz <= 1 && dx + dz > 0;
    }

    private static void cancelService(CafeMenuBlockEntity menu) {
        if (menu.getHasCustomer()) closeWithoutPenalty(menu);
        else {
            prepareClose(menu);
            release(menu);
        }
    }

    private static BlockPos exitPosition(CafeMenuBlockEntity menu) {
        if (!(menu.getLevel() instanceof ServerLevel level) || menu.getCafeManager() == null) return null;
        BlockEntity managerEntity = level.getBlockEntity(menu.getCafeManager());
        if (!(managerEntity instanceof CafeManagerBlockEntity manager)) return null;
        BlockPos sign = CozyCafeSpawnRegionService.exitSignPosition(level, manager.getBlockPos(), manager.getLinkedSign());
        return sign == null ? null : sign.below();
    }

    private record SeatInfo(BlockPos position, double x, double y, double z, float yaw) {}
    private record SeatPose(double x, double y, double z, float yaw) {}
    private record SeatKey(ResourceLocation dimension, BlockPos position) {}
    private record Reservation(BlockPos menu, UUID customer, BlockPos approach, long createdAt) {}
    private record ManagerKey(ResourceLocation dimension, BlockPos position) {}
    private record RouteSnapshot(UUID cycle, Map<BlockPos, MenuRoutes> menus) {}
    private static final class MenuRoutes {
        private final SeatInfo seat;
        private volatile List<Route> routes;
        private MenuRoutes(SeatInfo seat, List<Route> routes) { this.seat = seat; this.routes = routes; }
    }
    private record Route(BlockPos entrance, BlockPos approach, ApproachType type, List<BlockPos> nodes) {}
    private record Approach(BlockPos position, ApproachType type) {}
    private enum ApproachType { ORTHOGONAL, DIAGONAL }
    private record NavigationProgress(net.minecraft.world.phys.Vec3 position, int checkedAt, int failures,
                                      boolean recoveryRequested) {}

    private static final class RouteWarmup {
        private final ResourceLocation dimension;
        private final RouteSnapshot snapshot;
        private final Deque<MenuWarmup> menus;
        private RouteWarmup(ResourceLocation dimension, RouteSnapshot snapshot, Deque<MenuWarmup> menus) {
            this.dimension = dimension; this.snapshot = snapshot; this.menus = menus;
        }
        private boolean tick(ServerLevel level) {
            if (menus.isEmpty()) return true;
            // Round-robin menus so one large/blocked menu cannot delay the first route of every
            // other menu.  A completed first route is published by MenuWarmup.tick immediately,
            // allowing customer generation while remaining candidates continue warming.
            int count = menus.size();
            for (int index = 0; index < count; index++) {
                MenuWarmup current = menus.removeFirst();
                boolean done = current.tick(level, snapshot);
                if (!done) menus.addLast(current);
                if (done && menus.isEmpty()) return true;
                // One A* invocation per server tick remains the hard budget; the loop is written
                // for fairness and exits after the single task above.
                break;
            }
            return menus.isEmpty();
        }
    }

    private static final class MenuWarmup {
        private final BlockPos menu;
        private final SeatInfo seat;
        private final List<Approach> approaches;
        private final List<BlockPos> entrances;
        private int approachIndex;
        private int entranceIndex;
        private Approach selected;
        private Route pendingInbound;
        private final List<Route> routes = new ArrayList<>();
        private MenuWarmup(BlockPos menu, SeatInfo seat, List<Approach> approaches, List<BlockPos> entrances) {
            this.menu = menu; this.seat = seat; this.approaches = approaches; this.entrances = entrances;
        }
        private boolean tick(ServerLevel level, RouteSnapshot snapshot) {
            if (entrances.isEmpty() || approaches.isEmpty()) return true;
            if (pendingInbound != null) {
                CustomerEntity reverseProbe = CozyRegistry.EntityRegistry.CUSTOMER.get().create(level);
                boolean reversible = false;
                if (reverseProbe != null) {
                    reverseProbe.moveTo(pendingInbound.approach.getX() + 0.5D, pendingInbound.approach.getY(),
                            pendingInbound.approach.getZ() + 0.5D, 0.0F, 0.0F);
                    reverseProbe.setOnGround(true);
                    Path reverse = reverseProbe.getNavigation().createPath(pendingInbound.entrance, 0);
                    reversible = reverse != null && reverse.canReach();
                }
                if (reversible) {
                    selected = new Approach(pendingInbound.approach, pendingInbound.type);
                    routes.add(pendingInbound);
                    MenuRoutes pool = snapshot.menus.get(menu);
                    if (pool != null) pool.routes = List.copyOf(routes);
                }
                pendingInbound = null;
                entranceIndex++;
                if (routes.size() >= MAX_ROUTES_PER_MENU) return true;
                if (entranceIndex >= entrances.size()) {
                    if (selected != null) return true;
                    entranceIndex = 0; approachIndex++;
                }
                return approachIndex >= approaches.size();
            }
            Approach approach = selected == null ? approaches.get(approachIndex) : selected;
            BlockPos entrance = entrances.get(entranceIndex);
            CustomerEntity probe = CozyRegistry.EntityRegistry.CUSTOMER.get().create(level);
            if (probe != null) {
                probe.moveTo(entrance.getX() + 0.5D, entrance.getY(), entrance.getZ() + 0.5D, 0.0F, 0.0F);
                probe.setOnGround(true);
                Path path = probe.getNavigation().createPath(approach.position, 0);
                if (path != null && path.canReach()) {
                    List<BlockPos> nodes = new ArrayList<>(path.getNodeCount());
                    for (int i = 0; i < path.getNodeCount(); i++) {
                        Node node = path.getNode(i); nodes.add(new BlockPos(node.x, node.y, node.z));
                    }
                    pendingInbound = new Route(entrance, approach.position, approach.type, List.copyOf(nodes));
                    return false;
                }
            }
            entranceIndex++;
            if (routes.size() >= MAX_ROUTES_PER_MENU) return true;
            if (entranceIndex >= entrances.size()) {
                if (selected != null) return true;
                entranceIndex = 0; approachIndex++;
                if (approachIndex >= approaches.size()) return true;
            }
            return false;
        }
    }
}
