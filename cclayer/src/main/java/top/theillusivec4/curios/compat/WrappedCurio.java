package top.theillusivec4.curios.compat;

import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.events.extra.*;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.SoundEventData;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

public class WrappedCurio implements Accessory, LootingAdjustment, FortuneAdjustment, AllowWalkingOnSnow, EndermanMasked, PiglinNeutralInducer {

    private final ICurioItem iCurioItem;

    public WrappedCurio(ICurioItem iCurioItem){
        this.iCurioItem = iCurioItem;
    }

    @Override
    public void tick(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        this.iCurioItem.curioTick(context, stack);
    }

    @Override
    public void onEquip(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        this.iCurioItem.onEquip(context, ItemStack.EMPTY, stack);
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        this.iCurioItem.onUnequip(context, ItemStack.EMPTY, stack);
    }

    @Override
    public boolean canEquip(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurioItem.canEquip(context, stack);
    }

    @Override
    public boolean canUnequip(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurioItem.canUnequip(context, stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getModifiers(ItemStack stack, SlotReference reference, UUID uuid) {
        var context = CuriosWrappingUtils.create(reference);

        var modifiers = Accessory.super.getModifiers(stack, reference, uuid);

        //--

        modifiers.putAll(this.iCurioItem.getAttributeModifiers(context, uuid, stack));

        modifiers = CuriosWrappingUtils.getAttributeModifiers(modifiers, context, uuid, stack);

        //--

        return modifiers;
    }

    @Override
    public DropRule getDropRule(ItemStack stack, SlotReference reference, DamageSource source) {
        var context = CuriosWrappingUtils.create(reference);

        return CuriosWrappingUtils.convert(this.iCurioItem.getDropRule(context, source, 0, true, stack));
    }

    @Override
    public void onEquipFromUse(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        this.iCurioItem.onEquipFromUse(context, stack);
    }

    @Override
    public SoundEventData getEquipSound(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        var info = this.iCurioItem.getEquipSound(context, stack);

        return new SoundEventData(info.soundEvent(), info.volume(), info.pitch());
    }

    @Override
    public boolean canEquipFromUse(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurioItem.canEquipFromUse(context, stack);
    }

    @Override
    public void onBreak(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        this.iCurioItem.curioBreak(context, stack);
    }

    //--

    @Override
    public int getLootingAdjustment(ItemStack stack, SlotReference reference, LivingEntity target, DamageSource damageSource, int currentLevel) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurioItem.getLootingLevel(context, damageSource, target, currentLevel, stack);
    }

    @Override
    public int getFortuneAdjustment(ItemStack stack, SlotReference reference, LootContext context, int currentLevel) {
        var slotContext = CuriosWrappingUtils.create(reference);

        return this.iCurioItem.getFortuneLevel(slotContext, context, stack);
    }

    @Override
    public TriState makePiglinsNeutral(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurioItem.makesPiglinsNeutral(context, stack) ? TriState.TRUE : TriState.DEFAULT;
    }

    @Override
    public TriState allowWalkingOnSnow(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurioItem.canWalkOnPowderedSnow(context, stack) ? TriState.TRUE : TriState.DEFAULT;
    }

    @Override
    public TriState isEndermanMasked(EnderMan enderMan, ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        return this.iCurioItem.isEnderMask(context, enderMan, stack) ? TriState.TRUE : TriState.DEFAULT;
    }
}
