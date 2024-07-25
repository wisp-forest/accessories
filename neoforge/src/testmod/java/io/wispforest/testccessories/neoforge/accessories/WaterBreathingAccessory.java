package io.wispforest.testccessories.neoforge.accessories;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.testccessories.neoforge.TestItems;
import io.wispforest.testccessories.neoforge.Testccessories;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class WaterBreathingAccessory implements Accessory {

    private static final ResourceLocation GRAVITY_LOCATION = Testccessories.of("gravity_accessory_adjustment");

    public static void init() {
        AccessoriesAPI.registerAccessory(TestItems.testItem1.get(), new WaterBreathingAccessory());
    }

    public static final String REFILL_TIME_OUT_KEY = "RefillTimeout";

    @Override
    public void getDynamicModifiers(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder) {
        builder.addStackable(Attributes.FLYING_SPEED, GRAVITY_LOCATION, 10, AttributeModifier.Operation.ADDITION);
    }

    @Override
    public void tick(ItemStack stack, SlotReference reference) {
        var currentDamage = stack.getDamageValue();

        if(currentDamage >= 63) return;

        var tag = stack.getOrCreateTag();

        var refillTimeout = tag.contains(REFILL_TIME_OUT_KEY) ? tag.getInt(REFILL_TIME_OUT_KEY) : -1;

        if(refillTimeout != -1) {
            refillTimeout -= 1;

            if(refillTimeout > 0) {
                tag.putInt(REFILL_TIME_OUT_KEY, refillTimeout);
            } else {
                tag.remove(REFILL_TIME_OUT_KEY);
            }

            return;
        }

        if(reference.entity() instanceof ServerPlayer serverPlayer){
            var currentAirSupply = serverPlayer.getAirSupply();

            if(serverPlayer.getMaxAirSupply() - currentAirSupply >= 20) {
                stack.setDamageValue(currentDamage + 1);

                serverPlayer.setAirSupply(currentAirSupply + 20);


                tag.putInt(REFILL_TIME_OUT_KEY, 4);
            }
        }
    }
}
