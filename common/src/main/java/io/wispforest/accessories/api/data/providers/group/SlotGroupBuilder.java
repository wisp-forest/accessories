package io.wispforest.accessories.api.data.providers.group;

import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SlotGroupBuilder {

    private final boolean replace;

    private final String name;
    private final boolean uniqueGroup;

    private ResourceLocation icon = null;
    private Integer order = null;

    private final List<String> slots = new ArrayList<>();

    public SlotGroupBuilder(String name, boolean replace) {
        this.replace = replace;

        this.name = name;

        this.uniqueGroup = UniqueSlotHandling.isUniqueGroup(name, false);
    }

    public SlotGroupBuilder order(Integer value) {
        this.order = value;
        return this;
    }

    public SlotGroupBuilder icon(ResourceLocation value) {
        this.icon = value;
        return this;
    }

    public SlotGroupBuilder slots(String ...slot) {
        this.slots.addAll(List.of(slot));

        return this;
    }

    public RawSlotGroup create() {
        return new RawSlotGroup(
                this.name,
                this.replace ? Optional.of(true) : Optional.empty(),
                Optional.ofNullable(this.icon),
                Optional.ofNullable(this.order),
                !this.slots.isEmpty() ? Optional.of(slots) : Optional.empty()
        );
    }
}
