package io.wispforest.accessories.misc;

import net.minecraft.world.level.gamerules.GameRule;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public class AccessoriesGameRules {

    @Nullable
    private static GameRule<Boolean> RULE_KEEP_ACCESSORY_INVENTORY = null;

    public static void init(GameRule<Boolean> key) {
        RULE_KEEP_ACCESSORY_INVENTORY = key;
    }

    @Nullable
    public static GameRule<Boolean> getKeepAccessoryInventoryKey() {
        return RULE_KEEP_ACCESSORY_INVENTORY;
    }
}
