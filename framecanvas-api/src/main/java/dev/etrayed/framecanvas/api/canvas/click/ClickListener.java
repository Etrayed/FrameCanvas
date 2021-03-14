package dev.etrayed.framecanvas.api.canvas.click;

import dev.etrayed.framecanvas.api.canvas.CanvasSlice;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * @author Etrayed
 */
public interface ClickListener {

    void handleClick(@NotNull CanvasSlice slice, @NotNull Player player, boolean leftClick);

    void handlePositionalClick(@NotNull CanvasSlice slice, @NotNull Player player, @NotNull Vector position);
}
