package dev.etrayed.framecanvas.plugin.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import dev.etrayed.framecanvas.plugin.FrameCanvasPlugin;
import dev.etrayed.framecanvas.plugin.canvas.EntityCanvas;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;

import java.util.List;

/**
 * @author Etrayed
 */
public class EntityUseListener extends PacketAdapter {

    private final FrameCanvasPlugin plugin;

    public EntityUseListener(FrameCanvasPlugin plugin) {
        super(plugin, ListenerPriority.MONITOR, List.of(PacketType.Play.Client.USE_ENTITY), ListenerOptions.SYNC);

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

        WrappedEnumEntityUseAction useAction = container.getEnumEntityUseActions().read(0);

        canvas.fireClickEvent(entity.getLocation(), event.getPlayer(), useAction.getAction() == EnumWrappers.EntityUseAction.INTERACT_AT
                ? useAction.getPosition() : null, useAction.getAction() == EnumWrappers.EntityUseAction.ATTACK);
    }
}
