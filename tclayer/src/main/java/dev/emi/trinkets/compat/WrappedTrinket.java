package dev.emi.trinkets.compat;

import dev.emi.trinkets.api.SlotAttributes;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketEnums;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.SoundEventData;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

public class WrappedTrinket implements Accessory {

    private final Trinket trinket;

    public WrappedTrinket(Trinket trinket){
        this.trinket = trinket;
    }

    @Override
    public void tick(ItemStack stack, SlotReference reference) {
        var ref = WrappingTrinketsUtils.createTrinketsReference(reference);

        if(ref.isEmpty()) {
            Accessory.super.tick(stack, reference);

            return;
        }

        this.trinket.tick(stack, ref.get(), reference.entity());
    }

    @Override
    public void onEquip(ItemStack stack, SlotReference reference) {
        var ref = WrappingTrinketsUtils.createTrinketsReference(reference);

        if(ref.isEmpty()) {
            Accessory.super.onEquip(stack, reference);

            return;
        }

        this.trinket.onEquip(stack, ref.get(), reference.entity());
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference reference) {
        var ref = WrappingTrinketsUtils.createTrinketsReference(reference);

        if(ref.isEmpty()) {
            Accessory.super.onUnequip(stack, reference);

            return;
        }

        this.trinket.onUnequip(stack, ref.get(), reference.entity());
    }

    @Override
    public boolean canEquip(ItemStack stack, SlotReference reference) {
        var ref = WrappingTrinketsUtils.createTrinketsReference(reference);

        if(ref.isEmpty()) return Accessory.super.canEquip(stack, reference);

        return this.trinket.canEquip(stack, ref.get(), reference.entity());
    }

    @Override
    public boolean canUnequip(ItemStack stack, SlotReference reference) {
        var ref = WrappingTrinketsUtils.createTrinketsReference(reference);

        if(ref.isEmpty()) return Accessory.super.canUnequip(stack, reference);

        return this.trinket.canUnequip(stack, ref.get(), reference.entity());
    }

    @Override
    public void getDynamicModifiers(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder) {
        var ref = WrappingTrinketsUtils.createTrinketsReference(reference);

        if(ref.isEmpty()) Accessory.super.getDynamicModifiers(stack, reference, builder);

        var id = SlotAttributes.getIdentifier(ref.get());

        this.trinket.getModifiers(stack, ref.get(), reference.entity(), SlotAttributes.getIdentifier(ref.get())).asMap()
                .forEach((attribute, modifiers) -> {
                    for (var modifier : modifiers) {
                        if(modifier.id().equals(id)) {
                            builder.addStackable(attribute, Accessories.of("trinket_converted_attribute"), modifier.amount(), modifier.operation());
                        } else {
                            builder.addExclusive(attribute, modifier);
                        }
                    }
                });
    }

    @Override
    public DropRule getDropRule(ItemStack stack, SlotReference reference, DamageSource source) {
        var ref = WrappingTrinketsUtils.createTrinketsReference(reference);

        if(ref.isEmpty()) return Accessory.super.getDropRule(stack, reference, source);

        return TrinketEnums.convert(this.trinket.getDropRule(stack, ref.get(), reference.entity()));
    }

    @Override
    public void onBreak(ItemStack stack, SlotReference reference) {
        var ref = WrappingTrinketsUtils.createTrinketsReference(reference);

        if(ref.isEmpty()) {
            Accessory.super.onBreak(stack, reference);
        } else {
            this.trinket.onBreak(stack, ref.get(), reference.entity());
        }
    }

    @Override
    public boolean canEquipFromUse(ItemStack stack) {
        try {
            return this.trinket.canEquipFromUse(stack, null);
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public SoundEventData getEquipSound(ItemStack stack, SlotReference reference) {
        var ref = WrappingTrinketsUtils.createTrinketsReference(reference);

        if (ref.isEmpty()) {
            return Accessory.super.getEquipSound(stack, reference);
        } else {
            var holder = this.trinket.getEquipSound(stack, ref.get(), reference.entity());

            if(holder == null) return null;

            return new SoundEventData(holder, 1.0f, 1.0f);
        }
    }
}
