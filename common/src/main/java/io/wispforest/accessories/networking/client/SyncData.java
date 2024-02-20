package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.SlotGroupImpl;
import io.wispforest.accessories.impl.SlotTypeImpl;
import io.wispforest.accessories.networking.CacheableAccessoriesPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.stream.Collectors;

public class SyncData extends CacheableAccessoriesPacket {

    private List<SlotType> slotTypes = List.of();
    private Map<EntityType<?>, Collection<String>> entitySlots = Map.of();
    private Set<SlotGroup> slotGroups = Set.of();

    public SyncData(){}

    public SyncData(FriendlyByteBuf buf) {
        super(buf);
    }

    public SyncData(List<SlotType> slotTypes, Map<EntityType<?>, Collection<String>> entitySlots, Set<SlotGroup> slotGroups){
        super(false);

        this.slotTypes = slotTypes;
        this.entitySlots = entitySlots;
        this.slotGroups = slotGroups;
    }

    public static SyncData create(){
        var allSlotTypes = SlotTypeLoader.INSTANCE.getSlotTypes(false);

        var entitySlotData = EntitySlotLoader.INSTANCE.getEntitySlotData(false);

        var entitySlots = new HashMap<EntityType<?>, Collection<String>>();

        for (var entry : entitySlotData.entrySet()) {
            entitySlots.put(entry.getKey(), entry.getValue().keySet());
        }

        var slotGroups = new HashSet<SlotGroup>();

        slotGroups.addAll(SlotGroupLoader.INSTANCE.getGroups(false).values());

        return new SyncData(List.copyOf(allSlotTypes.values()), entitySlots, slotGroups);
    }

    @Override
    public void writeUncached(FriendlyByteBuf buf) {
        buf.writeCollection(
                this.slotTypes,
                (buf1, slotType) -> {
                    buf1.writeNbt(Util.getOrThrow(SlotTypeImpl.CODEC.codec().encodeStart(NbtOps.INSTANCE, (SlotTypeImpl) slotType), IllegalStateException::new));
                });

        buf.writeMap(
                this.entitySlots,
                (buf1, entityType) -> buf1.writeResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(entityType)),
                (buf1, value) -> buf1.writeCollection(value, FriendlyByteBuf::writeUtf));

        buf.writeCollection(
                this.slotGroups,
                (buf1, slotGroup) -> {
                    buf1.writeNbt(Util.getOrThrow(SlotGroupImpl.CODEC.codec().encodeStart(NbtOps.INSTANCE, slotGroup), IllegalStateException::new));
                });
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

        this.slotGroups = buf.readCollection(HashSet::new, buf1 -> {
            return Util.getOrThrow(SlotGroupImpl.CODEC.codec().decode(NbtOps.INSTANCE, buf1.readNbt()), IllegalStateException::new).getFirst();
        });
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void handle(Player player) {
        super.handle(player);

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
