package io.wispforest.accessories.pond;

import net.minecraft.Util;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.world.item.TooltipFlag;

public interface TooltipFlagExtension {
    default boolean hasControlDown() {
        return false;
    }

    default boolean hasShiftDown() {
        return false;
    }

    default boolean hasAltDown() {
        return false;
    }

    default int getModifiers() {
        return 0;
    }

    default TooltipFlag getInnerFlag() {
        return TooltipFlag.NORMAL;
    }

    static TooltipFlag createFlag(TooltipFlag flag, int modifiers) {
        return new TooltipFlag() {
            static final boolean IS_OSX = Util.getPlatform() == Util.OS.OSX;
            static final int EDIT_SHORTCUT_KEY_MODIFIER = IS_OSX ? 8 : 2;
            @Override
            public boolean isAdvanced() {
                return flag.isAdvanced();
            }

            @Override
            public boolean isCreative() {
                return flag.isCreative();
            }

            @Override
            public boolean hasAltDown() {
                return (this.getModifiers() & 4) != 0;
            }

            @Override
            public boolean hasShiftDown() {
                return (this.getModifiers() & 1) != 0;
            }

            @Override
            public boolean hasControlDown() {
                return (this.getModifiers() & EDIT_SHORTCUT_KEY_MODIFIER) != 0;
            }

            public int getModifiers() {
                return modifiers;
            }

            public TooltipFlag getInnerFlag() {
                return TooltipFlag.NORMAL;
            }
        };
    }
}
