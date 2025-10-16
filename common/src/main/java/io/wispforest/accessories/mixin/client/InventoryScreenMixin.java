package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.wispforest.accessories.pond.CosmeticArmorLookupTogglable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @WrapMethod(method = "renderEntityInInventory")
    private static void accessories$wrapWithArmorLookup(GuiGraphics guiGraphics, int i, int j, int k, int l, float f, Vector3f vector3f, Quaternionf quaternionf, Quaternionf quaternionf2, LivingEntity livingEntity, Operation<Void> original) {
        CosmeticArmorLookupTogglable.runWithLookupToggle(livingEntity, () -> {
            original.call(guiGraphics, i, j, k, l, f, vector3f, quaternionf, quaternionf2, livingEntity);
        });
    }
}
