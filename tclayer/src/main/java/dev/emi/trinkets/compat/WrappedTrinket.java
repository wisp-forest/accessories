package dev.emi.trinkets.compat;

import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketEnums;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class WrappedTrinket implements Accessory {

    private final Trinket trinket;

    public WrappedTrinket(Trinket trinket){
        this.trinket = trinket;
    }

    @Override
    public void tick(ItemStack stack, SlotReference reference) {
        var ref = WrappingTrinketsUtils.createReference(reference);

        if(ref.isEmpty()) {
            Accessory.super.tick(stack, reference);

            return;
        }

        this.trinket.tick(stack, ref.get(), reference.entity());
    }

    @Override
    public void onEquip(ItemStack stack, SlotReference reference) {
        var ref = WrappingTrinketsUtils.createReference(reference);

        if(ref.isEmpty()) {
            Accessory.super.onEquip(stack, reference);

            return;
        }

        this.trinket.onEquip(stack, ref.get(), reference.entity());
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference reference) {
        var ref = WrappingTrinketsUtils.createReference(reference);

        if(ref.isEmpty()) {
            Accessory.super.onUnequip(stack, reference);

            return;
        }

        this.trinket.onUnequip(stack, ref.get(), reference.entity());
    }

    @Override
    public boolean canEquip(ItemStack stack, SlotReference reference) {
        var ref = WrappingTrinketsUtils.createReference(reference);

        if(ref.isEmpty()) return Accessory.super.canEquip(stack, reference);

        return this.trinket.canEquip(stack, ref.get(), reference.entity());
    }

    @Override
    public boolean canUnequip(ItemStack stack, SlotReference reference) {
        var ref = WrappingTrinketsUtils.createReference(reference);

        if(ref.isEmpty()) return Accessory.super.canUnequip(stack, reference);

        return this.trinket.canUnequip(stack, ref.get(), reference.entity());
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getModifiers(ItemStack stack, SlotReference reference, UUID uuid) {
        var ref = WrappingTrinketsUtils.createReference(reference);

        if(ref.isEmpty()) return Accessory.super.getModifiers(stack, reference, uuid);

        return this.trinket.getModifiers(stack, ref.get(), reference.entity(), uuid);
    }

    @Override
    public DropRule getDropRule(ItemStack stack, SlotReference reference, DamageSource source) {
        var ref = WrappingTrinketsUtils.createReference(reference);

        if(ref.isEmpty()) return Accessory.super.getDropRule(stack, reference, source);

        return TrinketEnums.convert(this.trinket.getDropRule(stack, ref.get(), reference.entity()));
    }

    @Override
    public void onBreak(ItemStack stack, SlotReference reference) {
        var ref = WrappingTrinketsUtils.createReference(reference);

        if(ref.isEmpty()) {
            Accessory.super.onBreak(stack, reference);
        } else {
            this.trinket.onBreak(stack, ref.get(), reference.entity());
        }
    }
}
