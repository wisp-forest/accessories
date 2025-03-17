package io.wispforest.accessories.api.caching;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Predicate;

public class ItemStackPredicate extends ItemStackBasedPredicate {
    private final Predicate<ItemStack> predicate;

    public ItemStackPredicate(String name, Predicate<ItemStack> predicate) {
        super(name);

        this.predicate = predicate;
    }

    @Override
    public boolean test(ItemStack stack) {
        return this.predicate.test(stack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.predicate);
    }

    @Override
    protected boolean isEqual(Object other) {
        var itemPredicate = (ItemStackPredicate) other;

        return this.predicate.equals(itemPredicate.predicate);
    }

    @Override
    public String extraStringData() {
        return "Predicate: " + this.predicate.toString();
    }
}
