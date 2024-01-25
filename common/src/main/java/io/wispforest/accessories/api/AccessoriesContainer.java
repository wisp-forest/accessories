package io.wispforest.accessories.api;

import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.*;

public interface AccessoriesContainer extends InstanceCodecable {

    Optional<SlotType> slotType();

    String getSlotName();

    AccessoriesCapability capability();

    List<Boolean> renderOptions();

    //--

    ExpandedSimpleContainer getAccessories();

    ExpandedSimpleContainer getCosmeticAccessories();

    int getSize();

    void markChanged();

    void update();

    //--

    Map<UUID, AttributeModifier> getModifiers();

    Set<AttributeModifier> getCachedModifiers();

    Collection<AttributeModifier> getModifiersForOperation(AttributeModifier.Operation operation);

    void addModifier(AttributeModifier modifier);

    void addPersistentModifier(AttributeModifier modifier);

    void removeModifier(UUID uuid);

    void clearModifiers();

    void removeCachedModifiers(AttributeModifier modifier);

    void clearCachedModifiers();

    //--

    void write(CompoundTag tag);

    void read(CompoundTag tag);
}
