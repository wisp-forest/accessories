package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.api.slot.ExtraSlotTypeProperties;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.SlotGroupImpl;
import io.wispforest.accessories.impl.SlotTypeImpl;
import io.wispforest.accessories.utils.CollectionUtils;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.stream.Collectors;

public record SyncData(List<SlotType> slotTypes, Map<EntityType<?>, List<String>> entitySlots, List<SlotGroup> slotGroups, List<String> uniqueGroups, Map<String, ExtraSlotTypeProperties> uniqueExtraProperties) {

    private static final Endec<Map<EntityType<?>, List<String>>> ENTITY_SLOTS_ENDEC =
        EndecUtils.map(
            LinkedHashMap::new,
            type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
            s -> BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(s)),
            Endec.STRING.listOf());


    public static StructEndec<SyncData> ENDEC = StructEndecBuilder.of(
            SlotTypeImpl.ENDEC.listOf().fieldOf("slotTypes", SyncData::slotTypes),
            ENTITY_SLOTS_ENDEC.fieldOf("entitySlots", SyncData::entitySlots),
            SlotGroupImpl.ENDEC.listOf().fieldOf("slotGroups", SyncData::slotGroups),
            Endec.STRING.listOf().fieldOf("uniqueGroups", SyncData::uniqueGroups),
            ExtraSlotTypeProperties.ENDEC.mapOf().fieldOf("uniqueExtraProperties", SyncData::uniqueExtraProperties),
            SyncData::new
    );

    public static SyncData create(){
        var allSlotTypes = SlotTypeLoader.INSTANCE.getSlotTypes(false);

        var entitySlots = new LinkedHashMap<EntityType<?>, List<String>>();

        for (var entry : EntitySlotLoader.INSTANCE.getEntitySlotData(false).entrySet()) {
            entitySlots.put(entry.getKey(), List.copyOf(entry.getValue().keySet()));
        }

        var slotGroups = SlotGroupLoader.INSTANCE.getGroups(false, false);

        return new SyncData(List.copyOf(allSlotTypes.values()), entitySlots, slotGroups, List.copyOf(UniqueSlotHandling.getGroups(false)), ExtraSlotTypeProperties.getProperties(false));
    }

    @Environment(EnvType.CLIENT)
    public static void handlePacket(SyncData packet, Player player) {
        var slotTypes = new LinkedHashMap<String, SlotType>();

        for (SlotType slotType : packet.slotTypes()) {
            slotTypes.put(slotType.name(), slotType);
        }

        SlotTypeLoader.INSTANCE.setSlotType(slotTypes);

        UniqueSlotHandling.buildClientSlotReferences();

        var entitySlotTypes = new LinkedHashMap<EntityType<?>, SequencedMap<String, SlotType>>();

        for (var entry : packet.entitySlots().entrySet()) {
            var map = entry.getValue().stream()
                    .map(slotTypes::get)
                    .collect(CollectionUtils.toLinkedMap(SlotType::name));

            entitySlotTypes.put(entry.getKey(), map);
        }

        EntitySlotLoader.INSTANCE.setEntitySlotData(entitySlotTypes);

        var slotGroups = packet.slotGroups().stream()
                .collect(CollectionUtils.toLinkedMap(SlotGroup::name));

        SlotGroupLoader.INSTANCE.setGroups(slotGroups);

        UniqueSlotHandling.setClientGroups(packet.uniqueGroups());
        ExtraSlotTypeProperties.setClientPropertyMap(packet.uniqueExtraProperties());
    }
}
