package dev.etrayed.framecanvas.api.canvas;

import com.google.common.base.Preconditions;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * @author Etrayed
 */
public enum HorizontalAxis {

    X,
    Z;

    public int chooseBlockValue(@NotNull Vector vector) {
        Preconditions.checkNotNull(vector, "vector");

        return this == X ? vector.getBlockX() : vector.getBlockZ();
    }

    public void addValue(@NotNull Vector vector, int value) {
        Preconditions.checkNotNull(vector, "vector");

        vector.add(new Vector(this == X ? value : 0, 0, this == Z ? value : 0));
    }

    public void validateFace(@NotNull BlockFace face) {
        Preconditions.checkNotNull(face, "face is null!");

        if(this == X) {
            Preconditions.checkArgument(face == BlockFace.NORTH || face == BlockFace.SOUTH, "face must be NORTH or SOUTH (axis = X)");
        } else {
            Preconditions.checkArgument(face == BlockFace.WEST || face == BlockFace.EAST, "face must be WEST OR EAST (axis = Z)");
        }
    }
}
