package cn.ziyoutiandi.pausecleaner;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mod(value = PauseMenuCleanerMod.MOD_ID, dist = Dist.CLIENT)
public final class PauseMenuCleanerMod {
    public static final String MOD_ID = "ft_pause_menu_cleaner";

    private static final Set<String> SEND_FEEDBACK_KEYS = Set.of("menu.sendFeedback");
    private static final Set<String> REPORT_BUGS_KEYS = Set.of("menu.reportBugs");
    private static final Set<String> REPORT_PLAYER_KEYS = Set.of("menu.playerReporting");

    public PauseMenuCleanerMod(ModContainer container) {
        PauseMenuCleanerConfig.load();
        applyDefaultEpicFightPerspectiveMode();
        container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory) PauseMenuCleanerConfigScreen::new);
        NeoForge.EVENT_BUS.addListener(this::onScreenInitPost);
        EpicFightPerspectiveCompatibilityPatch.register();
    }

    private void onScreenInitPost(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen)) {
            return;
        }

        PauseMenuCleanerConfig config = PauseMenuCleanerConfig.get();
        if (!config.hideSendFeedback() && !config.hideReportBugs() && !config.replaceReportPlayerWithPerspectiveToggle()) {
            return;
        }

        List<GuiEventListener> toRemove = new ArrayList<>();
        Button playerReportingButton = null;
        for (GuiEventListener listener : event.getListenersList()) {
            ButtonRole role = findButtonRole(listener);
            if (role == ButtonRole.SEND_FEEDBACK && config.hideSendFeedback()
                    || role == ButtonRole.REPORT_BUGS && config.hideReportBugs()) {
                toRemove.add(listener);
            } else if (role == ButtonRole.REPORT_PLAYER && config.replaceReportPlayerWithPerspectiveToggle()) {
                toRemove.add(listener);
                playerReportingButton = (Button) listener;
            }
        }

        for (GuiEventListener listener : toRemove) {
            event.removeListener(listener);
        }

        if (playerReportingButton != null) {
            event.addListener(createPerspectiveToggleButton(playerReportingButton));
        }
    }

    private static ButtonRole findButtonRole(GuiEventListener listener) {
        if (!(listener instanceof Button button)) {
            return ButtonRole.OTHER;
        }

        String key = findTranslationKey(button);
        if (SEND_FEEDBACK_KEYS.contains(key)) {
            return ButtonRole.SEND_FEEDBACK;
        }
        if (REPORT_BUGS_KEYS.contains(key)) {
            return ButtonRole.REPORT_BUGS;
        }
        if (REPORT_PLAYER_KEYS.contains(key)) {
            return ButtonRole.REPORT_PLAYER;
        }
        return ButtonRole.OTHER;
    }

    private static Button createPerspectiveToggleButton(Button originalButton) {
        return Button.builder(perspectiveButtonLabel(EpicFightPerspectiveBridge.isAutoPerspectiveEnabled()), button -> {
                    boolean enabled = !EpicFightPerspectiveBridge.isAutoPerspectiveEnabled();
                    EpicFightPerspectiveBridge.setAutoPerspectiveEnabled(enabled);
                    button.setMessage(perspectiveButtonLabel(enabled));
                })
                .bounds(originalButton.getX(), originalButton.getY(), originalButton.getWidth(), originalButton.getHeight())
                .build();
    }

    private static Component perspectiveButtonLabel(boolean autoPerspectiveEnabled) {
        return Component.translatable(autoPerspectiveEnabled
                ? "ft_pause_menu_cleaner.pause.auto_third_person"
                : "ft_pause_menu_cleaner.pause.manual_third_person")
                .withStyle(autoPerspectiveEnabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static void applyDefaultEpicFightPerspectiveMode() {
        PauseMenuCleanerConfig config = PauseMenuCleanerConfig.get();
        if (config.appliedEpicFightPerspectiveDefault()) {
            return;
        }

        EpicFightPerspectiveBridge.setAutoPerspectiveEnabled(true);
        config.setAppliedEpicFightPerspectiveDefault(true);
        PauseMenuCleanerConfig.save();
    }

    private static String findTranslationKey(AbstractWidget widget) {
        Component message = widget.getMessage();
        if (message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translated) {
            return translated.getKey();
        }
        return "";
    }

    private enum ButtonRole {
        SEND_FEEDBACK,
        REPORT_BUGS,
        REPORT_PLAYER,
        OTHER
    }
}
