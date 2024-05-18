package top.theillusivec4.curios.common.capability;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.compat.CuriosWrappingUtils;
import top.theillusivec4.curios.compat.WrappedCurioItemHandler;

import java.util.*;
import java.util.function.Function;

public class CurioInventory implements INBTSerializable<CompoundTag> {

    CompoundTag deserialized = new CompoundTag();
    boolean markDeserialized = false;

    public void init(final ICuriosItemHandler curiosItemHandler) {
        init(((WrappedCurioItemHandler)curiosItemHandler).capability());
    }

    public void init(AccessoriesCapabilityImpl capability) {
        var livingEntity = capability.entity();

        if (this.markDeserialized) {
            var tagList = this.deserialized.getList("Curios", Tag.TAG_COMPOUND);

            readData(livingEntity, capability, tagList);

            this.deserialized = new CompoundTag();
            this.markDeserialized = false;
        }
    }

    public static void readData(LivingEntity livingEntity, AccessoriesCapability capability, ListTag data) {
        for (int i = 0; i < data.size(); i++) {
            var tag = data.getCompound(i);
            var identifier = tag.getString("Identifier");

            var slotType = SlotTypeLoader.getSlotType(livingEntity.level(), CuriosWrappingUtils.curiosToAccessories(identifier));

            var container = (slotType != null) ? capability.getContainer(slotType) : null;

            ((AccessoriesHolderImpl) capability.getHolder()).invalidStacks
                    .addAll(deserializeNBT_StackHandler(container, tag.getCompound("StacksHandler")));
        }
    }

    private static List<ItemStack> deserializeNBT_StackHandler(@Nullable AccessoriesContainer container, CompoundTag nbt){
        var dropped = new ArrayList<ItemStack>();

        if (nbt.contains("Stacks")) {
            dropped.addAll(deserializeNBT_Stacks(container, AccessoriesContainer::getAccessories, nbt.getCompound("Stacks")));
        }

        if (nbt.contains("Cosmetics")) {
            dropped.addAll(deserializeNBT_Stacks(container, AccessoriesContainer::getCosmeticAccessories, nbt.getCompound("Cosmetics")));
        }

        return dropped;
    }

    private static List<ItemStack> deserializeNBT_Stacks(@Nullable AccessoriesContainer container, Function<AccessoriesContainer, Container> containerFunc, CompoundTag nbt){
        var list = nbt.getList("Items", Tag.TAG_COMPOUND)
                .stream()
                .map(tagEntry -> ItemStack.of((tagEntry instanceof CompoundTag compoundTag) ? compoundTag : new CompoundTag()))
                .toList();

        var dropped = new ArrayList<ItemStack>();

        if(container != null) {
            var accessories = containerFunc.apply(container);

            for (var stack : list) {
                boolean consumedStack = false;

                for (int i = 0; i < accessories.getContainerSize() && !consumedStack; i++) {
                    var currentStack = accessories.getItem(i);

                    if (!currentStack.isEmpty()) continue;

                    var ref = new io.wispforest.accessories.api.slot.SlotReference(container.getSlotName(), container.capability().entity(), i);

                    if (!AccessoriesAPI.canInsertIntoSlot(stack, ref)) continue;

                    accessories.setItem(i, stack.copy());

                    consumedStack = true;
                }

                if (!consumedStack) dropped.add(stack.copy());
            }
        } else {
            dropped.addAll(list);
        }

        return dropped;
    }

    @Override
    public CompoundTag serializeNBT() {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.deserialized = nbt;
        this.markDeserialized = true;
    }
}
