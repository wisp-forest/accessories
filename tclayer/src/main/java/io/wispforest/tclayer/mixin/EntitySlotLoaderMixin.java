package io.wispforest.tclayer.mixin;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Debug(export = true)
@Mixin(EntitySlotLoader.class)
public abstract class EntitySlotLoaderMixin {

    @Unique
    private static final Logger TRINKET_LOGGER = LogUtils.getLogger();

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;"))
    private void injectTrinketSpecificSlots(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci, @Local(name = "tempMap") HashMap<EntityType<?>, Map<String, SlotType>> tempMap){
        var loader = dev.emi.trinkets.data.EntitySlotLoader.SERVER;

        var slotTypes = SlotTypeLoader.INSTANCE.getSlotTypes(false);

        for (var entry : loader.slotInfo.entrySet()) {
            var innerMap = tempMap.computeIfAbsent(entry.getKey(), entityType -> new HashMap<>());

            for (String s : entry.getValue()) {
                var convertedType = WrappingTrinketsUtils.trinketsToAccessories_Slot(s);

                if (innerMap.containsKey(convertedType)) {
                    continue;
                }

                var slotType = slotTypes.get(convertedType);

                if (slotType == null) {
                    TRINKET_LOGGER.warn("Unable to locate the given slot for a given entity binding, such will be skipped: [Name: {}]", convertedType);

                    continue;
                }

                innerMap.put(slotType.name(), slotType);
            }
        }
    }
}
