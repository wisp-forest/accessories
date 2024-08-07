package io.wispforest.accessories.mixin;

import io.wispforest.accessories.pond.ItemBasedSteerable;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.monster.Strider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Strider.class)
public abstract class StriderMixin implements ItemBasedSteerable {

    @Shadow
    @Final
    private ItemBasedSteering steering;

    @Override
    public ItemBasedSteering getInstance() {
        return steering;
    }
}
