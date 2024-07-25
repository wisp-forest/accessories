package io.wispforest.accessories.api;

import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface AccessoriesContainer {

    /**
     * @return The bound {@link AccessoriesCapability} this belongs to
     */
    AccessoriesCapability capability();

    /**
     * @return The containers {@link SlotType} name
     */
    String getSlotName();

    /**
     * @return The given {@link SlotType} of the given container or null if not found
     */
    @Nullable
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
     * @return List containing toggle values for if a given Accessory Slot should be rendered on the entity or not
     */
    List<Boolean> renderOptions();

    /**
     * @return If the given index for the container should render on the entity
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
     * @return The max size of the given Container
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
     * @param id The specific location
     */
    boolean hasModifier(UUID id);

    /**
     * Remove the specific attribute modifier from the map if found
     * @param id The specific location
     */
    void removeModifier(UUID id);

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
