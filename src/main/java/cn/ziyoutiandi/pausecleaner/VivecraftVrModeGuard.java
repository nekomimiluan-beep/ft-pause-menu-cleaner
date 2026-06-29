package cn.ziyoutiandi.pausecleaner;

import java.lang.reflect.Field;

final class VivecraftVrModeGuard {
    private static final String VR_STATE_CLASS = "org.vivecraft.client_vr.VRState";
    private static final String VR_RUNNING_FIELD = "VR_RUNNING";

    private static Field vrRunningField;

    private VivecraftVrModeGuard() {
    }

    static boolean isVrRunning() {
        try {
            return vrRunningField().getBoolean(null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static Field vrRunningField() throws ReflectiveOperationException {
        if (vrRunningField == null) {
            Class<?> vrStateClass = Class.forName(VR_STATE_CLASS);
            vrRunningField = vrStateClass.getField(VR_RUNNING_FIELD);
        }
        return vrRunningField;
    }
}
