package io.wispforest.accessories.api;

import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.core.ExpandedSimpleContainer;
import net.minecraft.world.Container;
import org.jetbrains.annotations.Nullable;

import java.util.List;

///
/// Lightweight storage access for a given [SlotType] typically bound to a
/// given [LivingEntity][net.minecraft.world.entity.LivingEntity] unless in
/// areas where not permitted direct entity access resulting in a [SimpleAccessoriesStorage]
/// instead.
///
/// Designed for use within [io.wispforest.accessories.pond.AccessoriesRenderStateExtension].
///
public interface AccessoriesStorage {
    /**
     * @return The containers {@link SlotType} name
     */
    String getSlotName();

    ///
    /// The [SlotType] located from using the stored [SlotType#name()].
    ///
    @Nullable
    default SlotType slotType() {
        return SlotTypeLoader.INSTANCE.getSlotType(isClientSide(), this.getSlotName());
    }

    default SlotPath createPath(int index){
        return SlotPath.of(this.getSlotName(), index);
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

    /**
     * @return The main container holding the primary Accessory Stacks
     */
    Container getAccessories();

    /**
     * @return The main container holding the cosmetic Accessory Stacks
     */
    Container getCosmeticAccessories();

    /**
     * @return The max size of the given Container
     */
    int getSize();

    boolean isClientSide();

    default AccessoriesStorage copy() {
        return new SimpleAccessoriesStorage(
                this.isClientSide(),
                this.getSlotName(),
                this.getSize(),
                List.copyOf(this.renderOptions()),
                copyContainer(this.getAccessories()),
                copyContainer(this.getCosmeticAccessories())
        );
    }

    private static Container copyContainer(Container container) {
        if (container instanceof ExpandedSimpleContainer expandedContainer) {
            return expandedContainer.toImmutable();
        }

        return container;
    }
}
