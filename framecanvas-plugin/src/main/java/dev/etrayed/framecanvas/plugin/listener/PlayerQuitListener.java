package dev.etrayed.framecanvas.plugin.listener;

import dev.etrayed.framecanvas.plugin.FrameCanvasPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * @author Etrayed
 */
public class PlayerQuitListener implements Listener {

    private final FrameCanvasPlugin plugin;

    public PlayerQuitListener(FrameCanvasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void handleQuit(PlayerQuitEvent quitEvent) {
        plugin.registeredCanvas().forEach(canvas -> canvas.clear(quitEvent.getPlayer()));
    }
}
