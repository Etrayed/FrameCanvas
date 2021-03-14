package dev.etrayed.framecanvas.api.canvas.click;

import dev.etrayed.framecanvas.api.canvas.CanvasSlice;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * @author Etrayed
 */
public interface ClickListener {

    void handleClick(CanvasSlice slice, Player player, boolean leftClick);

    void handlePositionalClick(CanvasSlice slice, Player player, Vector position);
}
