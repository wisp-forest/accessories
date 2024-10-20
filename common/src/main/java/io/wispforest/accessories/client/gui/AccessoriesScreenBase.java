package io.wispforest.accessories.client.gui;

import io.wispforest.accessories.Accessories;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.joml.Vector3d;
import org.joml.Vector4i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface AccessoriesScreenBase {
    // are we currently rendering an entity in a screen
    MutableBoolean IS_RENDERING_UI_ENTITY = new MutableBoolean(false);

    // are we currently rendering the entity that lines should be drawn to
    MutableBoolean IS_RENDERING_LINE_TARGET = new MutableBoolean(false);

    MutableBoolean COLLECT_ACCESSORY_POSITIONS = new MutableBoolean(false);

    static void togglePositionCollection() {
        var hoverOptions = Accessories.config().screenOptions.hoveredOptions;

        COLLECT_ACCESSORY_POSITIONS.setValue(hoverOptions.line() || hoverOptions.clickbait());
    }

    MutableBoolean FORCE_TOOLTIP_LEFT = new MutableBoolean(false);

    Map<String, Vector3d> NOT_VERY_NICE_POSITIONS = new HashMap<>();

    List<Pair<Vector3d, Vector3d>> ACCESSORY_LINES = new ArrayList<>();

    List<Vector3d> ACCESSORY_POSITIONS = new ArrayList<>();

    Vector4i SCISSOR_BOX = new Vector4i();

    void onHolderChange(String key);

    LivingEntity targetEntityDefaulted();

    Slot getHoveredSlot();
}
