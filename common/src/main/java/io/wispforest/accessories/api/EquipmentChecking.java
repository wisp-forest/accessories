package io.wispforest.accessories.api;

/**
 * Helper Enum class used to indicate within {@link AccessoriesCapability#getFirstEquipped} to allow for
 * checking Cosmetic Slots before Main slot for external renderers or to just check logical slots within
 * capability
 * <ul>
 *     <li> {@link #ACCESSORIES_ONLY} - Check against Accessories Only </li>
 *     <li> {@link #COSMETICALLY_OVERRIDABLE} - Check against Cosmetics and Accessories. If cosmetic is present, replace Accessory stack with cosmetic stack. </li>
 * </ul>
 */
public enum EquipmentChecking {
    ACCESSORIES_ONLY,
    COSMETICALLY_OVERRIDABLE;
}
