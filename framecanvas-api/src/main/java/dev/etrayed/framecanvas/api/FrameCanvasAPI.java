package dev.etrayed.framecanvas.api;

import com.google.common.collect.ImmutableList;
import dev.etrayed.framecanvas.api.canvas.Canvas;
import dev.etrayed.framecanvas.api.util.SerializedImage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.awt.image.BufferedImage;

/**
 * @author Etrayed
 */
public interface FrameCanvasAPI {

    FrameCanvasAPI INSTANCE = InstanceHolder.INSTANCE;

    @NotNull
    Canvas registerCanvas(@NotNull Location firstLocation, @NotNull Location secondLocation, @NotNull BlockFace direction, boolean isGlobal);

    @Unmodifiable
    @NotNull
    ImmutableList<Canvas> registeredCanvas();

    void unregisterCanvas(@NotNull Canvas canvas);

    SerializedImage serializeImage(@NotNull BufferedImage image);

    @ApiStatus.Internal
    final class InstanceHolder {

        private static final FrameCanvasAPI INSTANCE = Bukkit.getServicesManager().load(FrameCanvasAPI.class);
    }
}
