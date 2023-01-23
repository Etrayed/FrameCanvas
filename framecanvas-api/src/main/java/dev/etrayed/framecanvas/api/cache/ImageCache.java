package dev.etrayed.framecanvas.api.cache;

import java.awt.image.BufferedImage;

/**
 * @author Etrayed
 */
public interface ImageCache {

    boolean isAutoCachingEnabled();

    void setAutoCachingEnabled(boolean enabled);

    void cache(BufferedImage image);

    boolean isCached(BufferedImage image);

    void clear(BufferedImage image);

    void clear();

    Byte[] getCached(BufferedImage image);
}
