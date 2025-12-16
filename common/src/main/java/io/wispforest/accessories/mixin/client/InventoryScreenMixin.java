package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.wispforest.accessories.pond.CosmeticArmorLookupTogglable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @WrapMethod(method = "renderEntityInInventoryFollowsMouse")
    private static void accessories$wrapWithArmorLookup(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int scale, float yOffset, float mouseX, float mouseY, LivingEntity entity, Operation<Void> original) {
        CosmeticArmorLookupTogglable.runWithLookupToggle(entity, () -> {
            original.call(guiGraphics, x1, y1, x2, y2, scale, yOffset, mouseX, mouseY, entity);
        });
    }
}
