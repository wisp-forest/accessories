package io.wispforest.cclayer.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.compat.CuriosWrappingUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Mixin(value = AccessoriesAPI.class, remap = false)
public abstract class AccessoriesAPIMixin {

    @Inject(method = "getUsedSlotsFor(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/Container;)Ljava/util/Collection;",
            at = @At(value = "INVOKE", target = "Lio/wispforest/accessories/data/SlotTypeLoader;getSlotTypes(Lnet/minecraft/world/level/Level;)Ljava/util/Map;"))
    private static void cacheTrinketTags(LivingEntity entity, Container container, CallbackInfoReturnable<Collection<SlotType>> cir, @Share("curiosTags") LocalRef<Set<TagKey<Item>>> curiosTagsRef) {
        var curiosTags = new HashSet<TagKey<Item>>();

        BuiltInRegistries.ITEM.getTagNames().forEach(itemTagKey -> {
            if(!itemTagKey.location().getNamespace().equals(CuriosConstants.MOD_ID)) return;

            curiosTags.add(itemTagKey);
        });

        curiosTagsRef.set(curiosTags);
    }

    @ModifyReceiver(method = "getUsedSlotsFor(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/Container;)Ljava/util/Collection;",
            at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElse(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Optional<Boolean> checkTrinketTags(Optional<Boolean> optional, Object object, @Local SlotType slotType, @Share("curiosTags") LocalRef<Set<TagKey<Item>>> curiosTagsRef) {
        for (var itemTagKey : curiosTagsRef.get()) {
            var accessoryVersion = CuriosWrappingUtils.curiosToAccessories(itemTagKey.location().getPath());

            if((accessoryVersion.equals(slotType.name())) && !BuiltInRegistries.ITEM.getTag(itemTagKey).isEmpty()) {
                //System.out.println("Match: " + slotType.name() + " - Tag: " + itemTagKey + " / " + "Path: " + filterPath + " / " + accessoryVersion);
                return Optional.of(true);
            } /*else {
                System.out.println("Mismatch: " + slotType.name() + " - Tag: " + itemTagKey + " / " + "Path: " + filterPath + " / " + accessoryVersion);
            }*/
        }

        return optional;
    }

}
