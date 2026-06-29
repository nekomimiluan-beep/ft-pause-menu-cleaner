package cn.ziyoutiandi.pausecleaner;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class EpicFightPerspectiveBridge {
    private static final Path EPIC_FIGHT_CLIENT_CONFIG = FMLPaths.CONFIGDIR.get().resolve("epicfight-client.toml");
    private static final String CLIENT_CONFIG_CLASS = "yesman.epicfight.config.ClientConfig";
    private static final String SPEC_VALUE_FIELD = "AUTO_PERSPECTIVE_SWITCHING";
    private static final String RUNTIME_VALUE_FIELD = "autoPerspectiveSwithing";
    private static final String FILE_KEY = "camera_auto_switch";
    private static Field runtimeValueField;

    private EpicFightPerspectiveBridge() {
    }

    static boolean isAutoPerspectiveEnabled() {
        Boolean runtimeValue = getRuntimeValue();
        if (runtimeValue != null) {
            return runtimeValue;
        }
        return readFileValue();
    }

    static void setAutoPerspectiveEnabled(boolean enabled) {
        setRuntimeValue(enabled);
        writeFileValue(enabled);
    }

    private static Boolean getRuntimeValue() {
        try {
            return runtimeValueField().getBoolean(null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static void setRuntimeValue(boolean enabled) {
        try {
            Class<?> configClass = Class.forName(CLIENT_CONFIG_CLASS);

            runtimeValueField().setBoolean(null, enabled);

            Field specValueField = configClass.getField(SPEC_VALUE_FIELD);
            Object specValue = specValueField.get(null);
            Method setMethod = specValue.getClass().getMethod("set", Object.class);
            setMethod.invoke(specValue, Boolean.valueOf(enabled));

            Method saveMethod = specValue.getClass().getMethod("save");
            saveMethod.invoke(specValue);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // File write below still keeps the setting for the next config load.
        }
    }

    private static Field runtimeValueField() throws ReflectiveOperationException {
        if (runtimeValueField == null) {
            Class<?> configClass = Class.forName(CLIENT_CONFIG_CLASS);
            runtimeValueField = configClass.getField(RUNTIME_VALUE_FIELD);
        }
        return runtimeValueField;
    }

    private static boolean readFileValue() {
        if (!Files.isRegularFile(EPIC_FIGHT_CLIENT_CONFIG)) {
            return true;
        }

        try {
            for (String line : Files.readAllLines(EPIC_FIGHT_CLIENT_CONFIG, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.startsWith(FILE_KEY)) {
                    int equals = trimmed.indexOf('=');
                    if (equals >= 0) {
                        return Boolean.parseBoolean(trimmed.substring(equals + 1).trim());
                    }
                }
            }
        } catch (IOException ignored) {
            return true;
        }
        return true;
    }

    private static void writeFileValue(boolean enabled) {
        try {
            if (!Files.isRegularFile(EPIC_FIGHT_CLIENT_CONFIG)) {
                return;
            }

            List<String> lines = Files.readAllLines(EPIC_FIGHT_CLIENT_CONFIG, StandardCharsets.UTF_8);
            boolean changed = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String trimmed = line.trim();
                if (trimmed.startsWith(FILE_KEY)) {
                    String indent = line.substring(0, line.indexOf(trimmed));
                    lines.set(i, indent + FILE_KEY + " = " + enabled);
                    changed = true;
                    break;
                }
            }

            if (changed) {
                Files.write(EPIC_FIGHT_CLIENT_CONFIG, lines, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
            // Runtime value was already updated when Epic Fight is loaded.
        }
    }
}
