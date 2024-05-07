package io.wispforest.accessories.api;

public enum EquipmentChecking {
    ACCESSORIES_ONLY, // Check against Accessories Only
    COSMETICALLY_OVERRIDABLE, // Check against Cosmetics and Accessories. If cosmetic is present, replace Accessory stack with cosmetic stack.
}
