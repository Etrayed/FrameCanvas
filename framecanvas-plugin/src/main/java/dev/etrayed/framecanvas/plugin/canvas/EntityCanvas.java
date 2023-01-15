package dev.etrayed.framecanvas.plugin.canvas;

import com.google.common.base.Preconditions;
import dev.etrayed.framecanvas.api.canvas.Canvas;
import dev.etrayed.framecanvas.api.canvas.CanvasSlice;
import dev.etrayed.framecanvas.api.canvas.HorizontalAxis;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapView;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Etrayed
 */
public class EntityCanvas implements Canvas {

    private final World world;

    private final Vector lowerCorner;

    private final Vector higherCorner;

    private final HorizontalAxis axis;

    private final int width, height;

    private final BlockFace direction;

    private final boolean global;

    private final ItemFrameSlice[] slices;

    private final AtomicBoolean hasListeners = new AtomicBoolean();

    public EntityCanvas(World world, Vector lowerCorner, Vector higherCorner, BlockFace direction, boolean global) {
        Preconditions.checkArgument(lowerCorner.getBlockX() == higherCorner.getBlockX()
                || lowerCorner.getBlockZ() == higherCorner.getBlockZ(), "corners must match in one axis (x or z)");

        this.world = world;
        this.lowerCorner = lowerCorner;
        this.higherCorner = higherCorner;
        this.axis = lowerCorner.getBlockX() == higherCorner.getBlockX() ? HorizontalAxis.Z : HorizontalAxis.X;
        this.width = axis.chooseBlockValue(higherCorner) - axis.chooseBlockValue(lowerCorner) + 1;
        this.height = higherCorner.getBlockY() - lowerCorner.getBlockY() + 1;
        this.direction = direction;
        this.global = global;

        axis.validateFace(direction);

        this.slices = new ItemFrameSlice[width * height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                slices[y * width + x] = new ItemFrameSlice(this, x, y, global);
            }
        }

