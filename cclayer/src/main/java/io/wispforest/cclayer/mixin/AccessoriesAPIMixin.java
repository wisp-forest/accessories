package io.wispforest.cclayer.mixin;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.CurioAttributeModifiers;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.CuriosRegistry;

@Mixin(value = AccessoriesAPI.class)
public abstract class AccessoriesAPIMixin {
    @Inject(method = "getAttributeModifiers(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;IZ)Lio/wispforest/accessories/api/attributes/AccessoryAttributeBuilder;", at = @At("RETURN"))
    private static void trinkets$getDataAttributeModifiers(ItemStack stack, @Nullable LivingEntity entity, String slotName, int slot, boolean hideTooltipIfDisabled, CallbackInfoReturnable<AccessoryAttributeBuilder> cir) {
        Multimap<Holder<Attribute>, AttributeModifier> multimap = LinkedHashMultimap.create();

        if(!stack.has(CuriosRegistry.CURIO_ATTRIBUTE_MODIFIERS)) return;

        for (CurioAttributeModifiers.Entry entry : stack.getOrDefault(CuriosRegistry.CURIO_ATTRIBUTE_MODIFIERS, CurioAttributeModifiers.EMPTY).modifiers()) {
            var targetSlot = entry.slot();

            if (targetSlot.equals(slotName) || targetSlot.isBlank()) {
                var rl = entry.attribute();

                if (rl == null) continue;

                var attributeModifier = entry.modifier();

                var operation = attributeModifier.operation();
                var amount = attributeModifier.amount();
                var id = attributeModifier.id();

                if (rl.getNamespace().equals("curios")) {
                    var attributeSlotName = rl.getPath();
                    var clientSide = entity == null || entity.level().isClientSide();

                    if (CuriosApi.getSlot(attributeSlotName, clientSide).isPresent()) {
                        CuriosApi.addSlotModifier(multimap, attributeSlotName, id, amount, operation);
                    }
                } else {
                    BuiltInRegistries.ATTRIBUTE.getHolder(rl)
                            .ifPresent(attribute -> multimap.put(attribute, new AttributeModifier(id, amount, operation)));
                }
            }
        }

        var builder = cir.getReturnValue();

        multimap.forEach(builder::addExclusive);
    }
}
