package io.wispforest.accessories.api.attributes;

import io.wispforest.accessories.utils.AttributeUtils;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

public record AttributeModificationData(@Nullable String slotPath, Attribute attribute, AttributeModifier modifier) {

    public AttributeModificationData(Attribute attribute, AttributeModifier modifier) {
        this(null, attribute, modifier);
    }

    @Override
    public AttributeModifier modifier() {
        if (this.slotPath == null) return modifier;

        var location = AttributeUtils.getLocation(modifier.getName())
                .withPath((path) -> this.slotPath + "/" + path);

        var data = AttributeUtils.getModifierData(location);

        return new AttributeModifier(data.second(), data.first(), modifier.getAmount(), modifier.getOperation());
    }

    @Override
    public String toString() {
        return "AttributeModifierInstance[" +
                "attribute=" + this.attribute + ", " +
                "modifier=" + this.modifier +
                "slotPath=" + (this.slotPath != null ? this.slotPath : "none") +
                ']';
    }
}
