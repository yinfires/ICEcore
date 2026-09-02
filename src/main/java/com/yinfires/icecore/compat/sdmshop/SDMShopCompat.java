package com.yinfires.icecore.compat.sdmshop;

import com.yinfires.icecore.currency.CurrencyService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/** Optional bridge between SDM Shop Rework's economy callbacks and ICEcore's shared balance. */
public final class SDMShopCompat {
    private static final String BASIC_MONEY = "basic_money";
    private static Method currencySetMoney;
    private static boolean currencyLookupAttempted;

    /**
     * SDM's client shop is a singleton whose UUID is permanently {@code ShopBase.SHOP_UUID}, and the client's
     * tab refetch ({@code SendGetTabsC2S}) always resolves that UUID against {@code MultiShop.SHOP_MAP} on the
     * server. So every ICEcore shop must be registered in {@code MultiShop} under that fixed UUID while open.
     */
    private static java.util.UUID sharedShopUuid;
    private static boolean sharedShopUuidLookupAttempted;

    /** The entry whose snapshot must receive edits made while the shop is open. */
    private static SDMShopManager activeManager;
    private static java.util.UUID activeEntryUuid;

    private SDMShopCompat() {
    }

    public static boolean isLoaded() {
        return net.minecraftforge.fml.ModList.get().isLoaded("sdmshoprework");
    }

    /** Records which ICEcore entry an about-to-open shop belongs to, so edits can be written back. */
    public static void markActive(SDMShopManager manager, java.util.UUID entryUuid) {
        activeManager = manager;
        activeEntryUuid = entryUuid;
    }

    public static boolean openShop(ServerPlayer player, java.util.UUID uuid, net.minecraft.nbt.CompoundTag snapshot, boolean edit) {
        if (!isLoaded()) return false;
        try {
            Class<?> base = Class.forName("net.sixik.sdmshoprework.common.shop.ShopBase");
            java.util.UUID sharedUuid = sharedShopUuid();
            Object shop = base.getConstructor(java.util.UUID.class).newInstance(sharedUuid);
            if (snapshot != null) base.getMethod("deserializeNBT", net.minecraft.nbt.CompoundTag.class).invoke(shop, snapshot.copy());
            // The client always requests the shared UUID; force it so lookups resolve regardless of snapshot data.
            base.getField("shopUUID").set(shop, sharedUuid);
            registerInMultiShop(sharedUuid, shop);
            base.getField("SERVER").set(null, shop);
            // Push the full shop to the client synchronously (in packet order) instead of SDM's clear-then-async
            // refetch. This guarantees ShopBase.CLIENT holds tabs+entries when the screen's onConstruct runs, so
            // the first unlocked tab is auto-selected — otherwise ModernShopScreen.setSelectedTab's null-guard
            // leaves selectedTab null and no tab can ever be clicked/selected.
            net.minecraft.nbt.CompoundTag full = (net.minecraft.nbt.CompoundTag) base.getMethod("serializeNBT").invoke(shop);
            Class<?> sync = Class.forName("net.sixik.sdmshoprework.network.client.SyncShopS2C");
            Object syncPacket = sync.getConstructor(net.minecraft.nbt.CompoundTag.class).newInstance(full);
            sync.getMethod("sendTo", ServerPlayer.class).invoke(syncPacket, player);
            if (edit) {
                Class<?> r = Class.forName("net.sixik.sdmshoprework.SDMShopR");
                r.getMethod("setEditMode", ServerPlayer.class, boolean.class).invoke(null, player, true);
            }
            Class<?> msg = Class.forName("net.sixik.sdmshoprework.network.server.misc.SendOpenShopScreenS2C");
            Object packet = msg.getConstructor().newInstance();
            msg.getMethod("sendTo", ServerPlayer.class).invoke(packet, player);
            return true;
        } catch (ReflectiveOperationException e) { return false; }
    }

    @SuppressWarnings("unchecked")
    private static void registerInMultiShop(java.util.UUID uuid, Object shop) throws ReflectiveOperationException {
        Class<?> multiShop = Class.forName("net.sixik.sdmshoprework.common.shop.MultiShop");
        var uuids = (java.util.List<java.util.UUID>) multiShop.getField("SHOP_UUIDS").get(null);
        var map = (java.util.Map<java.util.UUID, Object>) multiShop.getField("SHOP_MAP").get(null);
        uuids.removeIf(uuid::equals);
        uuids.add(uuid);
        map.put(uuid, shop);
    }

    private static java.util.UUID sharedShopUuid() throws ReflectiveOperationException {
        if (!sharedShopUuidLookupAttempted) {
            sharedShopUuidLookupAttempted = true;
            Class<?> base = Class.forName("net.sixik.sdmshoprework.common.shop.ShopBase");
            sharedShopUuid = (java.util.UUID) base.getField("SHOP_UUID").get(null);
        }
        return sharedShopUuid;
    }

    /** Invoked from {@code ShopBase.saveShopToFile} so edits persist into the active ICEcore snapshot. */
    public static void captureActiveSnapshot() {
        if (activeManager == null || activeEntryUuid == null || !isLoaded()) {
            return;
        }
        try {
            Class<?> base = Class.forName("net.sixik.sdmshoprework.common.shop.ShopBase");
            Object shop = base.getField("SERVER").get(null);
            if (shop == null) {
                return;
            }
            net.minecraft.nbt.CompoundTag data =
                    (net.minecraft.nbt.CompoundTag) base.getMethod("serializeNBT").invoke(shop);
            activeManager.updateSnapshot(activeEntryUuid, data.copy());
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void installEconomyModule() {
        try {
            ClassLoader loader = SDMShopCompat.class.getClassLoader();
            Class<?> managerClass = Class.forName("net.sixik.sdmshoprework.economy.EconomyManager", false, loader);
            Class<?> moduleClass = Class.forName(
                    "net.sixik.sdmshoprework.economy.EconomyManager$EconomyModule", false, loader);
            Constructor<?> constructor = moduleClass.getConstructor(Consumer.class, BiConsumer.class, Function.class);

            Consumer<Player> sync = SDMShopCompat::syncPlayer;
            BiConsumer<Player, Long> set = SDMShopCompat::setBalance;
            Function<Player, Long> get = player -> CurrencyService.balance();
            Object module = constructor.newInstance(sync, set, get);

            Field economy = managerClass.getField("economy");
            economy.set(null, module);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to install SDM Shop Rework currency bridge", exception);
        }
    }

    public static void syncAll() {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncPlayer(player);
        }
    }

    public static void syncPlayer(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        Method method = currencySetMoney();
        if (method == null) {
            return;
        }
        try {
            method.invoke(null, player, BASIC_MONEY, CurrencyService.balance());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to synchronize SDM Shop Rework currency", exception);
        }
    }

    private static void setBalance(Player player, Long amount) {
        if (amount == null) {
            return;
        }
        CurrencyService.set(amount, "sdmshoprework", "sdmshoprework",
                player instanceof ServerPlayer serverPlayer ? serverPlayer.createCommandSourceStack() : null);
    }

    private static Method currencySetMoney() {
        if (!currencyLookupAttempted) {
            currencyLookupAttempted = true;
            try {
                Class<?> helper = Class.forName("net.sixik.sdm_economy.api.CurrencyHelper", false,
                        SDMShopCompat.class.getClassLoader());
                currencySetMoney = helper.getMethod("setMoney", Player.class, String.class, long.class);
            } catch (ReflectiveOperationException ignored) {
                currencySetMoney = null;
            }
        }
        return currencySetMoney;
    }
}
