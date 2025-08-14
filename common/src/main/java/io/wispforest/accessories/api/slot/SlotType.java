package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesStorage;
import io.wispforest.accessories.api.events.DropRule;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

///
/// As a holder object, [SlotType] stores information pertaining to a given properties that define a slot entry.
///
/// Due to the nature that these objects are reloadable from [SlotTypeLoader],
/// it is recommended not to hold onto such values as there inner properties might change.
///
public interface SlotType {

    ResourceLocation EMPTY_SLOT_ICON = Accessories.of("gui/slot/empty");

    ///
    /// @return The slot name which may or may not contain a namespace
    ///
    String name();

    ///
    /// @return A parsed slot name as either `accessories:{group_name_here}` or `{group_namespace_here}:{group_name_here}`
    ///
    default ResourceLocation getId() {
        return Accessories.parseLocationOrDefault(this.name());
    }

    ///
    /// @return A {@link Component} Translation key for the given slot
    ///
    default String translation(){
        return Accessories.translationKey("slot." + name().replace(":", "."));
    }

    ///
    /// @return The location for the given icon within the Block Atlas for the given slot type.
    ///
    ResourceLocation icon();

    ///
    /// Used with sorting all registered slots within a given group when creating list of slots
    ///
    /// @return The priority order for the given slot
    ///
    int order();

    ///
    /// @return The base amount that the given [AccessoriesStorage] will have to store accessories
    ///
    int amount();

    ///
    /// @return A set of [ResourceLocation] used to check if an accessory is valid for the given
    /// slot used within {@link SlotPredicateRegistry#canInsertIntoSlot}.
    ///
    Set<ResourceLocation> validators();

    ///
    /// @return The given {@link DropRule} used upon an entity's death to decided what action to perform with
    /// an accessory's equipped within the given slots [AccessoriesStorage]
    ///
    DropRule dropRule();
}
