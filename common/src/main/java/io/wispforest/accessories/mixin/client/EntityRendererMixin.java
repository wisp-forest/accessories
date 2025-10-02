package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.wispforest.accessories.api.AccessoriesStorage;
import io.wispforest.accessories.api.SimpleAccessoriesStorage;
import io.wispforest.accessories.pond.AccessoriesRenderStateExtension;
import io.wispforest.accessories.pond.CosmeticArmorLookupTogglable;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;

@Mixin(value = EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState>{

    @WrapMethod(method = {
        "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", // Mojmap
        "method_62425(Lnet/minecraft/class_1297;F)Lnet/minecraft/class_10017;",                                                  // Yarn Interm.
        "getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;"    // Yarn
    }, expect = 1, require = 1, allow = 1)
    private S accessories$adjustArmorLookup(T entity, float partialTick, Operation<S> original) {
        // TODO: THIS NEEDS BETTER METHOD FOR MAKING SURE THAT EXTRACTED RENDER STATES GET COSMETIC STACK REPLACEMENT PROPERLY
        return CosmeticArmorLookupTogglable.runWithLookupToggle(entity, () -> original.call(entity, partialTick));
    }

    //TODO: FIGURE OUT WHY ARCH LOOM DON'T REMAP WRAP METHOD
    @Inject(method = {
            "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"
    }, at = @At("HEAD"))
    private void accessories$setExtensionLookup(T entity, S reusedState, float partialTick, CallbackInfo ci) {
        if (reusedState instanceof AccessoriesRenderStateExtension extension && entity instanceof LivingEntity livingEntity) {
            extension.accessories$setEntity(livingEntity); // TODO: REMOVE WITHIN FUTURE UPDATE
            extension.accessories$setPartialTicks(partialTick);
            extension.accessoreis$setEntityUUID(livingEntity.getUUID());

            var capability = livingEntity.accessoriesCapability();

            if (capability != null) {
                var map = new LinkedHashMap<String, AccessoriesStorage>();

                for (var entry : capability.getContainers().entrySet()) {
                    map.put(entry.getKey(), SimpleAccessoriesStorage.copy(entry.getValue()));
                }

                extension.accessories$storageLookup(map);
            }
        }
    }
}
