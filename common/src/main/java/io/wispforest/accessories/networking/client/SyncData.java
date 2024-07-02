package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.endec.MinecraftEndecs;
import io.wispforest.accessories.impl.SlotGroupImpl;
import io.wispforest.accessories.impl.SlotTypeImpl;
import io.wispforest.accessories.networking.base.HandledPacketPayload;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.stream.Collectors;

public record SyncData(List<SlotType> slotTypes, Map<EntityType<?>, Set<String>> entitySlots, Set<SlotGroup> slotGroups) implements HandledPacketPayload {

    public static Endec<SyncData> ENDEC = StructEndecBuilder.of(
            SlotTypeImpl.ENDEC.listOf().fieldOf("slotTypes", SyncData::slotTypes),
            Endec.map(MinecraftEndecs.ofRegistry(Registries.ENTITY_TYPE), Endec.STRING.setOf()).fieldOf("entitySlots", SyncData::entitySlots),
            SlotGroupImpl.ENDEC.setOf().fieldOf("slotGroups", SyncData::slotGroups),
            SyncData::new
    );

    public static SyncData create(){
        var allSlotTypes = SlotTypeLoader.INSTANCE.getSlotTypes(false);

        var entitySlotData = EntitySlotLoader.INSTANCE.getEntitySlotData(false);

        var entitySlots = new HashMap<EntityType<?>, Set<String>>();

        for (var entry : entitySlotData.entrySet()) {
            entitySlots.put(entry.getKey(), entry.getValue().keySet());
        }

        var slotGroups = new HashSet<SlotGroup>();

        slotGroups.addAll(SlotGroupLoader.INSTANCE.getGroups(false, false));

        return new SyncData(List.copyOf(allSlotTypes.values()), entitySlots, slotGroups);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void handle(Player player) {
        Map<String, SlotType> slotTypes = new HashMap<>();

        for (SlotType slotType : this.slotTypes) {
            slotTypes.put(slotType.name(), slotType);
        }

        SlotTypeLoader.INSTANCE.setSlotType(slotTypes);

        Map<EntityType<?>, Map<String, SlotType>> entitySlotTypes = new HashMap<>();

        for (var entry : this.entitySlots.entrySet()) {
            var map = entry.getValue().stream()
                    .map(slotTypes::get)
                    .collect(Collectors.toUnmodifiableMap(SlotType::name, slotType -> slotType));

            entitySlotTypes.put(entry.getKey(), map);
        }

        EntitySlotLoader.INSTANCE.setEntitySlotData(entitySlotTypes);

        var slotGroups = this.slotGroups.stream()
                .collect(Collectors.toUnmodifiableMap(SlotGroup::name, group -> group));

        SlotGroupLoader.INSTANCE.setGroups(slotGroups);
    }
}
