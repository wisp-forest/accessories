package io.wispforest.tclayer.mixin;

import com.google.gson.JsonObject;
import dev.emi.trinkets.api.TrinketConstants;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(EntitySlotLoader.class)
public abstract class EntitySlotLoaderMixin {

    @Shadow
    @Mutable
    private Map<EntityType<?>, Map<String, SlotType>> server;

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    private void injectTrinketSpecificSlots(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci){
        var map = server;

        var loader = dev.emi.trinkets.data.EntitySlotLoader.SERVER;

        var slotTypes = SlotTypeLoader.INSTANCE.getSlotTypes(false);

        for (var entry : loader.slotInfo.entrySet()) {
            var innerMap = map.computeIfAbsent(entry.getKey(), entityType -> new HashMap<>());

            for (String s : entry.getValue()) {
                var convertedType = TrinketConstants.trinketsToAccessories(s);

                if (innerMap.containsKey(convertedType)) continue;

                var slotType = slotTypes.get(convertedType);

                innerMap.put(slotType.name(), slotType);
            }
        }
    }
}
