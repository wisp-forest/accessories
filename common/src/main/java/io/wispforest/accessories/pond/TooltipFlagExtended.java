package io.wispforest.accessories.pond;

import io.wispforest.accessories.AccessoriesClientInternals;
import net.minecraft.Util;
import net.minecraft.world.item.TooltipFlag;

public interface TooltipFlagExtended {
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

    default TooltipFlag withMask(int modifiers) {
        return new NestedTooltipFlag((TooltipFlag) this, modifiers);
    }

    default TooltipFlag withMask() {
        return new NestedTooltipFlag((TooltipFlag) this, AccessoriesClientInternals.getInstance().createBitFlag());
    }

    static TooltipFlag create() {
        return AccessoriesClientInternals.getInstance().createTooltipFlag();
    }

    static TooltipFlag create(boolean isAdvanced) {
        return (isAdvanced ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL).withMask();
    }
}

record NestedTooltipFlag(TooltipFlag flag, int modifiers) implements TooltipFlag {

    private static final boolean IS_OSX = Util.getPlatform() == Util.OS.OSX;
    private static final int EDIT_SHORTCUT_KEY_MODIFIER = IS_OSX ? 8 : 2;

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

    @Override
    public TooltipFlag getInnerFlag() {
        var baseFlag = flag.getInnerFlag();

        return (baseFlag instanceof NestedTooltipFlag nestedFlag) ? nestedFlag.getInnerFlag() : baseFlag;
    }
}
