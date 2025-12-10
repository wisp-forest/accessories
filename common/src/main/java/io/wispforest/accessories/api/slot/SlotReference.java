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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

///
/// An extension of [SlotPath] which contains a reference to the specific [LivingEntity] that this
/// path is valid for.
///
/// Typically, it is safe to assume that such is valid for any methods that take such as a parameter.
///
/// It is not recommend to hold onto such objects due to the lifespan of [LivingEntity] being unknown
/// combined with the fact of [SlotType]'s being reloadable and resizable meaning that such main not
/// be present depending on the amount of time that has elapsed.
///
/// You can confirm that this is still valid by using the references[isValid][#isValid()]
///
@ApiStatus.NonExtendable
public non-sealed interface SlotReference extends DelegatingSlotPath {

    static SlotReference of(LivingEntity livingEntity, String slotName, int index) {
        return of(livingEntity, SlotPath.of(slotName, index));
    }

    static SlotReference of(LivingEntity livingEntity, SlotPath slotPath) {
        if (slotPath instanceof DelegatingSlotPath delegating) {
            slotPath = delegating.unpack();
        }

        return new SlotReferenceImpl(livingEntity, slotPath);
    }

    @Deprecated
    static SlotReference ofNest(LivingEntity livingEntity, String slotName, int initialHolderSlot, List<Integer> innerSlotIndices) {
        return of(livingEntity, SlotPath.of(slotName, initialHolderSlot, innerSlotIndices));
    }

    ///
    /// @return the [LivingEntity] that has the given path to such accessory slot
    ///
    LivingEntity entity();

    ///
    /// @return the referenced [SlotPath] to the given accessory slot
    ///
    SlotPath slotPath();

    //--

    ///
    /// A Helper method similar to [LivingEntity#onEquippedItemBroken] to trigger effects of a given accessory to
    /// be broken on any tracking clients for the given entity.
    ///
    default void breakStack() {
        var entity = this.entity();

        AccessoriesNetworking.sendToTrackingAndSelf(entity, AccessoryBreak.of(this));

        var currentStack = this.getStack();

        if (currentStack != null) {
            ((AccessoriesLivingEntityExtension) entity).pushEnchantmentContext(currentStack, this);

            EnchantmentHelper.stopLocationBasedEffects(currentStack, entity, AccessoriesInternals.INSTANCE.getInternalEquipmentSlot());
        }
    }

    //--

    ///
    /// @return the given paths current registered [SlotType]
    ///
    @Nullable
    default SlotType type(){
        return SlotTypeLoader.getSlotType(entity().level(), slotName());
    }

    ///
    /// @return the given entity's [AccessoriesCapability]
    ///
    @Nullable
    default AccessoriesCapability capability() {
        return this.entity().accessoriesCapability();
    }

    ///
    /// @return the given [AccessoriesContainer] if present from the entity's slot storage
    ///
    @Nullable
    default AccessoriesContainer slotContainer() {
        var capability = this.capability();

        if(capability == null) return null;

        return capability.getContainers().get(slotName());
    }

    ///
    /// @return whether the given reference is still valid for the given [LivingEntity]
    ///
    boolean isValid();

    ///
    /// @return the current referenced stack at the [SlotPath] for the given [LivingEntity]
    ///
    @Nullable
    ItemStack getStack();

    ///
    /// @return `true` if the given stack was successfully set, other wise `false` if failed
    ///
    boolean setStack(ItemStack stack);

    //--

    @Deprecated
    static String createBaseSlotPath(SlotType slotType, int index) {
        return createBaseSlotPath(slotType.name(), index);
    }

    @Deprecated
    static String createBaseSlotPath(String name, int index) {
        return SlotPath.createBaseSlotPath(name, index);
    }

    @Nullable
    @Deprecated
    static Pair<String, Integer> parseBaseSlotPath(String path) {
        var parts = path.split("/");

        if (parts.length <= 1) return null;

        var baseSlotName = parts[0].replace("-", ":");
        var index = Integer.parseInt(parts[1]);

        return Pair.of(baseSlotName, index);
    }

    @Deprecated
    default String createSlotPath() {
        return createString();
    }

    @Deprecated
    default int slot() {
        return index();
    }
}
