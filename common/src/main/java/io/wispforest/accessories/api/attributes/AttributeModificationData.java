package io.wispforest.accessories.api.attributes;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

public record AttributeModificationData(@Nullable String slotPath, Holder<Attribute> attribute, AttributeModifier modifier) {

    public AttributeModificationData(Holder<Attribute> attribute, AttributeModifier modifier) {
        this(null, attribute, modifier);
    }

    @Override
    public AttributeModifier modifier() {
        return (this.slotPath != null)
                ? new AttributeModifier(modifier.id().withPath((path) -> this.slotPath + "/" + path), modifier.amount(), modifier.operation())
                : modifier;
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
