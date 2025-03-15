package io.wispforest.accessories.api.components;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
@Deprecated
public record AccessoryItemCosmeticOverride(ResourceLocation id) {
    public static final Endec<AccessoryItemCosmeticOverride> ENDEC = StructEndecBuilder.of(
            MinecraftEndecs.IDENTIFIER.fieldOf("id", AccessoryItemCosmeticOverride::id),
            AccessoryItemCosmeticOverride::new
    );
}
