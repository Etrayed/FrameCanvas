package dev.etrayed.framecanvas.plugin.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.etrayed.framecanvas.api.cache.ImageCache;
import org.bukkit.map.MapPalette;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * @author Etrayed
 */
public class SerializingImageCache implements ImageCache {

    private final Cache<Integer, Byte[]> backingCache;

    private boolean autoCachingEnabled;

    public SerializingImageCache() {
        this.backingCache = CacheBuilder.newBuilder().concurrencyLevel(4).build();
    }

    @Override
    public boolean isAutoCachingEnabled() {
        return autoCachingEnabled;
    }

    @Override
    public void setAutoCachingEnabled(boolean enabled) {
        this.autoCachingEnabled = enabled;
    }

    @Override
    public void cache(BufferedImage image) {
        backingCache.put(image.hashCode(), serialize(image));
    }

    public Byte[] serialize(BufferedImage image) {
        Byte[] data = new Byte[image.getWidth() * image.getHeight()];

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                byte color = MapPalette.matchColor(new Color(image.getRGB(x, y), true));

                if(color != MapPalette.TRANSPARENT) {
                    data[y * image.getWidth() + x] = color;
                }
            }
        }

        return data;
    }

    @Override
    public boolean isCached(BufferedImage image) {
        return backingCache.getIfPresent(image.hashCode()) != null;
    }

    @Override
    public void clear(BufferedImage image) {
        backingCache.invalidate(image.hashCode());
    }

    @Override
    public void clear() {
        backingCache.invalidateAll();
    }

    @Override
    public Byte[] getCached(BufferedImage image) {
        return backingCache.getIfPresent(image.hashCode());
    }
}
