package io.wispforest.tclayer.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.compat.WrappedAccessory;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(value = AccessoriesRendererRegistry.class)
public abstract class AccessoriesRendererRegistryMixin {
    @WrapOperation(method = "Lio/wispforest/accessories/api/client/AccessoriesRendererRegistry;getRender(Lnet/minecraft/world/item/Item;)Lio/wispforest/accessories/api/client/AccessoryRenderer;", at = @At(value = "INVOKE", target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object alterDefaultBehavior(Map<Item, AccessoryRenderer> CACHED_RENDERERS, Object item, Object defaultRenderer, Operation<Object> operation) {
        var trinket = TrinketsApi.getTrinket((Item) item);

        if(!(trinket == TrinketsApi.getDefaultTrinket() || trinket instanceof WrappedAccessory)) {
            defaultRenderer = null;
        }

        return operation.call(CACHED_RENDERERS, item, defaultRenderer);
    }
}
