package io.wispforest.accessories.mixin;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(DataComponentPatch.class)
public interface DataComponentPatchAccessor {
    @Accessor
    Reference2ObjectMap<DataComponentType<?>, Optional<?>> getMap();
}
