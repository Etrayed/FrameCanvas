package dev.etrayed.framecanvas.plugin.canvas;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import dev.etrayed.framecanvas.api.canvas.Canvas;
import dev.etrayed.framecanvas.api.canvas.CanvasSlice;
import dev.etrayed.framecanvas.api.canvas.click.ClickListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

import java.awt.Image;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Etrayed
 */
public class ItemFrameSlice extends MapRenderer implements CanvasSlice {

    static final int BUFFER_SIZE = 128 * 128;

    private final EntityCanvas canvas;

    private final int x, y;

    private final boolean global;

    private final List<ClickListener> listeners;

    private DirtyBuffer buffer; // only if global == true

    private Map<Player, DirtyBuffer> contextualBuffer; // only if global == false

    ItemFrameSlice(EntityCanvas canvas, int x, int y, boolean global) {
        super(!global);

        this.canvas = canvas;
        this.x = x;
        this.y = y;
        this.global = global;
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public @NotNull Canvas canvas() {
        return canvas;
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int x() {
        return x;
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int y() {
        return y;
    }

    @Override
    public @NotNull Location location() {
        Vector vector = canvas.topLeftCorner().toVector();

        canvas.axis().addValue(vector, canvas.direction(), x);

        vector.setY(vector.getY() - y);

        return vector.toLocation(canvas.world());
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
            byte[] imageBytes = MapPalette.imageToBytes(MapPalette.resizeImage(image));

            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    byte imgByte = imageBytes[y * 128 + x];

                    if(imgByte != MapPalette.TRANSPARENT) {
                        setPixel(player, x, y, imgByte);
                    }
                }
            }
        });
    }

    @Override
    public void setPixel(@Range(from = 0, to = 127) int x, @Range(from = 0, to = 127) int y, byte color) {
        setPixel(null, x, y, color);
    }

    @Override
    public void setPixel(@Nullable Player player, @Range(from = 0, to = 127) int x, @Range(from = 0, to = 127) int y, byte color) {
        Preconditions.checkArgument(x >= 0 && x < 127, "x must be between 0 and 127");
        Preconditions.checkArgument(y >= 0 && y < 127, "y must be between 0 and 127");

        ensureBuffer();

        obtainBuffer(player)[y * 128 + x] = color;

        setDirty(x, y, player);
    }

    @Override
    public void fill(byte color) {
        fill(null, color);
    }

    @Override
    public void fill(@Nullable Player player, byte color) {
        ensureBuffer();

        Arrays.fill(obtainBuffer(player), color);

        setDirty(0, 0, player);
        setDirty(127, 127, player);
    }

    @Override
    public void setAll(@NotNull byte[] colors) {
        setAll(null, colors);
    }

    @Override
    public void setAll(@Nullable Player player, @NotNull byte[] colors) {
        Preconditions.checkNotNull(colors, "colors");
        Preconditions.checkArgument(colors.length == BUFFER_SIZE, "colors.length must be " + BUFFER_SIZE);

        ensureBuffer();

        System.arraycopy(colors, 0, obtainBuffer(player), 0, BUFFER_SIZE);

        setDirty(0, 0, player);
        setDirty(127, 127, player);
    }

    private void ensureBuffer() {
        if(!isEmpty()) {
            return;
        }

        if(global) {
            buffer = new DirtyBuffer();
        } else {
            contextualBuffer = Collections.synchronizedMap(new WeakHashMap<>());
        }
    }

    @Override
    public byte obtainPixel(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y) {
        return obtainPixel(null, x, y);
    }

    @Override
    public byte obtainPixel(@Nullable Player player, @Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y) {
        if(isEmpty() || !(global || player == null || contextualBuffer.containsKey(player))) {
            return 0;
        }

        return obtainBuffer(player)[y * 128 + x];
    }

    private byte[] obtainBuffer(@Nullable Player player) {
        return global || player == null ? buffer.buffer() : contextualBuffer.computeIfAbsent(player, unused -> new DirtyBuffer()).buffer();
    }

    private void setDirty(int x, int y, @Nullable Player player) {
        DirtyBuffer buffer = global || player == null ? this.buffer : contextualBuffer.get(player);

        if(buffer == null) {
            return;
        }

        if(x == -1 && y == -1) {
            if(buffer.canvas != null) {
                try {
                    MapDirtyMarker.markDirty(buffer.canvas.getMapView(), 127, 127, player);
                    MapDirtyMarker.markDirty(buffer.canvas.getMapView(), 0, 0, player);

                    return;
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable); // Cannot occur in default spigot implementation.
                }
            }

            buffer.lowDirtyX = buffer.lowDirtyY = 0;
            buffer.highDirtyX = buffer.highDirtyY = 127;

            return;
        }

        if(buffer.canvas != null) {
            try {
                MapDirtyMarker.markDirty(buffer.canvas.getMapView(), x, y, player);

                return;
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable); // Cannot occur in default spigot implementation.
            }
        }

        if(buffer.lowDirtyX == -1) {
            buffer.lowDirtyX = x;
            buffer.lowDirtyY = y;
            buffer.highDirtyX = x;
            buffer.highDirtyY = y;
        } else {
            buffer.lowDirtyX = Math.min(x, buffer.lowDirtyX);
            buffer.lowDirtyY = Math.min(y, buffer.lowDirtyY);
            buffer.highDirtyX = Math.max(x, buffer.highDirtyX);
            buffer.highDirtyY = Math.max(y, buffer.highDirtyY);
        }
    }

    @Override
    public boolean isEmpty() {
        return (global ? buffer : contextualBuffer) == null;
    }

    @Override
    public boolean isGlobal() {
        return global;
    }

    @Override
    public byte[] buffer() {
        return buffer.buffer();
    }

    @Override
    public byte[] buffer(@Nullable Player player) {
        return obtainBuffer(player);
    }

    @Override
    public void clear(@NotNull Player player) {
        Preconditions.checkNotNull(player, "player");

        if(global || contextualBuffer == null) {
            return;
        }

        contextualBuffer.remove(player);
    }

    @Override
    public void addListener(@NotNull ClickListener listener) {
        Preconditions.checkNotNull(listener, "listener");

        listeners.add(listener);

        canvas.hasListeners().compareAndSet(false, true);
    }

    @Override
    public boolean isListening(@Nullable ClickListener listener) {
        return listener != null && listeners.contains(listener);
    }

    @Override
    public @NotNull @Unmodifiable ImmutableList<ClickListener> listeners() {
        return ImmutableList.copyOf(listeners);
    }

    @Override
    public void removeListener(@NotNull ClickListener listener) {
        Preconditions.checkNotNull(listener, "listener");

        listeners.remove(listener);

        canvas.reevaluateListeners();
    }

    @Override
    public void removeAllListeners() {
        listeners.clear();

        canvas.reevaluateListeners();
    }

    @Override
    public void fireClickEvent(Player player, Vector position, boolean leftClick) {
        Preconditions.checkNotNull(player, "player");

        for (ClickListener listener : listeners) {
            if(position != null) {
                listener.handlePositionalClick(this, player, position);
            } else {
                listener.handleClick(this, player, leftClick);
            }
        }
    }

    @Override
    public void initialize(MapView map) {
        map.getRenderers().stream().filter(renderer -> renderer != this).forEach(map::removeRenderer);
    }

    private static FieldAccessor bufferField;

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if(bufferField == null) {
            bufferField = Accessors.getFieldAccessor(canvas.getClass(), byte[].class, true);
        }

        if(isEmpty()) {
            return;
        }

        try {
            verifyBuffer(isContextual() ? player : null, canvas);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void verifyBuffer(Player player, MapCanvas canvas) {
        DirtyBuffer buffer = global || player == null ? this.buffer : contextualBuffer.get(player);

        if(buffer.canvas == canvas) {
            return;
        }

        if(buffer.buffer == null && buffer.canvas == null) {
            buffer.canvas = canvas;

            return;
        }

        Object oldBuffer;

        if(buffer.canvas == null) {
            oldBuffer = buffer.buffer;

            buffer.buffer = null;
        } else {
            oldBuffer = bufferField.get(buffer.canvas);
        }

        bufferField.set(canvas, oldBuffer);

        buffer.canvas = canvas;

        int lowDirtyX = buffer.lowDirtyX, lowDirtyY = buffer.lowDirtyY, highDirtyX = buffer.highDirtyX, highDirtyY = buffer.highDirtyY;

        if(lowDirtyX == -1) {
            lowDirtyX = 0;
            lowDirtyY = 0;
            highDirtyX = 127;
            highDirtyY = 127;
        }

        MapDirtyMarker.markDirty(canvas.getMapView(), lowDirtyX, lowDirtyY, player);
        MapDirtyMarker.markDirty(canvas.getMapView(), highDirtyX, highDirtyY, player);

        buffer.lowDirtyX = buffer.lowDirtyY = buffer.highDirtyX = buffer.highDirtyY = -1;
    }

    private static final class DirtyBuffer {

        private byte[] buffer;

        private MapCanvas canvas;

        private int lowDirtyX = -1, lowDirtyY = -1, highDirtyX = -1, highDirtyY = -1;

        @SuppressWarnings("CheckForOutOfMemoryOnLargeArrayAllocation")
        private byte[] buffer() {
            if(canvas != null) {
                return (byte[]) bufferField.get(canvas);
            }

            return buffer == null ? buffer = new byte[BUFFER_SIZE] : buffer;
        }
    }
}
