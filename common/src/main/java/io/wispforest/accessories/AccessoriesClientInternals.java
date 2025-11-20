package io.wispforest.accessories;

import io.wispforest.accessories.pond.TooltipFlagExtension;
import io.wispforest.accessories.utils.ServiceLoaderUtils;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

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

    public TooltipFlag createTooltipFlag(TooltipFlag flag) {
        return TooltipFlagExtension.createFlag(flag, Integer.MAX_VALUE);
    }
}
