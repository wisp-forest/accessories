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

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;<init>()V", shift = At.Shift.AFTER, ordinal = 2), remap = false)
    private void injectCuriosSpecificSlots(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci, @Local(name = "builders") HashMap<String, SlotTypeLoader.SlotBuilder> tempMap){
        for (var entry : CuriosSlotManager.INSTANCE.slotTypeBuilders.entrySet()) {
            var accessoryType = CuriosWrappingUtils.curiosToAccessories(entry.getKey());

            var curiosBuilder = entry.getValue();
            SlotTypeLoader.SlotBuilder builder;

            if (tempMap.containsKey(accessoryType)) {
                builder = tempMap.get(accessoryType);

                if(curiosBuilder.size != null && curiosBuilder.size > ((SlotTypeLoaderBuilderAccessor) builder).getAmount()) {
                    builder.amount(curiosBuilder.size);
                }
            } else {
                builder = new SlotTypeLoader.SlotBuilder(accessoryType);

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

                tempMap.put(accessoryType, builder);
            }

            for (ResourceLocation validatorPredicate : curiosBuilder.validators) {
                builder.validator(CuriosWrappingUtils.curiosToAccessories_Validators(validatorPredicate));
            }
        }
    }
}
