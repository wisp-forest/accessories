package io.wispforest.accessories.api;

import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
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
    default SlotType slotType() {
        return SlotTypeLoader.getSlotType(this.capability().entity(), this.getSlotName());
    }

    /**
     * @return A SlotReference based on the containers linked entity and slot name with the given index
     */
    default SlotReference createReference(int index){
        return SlotReference.of(this.capability().entity(), this.getSlotName(), index);
    }

    /**
     * @return List containing if a given accessories slot should render or not
     */
    List<Boolean> renderOptions();

    /**
     * @return Either the toggle value for the given index or true if an option is not found
     */
    default boolean shouldRender(int index){
        var options = this.renderOptions();

        return (index < options.size()) ? options.get(index) : true;
    }

    //--

    /**
     * @return The main container holding the primary Accessory Stacks
     */
    ExpandedSimpleContainer getAccessories();

    /**
     * @return The main container holding the cosmetic Accessory Stacks
     */
    ExpandedSimpleContainer getCosmeticAccessories();

    /**
     * @return The max size of the given Container referring to the max number of slots available
     */
    int getSize();

    /**
     * Used to mark the container dirty for any call made to {@link #update}
     */
    void markChanged(boolean resizingUpdate);

    default void markChanged() {
        markChanged(true);
    }

    /**
     * @return if the given container has had a change occurred
     */
    boolean hasChanged();

    /**
     * Used to update the container if marked as changed
     */
    void update();

    //--

    /**
     * @return All slot modifiers applied to the given Container
     */
    Map<UUID, AttributeModifier> getModifiers();

    /**
     * @return All cached modifiers sent within sync packet to the client
     */
    Set<AttributeModifier> getCachedModifiers();

    /**
     * @return A collection of attribute modifiers using the specified operation type
     */
    Collection<AttributeModifier> getModifiersForOperation(AttributeModifier.Operation operation);

    /**
     * Adds a temporary slot modifier to the given container which means it will not be
     * present on a reload
     * @param modifier The specific AttributeModifier
     */
    void addTransientModifier(AttributeModifier modifier);

    /**
     * Adds a persistent slot modifier to the given container which means it will be
     * present on a reload
     * @param modifier The specific AttributeModifier
     */
    void addPersistentModifier(AttributeModifier modifier);

    /**
     * Remove the specific attribute modifier from the map if found
     * @param uuid The specific UUID
     */
    void removeModifier(UUID uuid);

    /**
     * Remove all modifiers from the given container
     */
    void clearModifiers();

    /**
     * Remove the specific attribute modifier from the cached map
     * @param modifier The specific AttributeModifier
     */
    void removeCachedModifiers(AttributeModifier modifier);

    /**
     * Remove all cached modifiers from the given container
     */
    void clearCachedModifiers();
}
