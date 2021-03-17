package dev.etrayed.framecanvas.plugin.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.etrayed.framecanvas.plugin.FrameCanvasPlugin;
import dev.etrayed.framecanvas.plugin.canvas.EntityCanvas;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;

/**
 * @author Etrayed
 */
public class EntityUseListener extends PacketAdapter {

    private final FrameCanvasPlugin plugin;

    public EntityUseListener(FrameCanvasPlugin plugin) {
        super(plugin, ListenerPriority.MONITOR, PacketType.Play.Client.USE_ENTITY);

        this.plugin = plugin;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        PacketContainer container = event.getPacket();
        Entity entity = container.getEntityModifier(event).read(0);

        if(!(entity instanceof ItemFrame)) {
            return;
        }

        EntityCanvas canvas = plugin.locateCanvas(entity.getLocation());

        if(canvas == null) {
            return;
        }

        event.setReadOnly(false);
        event.setCancelled(true);

        if(!canvas.hasListeners().get()) {
            return;
        }

        EnumWrappers.EntityUseAction useAction = container.getEntityUseActions().read(0);

        canvas.fireClickEvent(entity.getLocation(), event.getPlayer(), useAction == EnumWrappers.EntityUseAction.INTERACT_AT
                ? container.getVectors().read(0) : null, useAction == EnumWrappers.EntityUseAction.ATTACK);
    }
}
