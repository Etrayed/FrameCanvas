package dev.etrayed.framecanvas.plugin.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import dev.etrayed.framecanvas.api.canvas.CanvasSlice;
import dev.etrayed.framecanvas.plugin.FrameCanvasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.List;

/**
 * @author Etrayed
 */
public class MapIdObfuscationListener extends PacketAdapter {

    private static WrappedDataWatcher.WrappedDataWatcherObject itemObject;

    static {
        if(MinecraftReflection.watcherObjectExists()) {
            itemObject = new WrappedDataWatcher.WrappedDataWatcherObject(7, WrappedDataWatcher.Registry.getItemStackSerializer(false));
        }
    }

    private final FrameCanvasPlugin plugin;

    public MapIdObfuscationListener(FrameCanvasPlugin plugin) {
        super(plugin, ListenerPriority.MONITOR, PacketType.Play.Server.MAP, PacketType.Play.Server.ENTITY_METADATA);

        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer container = event.getPacket();

        if(plugin.obfuscator() == null) {
            return;
        }

        if(event.getPacketType() == PacketType.Play.Server.MAP) {
            MapView view = Bukkit.getMap(container.getIntegers().read(0).shortValue());
            CanvasSlice slice = view == null ? null : findSlice(view.getRenderers());

            if(slice != null) {
                container.getIntegers().write(0, (int) plugin.obfuscator().obfuscate(event.getPlayer(), slice, view.getId()));
            }
        } else if(event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            if(!(container.getEntityModifier(event).readSafely(0) instanceof ItemFrame)) {
                return;
            }

            List<WrappedWatchableObject> objects = container.getWatchableCollectionModifier().read(0);
            int searchedIndex = itemObject != null ? 7 : 8;
            WrappedWatchableObject theObject = null;

            for (WrappedWatchableObject object : objects) {
                if(object.getIndex() == searchedIndex) {
                    theObject = object;

                    break;
                }
            }

            if(theObject != null && theObject.getRawValue() != null) {
                MapView view = Bukkit.getMap(((ItemStack) theObject.getValue()).getDurability());
                CanvasSlice slice = view == null ? null : findSlice(view.getRenderers());

                if(slice != null) {
                    objects.remove(theObject);

                    ItemStack itemStack = ((ItemStack) theObject.getValue()).clone();

                    itemStack.setDurability(plugin.obfuscator().obfuscate(event.getPlayer(), slice, view.getId()));

                    objects.add(itemObject != null ? new WrappedWatchableObject(itemObject, MinecraftReflection.getMinecraftItemStack(itemStack))
                            : new WrappedWatchableObject(8, MinecraftReflection.getMinecraftItemStack(itemStack)));

                    container.getWatchableCollectionModifier().write(0, objects);
                }
            }
        }
    }

    private CanvasSlice findSlice(List<MapRenderer> renderers) {
        for (MapRenderer renderer : renderers) {
            if(renderer instanceof CanvasSlice) {
                return (CanvasSlice) renderer;
            }
        }

        return null;
    }
}
