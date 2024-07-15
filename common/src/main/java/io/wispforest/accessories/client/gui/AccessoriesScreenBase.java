package io.wispforest.accessories.client.gui;

import net.minecraft.world.entity.LivingEntity;

public interface AccessoriesScreenBase {
    void onHolderChange(String key);

    LivingEntity targetEntityDefaulted();
}
