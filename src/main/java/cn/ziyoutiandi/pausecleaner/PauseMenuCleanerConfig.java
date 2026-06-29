package cn.ziyoutiandi.pausecleaner;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PauseMenuCleanerConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("ft_pause_menu_cleaner.properties");
    private static PauseMenuCleanerConfig INSTANCE = new PauseMenuCleanerConfig(true, true, true, false);

    private boolean hideSendFeedback;
    private boolean hideReportBugs;
    private boolean replaceReportPlayerWithPerspectiveToggle;
    private boolean appliedEpicFightPerspectiveDefault;

    private PauseMenuCleanerConfig(
            boolean hideSendFeedback,
            boolean hideReportBugs,
            boolean replaceReportPlayerWithPerspectiveToggle,
            boolean appliedEpicFightPerspectiveDefault
    ) {
        this.hideSendFeedback = hideSendFeedback;
        this.hideReportBugs = hideReportBugs;
        this.replaceReportPlayerWithPerspectiveToggle = replaceReportPlayerWithPerspectiveToggle;
        this.appliedEpicFightPerspectiveDefault = appliedEpicFightPerspectiveDefault;
    }

    public static PauseMenuCleanerConfig get() {
        return INSTANCE;
    }

    public static void load() {
        Properties properties = new Properties();
        if (Files.isRegularFile(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                properties.load(in);
            } catch (IOException ignored) {
                properties.clear();
            }
        }

        INSTANCE = new PauseMenuCleanerConfig(
                readBoolean(properties, "hideSendFeedback", true),
                readBoolean(properties, "hideReportBugs", true),
                readBoolean(properties, "replaceReportPlayerWithPerspectiveToggle",
                        readBoolean(properties, "hideReportPlayer", true)),
                readBoolean(properties, "appliedEpicFightPerspectiveDefault", false)
        );

        save();
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("hideSendFeedback", Boolean.toString(INSTANCE.hideSendFeedback));
        properties.setProperty("hideReportBugs", Boolean.toString(INSTANCE.hideReportBugs));
        properties.setProperty("replaceReportPlayerWithPerspectiveToggle",
                Boolean.toString(INSTANCE.replaceReportPlayerWithPerspectiveToggle));
        properties.setProperty("appliedEpicFightPerspectiveDefault",
                Boolean.toString(INSTANCE.appliedEpicFightPerspectiveDefault));

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(out, "Free Heaven pause menu cleaner");
            }
        } catch (IOException ignored) {
            // The in-memory config still works for this session if disk saving fails.
        }
    }

    private static boolean readBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    public boolean hideSendFeedback() {
        return hideSendFeedback;
    }

    public void setHideSendFeedback(boolean hideSendFeedback) {
        this.hideSendFeedback = hideSendFeedback;
    }

    public boolean hideReportBugs() {
        return hideReportBugs;
    }

    public void setHideReportBugs(boolean hideReportBugs) {
        this.hideReportBugs = hideReportBugs;
    }

    public boolean replaceReportPlayerWithPerspectiveToggle() {
        return replaceReportPlayerWithPerspectiveToggle;
    }

    public void setReplaceReportPlayerWithPerspectiveToggle(boolean replaceReportPlayerWithPerspectiveToggle) {
        this.replaceReportPlayerWithPerspectiveToggle = replaceReportPlayerWithPerspectiveToggle;
    }

    public boolean appliedEpicFightPerspectiveDefault() {
        return appliedEpicFightPerspectiveDefault;
    }

    public void setAppliedEpicFightPerspectiveDefault(boolean appliedEpicFightPerspectiveDefault) {
        this.appliedEpicFightPerspectiveDefault = appliedEpicFightPerspectiveDefault;
    }
}
