package io.wispforest.accessories.api.caching;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class ItemTagPredicate extends ItemStackBasedPredicate{

    private final TagKey<Item> itemTagKey;

    public ItemTagPredicate(String name, TagKey<Item> itemTagKey) {
        super(name);
        this.itemTagKey = itemTagKey;
    }

    @Override
    public boolean test(ItemStack stack) {
        return stack.is(this.itemTagKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.itemTagKey);
    }

    @Override
    protected boolean isEqual(Object other) {
        var itemTagPredicate = (ItemTagPredicate) other;

        return this.itemTagKey.equals(itemTagPredicate.itemTagKey);
    }

    @Override
    public String extraStringData() {
        return "ItemTag: " + this.itemTagKey.toString();
    }
}
