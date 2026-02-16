package io.wispforest.accessories.mixin.client;

import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EquipmentLayerRenderer.class)
public interface EquipmentLayerRendererAccessor {
    @Accessor("equipmentAssets")
    EquipmentAssetManager accessories$equipmentAssetManager();
}
