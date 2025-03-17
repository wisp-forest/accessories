package io.wispforest.accessories.mixin;

import io.wispforest.accessories.pond.ItemBasedSteerable;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.animal.Pig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Pig.class)
public abstract class PigEntityMixin implements ItemBasedSteerable {

    @Shadow @Final private ItemBasedSteering steering;

    @Override
    public ItemBasedSteering getInstance() {
        return this.steering;
    }
}
