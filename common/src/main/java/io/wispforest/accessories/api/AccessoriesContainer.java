package io.wispforest.accessories.api;

import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.*;

public interface AccessoriesContainer {

    /**
     * @return the given AccessoriesCapability from which such was initialized with
     */
    AccessoriesCapability capability();

    /**
     * @return The Containers slot name
     */
    String getSlotName();

    /**
     * @return An Optional of the given slotType based on the {@link #getSlotName} if found or an empty optional
     */
    default Optional<SlotType> slotType() {
        return SlotTypeLoader.getSlotType(this.capability().getEntity().level(), this.getSlotName());
    }

    default SlotReference createReference(int index){
        return new SlotReference(getSlotName(), capability().getEntity(), index);
    }

    /**
     * @return List containing if a given accessories slot should render or not
     */
    List<Boolean> renderOptions();

    /**
     * @return Either the toggle value for the given index or true if an option is not found
     */
    default boolean shouldRender(int index){
        var options = renderOptions();

        return (index < options.size()) ? options.get(index) : true;
    }

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
}
