package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.wispforest.accessories.pond.CosmeticArmorLookupTogglable;
import io.wispforest.accessories.pond.LivingEntityRenderStateExtension;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = EntityRenderer.class, remap = false)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState>{
    @WrapMethod(method = {
            "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            "method_55269(Lnet/minecraft/class_1297;F)Lnet/minecraft/class_10017;" //TODO: WHY DO I NEEDED THIS!
    }, remap = false, expect = 1, require = 1, allow = 1)
    private S accessories$adjustArmorLookup(T entity, float f, Operation<S> original) {
        var bl = entity instanceof LivingEntity;

        if (bl) ((CosmeticArmorLookupTogglable) entity).setLookupToggle(true);

        var state = original.call(entity, f);

        if (bl) {
            ((CosmeticArmorLookupTogglable) entity).setLookupToggle(false);

            ((LivingEntityRenderStateExtension) state).setEntity((LivingEntity) entity);
        }

        return state;
    }
}
