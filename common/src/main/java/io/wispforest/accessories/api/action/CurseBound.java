package io.wispforest.accessories.api.action;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.function.Consumer;

public class CurseBound extends ActionResponseBase {

    protected CurseBound() {
        super(false);
    }

    public static boolean checkIfCursed(ItemStack stack, LivingEntity entity, ActionResponseBuffer buffer) {
        if(EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            if (!(entity instanceof Player player) || !player.isCreative()) {
                buffer.respondWith(new CurseBound());

                return true;
            }
        }

        return false;
    }

    @Override
    public void gatherReason(Consumer<Component> messageAdditionCallback, Item.TooltipContext ctx, TooltipFlag type) {
        messageAdditionCallback.accept(Component.literal("Such an item is bound to the Entity till death!"));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CurseBound;
    }

    @Override
    public int hashCode() {
        return CurseBound.class.hashCode();
    }
}
