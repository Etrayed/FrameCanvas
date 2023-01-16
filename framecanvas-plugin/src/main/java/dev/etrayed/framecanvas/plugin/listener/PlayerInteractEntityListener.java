package dev.etrayed.framecanvas.plugin.listener;

import dev.etrayed.framecanvas.plugin.FrameCanvasPlugin;
import dev.etrayed.framecanvas.plugin.canvas.EntityCanvas;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * @author Etrayed
 */
public class PlayerInteractEntityListener implements Listener {

    private final FrameCanvasPlugin plugin;

    public PlayerInteractEntityListener(FrameCanvasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void handleInteract(PlayerInteractEntityEvent event) {
        if(!(event.getRightClicked() instanceof ItemFrame entity)) {
            return;
        }

        EntityCanvas canvas = plugin.locateCanvas(entity.getLocation());

        if(canvas == null) {
            return;
        }

        event.setCancelled(true);

        if(!canvas.hasListeners().get()) {
            return;
        }

        canvas.fireClickEvent(entity.getLocation(), event.getPlayer(), event instanceof PlayerInteractAtEntityEvent atEvent
                ? atEvent.getClickedPosition() : null, false);
    }

    @EventHandler
    public void handleDamage(EntityDamageByEntityEvent event) {
        if(!(event.getEntity() instanceof ItemFrame entity) || !(event.getDamager() instanceof Player player)) {
            return;
        }

        EntityCanvas canvas = plugin.locateCanvas(entity.getLocation());

        if(canvas == null) {
            return;
        }

        event.setCancelled(true);

        if(!canvas.hasListeners().get()) {
            return;
        }

        canvas.fireClickEvent(entity.getLocation(), player, null, true);
    }
}
