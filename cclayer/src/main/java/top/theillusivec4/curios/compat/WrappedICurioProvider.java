package top.theillusivec4.curios.compat;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.SoundEventData;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.events.extra.*;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.utils.AttributeUtils;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.UUID;

public class WrappedICurioProvider implements Accessory, LootingAdjustment, FortuneAdjustment, AllowWalkingOnSnow, EndermanMasked, PiglinNeutralInducer {

    public WrappedICurioProvider() {}

    public ICurio iCurio(ItemStack stack){
        return stack.getCapability(CuriosCapability.ITEM).orElse(null);
    }

    @Override
    public void tick(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        this.iCurio(stack).curioTick(context);
    }

    @Override
    public void onEquip(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        this.iCurio(stack).onEquip(context, ItemStack.EMPTY);
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        this.iCurio(stack).onUnequip(context, ItemStack.EMPTY);
    }

    @Override
    public boolean canEquip(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurio(stack).canEquip(context);
    }

    @Override
    public boolean canUnequip(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurio(stack).canUnequip(context);
    }

    @Override
    public void getDynamicModifiers(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder) {
        var context = CuriosWrappingUtils.create(reference);

        Accessory.super.getDynamicModifiers(stack, reference, builder);
        //--

        var data = AttributeUtils.getModifierData(Accessories.of(AccessoryAttributeBuilder.createSlotPath(reference)));

        Multimap<Attribute, AttributeModifier> attributes = HashMultimap.create();

        attributes.putAll(this.iCurio(stack).getAttributeModifiers(context, data.right()));

        CuriosWrappingUtils.getAttributeModifiers(attributes, context, data.right(), stack).forEach((attribute, modifier) -> {
            builder.addModifier(attribute, modifier, reference, s -> new ResourceLocation(CuriosConstants.MOD_ID, s));
        });
    }

    @Override
    public DropRule getDropRule(ItemStack stack, SlotReference reference, DamageSource source) {
        var context = CuriosWrappingUtils.create(reference);

        return CuriosWrappingUtils.convert(this.iCurio(stack).getDropRule(context, source, 0, true));
    }

    @Override
    public void onEquipFromUse(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        this.iCurio(stack).onEquipFromUse(context);
    }

    @Override
    public SoundEventData getEquipSound(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        var info = this.iCurio(stack).getEquipSound(context);

        return new SoundEventData(info.soundEvent(), info.volume(), info.pitch());
    }

    @Override
    public boolean canEquipFromUse(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurio(stack).canEquipFromUse(context);
    }

    @Override
    public void onBreak(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        this.iCurio(stack).curioBreak(context);
    }

    //--

    @Override
    public int getLootingAdjustment(ItemStack stack, SlotReference reference, LivingEntity target, DamageSource damageSource, int currentLevel) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurio(stack).getLootingLevel(context, damageSource, target, currentLevel);
    }

    @Override
    public int getFortuneAdjustment(ItemStack stack, SlotReference reference, LootContext context, int currentLevel) {
        var slotContext = CuriosWrappingUtils.create(reference);

        return this.iCurio(stack).getFortuneLevel(slotContext, context);
    }

    @Override
    public TriState makePiglinsNeutral(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurio(stack).makesPiglinsNeutral(context) ? TriState.TRUE : TriState.DEFAULT;
    }

    @Override
    public TriState allowWalkingOnSnow(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurio(stack).canWalkOnPowderedSnow(context) ? TriState.TRUE : TriState.DEFAULT;
    }

    @Override
    public TriState isEndermanMasked(EnderMan enderMan, ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurio(stack).isEnderMask(context, enderMan) ? TriState.TRUE : TriState.DEFAULT;
    }
}
