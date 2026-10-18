package net.silvertide.beasts_and_bounties.feature.bestiary.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;

public final class BestiaryToast implements Toast {
    private static final Sprite BACKGROUND_SPRITE = Sprite.of("toast", 160, 32);
    private static final Sprite ICON_SPRITE = Sprite.of("icon_overview", 16, 16);
    private static final long DISPLAY_MILLIS = 5000L;
    private static final int ICON_X = 8;
    private static final int ICON_Y = 8;
    private static final int ICON_SIZE = 16;
    private static final int TEXT_X = 30;
    private static final int TITLE_Y = 7;
    private static final int MESSAGE_Y = 18;
    private static final int TITLE_COLOR = 0xFFFF00;
    private static final int MESSAGE_COLOR = 0xFFFFFF;

    private Component title;
    private Component message;
    private long shownSinceMillis;
    private boolean restartTimer;

    private BestiaryToast(Component title, Component message) {
        this.title = title;
        this.message = message;
    }

    public static void show(Component title, Component message) {
        ToastComponent toasts = Minecraft.getInstance().getToasts();
        BestiaryToast visible = toasts.getToast(BestiaryToast.class, NO_TOKEN);
        if (visible == null) {
            toasts.addToast(new BestiaryToast(title, message));
            return;
        }
        visible.title = title;
        visible.message = message;
        visible.restartTimer = true;
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toasts, long timeSinceLastVisible) {
        if (restartTimer) {
            shownSinceMillis = timeSinceLastVisible;
            restartTimer = false;
        }
        BACKGROUND_SPRITE.draw(graphics, 0, 0, width(), height());
        ICON_SPRITE.draw(graphics, ICON_X, ICON_Y, ICON_SIZE, ICON_SIZE);
        graphics.drawString(toasts.getMinecraft().font, title, TEXT_X, TITLE_Y, TITLE_COLOR, false);
        graphics.drawString(toasts.getMinecraft().font, message, TEXT_X, MESSAGE_Y, MESSAGE_COLOR, false);
        double displayMillis = DISPLAY_MILLIS * toasts.getNotificationDisplayTimeMultiplier();
        return timeSinceLastVisible - shownSinceMillis < displayMillis ? Visibility.SHOW : Visibility.HIDE;
    }
}
