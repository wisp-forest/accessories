package top.theillusivec4.curios.compat;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.SoundEventData;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.events.extra.*;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.utils.AttributeUtils;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WrappedICurioProvider implements Accessory, LootingAdjustment, FortuneAdjustment, AllowWalkingOnSnow, EndermanMasked, PiglinNeutralInducer {

    private final ICapabilityProvider<ItemStack, Void, ICurio> icurioProvider;

    public WrappedICurioProvider(ICapabilityProvider<ItemStack, Void, ICurio> icurioProvider){
        this.icurioProvider = icurioProvider;
    }

    public ICurio iCurio(ItemStack stack){
        return icurioProvider.getCapability(stack, null);
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

        var id = ResourceLocation.fromNamespaceAndPath(CuriosConstants.MOD_ID, AccessoryAttributeBuilder.createSlotPath(reference));

        this.iCurio(stack).getAttributeModifiers(context, id).forEach(builder::addExclusive);
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

        return new SoundEventData(Holder.direct(info.soundEvent()), info.volume(), info.pitch());
    }

    @Override
    public boolean canEquipFromUse(ItemStack stack) {
        try {
            return this.iCurio(stack).canEquipFromUse(null);
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Override
    public void onBreak(ItemStack stack, SlotReference reference) {
        var context = CuriosWrappingUtils.create(reference);

        this.iCurio(stack).curioBreak(context);
    }

    @Override
    public void getAttributesTooltip(ItemStack stack, SlotType type, List<Component> tooltips, Item.TooltipContext tooltipContext, TooltipFlag tooltipType){
        var copyData = new ArrayList<>(tooltips);

        var data = this.iCurio(stack).getAttributesTooltip(copyData);

        tooltips.clear();
        tooltips.addAll(data);
    }

    @Override
    public void getExtraTooltip(ItemStack stack, List<Component> tooltips, Item.TooltipContext tooltipContext, TooltipFlag tooltipType){
        var components = new ArrayList<Component>();

        var data = this.iCurio(stack).getSlotsTooltip(components);

        tooltips.addAll(data);
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
