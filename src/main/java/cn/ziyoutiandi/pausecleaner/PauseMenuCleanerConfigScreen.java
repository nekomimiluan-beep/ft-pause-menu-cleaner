package cn.ziyoutiandi.pausecleaner;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;

public final class PauseMenuCleanerConfigScreen extends Screen {
    private final Screen parent;

    public PauseMenuCleanerConfigScreen(ModContainer ignored, Screen parent) {
        super(Component.translatable("ft_pause_menu_cleaner.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        PauseMenuCleanerConfig config = PauseMenuCleanerConfig.get();
        int centerX = this.width / 2;
        int y = Math.max(48, this.height / 2 - 62);

        addRenderableWidget(toggleButton(centerX - 102, y, "hide_send_feedback", config.hideSendFeedback(), value -> {
            config.setHideSendFeedback(value);
            PauseMenuCleanerConfig.save();
        }));
        addRenderableWidget(toggleButton(centerX - 102, y + 26, "hide_report_bugs", config.hideReportBugs(), value -> {
            config.setHideReportBugs(value);
            PauseMenuCleanerConfig.save();
        }));
        addRenderableWidget(toggleButton(centerX - 102, y + 52, "replace_report_player", config.replaceReportPlayerWithPerspectiveToggle(), value -> {
            config.setReplaceReportPlayerWithPerspectiveToggle(value);
            PauseMenuCleanerConfig.save();
        }));

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(centerX - 102, y + 92, 204, 20)
                .build());
    }

    private static CycleButton<Boolean> toggleButton(int x, int y, String key, boolean initialValue, ToggleSink sink) {
        return CycleButton.onOffBuilder()
                .withInitialValue(initialValue)
                .create(x, y, 204, 20, Component.translatable("ft_pause_menu_cleaner.config." + key),
                        (button, value) -> sink.accept(value));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private interface ToggleSink {
        void accept(boolean value);
    }
}
