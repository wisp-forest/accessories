package io.wispforest.accessories.api.data.providers.entity;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class EntityBindingBuilder {

    private final boolean replace;

    private final List<TagKey<EntityType<?>>> tags = new ArrayList<>();
    private final List<EntityType<?>> entityTypes = new ArrayList<>();
    private final List<String> slots = new ArrayList<>();

    public EntityBindingBuilder(boolean replace) {
        this.replace = replace;
    }

    @SafeVarargs
    public final EntityBindingBuilder tag(TagKey<EntityType<?>>... tagKeys) {
        this.tags.addAll(List.of(tagKeys));

        return this;
    }

    public final EntityBindingBuilder entityType(EntityType<?>... entityTypes) {
        this.entityTypes.addAll(List.of(entityTypes));

        return this;
    }

    public final EntityBindingBuilder slots(String... slots) {
        this.slots.addAll(List.of(slots));

        return this;
    }

    public RawEntityBinding create() {
        return new RawEntityBinding(
                this.replace ? Optional.of(true) : Optional.empty(),
                this.tags,
                this.entityTypes,
                this.slots
        );
    }
}
