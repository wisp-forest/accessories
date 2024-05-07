package io.wispforest.tclayer.mixin;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import dev.emi.trinkets.data.SlotLoader;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(SlotTypeLoader.class)
public abstract class SlotTypeLoaderMixin {

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;"), remap = false)
    private void injectTrinketSpecificSlots(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci,  @Local(name = "tempMap") HashMap<String, SlotType> tempMap){
        for (var groupsData : SlotLoader.INSTANCE.getSlots().entrySet()) {
            var groupData = groupsData.getValue();
            var slots = groupData.slots;

            for (var entry : slots.entrySet()) {
                var accessoryType = WrappingTrinketsUtils.trinketsToAccessories_Slot(entry.getKey());

                if (tempMap.containsKey(accessoryType)) continue;

                var builder = new SlotTypeLoader.SlotBuilder(accessoryType);

                var slotData = entry.getValue();

                if (slotData.amount != -1) builder.amount(slotData.amount);

                builder.order(slotData.order);

                builder.icon(new ResourceLocation(slotData.icon));

                builder.dropRule(TrinketEnums.convert(TrinketEnums.DropRule.valueOf(slotData.dropRule)));

                builder.alternativeTranslation("trinkets.slot." + WrappingTrinketsUtils.accessoriesToTrinkets_Group(groupsData.getKey()) + "." + entry.getKey());

                for (String validatorPredicate : slotData.validatorPredicates) {
                    var location = ResourceLocation.tryParse(validatorPredicate);

                    if(location == null) continue;

                    builder.validator(location);
                }

                var slotType = builder.create();

                tempMap.put(accessoryType, slotType);
            }
        }
    }
}
