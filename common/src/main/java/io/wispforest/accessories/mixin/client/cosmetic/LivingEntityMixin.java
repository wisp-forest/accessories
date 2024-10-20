package io.wispforest.accessories.mixin.client.cosmetic;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.pond.CosmeticArmorLookupTogglable;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements CosmeticArmorLookupTogglable {

    private boolean accessories$cosmeticArmorAlternative = false;

    @Override
    public void setLookupToggle(boolean value) {
        var capability = AccessoriesCapability.get((LivingEntity) (Object) this);

        if(capability == null) {
            this.accessories$cosmeticArmorAlternative = false;

            return;
        }

        this.accessories$cosmeticArmorAlternative = value;
    }

    @Override
    public boolean getLookupToggle() {
        if(!((LivingEntity)(Object) this).level().isClientSide()) return false;

        return accessories$cosmeticArmorAlternative;
    }
}
