package io.wispforest.accessories.api.data.providers.group;

import io.wispforest.accessories.api.data.providers.slot.RawSlotType;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record RawSlotGroup(String name, Optional<Boolean> replace, Optional<ResourceLocation> icon, Optional<Integer> order, Optional<List<String>> slots) {
    public static final StructEndec<RawSlotGroup> ENDEC = StructEndecBuilder.of(
            Endec.STRING.fieldOf("name", RawSlotGroup::name),
            Endec.BOOLEAN.optionalOf().fieldOf("replace", RawSlotGroup::replace),
            MinecraftEndecs.IDENTIFIER.optionalOf().fieldOf("icon", RawSlotGroup::icon),
            Endec.INT.optionalOf().fieldOf("order", RawSlotGroup::order),
            Endec.STRING.listOf().optionalOf().fieldOf("slots", RawSlotGroup::slots),
            RawSlotGroup::new
    );
}
