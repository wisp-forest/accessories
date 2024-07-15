package io.wispforest.accessories.impl;

import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.endec.MinecraftEndecs;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record SlotGroupImpl(String name, int order, Set<String> slots, ResourceLocation icon) implements SlotGroup {

    public static final StructEndec<SlotGroup> ENDEC = StructEndecBuilder.of(
            Endec.STRING.fieldOf("name", SlotGroup::name),
            Endec.INT.fieldOf("order", SlotGroup::order),
            Endec.STRING.listOf().<Set<String>>xmap(LinkedHashSet::new, ArrayList::new).fieldOf("slots", SlotGroup::slots),
            MinecraftEndecs.IDENTIFIER.fieldOf("icon", SlotGroup::icon),
            SlotGroupImpl::new
    );
}
