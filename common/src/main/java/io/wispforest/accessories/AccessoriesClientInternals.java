package io.wispforest.accessories;

import io.wispforest.accessories.api.tooltip.*;
import io.wispforest.accessories.pond.TooltipFlagExtended;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.TooltipFlag;

import java.util.Collection;
import java.util.stream.Stream;

public abstract class AccessoriesClientInternals {

    private static boolean hasInstanceBeenSet = false;

    private static AccessoriesClientInternals INSTANCE = new AccessoriesClientInternals() {};

    public static AccessoriesClientInternals getInstance() {
        return INSTANCE;
    }

    public static void setInstance(AccessoriesClientInternals instance) {
        if (hasInstanceBeenSet) {
            throw new IllegalStateException("Unable to set AccessoriesClientInternals as it already has been setup before!");
        }

        INSTANCE = instance;

        hasInstanceBeenSet = true;
    }

    public TooltipComponentBuilder createTooltipBuilder() {
        return new TooltipComponentBuilder() {
            @Override
            public TooltipComponentBuilder add(TooltipComponentHolder holder) {
                return this;
            }

            @Override
            public TooltipComponentBuilder divider() {
                return this;
            }

            @Override
            public TooltipComponentBuilder divider(int height) {
                return this;
            }

            @Override
            public TooltipComponentBuilder add(FormattedText text) {
                return this;
            }

            @Override
            public TooltipComponentBuilder add(TooltipComponentBuilder builder) {
                return this;
            }

            @Override
            public TooltipComponentBuilder addAll(Collection<? extends FormattedText> text) {
                return this;
            }

            @Override
            public TooltipComponentBuilder addAll(TextPrefixer prefixer, FormattedTextBuilder builder) {
                return this;
            }

            public boolean isEmpty() {
                return true;
            }
        };
    }

    public TextWrapper createWrapper(int maxWidth, Style overrideStyle) {
        return TextWrapper.NONE;
    }

    public TooltipFlag createTooltipFlag() {
        return TooltipFlagExtended.create(false);
    }

    public int createBitFlag() {
        return createBitFlag(true, true, true);
    }

    public int createBitFlag(boolean hasShift, boolean hasControl, boolean hasAlt) {
        return Integer.MAX_VALUE;
    }
}
