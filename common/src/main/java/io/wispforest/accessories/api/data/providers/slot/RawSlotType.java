package io.wispforest.accessories.api.data.providers.slot;

import io.wispforest.accessories.api.DropRule;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

public record RawSlotType(String name, Optional<Boolean> replace, Optional<ResourceLocation> icon, Optional<Integer> order, Optional<Integer> amount, Optional<Set<ResourceLocation>> validators, Optional<DropRule> dropRule) {
    public static final StructEndec<RawSlotType> ENDEC = StructEndecBuilder.of(
            Endec.STRING.fieldOf("name", RawSlotType::name),
            Endec.BOOLEAN.optionalOf().fieldOf("replace", RawSlotType::replace),
            MinecraftEndecs.IDENTIFIER.optionalOf().fieldOf("icon", RawSlotType::icon),
            Endec.INT.optionalOf().fieldOf("order", RawSlotType::order),
            Endec.INT.optionalOf().fieldOf("amount", RawSlotType::amount),
            MinecraftEndecs.IDENTIFIER.setOf().optionalOf().fieldOf("validators", RawSlotType::validators),
            Endec.STRING.xmap(DropRule::valueOf, DropRule::name).optionalOf().fieldOf("dropRule", RawSlotType::dropRule),
            RawSlotType::new
    );
}
