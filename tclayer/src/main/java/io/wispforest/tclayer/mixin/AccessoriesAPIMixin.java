package io.wispforest.tclayer.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.emi.trinkets.TrinketModifiers;
import dev.emi.trinkets.api.TrinketConstants;
import dev.emi.trinkets.api.TrinketsAttributeModifiersComponent;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Mixin(value = AccessoriesAPI.class)
public abstract class AccessoriesAPIMixin {

    @Inject(method = "getUsedSlotsFor(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/Container;)Ljava/util/Collection;",
            at = @At(value = "INVOKE", target = "Lio/wispforest/accessories/data/SlotTypeLoader;getSlotTypes(Lnet/minecraft/world/level/Level;)Ljava/util/Map;"))
    private static void cacheTrinketTags(LivingEntity entity, Container container, CallbackInfoReturnable<Collection<SlotType>> cir, @Share("trinketTags") LocalRef<Set<TagKey<Item>>> trinketTagsRef) {
        var trinketTags = new HashSet<TagKey<Item>>();

        BuiltInRegistries.ITEM.getTagNames().forEach(itemTagKey -> {
            if (!itemTagKey.location().getNamespace().equals(TrinketConstants.MOD_ID)) return;

            trinketTags.add(itemTagKey);
        });

        trinketTagsRef.set(trinketTags);
    }

    @ModifyReceiver(method = "getUsedSlotsFor(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/Container;)Ljava/util/Collection;",
            at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElse(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Optional<Boolean> checkTrinketTags(Optional<Boolean> optional, Object object, @Local SlotType slotType, @Share("trinketTags") LocalRef<Set<TagKey<Item>>> trinketTagsRef) {
        for (var itemTagKey : trinketTagsRef.get()) {
            var filterPath = WrappingTrinketsUtils.splitGroupInfo(itemTagKey.location().getPath());

            var accessoryVersion = WrappingTrinketsUtils.trinketsToAccessories_Slot(filterPath.left(), filterPath.right());

            if ((filterPath.equals(slotType.name()) || accessoryVersion.equals(slotType.name())) && !BuiltInRegistries.ITEM.getTag(itemTagKey).isEmpty()) {
                //System.out.println("Match: " + slotType.name() + " - Tag: " + itemTagKey + " / " + "Path: " + filterPath + " / " + accessoryVersion);
                return Optional.of(true);
            } /*else {
                System.out.println("Mismatch: " + slotType.name() + " - Tag: " + itemTagKey + " / " + "Path: " + filterPath + " / " + accessoryVersion);
            }*/
        }

        return optional;
    }

    @Inject(method = "getAttributeModifiers(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;IZ)Lio/wispforest/accessories/api/attributes/AccessoryAttributeBuilder;", at = @At("RETURN"))
    private static void trinkets$getDataAttributeModifiers(ItemStack stack, @Nullable LivingEntity entity, String slotName, int slot, boolean hideTooltipIfDisabled, CallbackInfoReturnable<AccessoryAttributeBuilder> cir) {
        if (!stack.has(TrinketsAttributeModifiersComponent.TYPE)) return;

        var builder = cir.getReturnValue();

        for (var entry : stack.getOrDefault(TrinketsAttributeModifiersComponent.TYPE, TrinketsAttributeModifiersComponent.DEFAULT).modifiers()) {
            if (entry.slot().isEmpty()) {
                builder.addExclusive(entry.attribute(), entry.modifier());
            } else if(entity != null) {
                var group = WrappingTrinketsUtils.getGroup(entity.level(), slotName);

                var slotId = WrappingTrinketsUtils.accessoriesToTrinkets_Group(group.name()) + "/" + WrappingTrinketsUtils.accessoriesToTrinkets_Slot(slotName);

                if(entry.slot().get().equals(slotId)) builder.addExclusive(entry.attribute(), entry.modifier());
            }
        }
    }
}
