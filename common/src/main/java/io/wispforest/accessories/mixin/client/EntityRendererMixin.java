package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.wispforest.accessories.api.AccessoriesStorage;
import io.wispforest.accessories.api.SimpleAccessoriesStorage;
import io.wispforest.accessories.pond.AccessoriesRenderStateExtension;
import io.wispforest.accessories.pond.CosmeticArmorLookupTogglable;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

import java.util.LinkedHashMap;

@Mixin(value = EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState>{
    //TODO: FIGURE OUT WHY ARCH LOOM DON'T REMAP WRAP METHOD
    @WrapMethod(method = {
            "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", // Mojmap
            "method_62354(Lnet/minecraft/class_1297;Lnet/minecraft/class_10017;F)V",                                                   // Yarn Interm.
            "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V"           // Yarn
    }, expect = 1, require = 1, allow = 1)
    private void accessories$adjustArmorLookup(T entity, S reusedState, float partialTick, Operation<Void> original) {
        var bl = entity instanceof CosmeticArmorLookupTogglable;

        if (bl) ((CosmeticArmorLookupTogglable) entity).setLookupToggle(true);

        original.call(entity, reusedState, partialTick);

        if (bl) ((CosmeticArmorLookupTogglable) entity).setLookupToggle(false);

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
