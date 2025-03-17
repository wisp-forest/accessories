package io.wispforest.accessories.api.caching;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class ItemPredicate extends ItemStackBasedPredicate {

    private final Item item;

    public ItemPredicate(String name, Item item) {
        super(name);
        this.item = item;
    }

    @Override
    public boolean test(ItemStack stack) {
        return stack.getItem().equals(this.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.item);
    }

    @Override
    protected boolean isEqual(Object other) {
        var itemPredicate = (ItemPredicate) other;

        return this.item.equals(itemPredicate.item);
    }

    @Override
    public String extraStringData() {
        return "Item: " + this.item.toString();
    }
}
