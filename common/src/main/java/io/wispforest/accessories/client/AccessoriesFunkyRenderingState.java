package io.wispforest.accessories.client;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.slot.SlotPath;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3d;
import org.joml.Vector4i;
import org.joml.Vector4ic;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

///
/// An internal class for getting some extra context to various areas of rendering to assist some fancy shit
///
@ApiStatus.Internal
public class AccessoriesFunkyRenderingState {

    public static AccessoriesFunkyRenderingState INSTANCE = new AccessoriesFunkyRenderingState();

    // Used with the AccessoryRenderLayer to set if the buffer should be the hover buffer instead
    private boolean OVERRIDE_RENDER_TARGET = false;

    // are we currently rendering an entity in a screen
    private boolean IS_RENDERING_UI_ENTITY = false;

    // are we currently rendering the entity that lines should be drawn to
    private boolean IS_RENDERING_LINE_TARGET = false;

    // are we collecting positions for hover i.e. line drawing or clickbait
    private boolean COLLECT_ACCESSORY_POSITIONS = false;

    public void wrapEntityRendering(int x1, int y1, int x2, int y2, Consumer<Consumer<Runnable>> renderingCall) {
        SCISSOR_BOX.set(x1, y1, x2, y2);

        var hoverOptions = Accessories.config().screenOptions.hoveredOptions;

        COLLECT_ACCESSORY_POSITIONS = hoverOptions.line() || hoverOptions.clickbait();

        IS_RENDERING_UI_ENTITY = true;

        renderingCall.accept(runnable -> {
            IS_RENDERING_LINE_TARGET = true;

            runnable.run();

            IS_RENDERING_LINE_TARGET = false;
        });

        IS_RENDERING_UI_ENTITY = false;

        COLLECT_ACCESSORY_POSITIONS = false;

        SCISSOR_BOX.set(0, 0,0, 0);
    }

    public void wrapBufferManipulation(Runnable runnable) {
        OVERRIDE_RENDER_TARGET = true;
        try {
            runnable.run();
        } finally {
            OVERRIDE_RENDER_TARGET = false;
        }
    }

    private Map<SlotPath, Vector3d> NOT_VERY_NICE_POSITIONS = new HashMap<>();

    private Vector4i SCISSOR_BOX = new Vector4i();

    public boolean isOverrideRenderTarget() {
        return OVERRIDE_RENDER_TARGET;
    }

    public boolean isIsRenderingUiEntity() {
        return IS_RENDERING_UI_ENTITY;
    }

    public boolean isIsRenderingLineTarget() {
        return IS_RENDERING_LINE_TARGET;
    }

    public boolean isCollectAccessoryPositions() {
        return COLLECT_ACCESSORY_POSITIONS;
    }

    public Map<SlotPath, Vector3d> getNotVeryNicePositions() {
        return NOT_VERY_NICE_POSITIONS;
    }

    public Vector4ic getScissorBox() {
        return SCISSOR_BOX;
    }
}
