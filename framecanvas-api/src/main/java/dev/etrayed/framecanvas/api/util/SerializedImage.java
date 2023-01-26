package dev.etrayed.framecanvas.api.util;

import com.google.common.base.Preconditions;
import dev.etrayed.framecanvas.api.canvas.Drawable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Etrayed
 */
public record SerializedImage(int width, int height, Byte[] data) {

    public void displayAt(@NotNull Drawable drawable) {
        displayAt(drawable, null, 0, 0);
    }

    public void displayAt(@NotNull Drawable drawable, int startX, int startY) {
        displayAt(drawable, null, startX, startY);
    }

    public void displayAt(@NotNull Drawable drawable, @Nullable Player player) {
        displayAt(drawable, player, 0, 0);
    }

    public void displayAt(@NotNull Drawable drawable, @Nullable Player player, int startX, int startY) {
        Preconditions.checkNotNull(drawable, "drawable");

        int boundX = startX + width;
        int boundY = startY + height;

        for (int x = startX, dataX = 0; x < boundX; x++, dataX++) {
            for (int y = startY, dataY = 0; y < boundY; y++, dataY++) {
                Byte color = data[dataY * width + dataX];

                if(color != null) {
                    drawable.setPixel(player, x, y, color);
                }
            }
        }
    }
}
