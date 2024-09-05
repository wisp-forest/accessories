package io.wispforest.accessories.api.client;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.function.Predicate;

/**
 * Class used to check for if a {@link Item} is
 * a valid Target for rendering and used within {@link DefaultAccessoryRenderer#render}
 */
public enum TargetType {
    ITEM(item -> !(item instanceof BlockItem)),
    BLOCK(item -> item instanceof BlockItem),
    ALL(item -> true);

    private final Predicate<Item> predicate;

    TargetType(Predicate<Item> predicate) {
        this.predicate = predicate;
    }

    public boolean isValid(Item item) {
        return this.predicate.test(item);
    }
}
