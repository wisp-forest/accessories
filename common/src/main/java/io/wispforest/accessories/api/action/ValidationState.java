package io.wispforest.accessories.api.action;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.tooltip.ComponentBuilder;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Locale;

///
/// An enum class used to indicate if a given response is valid, invalid, irrelevant.
///
public enum ValidationState {
    ///
    /// Invalid means that something about the given input is unable to perform the given action.
    ///
    INVALID,
    ///
    /// Valid means that something about the given input is able to perform the given action.
    ///
    VALID,
    ///
    /// Irrelevant means that something about the given input was found to not indicate any invalid or valid reasons
    /// to preform the action. Typically, means that the action must use other responses if any to determine if
    /// the action is valid or invalid.
    ///
    IRRELEVANT;

    public String formatedName(boolean allowIrrelevant) {
        return (!allowIrrelevant && this == IRRELEVANT)
            ? INVALID.formatedName()
            : formatedName();
    }

    public String formatedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public boolean isValid() {
        return this == VALID;
    }

    public boolean isValid(boolean defaultValue) {
        return isValid() || (this == IRRELEVANT && defaultValue);
    }

    public @Nullable Boolean unbox() {
        return switch (this) {
            case INVALID -> false;
            case VALID -> true;
            case IRRELEVANT -> null;
        };
    }

    public TriState toTriState() {
        return switch (this) {
            case INVALID -> TriState.FALSE;
            case VALID -> TriState.TRUE;
            case IRRELEVANT -> TriState.DEFAULT;
        };
    }

    public static ValidationState of(boolean isValid) {
        return isValid ? VALID : INVALID;
    }

    public static ValidationState ofOrIrrelevant(boolean isValid) {
        return (isValid ? VALID : IRRELEVANT);
    }

    public static ValidationState of(TriState state) {
        return switch (state) {
            case FALSE -> ValidationState.INVALID;
            case TRUE -> ValidationState.VALID;
            case DEFAULT -> ValidationState.IRRELEVANT;
        };
    }

    public ComponentBuilder asColorBuilder() {
        return Accessories.translationWithArgs("tooltip.slot", this.name().toLowerCase(Locale.ROOT));
    }

    public MutableComponent asEntryComponent() {
        return switch (this) {
            case VALID -> Accessories.translation("tooltip.equipment_reasoning.list_entry.valid");
            case INVALID -> Accessories.translation("tooltip.equipment_reasoning.list_entry.invalid");
            case IRRELEVANT -> Accessories.translation("tooltip.equipment_reasoning.list_entry.irrelevant");
        };
    }
}
