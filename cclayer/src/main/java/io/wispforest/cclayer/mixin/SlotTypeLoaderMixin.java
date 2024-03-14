package io.wispforest.cclayer.mixin;

import com.google.gson.JsonObject;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.common.data.CuriosSlotManager;
import top.theillusivec4.curios.compat.CuriosWrappingUtils;

import java.util.HashMap;
import java.util.Map;

@Mixin(SlotTypeLoader.class)
public abstract class SlotTypeLoaderMixin {

    @Shadow
    @Mutable
    private Map<String, SlotType> server;

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    private void injectCuriosSpecificSlots(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci){
        var map = this.server;

        for (var entry : CuriosSlotManager.INSTANCE.slotTypeBuilders.entrySet()) {
            var accessoryType = CuriosWrappingUtils.curiosToAccessories(entry.getKey());

            if (!map.containsKey(accessoryType)) {
                var builder = new SlotTypeLoader.SlotBuilder(accessoryType);

                var curiosBuilder = entry.getValue();

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

                map.put(accessoryType, builder.create());
            }
        }
    }
}
