package io.wispforest.cclayer.mixin;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.SlotTypeImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.common.data.CuriosSlotManager;
import top.theillusivec4.curios.compat.CuriosWrappingUtils;

import java.util.HashMap;
import java.util.Map;

@Mixin(SlotTypeLoader.class)
public abstract class SlotTypeLoaderMixin {

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;"), remap = false)
    private void injectCuriosSpecificSlots(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci, @Local(name = "tempMap") HashMap<String, SlotType> tempMap){
        for (var entry : CuriosSlotManager.INSTANCE.slotTypeBuilders.entrySet()) {
            var accessoryType = CuriosWrappingUtils.curiosToAccessories(entry.getKey());

            var curiosBuilder = entry.getValue();

            if (tempMap.containsKey(accessoryType)) {
                var existingSlot = tempMap.get(accessoryType);

                if(curiosBuilder.size != null && curiosBuilder.size > existingSlot.amount()) {
                    var newSlot = new SlotTypeImpl(existingSlot.name(), existingSlot.icon(), existingSlot.order(), curiosBuilder.size, existingSlot.validators(), existingSlot.dropRule());

                    tempMap.put(accessoryType, newSlot);
                }

                continue;
            }

            var builder = new SlotTypeLoader.SlotBuilder(accessoryType);

            if (curiosBuilder.size != null) {
                builder.amount(curiosBuilder.size);
            }

            if (curiosBuilder.sizeMod != 0) {
                builder.addAmount(curiosBuilder.sizeMod);
            }

            if (curiosBuilder.icon != null) {
                builder.icon(curiosBuilder.icon);
            }

            if (curiosBuilder.order != null) {
                builder.order(curiosBuilder.order);
            }

            if (curiosBuilder.dropRule != null) {
                builder.dropRule(CuriosWrappingUtils.convert(curiosBuilder.dropRule));
            }

            builder.alternativeTranslation("curios.identifier." + entry.getKey());

            tempMap.put(accessoryType, builder.create());
        }
    }
}
