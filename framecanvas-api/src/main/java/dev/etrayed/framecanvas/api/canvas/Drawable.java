package dev.etrayed.framecanvas.api.canvas;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author Etrayed
 */
public interface Drawable {

    @NotNull
    CompletableFuture<Void> displayImage(@NotNull Image image);

    @NotNull
    CompletableFuture<Void> displayImage(@Nullable Player player, @NotNull Image image);

    void setPixel(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y, Color color);

    void setPixel(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y, byte color);

    void setPixel(@Nullable Player player, @Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y, Color color);

    void setPixel(@Nullable Player player, @Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y, byte color);

    void fill(Color color);

    void fill(byte color);

    void fill(@Nullable Player player, Color color);

    void fill(@Nullable Player player, byte color);

    void setAll(@NotNull Color[] colors);

    void setAll(@NotNull byte[] colors);

    void setAll(@Nullable Player player, @NotNull Color[] colors);

    void setAll(@Nullable Player player, @NotNull byte[] colors);

    Color obtainPixelColor(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y);

    byte obtainPixel(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y);

    Color obtainPixelColor(@Nullable Player player, @Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y);

    byte obtainPixel(@Nullable Player player, @Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y);

    /* unmodifiable */
    Color[] colorBuffer();

    /* unmodifiable */
    Color[] colorBuffer(@Nullable Player player);

    /* modifiable */
    byte[] buffer();

    /* modifiable */
    byte[] buffer(@Nullable Player player);

    boolean isGlobal();

    void clear(@NotNull Player player);
}
