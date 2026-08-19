package com.yinfires.icecore.compat.cozycafe;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

public final class CozyCafeCompat {
    public static final String MENU_SELECTOR_SCREEN = "io.github.chakyl.cozycafe.gui.MenuSelectorScreen";
    private static final String MENU_SELECTOR_MENU = "io.github.chakyl.cozycafe.gui.MenuSelectorMenu";
    private static final String CAFE_MENU_ITEM_REGISTRY = "io.github.chakyl.cozycafe.data.CafeMenuItemRegistry";

    private CozyCafeCompat() {
    }

    public static boolean addToMenu(ServerPlayer player, ItemStack itemStack) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || itemStack.isEmpty() || !isMenuSelectorMenu(menu)) {
            return false;
        }
        if (!menu.stillValid(player)) {
            return false;
        }

        try {
            Field blockEntityField = menu.getClass().getField("blockEntity");
            Object blockEntity = blockEntityField.get(menu);
            Method addToMenu = blockEntity.getClass().getMethod("addToMenu", ItemStack.class);
            ItemStack normalizedStack = itemStack.copy();
            normalizedStack.setCount(1);
            boolean added = (Boolean) addToMenu.invoke(blockEntity, normalizedStack);
            return added;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    public static boolean isMenuSelectorScreen(Object screen) {
        return screen != null && screen.getClass().getName().equals(MENU_SELECTOR_SCREEN);
    }

    public static boolean isMenuSelectorMenu(Object menu) {
        return menu != null && menu.getClass().getName().equals(MENU_SELECTOR_MENU);
    }

    public static void handleClientMenuAddition(ItemStack itemStack, boolean added) {
        Object minecraft;
        Object screen;
        Object menu;
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            minecraft = findNoArgMethod(minecraftClass, "getInstance", "m_91087_").invoke(null);
            screen = findField(minecraftClass, "screen", "f_91080_").get(minecraft);
            if (screen == null) {
                return;
            }

            menu = findNoArgMethod(screen.getClass(), "getMenu", "m_6262_").invoke(screen);
            if (!isMenuSelectorMenu(menu)) {
                return;
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return;
        }

        if (added) {
            try {
                findMethod(menu.getClass(), "addToClientMenu", ItemStack.class).invoke(menu, itemStack.copy());
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        setAdditionStatus(menu, added);
        syncClientScreen(screen, menu);
        setScreenLastStatus(screen, added);
        playAdditionSound(minecraft, added);
    }

    public static Optional<MenuItemInfo> getMenuItemInfo(Item item) {
        try {
            Class<?> registryClass = Class.forName(CAFE_MENU_ITEM_REGISTRY);
            Object registry = registryClass.getField("INSTANCE").get(null);
            Object menuItem = registryClass.getMethod("getForItem", Item.class).invoke(registry, item);
            if (menuItem == null) {
                return Optional.empty();
            }

            Object category = menuItem.getClass().getMethod("category").invoke(menuItem);
            return Optional.of(new MenuItemInfo(
                    (Integer) menuItem.getClass().getMethod("price").invoke(menuItem),
                    category.toString().toLowerCase(Locale.ROOT),
                    (Boolean) menuItem.getClass().getMethod("bowlFood").invoke(menuItem),
                    (Boolean) menuItem.getClass().getMethod("bottleDrink").invoke(menuItem)
            ));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static void setAdditionStatus(Object menu, boolean added) {
        try {
            Method method = findDeclaredMethod(menu.getClass(), "toggleMenuItemAdditionStatus", boolean.class);
            method.setAccessible(true);
            method.invoke(menu, added);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void syncClientScreen(Object screen, Object menu) {
        try {
            Object cafeMenu = findMethod(menu.getClass(), "getCafeMenu").invoke(menu);
            Field cafeMenuField = findDeclaredField(screen.getClass(), "cafeMenu");
            cafeMenuField.setAccessible(true);
            cafeMenuField.set(screen, cafeMenu);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void setScreenLastStatus(Object screen, boolean added) {
        try {
            Field lastStatusField = findDeclaredField(screen.getClass(), "lastStatus");
            lastStatusField.setAccessible(true);
            Object[] constants = lastStatusField.getType().getEnumConstants();
            if (constants == null) {
                return;
            }

            String expectedName = added ? "VALID" : "INVALID";
            for (Object constant : constants) {
                if (constant instanceof Enum<?> enumConstant && enumConstant.name().equals(expectedName)) {
                    lastStatusField.set(screen, constant);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void playAdditionSound(Object minecraft, boolean added) {
        try {
            Class<?> soundEventsClass = Class.forName("net.minecraft.sounds.SoundEvents");
            String soundName = added ? "NOTE_BLOCK_CHIME" : "NOTE_BLOCK_BASS";
            String fallbackSoundName = added ? "f_12211_" : "f_12209_";
            Object soundHolder = findField(soundEventsClass, soundName, fallbackSoundName).get(null);
            Class<?> soundEventClass = Class.forName("net.minecraft.sounds.SoundEvent");
            Object soundEvent = soundEventClass.isInstance(soundHolder)
                    ? soundHolder
                    : findNoArgMethod(soundHolder.getClass(), "get", "value").invoke(soundHolder);
            Object player = findField(minecraft.getClass(), "player", "f_91074_").get(minecraft);
            if (player == null || !soundEventClass.isInstance(soundEvent)) {
                return;
            }

            findMethod(player.getClass(), "playSound", "m_5496_", soundEventClass, float.class, float.class)
                    .invoke(player, soundEvent, 1.0F, 1.0F);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    private static Method findNoArgMethod(Class<?> type, String name, String... fallbackNames)
            throws NoSuchMethodException {
        try {
            Class<?>[] parameterTypes = new Class<?>[0];
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            for (String fallbackName : fallbackNames) {
                try {
                    return type.getMethod(fallbackName);
                } catch (NoSuchMethodException ignoredFallback) {
                }
            }
            throw new NoSuchMethodException(type.getName() + "#" + name);
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        return type.getMethod(name, parameterTypes);
    }

    private static Method findMethod(Class<?> type, String name, String fallbackName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return type.getMethod(fallbackName, parameterTypes);
        }
    }

    private static Method findDeclaredMethod(Class<?> type, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        return type.getDeclaredMethod(name, parameterTypes);
    }

    private static Field findDeclaredField(Class<?> type, String name) throws NoSuchFieldException {
        return type.getDeclaredField(name);
    }

    private static Field findField(Class<?> type, String name, String... fallbackNames)
            throws NoSuchFieldException {
        try {
            return type.getField(name);
        } catch (NoSuchFieldException ignored) {
            for (String fallbackName : fallbackNames) {
                try {
                    return type.getField(fallbackName);
                } catch (NoSuchFieldException ignoredFallback) {
                }
            }
            throw new NoSuchFieldException(type.getName() + "#" + name);
        }
    }

    public record MenuItemInfo(int price, String category, boolean bowlFood, boolean bottleDrink) {
    }
}
