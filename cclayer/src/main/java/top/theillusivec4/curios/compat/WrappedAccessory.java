package top.theillusivec4.curios.compat;

import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.events.extra.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

public class WrappedAccessory implements ICurioItem {

    private final Accessory accessory;

    public WrappedAccessory(Accessory accessory){
        this.accessory = accessory;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        accessory.tick(stack, CuriosWrappingUtils.fromContext(slotContext));
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        accessory.onEquip(stack, CuriosWrappingUtils.fromContext(slotContext));
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        accessory.onUnequip(stack, CuriosWrappingUtils.fromContext(slotContext));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return accessory.canEquip(stack, CuriosWrappingUtils.fromContext(slotContext));
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return accessory.canUnequip(stack, CuriosWrappingUtils.fromContext(slotContext));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        return accessory.getModifiers(stack, CuriosWrappingUtils.fromContext(slotContext), uuid);
    }

    @Override
    public void onEquipFromUse(SlotContext slotContext, ItemStack stack) {
        accessory.onEquipFromUse(stack, CuriosWrappingUtils.fromContext(slotContext));
    }

    @NotNull
    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        var data = accessory.getEquipSound(stack, CuriosWrappingUtils.fromContext(slotContext));

        return new ICurio.SoundInfo(data.event(), data.volume(), data.pitch());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return accessory.canEquipFromUse(stack, CuriosWrappingUtils.fromContext(slotContext));
    }

    @Override
    public ICurio.@NotNull DropRule getDropRule(SlotContext slotContext, DamageSource source, int lootingLevel, boolean recentlyHit, ItemStack stack) {
        return CuriosWrappingUtils.convert(accessory.getDropRule(stack, CuriosWrappingUtils.fromContext(slotContext), source));
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, ItemStack stack) {
        accessory.getAttributesTooltip(stack, null, tooltips);

        return tooltips;
    }

    @Override
    public int getFortuneLevel(SlotContext slotContext, LootContext lootContext, ItemStack stack) {
        if(accessory instanceof FortuneAdjustment fortuneAdjustment){
            return fortuneAdjustment.getFortuneAdjustment(stack, CuriosWrappingUtils.fromContext(slotContext), lootContext, 0);
        }

        return 0;
    }

    @Override
    public int getLootingLevel(SlotContext slotContext, DamageSource source, LivingEntity target, int baseLooting, ItemStack stack) {
        if(accessory instanceof LootingAdjustment lootingAdjustment){
            return lootingAdjustment.getLootingAdjustment(stack, CuriosWrappingUtils.fromContext(slotContext), target, source, baseLooting);
        }

        return 0;
    }

    @Override
    public boolean makesPiglinsNeutral(SlotContext slotContext, ItemStack stack) {
        if(accessory instanceof PiglinNeutralInducer piglinNeutralInducer){
            return piglinNeutralInducer.makePiglinsNeutral(stack, CuriosWrappingUtils.fromContext(slotContext)).orElse(false);
        }

        return false;
    }

    @Override
    public boolean canWalkOnPowderedSnow(SlotContext slotContext, ItemStack stack) {
        if(accessory instanceof AllowWalkingOnSnow allowWalingOnSnow){
            return allowWalingOnSnow.allowWalkingOnSnow(stack, CuriosWrappingUtils.fromContext(slotContext)).orElse(false);
        }

        return false;
    }

    @Override
    public boolean isEnderMask(SlotContext slotContext, EnderMan enderMan, ItemStack stack) {
        if(accessory instanceof EndermanMasked endermanMasked){
            return endermanMasked.isEndermanMasked(enderMan, stack, CuriosWrappingUtils.fromContext(slotContext)).orElse(false);
        }

        return false;
    }
}
