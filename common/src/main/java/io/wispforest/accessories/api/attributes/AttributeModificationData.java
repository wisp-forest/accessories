package io.wispforest.accessories.api.attributes;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

///
/// Helper record to hold data on a Attribute Modification made within the [AccessoryAttributeBuilder]
///
@ApiStatus.Internal
public record AttributeModificationData(@Nullable String slotPath, Holder<Attribute> attribute, AttributeModifier modifier, boolean usedInSlotValidation) {

    public AttributeModificationData(@Nullable String slotPath, Holder<Attribute> attribute, AttributeModifier modifier) {
        this(slotPath, attribute, modifier, false);
    }

    public AttributeModificationData(Holder<Attribute> attribute, AttributeModifier modifier, boolean usedInSlotValidation) {
        this(null, attribute, modifier, usedInSlotValidation);
    }

    public AttributeModificationData(Holder<Attribute> attribute, AttributeModifier modifier) {
        this(attribute, modifier, false);
    }

    public boolean isValid(boolean filterSlotBasedPredicates, boolean isSlotValidation) {
        return isValid(filterSlotBasedPredicates ? AllowedType.BASE : AllowedType.ALL, isSlotValidation);
    }

    public boolean isValid(AllowedType mode, boolean isSlotValidation) {
        if (!mode.equals(AllowedType.ALL)) {
            if (mode.equals(AllowedType.SLOT) != (attribute().value() instanceof SlotAttribute)) return false;
        }

        if (isSlotValidation) return this.usedInSlotValidation();

        return true;
    }

    public boolean equalsWithoutPath(Object object) {
        return object instanceof AttributeModificationData that
            && usedInSlotValidation == that.usedInSlotValidation
            && Objects.equals(modifier, that.modifier)
            && Objects.equals(attribute, that.attribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute, modifier, usedInSlotValidation);
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

    public enum AllowedType {
        BASE,
        SLOT,
        ALL
    }
}
