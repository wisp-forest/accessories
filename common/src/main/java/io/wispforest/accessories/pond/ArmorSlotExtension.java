package io.wispforest.accessories.pond;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ArmorSlot;
import org.jetbrains.annotations.Nullable;

public interface ArmorSlotExtension {

    default ArmorSlot setAtlasLocation(ResourceLocation atlasLocation) {
        throw new IllegalStateException("Extension method not implemented!");
    }

    @Nullable
    default ResourceLocation getAtlasLocation(){
        throw new IllegalStateException("Extension method not implemented!");
    }
}
