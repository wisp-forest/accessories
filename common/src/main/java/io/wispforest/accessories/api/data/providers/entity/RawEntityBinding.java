package io.wispforest.accessories.api.data.providers.entity;

import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record RawEntityBinding(Optional<Boolean> replace, List<TagKey<EntityType<?>>> tags, List<EntityType<?>> entityTypes, List<String> slots) {

    public static final StructEndec<RawEntityBinding> ENDEC = StructEndecBuilder.of(
            Endec.BOOLEAN.optionalOf().optionalFieldOf("replace", RawEntityBinding::replace, Optional.empty()),
            Endec.STRING.listOf().fieldOf("entities", rawEntityBinding -> {
                return Stream.concat(
                        rawEntityBinding.tags().stream().map(tag -> "#" + tag.location()),
                        rawEntityBinding.entityTypes().stream().map(entityType -> BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString())
                ).toList();
            }),
            Endec.STRING.listOf().fieldOf("slots", RawEntityBinding::slots),
            (replace, entities, slots) -> {
                var tags = new ArrayList<TagKey<EntityType<?>>>();
                var entityTypes = new ArrayList<EntityType<?>>();

                for (var entity : entities) {
                    if(entity.charAt(0) == '#') {
                        var tag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(entity.replace("#", "")));

                        tags.add(tag);
                    } else {
                        var entityType = BuiltInRegistries.ENTITY_TYPE.getValueOrThrow(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(entity)));

                        entityTypes.add(entityType);
                    }
                }

                return new RawEntityBinding(replace, tags, entityTypes, slots);
            }
    );
}
