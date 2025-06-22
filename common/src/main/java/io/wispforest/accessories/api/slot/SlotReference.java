package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.slot.SlotReferenceImpl;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.client.AccessoryBreak;
import io.wispforest.accessories.pond.AccessoriesLivingEntityExtension;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A reference to a specific accessory slot of a {@link LivingEntity}.
 */
public non-sealed interface SlotReference extends DelegatingSlotPath {

    static SlotReference of(LivingEntity livingEntity, String slotName, int index) {
        return of(livingEntity, SlotPath.of(slotName, index));
    }

    static SlotReference of(LivingEntity livingEntity, SlotPath slotPath) {
        return new SlotReferenceImpl(livingEntity, slotPath);
    }

    @Deprecated
    static SlotReference ofNest(LivingEntity livingEntity, String slotName, int initialHolderSlot, List<Integer> innerSlotIndices) {
        return of(livingEntity, SlotPath.of(slotName, initialHolderSlot, innerSlotIndices));
    }

    /**
     * @return the referenced entity
     */
    LivingEntity entity();

    /**
     * @return the referenced slot name
     */
    default String slotName() {
        return slotPath().slotName();
    }

    /**
     * @return the referenced slot index
     */
    default int index() {
        return slotPath().index();
    }

    SlotPath slotPath();

    //--

    /**
     * Helper method to trigger effects of a given accessory being broken on any tracking clients for the given entity
     */
    default void breakStack() {
        var entity = this.entity();

        AccessoriesNetworking.sendToTrackingAndSelf(entity, AccessoryBreak.of(this));

        var currentStack = this.getStack();

        if (currentStack != null) {
            ((AccessoriesLivingEntityExtension) entity).pushEnchantmentContext(currentStack, this);

            EnchantmentHelper.stopLocationBasedEffects(currentStack, entity, AccessoriesInternals.INTERNAL_SLOT);
        }
    }

    //--

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

    boolean isValid();

    /**
     * @return the current referenced stack
     */
    @Nullable
    ItemStack getStack();

    /**
     * @return {@code true} if the stack was successfully set, {@code false} otherwise
     */
    boolean setStack(ItemStack stack);

    //--

    static String createBaseSlotPath(SlotType slotType, int index) {
        return createBaseSlotPath(slotType.name(), index);
    }

    static String createBaseSlotPath(String name, int index) {
        return SlotPath.of(name, index).createString();
    }

    @Nullable
    static Pair<String, Integer> parseBaseSlotPath(String path) {
        var parts = path.split("/");

        if (parts.length <= 1) return null;

        var baseSlotName = parts[0].replace("-", ":");
        var index = Integer.parseInt(parts[1]);

        return Pair.of(baseSlotName, index);
    }

    //--

    @Deprecated
    default String createSlotPath() {
        return createString();
    }

    @Deprecated
    default int slot() {
        return index();
    }
}
