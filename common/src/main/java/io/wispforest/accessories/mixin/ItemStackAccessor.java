package io.wispforest.accessories.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public interface ItemStackAccessor {
    @Invoker("addModifierTooltip")
    void accessories$addModifierTooltip(Consumer<Component> consumer, @Nullable Player player, Holder<Attribute> holder, AttributeModifier attributeModifier);

    @Accessor("components")
    PatchedDataComponentMap accessories$components();
}
