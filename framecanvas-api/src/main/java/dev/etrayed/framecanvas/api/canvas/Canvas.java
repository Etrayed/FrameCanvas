package dev.etrayed.framecanvas.api.canvas;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * @author Etrayed
 */
public interface Canvas extends Drawable {

    @NotNull
    World world();

    @NotNull
    Location topLeftCorner();

    @Range(from = 0, to = Integer.MAX_VALUE)
    int width();

    @Range(from = 0, to = Integer.MAX_VALUE)
    int height();

    @NotNull
    BlockFace direction();

    @NotNull
    HorizontalAxis axis();

    @Contract("null -> false")
    boolean isCovering(@Nullable Location location);

    @NotNull
    CanvasSlice sliceAt(@Range(from = 0, to = Integer.MAX_VALUE) int globalX, @Range(from = 0, to = Integer.MAX_VALUE) int globalY);

    @NotNull
    CanvasSlice slice(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y);

    void verifyFrames();
}
