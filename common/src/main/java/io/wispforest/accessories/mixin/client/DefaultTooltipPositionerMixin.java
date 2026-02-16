package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.wispforest.accessories.client.gui.AccessoriesScreenBase;
import io.wispforest.accessories.pond.DefaultTooltipPositionerExt;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.UnaryOperator;

@Mixin(DefaultTooltipPositioner.class)
public abstract class DefaultTooltipPositionerMixin implements DefaultTooltipPositionerExt {

    private @Nullable PositionAdjuster adjuster = null;

    @Invoker("<init>")
    public static DefaultTooltipPositioner accessories$createPositioner() {
        throw new IllegalStateException();
    }

    @WrapMethod(method = "positionTooltip(IILorg/joml/Vector2i;II)V")
    private void accessories$forceLeftPositioning(int screenWidth, int screenHeight, Vector2i tooltipPos, int tooltipWidth, int tooltipHeight, Operation<Void> original) {
        if (adjuster != null) adjuster.adjust(screenWidth, tooltipPos, tooltipWidth);

        original.call(screenWidth, screenHeight, tooltipPos, tooltipWidth, tooltipHeight);
    }

    @Override
    public void accessories$setOperator(PositionAdjuster adjuster) {
        this.adjuster = adjuster;
    }

    @Override
    public DefaultTooltipPositioner accessories$copyWith(PositionAdjuster adjuster) {
        var positioner = accessories$createPositioner();

        ((DefaultTooltipPositionerExt) positioner).accessories$setOperator(adjuster);

        return positioner;
    }
}