package io.wispforest.accessories.api.slot;

import com.google.common.collect.ImmutableList;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.slot.NestedSlotReferenceImpl;
import io.wispforest.accessories.impl.slot.SlotReferenceImpl;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.client.AccessoryBreak;
import io.wispforest.accessories.pond.AccessoriesLivingEntityExtension;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A reference to a specific accessory slot of a {@link LivingEntity}.
 */
public interface SlotReference {

    static SlotReference of(LivingEntity livingEntity, String slotName, int slot) {
        return new SlotReferenceImpl(livingEntity, slotName, slot);
    }

    static SlotReference ofNest(LivingEntity livingEntity, String slotName, int initialHolderSlot, List<Integer> innerSlotIndices) {
        return new NestedSlotReferenceImpl(livingEntity, slotName, initialHolderSlot, ImmutableList.copyOf(innerSlotIndices));
    }

    /**
     * @return the referenced slot name
     */
    String slotName();

    /**
     * @return the referenced entity
     */
    LivingEntity entity();

    /**
     * @return the referenced slot index
     */
    int slot();

    //--

    /**
     * Helper method to trigger effects of a given accessory being broken on any tracking clients for the given entity
     */
    default void breakStack() {
        var entity = this.entity();

        AccessoriesNetworking.sendToTrackingAndSelf(entity, AccessoryBreak.of(this));

        var currentStack = this.getStack();

        ((AccessoriesLivingEntityExtension) entity).pushEnchantmentContext(currentStack, this);

        EnchantmentHelper.stopLocationBasedEffects(currentStack, entity, AccessoriesInternals.INTERNAL_SLOT);
    }

    default boolean isValid() {
        var capability = this.capability();

        if(capability == null) return false;

        var container = capability.getContainers().get(this.slotName());

        if(container == null) return false;

        return slot() < container.getSize();
    }

    default String createSlotPath() {
        return createBaseSlotPath(this.slotName(), this.slot());
    }

    @Nullable
    default SlotType type(){
        return SlotTypeLoader.getSlotType(entity().level(), slotName());
    }

    @Nullable
    default AccessoriesCapability capability() {
        return this.entity().accessoriesCapability();
    }

    @Nullable
    default AccessoriesContainer slotContainer() {
        var capability = this.capability();

        if(capability == null) return null;

        return capability.getContainers().get(slotName());
    }

    /**
     * @return the current referenced stack
     */
    @Nullable
    default ItemStack getStack() {
        var container = this.slotContainer();

        if(container == null) return null;

        return container.getAccessories().getItem(slot());
    }

    /**
     * @return {@code true} if the stack was successfully set, {@code false} otherwise
     */
    default boolean setStack(ItemStack stack) {
        var container = this.slotContainer();

        if(container == null) return false;

        container.getAccessories().setItem(slot(), stack);

        return true;
    }

    //--

    static String createBaseSlotPath(SlotType slotType, int index) {
        return createBaseSlotPath(slotType.name(), index);
    }

    static String createBaseSlotPath(String name, int index) {
        return name.replace(":", "-") + "/" + index;
    }

    @Nullable
    static Pair<String, Integer> parseBaseSlotPath(String path) {
        var parts = path.split("/");

        if (parts.length < 1) return null;

        var baseSlotName = parts[0].replace("-", ":");
        var index = Integer.parseInt(parts[1]);

        return Pair.of(baseSlotName, index);
    }
}
