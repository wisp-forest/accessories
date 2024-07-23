package io.wispforest.accessories.api;

/**
 * Represents what happens to an accessory after death.
 */
public enum DropRule {
    /**
     * Accessory remains in the player's inventory.
     */
    KEEP,
    /**
     * Accessory is dropped on the ground as an item entity.
     */
    DROP,
    /**
     * Accessory is voided.
     */
    DESTROY,
    /**
     * Default vanilla behaviour (game rules/enchantments) is used.
     */
    DEFAULT
}
