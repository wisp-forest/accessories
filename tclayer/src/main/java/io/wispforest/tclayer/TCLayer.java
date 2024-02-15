package io.wispforest.tclayer;

import dev.emi.trinkets.api.TrinketConstants;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.data.SlotLoader;
import io.wispforest.accessories.data.DataLoadingModifications;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.HashMap;
import java.util.function.Consumer;

@DataLoadingModifications.DataLoadingModificationsCapable
public class TCLayer implements ModInitializer, DataLoadingModifications {

    @Override
    public void onInitialize() {}

    private static boolean eventSetup = false;

    @Override
    public void beforeRegistration(Consumer<PreparableReloadListener> registrationMethod) {
        if(!eventSetup) {
            SlotTypeLoader.INSTANCE.externalEventHooks.add(map -> {
                for (var data : SlotLoader.INSTANCE.getSlots().entrySet()) {
                    for (var entry : data.getValue().slots.entrySet()) {
                        var accessoryType = TrinketConstants.trinketsToAccessories(entry.getKey());

                        if (!map.containsKey(accessoryType)) {
                            var builder = new SlotTypeLoader.SlotBuilder(accessoryType);

                            var slotData = entry.getValue();

                            if (slotData.amount != -1) builder.amount(slotData.amount);

                            builder.order(slotData.order);

                            builder.icon(new ResourceLocation(slotData.icon));

                            builder.dropRule(TrinketEnums.convert(TrinketEnums.DropRule.valueOf(slotData.dropRule)));

                            builder.alternativeTranslation("trinkets.slot." + data.getKey() + "." + entry.getKey());

                            var slotType = builder.create();

                            map.put(accessoryType, slotType);
                        }
                    }
                }
            });

            EntitySlotLoader.INSTANCE.externalEventHooks.add(map -> {
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
            });

            SlotTypeLoader.INSTANCE.dependentLoaders.add(SlotLoader.INSTANCE.getFabricId());
            EntitySlotLoader.INSTANCE.dependentLoaders.add(dev.emi.trinkets.data.EntitySlotLoader.SERVER.getFabricId());

            eventSetup = true;
        }

        registrationMethod.accept(SlotLoader.INSTANCE);
        registrationMethod.accept(dev.emi.trinkets.data.EntitySlotLoader.SERVER);
    }
}
