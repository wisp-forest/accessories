package io.wispforest.accessories.api;

import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.world.Container;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

///
/// Lightweight storage access for a given [SlotType] typically bound to a
/// given [LivingEntity][net.minecraft.world.entity.LivingEntity] unless in
/// areas where not permitted direct entity access resulting in a [SimpleAccessoriesStorage]
/// instead.
///
/// Designed for use within [io.wispforest.accessories.pond.AccessoriesRenderStateExtension].
///
public interface AccessoriesStorage {
    ///
    /// @return The containers [SlotType] name
    ///
    String getSlotName();

    ///
    /// The [SlotType] located from using the stored [SlotType#name()].
    ///
    @Nullable
    default SlotType slotType() {
        return SlotTypeLoader.INSTANCE.getSlotType(isClientSide(), this.getSlotName());
    }

    ///
    /// Returns the [SlotPath] based on the containers slot name with the given `index`
    ///
    default SlotPath createPath(int index){
        return SlotPath.of(this.getSlotName(), index);
    }

    ///
    /// Returns a [Map] containing if the renderer has been disabled, if such index is not found within map
    /// then such is enabled. Recommend to use [#shouldRender(int)] instead of direct map access
    ///
    @ApiStatus.Internal
    Map<Integer, Boolean> renderOptions();

    ///
    /// @return If the given index for the container should render on the entity
    ///
    default boolean shouldRender(int index) {
        return renderOptions().getOrDefault(index, true);
    }

    ///
    /// Returns the Vanilla [Container] that holds primary Accessories
    ///
    Container getAccessories();

    ///
    /// Returns the Vanilla [Container] that holds cosmetic Accessories
    ///
    Container getCosmeticAccessories();

    ///
    /// @return The max size of the given storage
    ///
    int getSize();

    ///
    /// @return weather the current storage is from the client or server
    ///
    boolean isClientSide();
}
