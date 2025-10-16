package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.wispforest.accessories.api.client.AccessoriesRenderStateKeys;
import io.wispforest.accessories.pond.CosmeticArmorLookupTogglable;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState>{

    @WrapMethod(method = {
        "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", // Mojmap
        "method_62425(Lnet/minecraft/class_1297;F)Lnet/minecraft/class_10017;",                                                  // Yarn Interm.
        "getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;"    // Yarn
    }, expect = 1, require = 1, allow = 1)
    private S accessories$adjustArmorLookup(T entity, float partialTick, Operation<S> original) {
        // TODO: THIS NEEDS BETTER METHOD FOR MAKING SURE THAT EXTRACTED RENDER STATES GET COSMETIC STACK REPLACEMENT PROPERLY
        var state = CosmeticArmorLookupTogglable.runWithLookupToggle(entity, () -> original.call(entity, partialTick));

        AccessoriesRenderStateKeys.setupStateForAccessories(state, entity, partialTick);

        return state;
    }
}
