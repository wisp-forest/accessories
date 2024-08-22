package io.wispforest.cclayer.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.capability.ItemizedCurioCapability;
import top.theillusivec4.curios.compat.WrappedAccessory;

import java.util.Map;

@Mixin(value = AccessoriesRendererRegistry.class, remap = false)
public abstract class AccessoriesRendererRegistryMixin {
    @WrapOperation(method = "Lio/wispforest/accessories/api/client/AccessoriesRendererRegistry;getRender(Lnet/minecraft/world/item/Item;)Lio/wispforest/accessories/api/client/AccessoryRenderer;", at = @At(value = "INVOKE", target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), remap = false)
    private static Object alterDefaultBehavior(Map<Item, AccessoryRenderer> CACHED_RENDERERS, Object item, Object defaultRenderer, Operation<Object> operation) {
        var stack = ((Item) (item)).getDefaultInstance();

        var iCurioItem = CuriosApi.getCurio(stack).orElse(null);

        if(!(iCurioItem instanceof ItemizedCurioCapability capability && capability.curioItem instanceof WrappedAccessory)) {
            defaultRenderer = null;
        }

        return operation.call(CACHED_RENDERERS, item, defaultRenderer);
    }
}