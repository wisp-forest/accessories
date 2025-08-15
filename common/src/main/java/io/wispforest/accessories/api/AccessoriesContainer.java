package io.wispforest.accessories.api;

import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.impl.core.ExpandedContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

///
/// An entity based version of [AccessoriesStorage] for a given [SlotType]. Primary access when not
/// with render context with ability to adjust slots attribute controlling its sizing or ability
/// to access owners [AccessoriesCapability]
///
public interface AccessoriesContainer extends AccessoriesStorage {

    ///
    /// Returns the [AccessoriesCapability] of the owner of this container
    ///
    AccessoriesCapability capability();

    ///
    /// Returns the [SlotReference] based on the containers linked entity and slot name with the given `index`
    ///
    default SlotReference createReference(int index){
        return SlotReference.of(this.capability().entity(), this.createPath(index));
    }

    //--

    ///
    /// Returns the Vanilla [Container] that holds primary Accessories
    ///
    @Override
    ExpandedContainer getAccessories();

    ///
    /// Returns the Vanilla [Container] that holds cosmetic Accessories
    ///
    @Override
    ExpandedContainer getCosmeticAccessories();

    //--

    ///
    /// Sets if the given `index` accessory slot should be rendering
    ///
    void setShouldRender(int index, boolean value);

    ///
    /// Used to mark the container dirty for any call made to [#update]
    ///
    default void markChanged() {
        markChanged(true);
    }

    ///
    /// Used to mark the container dirty for any call made to [#update]
    ///
    void markChanged(boolean resizingUpdate);

    ///
    /// Returns whether the given container has had a change occurred
    ///
    boolean hasChanged();

    ///
    /// Method that will attempt to update the container if marked as changed
    ///
    void update();

    //--

    ///
    /// @return All slot modifiers applied to the given Container
    ///
    Map<ResourceLocation, AttributeModifier> getModifiers();

    ///
    /// @return All cached modifiers sent within sync packet to the client
    ///
    Set<AttributeModifier> getCachedModifiers();

    ///
    /// @return A collection of attribute modifiers using the specified operation type
    ///
    Collection<AttributeModifier> getModifiersForOperation(AttributeModifier.Operation operation);

    ///
    /// Adds a temporary slot modifier to the given container which means it will not be
    /// present on a reload
    /// @param modifier The specific AttributeModifier
    ///
    void addTransientModifier(AttributeModifier modifier);

    ///
    /// Adds a persistent slot modifier to the given container which means it will be
    /// present on a reload
    /// @param modifier The specific AttributeModifier
    ///
    void addPersistentModifier(AttributeModifier modifier);

    ///
    /// Remove the specific attribute modifier from the map if found
    /// @param location The specific location
    ///
    boolean hasModifier(ResourceLocation location);

    ///
    /// Remove the specific attribute modifier from the map if found
    /// @param location The specific location
    ///
    void removeModifier(ResourceLocation location);

    ///
    /// Remove all modifiers from the given container
    ///
    void clearModifiers();

    ///
    /// Remove the specific attribute modifier from the cached map
    /// @param modifier The specific AttributeModifier
    ///
    void removeCachedModifiers(AttributeModifier modifier);

    ///
    /// Remove all cached modifiers from the given container
    ///
    void clearCachedModifiers();
}
