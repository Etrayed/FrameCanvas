package dev.etrayed.framecanvas.plugin.canvas;

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

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
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
    public void setPixel(@Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y, byte color) {
        setPixel(null, x, y, color);
    }

    @Override
    public void setPixel(@Nullable Player player, @Range(from = 0, to = Integer.MAX_VALUE) int x, @Range(from = 0, to = Integer.MAX_VALUE) int y, byte color) {
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
        return global || player == null ? buffer.buffer : contextualBuffer.computeIfAbsent(player, unused -> new DirtyBuffer()).buffer;
    }

    private void setDirty(int x, int y, @Nullable Player player) {
        DirtyBuffer buffer = global || player == null ? this.buffer : contextualBuffer.get(player);

        if(buffer == null) {
            return;
        }

        if(x == -1 && y == -1) {
            buffer.lowDirtyX = -1;
            buffer.lowDirtyY = -1;
            buffer.highDirtyX = -1;
            buffer.highDirtyY = - 1;
        } else {
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
    public void clear(@NotNull Player player) {
        Preconditions.checkNotNull(player, "player");

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

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if(isEmpty()) {
            return;
        }

        try {
            omitBuffer(isContextual() ? player : null, canvas);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static Field bufferField, worldMapField;

    private static Method flagDirtyMethod;

    private void omitBuffer(Player player, MapCanvas canvas) throws ReflectiveOperationException {
        if(bufferField == null) {
            initReflectiveCanvasFields(canvas);
        }

        DirtyBuffer buffer = global || player == null ? this.buffer : contextualBuffer.get(player);

        int lowDirtyX = buffer.lowDirtyX, lowDirtyY = buffer.lowDirtyY, highDirtyX = buffer.highDirtyX, highDirtyY = buffer.highDirtyY;

        if(lowDirtyX == -1) { // seems like we're forced to update everything
            lowDirtyX = 0;
            lowDirtyY = 0;
            highDirtyX = 127;
            highDirtyY = 127;
        }

        setDirty(-1, -1, player);

        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(buffer.buffer, 0, bufferField.get(canvas), 0, BUFFER_SIZE);

        Object worldMapInstance = worldMapField.get(canvas.getMapView());

        flagDirtyMethod.invoke(worldMapInstance, lowDirtyX, lowDirtyY);
        flagDirtyMethod.invoke(worldMapInstance, highDirtyX, highDirtyY);
    }

    private void initReflectiveCanvasFields(MapCanvas canvas) throws ReflectiveOperationException {
        bufferField = canvas.getClass().getDeclaredField("buffer");
        worldMapField = canvas.getMapView().getClass().getDeclaredField("worldMap");
        flagDirtyMethod = worldMapField.getType().getDeclaredMethod("flagDirty", Integer.TYPE, Integer.TYPE);

        bufferField.setAccessible(true);
        worldMapField.setAccessible(true);
        flagDirtyMethod.setAccessible(true);
    }

    private static final class DirtyBuffer {

        private final byte[] buffer;

        private int lowDirtyX = -1, lowDirtyY = -1, highDirtyX = -1, highDirtyY = -1;

        @SuppressWarnings("CheckForOutOfMemoryOnLargeArrayAllocation")
        private DirtyBuffer() {
            this.buffer = new byte[BUFFER_SIZE];
        }
    }
}
