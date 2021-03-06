package dev.etrayed.framecanvas.api.canvas;

import dev.etrayed.framecanvas.api.canvas.click.Clickable;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * @author Etrayed
 */
public interface CanvasSlice extends Drawable, Clickable {

    @NotNull
    Canvas canvas();

    @Range(from = 0, to = Integer.MAX_VALUE)
    int x();

    @Range(from = 0, to = Integer.MAX_VALUE)
    int y();

    @NotNull
    Location location();

    boolean isEmpty();
}