        verifyFrames();
    }

    @NotNull
    @Override
    public World world() {
        return world;
    }

    @Override
    public @NotNull Location topLeftCorner() {
        if(direction == BlockFace.NORTH || direction == BlockFace.EAST) {
            return higherCorner.toLocation(world);
        }

        return new Location(world, lowerCorner.getBlockX(), higherCorner.getBlockY(), lowerCorner.getBlockZ());
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int width() {
        return width;
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int height() {
        return height;
    }

    @Override
    public @NotNull BlockFace direction() {
        return direction;
    }

    @Override
    public @NotNull HorizontalAxis axis() {
        return axis;
    }

    @Override
    public boolean isCovering(@Nullable Location location) {
        return location != null && location.getWorld() != null && location.getWorld().equals(world)
                && location.getBlockX() >= lowerCorner.getBlockX() && location.getBlockX() <= higherCorner.getBlockX()
                && location.getBlockY() >= lowerCorner.getBlockY() && location.getBlockY() <= higherCorner.getBlockY()
                && location.getBlockZ() >= lowerCorner.getBlockZ() && location.getBlockZ() <= higherCorner.getBlockZ();
    }

    @Override
    public @NotNull ItemFrameSlice sliceAt(@Range(from = 0, to = Integer.MAX_VALUE) int globalX, @Range(from = 0, to = Integer.MAX_VALUE) int globalY) {
        return slice(globalX / 128, globalY / 128);
    }

    @Override
    public @NotNull ItemFrameSlice slice(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y) {
        return slices[y * width + x];
    }

    @Override
    public @NotNull CompletableFuture<Void> displayImage(@NotNull BufferedImage image) {
        return displayImage(null, image);
    }

    @Override
    public @NotNull CompletableFuture<Void> displayImage(@Range(from = 0, to = 126) int startX, @Range(from = 0, to = 126) int startY, @NotNull BufferedImage image) {
        return displayImage(null, startX, startY, image);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull CompletableFuture<Void> displayImage(@Nullable Player player, @NotNull BufferedImage image) {
        return displayImage(player, 0, 0, image);
    }

    @Override
    public @NotNull CompletableFuture<Void> displayImage(@Nullable Player player, @Range(from = 0, to = 126) int startX, @Range(from = 0, to = 126) int startY, @NotNull BufferedImage image) {
        Preconditions.checkNotNull(image, "image");

        return CompletableFuture.runAsync(() -> {
            int boundX = Math.min(width * 128, startX + image.getWidth(null));
            int boundY = Math.min(height * 128, startY + image.getHeight(null));

            for (int x = startX, imgX = 0; x < boundX; x++, imgX++) {
                for (int y = startY, imgY = 0; y < boundY; y++, imgY++) {
                    setPixel(player, x, y, new Color(image.getRGB(imgX, imgY), true));
                }
            }
        });
    }

    @Override
    public void setPixel(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y, Color color) {
        setPixel(null, x, y, color);
    }

    @Override
    public void setPixel(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y, byte color) {
        setPixel(null, x, y, color);
    }

    @Override
    public void setPixel(@Nullable Player player, @Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y, Color color) {
        sliceAt(x, y).setPixel(player, x % 128, y % 128, color);
    }

    @Override
    public void setPixel(@Nullable Player player, @Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y, byte color) {
        sliceAt(x, y).setPixel(player, x % 128, y % 128, color);
    }

    @Override
    public void fill(Color color) {
        fill(null, color);
    }

    @Override
    public void fill(byte color) {
        fill(null, color);
    }

    @Override
    public void fill(@Nullable Player player, Color color) {
        for (CanvasSlice direct : slices) {
            direct.fill(player, color);
        }
    }

    @Override
    public void fill(@Nullable Player player, byte color) {
        for (CanvasSlice direct : slices) {
            direct.fill(player, color);
        }
    }

    @Override
    public void setAll(@NotNull Color[] colors) {
        setAll(null, colors);
    }

    @Override
    public void setAll(@NotNull byte[] colors) {
        setAll(null, colors);
    }

    @Override
    public void setAll(@Nullable Player player, @NotNull Color[] colors) {
        byte[] bytes = new byte[colors.length];

        for (int i = 0; i < colors.length; i++) {
            bytes[i] = MapPalette.matchColor(colors[i]);
        }

        setAll(player, bytes);
    }

    @Override
    public void setAll(@Nullable Player player, @NotNull byte[] colors) {
        Preconditions.checkNotNull(colors, "colors");
        Preconditions.checkArgument(colors.length == width * height * ItemFrameSlice.BUFFER_SIZE, "colors.length must be the same as width * height * 128");

        byte[] dst = new byte[ItemFrameSlice.BUFFER_SIZE];

        for (CanvasSlice slice : slices) {
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    dst[y * 128 + x] = colors[(slice.y() * 128 + y) * width * 128 + slice.x() * 128 + x];
                }
            }

            slice.setAll(player, dst);
        }
    }

    @Override
    public Color obtainPixelColor(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y) {
        return obtainPixelColor(null, x, y);
    }

    @Override
    public byte obtainPixel(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y) {
        return obtainPixel(null, x, y);
    }

    @Override
    public Color obtainPixelColor(@Nullable Player player, @Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y) {
        return sliceAt(x, y).obtainPixelColor(player, x % 128, y % 128);
    }

    @Override
    public byte obtainPixel(@Nullable Player player, @Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y) {
        return sliceAt(x, y).obtainPixel(player, x % 128, y % 128);
    }

    @Override
    public Color[] colorBuffer() {
        return colorBuffer(null);
    }

    @Override
    public Color[] colorBuffer(@Nullable Player player) {
        byte[] buffer = buffer(player);
        Color[] colors = new Color[buffer.length];

        for (int i = 0; i < buffer.length; i++) {
            colors[i] = MapPalette.getColor(buffer[i]);
        }

        return colors;
    }

    @Override
    public byte[] buffer() {
        return buffer(null);
    }

    @Override
    public byte[] buffer(@Nullable Player player) {
        byte[] bytes = new byte[width * height * ItemFrameSlice.BUFFER_SIZE];

        for (ItemFrameSlice slice : slices) {
            byte[] buffer = slice.buffer(player);

            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    bytes[(slice.y() * 128 + y) * width * 128 + slice.x() * 128 + x] = buffer[y * 128 + x];;
                }
            }
        }

        return bytes;
    }

    @Override
    public boolean isGlobal() {
        return global;
    }

    @Override
    public void clear(@NotNull Player player) {
        Preconditions.checkNotNull(player, "player");

        for (ItemFrameSlice slice : slices) {
            slice.clear(player);
        }
    }

    @Override
    public void verifyFrames() {
        Map<Integer, ItemFrame> availableFrames = new HashMap<>();
        int upperChunkX = higherCorner.getBlockX() >> 4;
        int upperChunkZ = higherCorner.getBlockZ() >> 4;

        for (int x = (lowerCorner.getBlockX() >> 4) - 1; x <= upperChunkX; x++) {
            for (int z = (lowerCorner.getBlockZ() >> 4) - 1; z <= upperChunkZ; z++) {
                world.loadChunk(x, z, false);
            }
        }

        world.getNearbyEntities(topLeftCorner(), axis == HorizontalAxis.X ? width : 1,
                height, axis == HorizontalAxis.Z ? width : 1).stream().filter(entity -> entity instanceof ItemFrame)
                .forEach(entity -> availableFrames.put(entity.getLocation().getBlock().hashCode(), (ItemFrame) entity));

        for (ItemFrameSlice slice : slices) {
            ItemFrame frame = availableFrames.get(slice.location().getBlock().hashCode());

            if(frame == null || isValidFrameItem(slice, frame.getItem())) {
                continue;
            }

            MapView view = Bukkit.createMap(world);

            ItemStack mapStack = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) mapStack.getItemMeta();

            meta.setMapView(view);

            mapStack.setItemMeta(meta);

            frame.setItem(mapStack);
            frame.setRotation(Rotation.NONE);
            frame.setFixed(true);

            view.addRenderer(slice);
        }
    }

    private boolean isValidFrameItem(ItemFrameSlice slice, ItemStack itemStack) {
        return itemStack != null && itemStack.getType() == Material.FILLED_MAP && itemStack.getItemMeta() instanceof MapMeta meta
                && meta.getMapView() != null && meta.getMapView().getRenderers().stream().anyMatch(mapRenderer -> mapRenderer == slice);
    }

    public AtomicBoolean hasListeners() {
        return hasListeners;
    }

    void reevaluateListeners() {
        for (CanvasSlice slice : slices) {
            if(!slice.listeners().isEmpty()) {
                hasListeners.set(true);

                break;
            }
        }
    }

    public void fireClickEvent(Location location, Player player, Vector position, boolean leftClick) {
        Location topLeftCorner = topLeftCorner();

        slice(Math.abs(axis.chooseBlockValue(topLeftCorner.toVector()) - axis.chooseBlockValue(location.toVector())),
                higherCorner.getBlockY() - location.getBlockY()).fireClickEvent(player, position, leftClick);
    }
}
