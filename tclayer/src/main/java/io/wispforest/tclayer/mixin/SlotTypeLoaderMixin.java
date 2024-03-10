package io.wispforest.tclayer.mixin;

import com.google.gson.JsonObject;
import dev.emi.trinkets.api.TrinketConstants;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.data.SlotLoader;
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

import java.util.Map;

@Mixin(SlotTypeLoader.class)
public abstract class SlotTypeLoaderMixin {

    @Shadow
    @Mutable
    private Map<String, SlotType> server;

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    private void injectCuriosSpecificSlots(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci){
        var map = this.server;

        for (var groupData : SlotLoader.INSTANCE.getSlots().entrySet()) {
            for (var entry : groupData.getValue().slots.entrySet()) {
                var accessoryType = TrinketConstants.trinketsToAccessories(entry.getKey());

                if (!map.containsKey(accessoryType)) {
                    var builder = new SlotTypeLoader.SlotBuilder(accessoryType);

                    var slotData = entry.getValue();

                    if (slotData.amount != -1) builder.amount(slotData.amount);

                    builder.order(slotData.order);

                    builder.icon(new ResourceLocation(slotData.icon));

                    builder.dropRule(TrinketEnums.convert(TrinketEnums.DropRule.valueOf(slotData.dropRule)));

                    builder.alternativeTranslation("trinkets.slot." + groupData.getKey() + "." + entry.getKey());

                    var slotType = builder.create();

                    map.put(accessoryType, slotType);
                }
            }
        }
    }
}
