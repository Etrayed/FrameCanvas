package dev.etrayed.framecanvas.plugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import dev.etrayed.framecanvas.api.FrameCanvasAPI;
import dev.etrayed.framecanvas.api.canvas.Canvas;
import dev.etrayed.framecanvas.plugin.canvas.EntityCanvas;
import dev.etrayed.framecanvas.plugin.listener.EntityUseListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.dependency.Dependency;
import org.bukkit.plugin.java.annotation.dependency.DependsOn;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * @author Etrayed
 */
@Plugin(name = "FrameCanvas", version = "1.0.0")
@Author("Etrayed")
@DependsOn(@Dependency("ProtocolLib"))
public class FrameCanvasPlugin extends JavaPlugin implements FrameCanvasAPI {

    private final ListMultimap<World, Canvas> registeredCanvas = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());

    @Override
    public void onEnable() {
        Bukkit.getServicesManager().register(FrameCanvasAPI.class, this, this, ServicePriority.Highest);

        ProtocolLibrary.getProtocolManager().addPacketListener(new EntityUseListener(this));
    }

    @Override
    public @NotNull Canvas registerCanvas(@NotNull Location firstLocation, @NotNull Location secondLocation,
                                          @NotNull BlockFace direction, boolean isGlobal) {
        Preconditions.checkNotNull(firstLocation, "firstLocation");
        Preconditions.checkNotNull(secondLocation, "secondLocation");
        Preconditions.checkNotNull(direction, "direction");
        Preconditions.checkArgument(firstLocation.getWorld() != null && firstLocation.getWorld()
                .equals(secondLocation.getWorld()), "worlds of locations must match");

        EntityCanvas canvas = new EntityCanvas(firstLocation.getWorld(), new Vector(
                Math.min(firstLocation.getBlockX(), secondLocation.getBlockX()),
                Math.min(firstLocation.getBlockY(), secondLocation.getBlockY()),
                Math.min(firstLocation.getBlockZ(), secondLocation.getBlockZ())
        ), new Vector(
                Math.max(firstLocation.getBlockX(), secondLocation.getBlockX()),
                Math.max(firstLocation.getBlockY(), secondLocation.getBlockY()),
                Math.max(firstLocation.getBlockZ(), secondLocation.getBlockZ())
        ), direction, isGlobal);

        registeredCanvas.put(canvas.world(), canvas);

        return canvas;
    }

    @Override
    public @Unmodifiable @NotNull ImmutableList<Canvas> registeredCanvas() {
        return ImmutableList.copyOf(registeredCanvas.values());
    }

    @Override
    public void unregisterCanvas(@NotNull Canvas canvas) {
        Preconditions.checkNotNull(canvas, "canvas");

        registeredCanvas.remove(canvas.world(), canvas);
    }

    public EntityCanvas locateCanvas(Location location) {
        if(!registeredCanvas.containsKey(location.getWorld())) {
            return null;
        }

        for (Canvas canvas : registeredCanvas.values()) {
            if(canvas.isCovering(location)) {
                return (EntityCanvas) canvas;
            }
        }

        return null;
    }
}
