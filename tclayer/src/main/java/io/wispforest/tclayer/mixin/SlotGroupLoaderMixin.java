package io.wispforest.tclayer.mixin;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.trinkets.data.SlotLoader;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotGroupLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(SlotGroupLoader.class)
public class SlotGroupLoaderMixin {
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private void injectTrinketsGroupingInfo(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci, @Local(name = "slotGroups") HashMap<String, SlotGroupLoader.SlotGroupBuilder> slotGroups, @Local(name = "allSlots") HashMap<String, SlotType> allSlots){
        for (var groupEntry : SlotLoader.INSTANCE.getSlots().entrySet()) {
            var groupData = groupEntry.getValue();
            var slots = groupData.slots;

            for (var slotEntry : slots.entrySet()) {
                var slotName = slotEntry.getKey();

                var group = slotGroups.getOrDefault(groupEntry.getKey(), null);

                if(group == null || !allSlots.containsKey(slotName)) continue;

                group.addSlot(slotName);

                allSlots.remove(slotName);
            }
        }
    }
}
