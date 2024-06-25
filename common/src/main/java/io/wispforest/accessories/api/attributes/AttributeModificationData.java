package io.wispforest.accessories.api.attributes;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class AttributeModificationData {

    @Nullable private final String slotPath;

    private final Holder<Attribute> attribute;
    private final AttributeModifier modifier;

    public AttributeModificationData(Holder<Attribute> attribute, AttributeModifier modifier) {
        this(null, attribute, modifier);
    }

    public AttributeModificationData(String slotPath, Holder<Attribute> attribute, AttributeModifier modifier) {
        this.slotPath = slotPath;
        this.attribute = attribute;
        this.modifier = modifier;
    }

    public Holder<Attribute> attribute() {
        return attribute;
    }

    public AttributeModifier modifier() {
        if(this.slotPath != null) {
            return new AttributeModifier(modifier.id().withPath((path) -> this.slotPath + "/" + path), modifier.amount(), modifier.operation());
        }

        return modifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof AttributeModificationData otherInstance)) return false;

        return Objects.equals(this.attribute, otherInstance.attribute) &&
                Objects.equals(this.modifier, otherInstance.modifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.attribute, this.modifier);
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
