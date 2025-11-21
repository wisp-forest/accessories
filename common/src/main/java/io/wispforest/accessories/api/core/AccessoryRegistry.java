package io.wispforest.accessories.api.core;

import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.events.v2.CanEquipCallback;
import io.wispforest.accessories.api.events.v2.CanUnequipCallback;
import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AccessoryRegistry {

    private static final Map<Item, Accessory> ACCESSORIES = new HashMap<>();

    public static Map<Item, Accessory> getAllAccessories() {
        return Collections.unmodifiableMap(ACCESSORIES);
    }

    /**
     * Registers an accessory implementation for a given item.
     */
    public static void register(Item item, Accessory accessory) {
        ACCESSORIES.put(item, accessory);
    }

    /**
     * @return the accessory bound to this stack or {@code null} if there is none
     */
    public static Accessory getAccessoryOrDefault(ItemStack stack){
        var accessory = ACCESSORIES.get(stack.getItem());

        if(accessory == null) {
            accessory = stack.has(AccessoriesDataComponents.NESTED_ACCESSORIES) ? DEFAULT_NEST : DEFAULT;
        }

        return accessory;
    }

    /**
     * @return the accessory bound to this item or {@link #defaultAccessory()} if there is none
     */
    public static Accessory getAccessoryOrDefault(Item item){
        return ACCESSORIES.getOrDefault(item, DEFAULT);
    }

    /**
     * @return the accessory bound to this item or {@code null} if there is none
     */
    @Nullable
    public static Accessory getAccessory(Item item) {
        return ACCESSORIES.get(item);
    }

    /**
     * @return the default accessory implementation
     */
    public static Accessory defaultAccessory(){
        return DEFAULT;
    }

    public static boolean isDefaultAccessory(ItemStack stack) {
        return isDefaultAccessory(getAccessoryOrDefault(stack));
    }

    public static boolean isDefaultAccessory(Accessory accessory) {
        return accessory == DEFAULT || accessory == DEFAULT_NEST;
    }

    /**
     * Method used to check weather or not the given stack can be equipped within the slot referenced
     *
     * @param stack
     * @param reference
     * @return if the stack can be equipped or not
     */
    public static boolean canEquip(ItemStack stack, SlotReference reference){
        var buffer = new ActionResponseBuffer(true);

        CanEquipCallback.EVENT.invoker().canEquip(stack, reference, buffer);

        var state = buffer.canPerformAction();

        if(!state.equals(TriState.DEFAULT)) return state.orElse(true);

        getAccessoryOrDefault(stack).canEquip(stack, reference, buffer);

        return buffer.canPerformAction().orElse(true);
    }

    /**
     * Method used to check weather or not the given stack can be unequipped within the slot referenced
     *
     * @param stack
     * @param reference
     * @return if the stack can be unequipped or not
     */
    public static boolean canUnequip(ItemStack stack, SlotReference reference){
        var buffer = new ActionResponseBuffer(true);

        CanUnequipCallback.EVENT.invoker().canUnequip(stack, reference, buffer);

        var state = buffer.canPerformAction();

        if(!state.equals(TriState.DEFAULT)) return state.orElse(true);

        getAccessoryOrDefault(stack).canUnequip(stack, reference, buffer);

        return buffer.canPerformAction().orElse(true);
    }

    public static ActionResponseBuffer canEquipResponse(ItemStack stack, SlotReference reference){
        var buffer = new ActionResponseBuffer(false);

        CanEquipCallback.EVENT.invoker().canEquip(stack, reference, buffer);

        getAccessoryOrDefault(stack).canEquip(stack, reference, buffer);

        return buffer;
    }

    public static ActionResponseBuffer canUnequipResponse(ItemStack stack, SlotReference reference){
        var buffer = new ActionResponseBuffer(false);

        CanUnequipCallback.EVENT.invoker().canUnequip(stack, reference, buffer);

        getAccessoryOrDefault(stack).canUnequip(stack, reference, buffer);

        return buffer;
    }


    //--

    @ApiStatus.Internal
    private static final Accessory DEFAULT = new Accessory() {};

    @ApiStatus.Internal
    private static final AccessoryNest DEFAULT_NEST = new AccessoryNest() {};

}
