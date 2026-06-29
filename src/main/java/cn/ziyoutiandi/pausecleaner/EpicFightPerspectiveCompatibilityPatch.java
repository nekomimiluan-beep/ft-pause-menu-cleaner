package cn.ziyoutiandi.pausecleaner;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

final class EpicFightPerspectiveCompatibilityPatch {
    private static final String CLIENT_CONFIG_CLASS = "yesman.epicfight.config.ClientConfig";
    private static final String CLIENT_ENGINE_CLASS = "yesman.epicfight.client.ClientEngine";
    private static final String LOCAL_PLAYER_PATCH_CLASS = "yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch";
    private static final String ADAPTIVE_STRATEGY = "ADAPTIVE";

    private static Reflection reflection;
    private static LocalPlayer lastPlayer;
    private static Boolean lastEpicFightMode;
    private static Item lastMainHandItem;
    private static Item lastOffhandItem;
    private static boolean lastAutoPerspectiveEnabled;
    private static boolean autoPerspectiveStateKnown;

    private EpicFightPerspectiveCompatibilityPatch() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(EpicFightPerspectiveCompatibilityPatch::onClientTickPost);
    }

    private static void onClientTickPost(ClientTickEvent.Post ignored) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            resetTracking();
            return;
        }

        applyAutomaticMode(minecraft, player);
    }

    private static void applyAutomaticMode(Minecraft minecraft, LocalPlayer player) {
        try {
            Reflection bridge = reflection();
            Object playerPatch = bridge.playerPatch();
            if (playerPatch == null) {
                resetTracking();
                return;
            }

            boolean epicFightMode = bridge.isEpicFightMode(playerPatch);
            boolean vanillaMode = bridge.isVanillaMode(playerPatch);
            if (!epicFightMode && !vanillaMode) {
                return;
            }

            if (VivecraftVrModeGuard.isVrRunning()) {
                if (!vanillaMode) {
                    bridge.toVanillaModeWithoutPerspective(playerPatch);
                }
                resetTracking();
                return;
            }

            if (!bridge.isAdaptiveStrategy()) {
                resetTracking();
                return;
            }

            boolean autoPerspectiveEnabled = EpicFightPerspectiveBridge.isAutoPerspectiveEnabled();
            Item mainHandItem = player.getMainHandItem().getItem();
            Item offhandItem = player.getOffhandItem().getItem();
            boolean playerChanged = player != lastPlayer;
            boolean handChanged = playerChanged
                    || mainHandItem != lastMainHandItem
                    || offhandItem != lastOffhandItem;
            if (handChanged && (bridge.isCombatItem(mainHandItem) || bridge.isCombatItem(offhandItem))) {
                if (!epicFightMode) {
                    bridge.toEpicFightMode(playerPatch);
                    epicFightMode = true;
                }
            } else if (handChanged && bridge.isMiningItem(mainHandItem)) {
                if (!vanillaMode) {
                    bridge.toVanillaMode(playerPatch);
                    epicFightMode = false;
                }
            }

            boolean modeChanged = playerChanged
                    || lastEpicFightMode == null
                    || lastEpicFightMode.booleanValue() != epicFightMode;
            boolean autoPerspectiveChanged = !autoPerspectiveStateKnown
                    || lastAutoPerspectiveEnabled != autoPerspectiveEnabled;
            lastPlayer = player;
            lastEpicFightMode = epicFightMode;
            lastMainHandItem = mainHandItem;
            lastOffhandItem = offhandItem;
            lastAutoPerspectiveEnabled = autoPerspectiveEnabled;
            autoPerspectiveStateKnown = true;
            if (!autoPerspectiveEnabled || !modeChanged && !handChanged && !autoPerspectiveChanged) {
                return;
            }

            minecraft.options.setCameraType(epicFightMode ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            resetTracking();
            // Epic Fight is optional. If its internals are unavailable, the menu cleaner still works.
        }
    }

    private static Reflection reflection() throws ReflectiveOperationException {
        if (reflection == null) {
            reflection = new Reflection();
        }
        return reflection;
    }

    private static void resetTracking() {
        lastPlayer = null;
        lastEpicFightMode = null;
        lastMainHandItem = null;
        lastOffhandItem = null;
        lastAutoPerspectiveEnabled = false;
        autoPerspectiveStateKnown = false;
    }

    private static final class Reflection {
        private final Field playerBehaviorStrategy;
        private final Field combatCategorizedItems;
        private final Field miningCategorizedItems;
        private final Method getEngineInstance;
        private final Method getPlayerPatch;
        private final Method isEpicFightMode;
        private final Method isVanillaMode;
        private final Method toEpicFightMode;
        private final Method toVanillaMode;
        private final Field autoPerspectiveSwitching;

        private Reflection() throws ReflectiveOperationException {
            Class<?> configClass = Class.forName(CLIENT_CONFIG_CLASS);
            Class<?> engineClass = Class.forName(CLIENT_ENGINE_CLASS);
            Class<?> patchClass = Class.forName(LOCAL_PLAYER_PATCH_CLASS);

            this.playerBehaviorStrategy = field(configClass, "playerBehaviorStrategy");
            this.combatCategorizedItems = field(configClass, "combatCategorizedItems");
            this.miningCategorizedItems = field(configClass, "miningCategorizedItems");
            this.getEngineInstance = method(engineClass, "getInstance");
            this.getPlayerPatch = method(engineClass, "getPlayerPatch");
            this.isEpicFightMode = method(patchClass, "isEpicFightMode");
            this.isVanillaMode = method(patchClass, "isVanillaMode");
            this.toEpicFightMode = method(patchClass, "toEpicFightMode", boolean.class);
            this.toVanillaMode = method(patchClass, "toVanillaMode", boolean.class);
            this.autoPerspectiveSwitching = field(configClass, "autoPerspectiveSwithing");
        }

        private boolean isAdaptiveStrategy() throws ReflectiveOperationException {
            Object strategy = playerBehaviorStrategy.get(null);
            if (strategy instanceof Enum<?> enumValue) {
                return ADAPTIVE_STRATEGY.equals(enumValue.name());
            }
            return strategy != null && ADAPTIVE_STRATEGY.equals(strategy.toString());
        }

        private Object playerPatch() throws ReflectiveOperationException {
            Object engine = getEngineInstance.invoke(null);
            return engine == null ? null : getPlayerPatch.invoke(engine);
        }

        private boolean isEpicFightMode(Object playerPatch) throws ReflectiveOperationException {
            return (Boolean) isEpicFightMode.invoke(playerPatch);
        }

        private boolean isVanillaMode(Object playerPatch) throws ReflectiveOperationException {
            return (Boolean) isVanillaMode.invoke(playerPatch);
        }

        private boolean isCombatItem(Item item) throws ReflectiveOperationException {
            return containsItem(combatCategorizedItems, item);
        }

        private boolean isMiningItem(Item item) throws ReflectiveOperationException {
            return containsItem(miningCategorizedItems, item);
        }

        private void toEpicFightMode(Object playerPatch) throws ReflectiveOperationException {
            toEpicFightMode.invoke(playerPatch, true);
        }

        private void toVanillaMode(Object playerPatch) throws ReflectiveOperationException {
            toVanillaMode.invoke(playerPatch, true);
        }

        private void toVanillaModeWithoutPerspective(Object playerPatch) throws ReflectiveOperationException {
            boolean previous = autoPerspectiveSwitching.getBoolean(null);
            autoPerspectiveSwitching.setBoolean(null, false);
            try {
                toVanillaMode.invoke(playerPatch, false);
            } finally {
                autoPerspectiveSwitching.setBoolean(null, previous);
            }
        }

        private static boolean containsItem(Field field, Item item) throws ReflectiveOperationException {
            Object value = field.get(null);
            return value instanceof Set<?> set && set.contains(item);
        }

        private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
            Field field;
            try {
                field = owner.getField(name);
            } catch (NoSuchFieldException ignored) {
                field = owner.getDeclaredField(name);
            }
            field.setAccessible(true);
            return field;
        }

        private static Method method(Class<?> owner, String name, Class<?>... parameterTypes) throws ReflectiveOperationException {
            Method method;
            try {
                method = owner.getMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                method = owner.getDeclaredMethod(name, parameterTypes);
            }
            method.setAccessible(true);
            return method;
        }
    }
}
