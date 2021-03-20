package dev.etrayed.framecanvas.plugin.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import dev.etrayed.framecanvas.plugin.FrameCanvasPlugin;

/**
 * @author Etrayed
 */
public class MapIdObfuscationListener extends PacketAdapter {

    private final FrameCanvasPlugin plugin;

    public MapIdObfuscationListener(FrameCanvasPlugin plugin) {
        super(plugin, ListenerPriority.MONITOR, PacketType.Play.Server.MAP, PacketType.Play.Server.ENTITY_METADATA);

        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {

    }
}
