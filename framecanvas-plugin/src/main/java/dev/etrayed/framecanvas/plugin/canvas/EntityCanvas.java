package dev.etrayed.framecanvas.plugin.canvas;

import com.google.common.base.Preconditions;
import dev.etrayed.framecanvas.api.canvas.Canvas;
import dev.etrayed.framecanvas.api.canvas.CanvasSlice;
import dev.etrayed.framecanvas.api.canvas.HorizontalAxis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapView;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.awt.*;
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
    public @NotNull CompletableFuture<Void> displayImage(@NotNull Image image) {
        return displayImage(null, image);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull CompletableFuture<Void> displayImage(@Nullable Player player, @NotNull Image image) {
        Preconditions.checkNotNull(image, "image");

        return CompletableFuture.runAsync(() -> {
            byte[] imageBytes = MapPalette.imageToBytes(resizeImage(image));

            for (CanvasSlice slice : slices) {
                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < 128; y++) {
                        slice.setPixel(player, x, y, imageBytes[(slice.y() * 128 + y) * 128 * this.width + slice.x() * 128 + x]);
                    }
                }
            }
        });
    }

    private Image resizeImage(Image image) {
        if(image.getWidth(null) % 128 == 0 && image.getHeight(null) % 128 == 0) {
            return image;
        }

        BufferedImage resizedImage = new BufferedImage(width << 7 /* -> * 128 */, height << 7, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resizedImage.createGraphics();

        graphics.drawImage(image, 0, 0, resizedImage.getWidth(), resizedImage.getHeight(), null);
        graphics.dispose();

        return resizedImage;
    }

    @Override
    public void setPixel(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y, byte color) {
        setPixel(null, x, y, color);
    }

    @Override
    public void setPixel(@Nullable Player player, @Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y, byte color) {
        sliceAt(x, y).setPixel(player, x % 128, y % 128, color);
    }

    @Override
    public void fill(byte color) {
        fill(null, color);
    }

    @Override
    public void fill(@Nullable Player player, byte color) {
        for (CanvasSlice direct : slices) {
            direct.fill(player, color);
        }
    }

    @Override
    public void setAll(@NotNull byte[] colors) {
        setAll(null, colors);
    }

    @Override
    public void setAll(@Nullable Player player, @NotNull byte[] colors) {
        Preconditions.checkNotNull(colors, "colors");
        Preconditions.checkArgument(colors.length == width * height * ItemFrameSlice.BUFFER_SIZE, "colors.length must be the same as width * height * 128");

        byte[] dst = new byte[ItemFrameSlice.BUFFER_SIZE];

        for (CanvasSlice direct : slices) {
            System.arraycopy(colors, direct.y() * width + direct.x() * 128, dst, 0, ItemFrameSlice.BUFFER_SIZE);

            direct.setAll(player, dst);
        }
    }

    @Override
    public byte obtainPixel(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y) {
        return obtainPixel(null, x, y);
    }

    @Override
    public byte obtainPixel(@Nullable Player player, @Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y) {
        return sliceAt(x, y).obtainPixel(player, x % 128, y % 128);
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

    @SuppressWarnings("deprecation")
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

            frame.setItem(new ItemStack(Material.MAP, 1, view.getId()));

            view.addRenderer(slice);
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isValidFrameItem(ItemFrameSlice slice, ItemStack itemStack) {
        return itemStack != null && itemStack.getType() == Material.MAP && Bukkit.getMap(itemStack.getDurability()) != null
                && Bukkit.getMap(itemStack.getDurability()).getRenderers().stream().anyMatch(mapRenderer -> mapRenderer == slice);
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
        slice(axis.chooseBlockValue(location.toVector()) - axis.chooseBlockValue(lowerCorner),
                higherCorner.getBlockY() - location.getBlockY()).fireClickEvent(player, position, leftClick);
    }
}
