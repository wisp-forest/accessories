package io.wispforest.cclayer.mixin;

import com.google.gson.JsonObject;
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
import top.theillusivec4.curios.common.data.CuriosEntityManager;
import top.theillusivec4.curios.compat.CuriosWrappingUtils;

import java.util.HashMap;
import java.util.Map;

@Mixin(EntitySlotLoader.class)
public abstract class EntitySlotLoaderMixin {

    @Shadow
    @Mutable
    private Map<EntityType<?>, Map<String, SlotType>> server;

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    private void injectCuriosSpecificSlots(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci){
        var map = server;

        for (var entry : CuriosEntityManager.INSTANCE.entityTypeSlotData.entrySet()) {
            var slotTypes = map.computeIfAbsent(entry.getKey(), entityType -> new HashMap<>());

            for (String s : entry.getValue().build()) {
                var typeid = CuriosWrappingUtils.curiosToAccessories(s);

                if (!slotTypes.containsKey(typeid)) {
                    var type = SlotTypeLoader.INSTANCE.getSlotTypes(false).get(typeid);

                    //TODO: ERROR ABOUT INFO?
                    if (type != null) {
                        slotTypes.put(typeid, type);
                    }
                }
            }
        }
    }
}
