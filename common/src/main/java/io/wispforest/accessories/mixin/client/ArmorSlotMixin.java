package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Pair;
import io.wispforest.accessories.pond.ArmorSlotExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorSlot.class)
public abstract class ArmorSlotMixin implements ArmorSlotExtension {

    @Shadow
    @Final
    @Nullable
    private ResourceLocation emptyIcon;

    @Nullable
    private ResourceLocation accessories$atlasLocation = null;

    @Override
    public ArmorSlot setAtlasLocation(ResourceLocation atlasLocation) {
        this.accessories$atlasLocation = atlasLocation;

        return (ArmorSlot)(Object)this;
    }

    @Override
    @Nullable
    public ResourceLocation getAtlasLocation() {
        return accessories$atlasLocation;
    }

    @Inject(method = "getNoItemIcon", at = @At(value = "HEAD"), cancellable = true)
    private void accessories$useAlternativeAtlas(CallbackInfoReturnable<Pair<ResourceLocation, ResourceLocation>> cir) {
        if(accessories$atlasLocation == null) return;

        cir.setReturnValue(Pair.of(accessories$atlasLocation, this.emptyIcon));
    }
}
