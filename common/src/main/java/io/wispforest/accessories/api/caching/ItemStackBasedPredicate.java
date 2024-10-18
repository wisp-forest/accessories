package io.wispforest.accessories.api.caching;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public abstract class ItemStackBasedPredicate implements Predicate<ItemStack> {

    private static final String UNKNOWN_PREDICATE = "UNKNOWN";

    private final String name;

    protected ItemStackBasedPredicate(String name) {
        this.name = name;
    }

    //--

    public static ItemStackBasedPredicate ofItem(Item item) {
        return ofItem(UNKNOWN_PREDICATE, item);
    }

    public static ItemStackBasedPredicate ofItem(String name, Item item) {
        return new ItemPredicate(name, item);
    }

    public static ItemStackBasedPredicate ofComponents(DataComponentType<?>... dataComponentTypes) {
        return ofComponents(UNKNOWN_PREDICATE, dataComponentTypes);
    }

    public static ItemStackBasedPredicate ofComponents(String name, DataComponentType<?>... dataComponentTypes) {
        return new DataComponentsPredicate(name, dataComponentTypes);
    }

    public static ItemStackBasedPredicate ofPredicate(Predicate<ItemStack> predicate) {
        return ofPredicate(UNKNOWN_PREDICATE, predicate);
    }

    public static ItemStackBasedPredicate ofPredicate(String name, Predicate<ItemStack> predicate) {
        if (predicate instanceof ItemStackBasedPredicate itemStackBasedPredicate) {
            return itemStackBasedPredicate;
        }

        return new ItemStackPredicate(name, predicate);
    }

    //--

    public final String name() {
        return this.name;
    }

    @Override
    public String toString() {
        return "Name: " + this.name + ", " + extraStringData();
    }

    public abstract String extraStringData();

    @Override
    public abstract boolean test(ItemStack stack);

    @Override
    public abstract int hashCode();

    protected abstract boolean isEqual(Object other);

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!this.getClass().isInstance(other)) return false;

        return this.isEqual(other);
    }
}
