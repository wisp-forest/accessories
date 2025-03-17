package io.wispforest.testccessories.fabric.accessories;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.testccessories.fabric.TestItems;
import io.wispforest.testccessories.fabric.Testccessories;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class WaterBreathingAccessory implements Accessory {

    private static final ResourceLocation GRAVITY_LOCATION = Testccessories.of("gravity_accessory_adjustment");

    public static void init() {
        AccessoriesAPI.registerAccessory(TestItems.testItem1, new WaterBreathingAccessory());
    }

    public static final String REFILL_TIME_OUT_KEY = "RefillTimeout";

    @Override
    public void getDynamicModifiers(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder) {
        builder.addStackable(Attributes.GRAVITY, new AttributeModifier(GRAVITY_LOCATION, -0.01, AttributeModifier.Operation.ADD_VALUE));
    }

    @Override
    public void tick(ItemStack stack, SlotReference reference) {
        var currentDamage = stack.getDamageValue();

        if(currentDamage >= 63) return;

        var customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);

        var tag = customData.getUnsafe();

        var refillTimeout = tag.contains(REFILL_TIME_OUT_KEY) ? tag.getInt(REFILL_TIME_OUT_KEY) : -1;

        if(refillTimeout != -1) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag1 -> {
                var newRefillTimeout = refillTimeout - 1;

                if(newRefillTimeout > 0) {
                    tag1.putInt(REFILL_TIME_OUT_KEY, newRefillTimeout);
                } else {
                    tag1.remove(REFILL_TIME_OUT_KEY);
                }
            });

            return;
        }

        if(reference.entity() instanceof ServerPlayer serverPlayer){
            var currentAirSupply = serverPlayer.getAirSupply();

            if(serverPlayer.getMaxAirSupply() - currentAirSupply >= 20) {
                stack.setDamageValue(currentDamage + 1);

                serverPlayer.setAirSupply(currentAirSupply + 20);

                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag1 -> {
                    tag1.putInt(REFILL_TIME_OUT_KEY, 4);
                });
            }
        }
    }
}
