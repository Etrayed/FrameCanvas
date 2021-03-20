package dev.etrayed.framecanvas.api.util;

import dev.etrayed.framecanvas.api.canvas.CanvasSlice;
import org.bukkit.entity.Player;

/**
 * @author Etrayed
 */
@FunctionalInterface
public interface MapIdObfuscator {

    short obfuscate(Player receiver, CanvasSlice slice, short mapId);
}
