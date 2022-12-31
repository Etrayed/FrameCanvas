package dev.etrayed.framecanvas.plugin.canvas;

import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.reflect.fuzzy.FuzzyFieldContract;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Etrayed
 */
final class MapDirtyMarker {

    private static FieldAccessor handleFieldAccessor, playerMapDataAccessor;

    private static MethodAccessor markDirtyAccessor, globalDirtyAccessor, handleMethod;

    static {
        Class<?> craftClass = MinecraftReflection.getCraftBukkitClass("map.CraftMapView");
        Class<?> worldMapClass = craftClass.getConstructors()[0].getParameterTypes()[0];
        Class<?> innerClass = Arrays.stream(worldMapClass.getDeclaredClasses()).filter(toCheck -> !Modifier.isStatic(toCheck.getModifiers())).findFirst().get();

        handleFieldAccessor = Accessors.getFieldAccessor(craftClass, worldMapClass, true);
        playerMapDataAccessor = Accessors.getFieldAccessor(FuzzyReflection.fromClass(worldMapClass, true).getFieldList(FuzzyFieldContract.newBuilder().typeExact(Map.class).requirePublic().requireModifier(Modifier.FINAL).build()).get(0));
        markDirtyAccessor = Accessors.getMethodAccessor(FuzzyReflection.fromClass(innerClass, true).getMethod(FuzzyMethodContract.newBuilder()
                .parameterExactArray(int.class, int.class)
                .returnTypeVoid()
                .build()
        ));
        globalDirtyAccessor = Accessors.getMethodAccessor(FuzzyReflection.fromClass(worldMapClass, true)
                .getMethod(FuzzyMethodContract.newBuilder()
                        .parameterExactArray(int.class, int.class)
                        .returnTypeVoid()
                        .requirePublic()
                        .build()));
        handleMethod = Accessors.getMethodAccessor(MinecraftReflection.getCraftPlayerClass(), "getHandle");
    }

    private MapDirtyMarker() {
    }

    static void markDirty(MapView view, int startX, int startY, Player player) {
        if(player == null) {
            markGlobalDirty(view, startX, startY);
        } else {
            markSingleDirty(view, startX, startY, player);
        }
    }

    private static void markGlobalDirty(MapView view, int startX, int startY) {
        globalDirtyAccessor.invoke(handleFieldAccessor.get(view), startX, startY);
    }

    private static void markSingleDirty(MapView view, int startX, int startY, Player player) {
        Object mapHandle = handleFieldAccessor.get(view);
        Object mapDataHandle = ((Map) playerMapDataAccessor.get(mapHandle)).get(handleMethod.invoke(player));

        markDirtyAccessor.invoke(mapDataHandle, startX, startY);
    }
}
