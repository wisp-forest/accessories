package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.api.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.SlotTypeImpl;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.stream.Collectors;

public class SyncData extends AccessoriesPacket {

    private List<SlotType> slotTypes = List.of();
    private Map<EntityType<?>, Collection<String>> entitySlots = Map.of();

    public SyncData(){}

    public SyncData(List<SlotType> slotTypes, Map<EntityType<?>, Collection<String>> entitySlots){
        this.slotTypes = slotTypes;
        this.entitySlots = entitySlots;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(
                this.slotTypes,
                (buf1, slotType) -> {
                    buf1.writeNbt(Util.getOrThrow(SlotTypeImpl.CODEC.codec().encodeStart(NbtOps.INSTANCE, slotType), IllegalStateException::new));
                });

        buf.writeMap(
                this.entitySlots,
                (buf1, entityType) -> buf1.writeResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(entityType)),
                (buf1, value) -> buf1.writeCollection(value, FriendlyByteBuf::writeUtf));
    }

    @Override
    protected void read(FriendlyByteBuf buf) {
        this.slotTypes = buf.readList(buf1 -> {
            return Util.getOrThrow(SlotTypeImpl.CODEC.codec().decode(NbtOps.INSTANCE, buf1.readNbt()), IllegalStateException::new).getFirst();
        });

        this.entitySlots = buf.readMap(
                buf1 -> BuiltInRegistries.ENTITY_TYPE.get(buf1.readResourceLocation()),
                buf1 -> buf1.readCollection(HashSet::new, FriendlyByteBuf::readUtf)
        );
    }

    @Override
    protected void handle(Player player) {
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
    }


}
