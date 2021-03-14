package dev.etrayed.framecanvas.api.canvas.click;

import com.google.common.collect.ImmutableList;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * @author Etrayed
 */
public interface Clickable {

    void addListener(@NotNull ClickListener listener);

    @Contract("null -> false")
    boolean isListening(@Nullable ClickListener listener);

    @NotNull
    @Unmodifiable
    ImmutableList<ClickListener> listeners();

    void removeListener(@NotNull ClickListener listener);

    void removeAllListeners();

    void fireClickEvent(@NotNull Player player, @Nullable Vector position, boolean leftClick);
}
