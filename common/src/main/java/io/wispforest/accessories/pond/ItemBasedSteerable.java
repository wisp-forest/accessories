package io.wispforest.accessories.pond;

import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.Saddleable;

public interface ItemBasedSteerable extends Saddleable {
    ItemBasedSteering getInstance();
}
