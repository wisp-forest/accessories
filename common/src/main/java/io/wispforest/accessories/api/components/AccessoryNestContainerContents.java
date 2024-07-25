package io.wispforest.accessories.api.components;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.AccessoryNest;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.impl.AccessoryNestUtils;
import io.wispforest.endec.*;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.endec.impl.StructField;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

public final class AccessoryNestContainerContents {

    public static final AccessoryNestContainerContents EMPTY = new AccessoryNestContainerContents(ItemStack.EMPTY, List.of());

    public static final Endec<AccessoryNestContainerContents> ENDEC = new StructEndec<>() {
        private final StructField<AccessoryNestContainerContents, List<ItemStack>> field = CodecUtils.ofCodec(ItemStack.CODEC).listOf().fieldOf("accessories", AccessoryNestContainerContents::accessories);

        @Override
        public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, AccessoryNestContainerContents accessoryNestContainerContents) {
            field.encodeField(ctx, serializer, struct, accessoryNestContainerContents);
        }

        @Override
        public AccessoryNestContainerContents decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
            var data = field.decodeField(ctx, deserializer, struct);

            return new AccessoryNestContainerContents(ctx.requireAttributeValue(AccessoriesDataComponents.StackAttribute.INSTANCE).stack(), data);
        }
    };

    //--

    private final AccessoryNest accessoryNest;
    private final ItemStack stack;

    private final List<ItemStack> accessories;
    private final List<ItemStack> defensiveCopies = new ArrayList<>();
    private CompoundTag defensiveNbtData;

    //--

    public AccessoryNestContainerContents(ItemStack stack, List<ItemStack> accessories) {
        this.accessoryNest = (AccessoryNest) AccessoriesAPI.getAccessory(stack.getItem());
        this.stack = stack;

        this.accessories = accessories;

        for (var accessory : this.accessories) {
            defensiveCopies.add(accessory.copy());
        }

        if(stack.hasTag()) {
            this.defensiveNbtData = stack.getTag().copy();
        }
    }

    private AccessoryNestContainerContents(AccessoryNestContainerContents contents, List<ItemStack> accessories) {
        this.stack = contents.stack;

        this.accessoryNest = (AccessoryNest) AccessoriesAPI.getAccessory(stack.getItem());

        this.accessories = accessories;
    }

    public boolean hasChangesOccured(ItemStack holderStack){
        var prevStacks = this.defensiveCopies;
        var currentStacks = this.accessories;

        for (int i = 0; i < prevStacks.size(); i++) {
            var currentStack = currentStacks.get(i);

            if(ItemStack.matches(prevStacks.get(i), currentStack)) continue;

            accessoryNest.setInnerStack(holderStack, i, currentStack);
        }

        return true;
    }

    public final boolean isInvalid() {
        return !Objects.equals(stack.getTag(), defensiveNbtData);
    }

    @ApiStatus.Internal
    public AccessoryNestContainerContents setStack(int index, ItemStack stack) {
        var accessories = new ArrayList<>(accessories());

        accessories.set(index, stack);

        return new AccessoryNestContainerContents(this, accessories);
    }

    public Map<ItemStack, Accessory> getMap() {
        var map = new LinkedHashMap<ItemStack, Accessory>();

        this.accessories().forEach(stack1 -> map.put(stack1, AccessoriesAPI.getOrDefaultAccessory(stack1)));

        return map;
    }

    public Map<SlotEntryReference, Accessory> getMap(SlotReference slotReference) {
        var map = new LinkedHashMap<SlotEntryReference, Accessory>();

        var innerStacks = this.accessories();

        for (int i = 0; i < innerStacks.size(); i++) {
            var innerStack = innerStacks.get(i);

            if (innerStack.isEmpty()) continue;

            map.put(new SlotEntryReference(AccessoryNestUtils.create(slotReference, i), innerStack), AccessoriesAPI.getOrDefaultAccessory(innerStack));
        }

        return map;
    }

    public List<ItemStack> accessories() {
        return accessories;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AccessoryNestContainerContents) obj;
        return Objects.equals(this.accessories, that.accessories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessories);
    }

    @Override
    public String toString() {
        return "AccessoryNestContainerContents[" +
                "accessories=" + accessories + ']';
    }

}
