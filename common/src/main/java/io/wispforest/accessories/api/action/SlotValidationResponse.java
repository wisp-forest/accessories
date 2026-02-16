package io.wispforest.accessories.api.action;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.tooltip.ListTooltipAdder;
import io.wispforest.accessories.api.tooltip.impl.ListTooltipEntry;
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.api.slot.SlotType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.Objects;
import java.util.SequencedCollection;

public class SlotValidationResponse extends ActionResponseBase {

    private final String slotName;

    private final SequencedCollection<String> validSlots;
    private final SequencedCollection<String> invalidSlots;

    public SlotValidationResponse(String slotName, SequencedCollection<String> validSlots, SequencedCollection<String> invalidSlots) {
        super(isValid(slotName, validSlots, invalidSlots));

        this.slotName = slotName;

        this.validSlots = validSlots;
        this.invalidSlots = invalidSlots;
    }

    public static ValidationState isValid(String slotName, SequencedCollection<String> validSlots, SequencedCollection<String> invalidSlots) {
        if (invalidSlots.contains(slotName)) {
            return ValidationState.INVALID;
        } else if (validSlots.contains(slotName) || validSlots.contains(AccessoriesBaseData.ANY_SLOT)) {
            return ValidationState.VALID;
        } else {
            return ValidationState.IRRELEVANT;
        }
    }

    public String getSlotName() {
        return slotName;
    }

    public SequencedCollection<String> getValidSlots() {
        return validSlots;
    }

    public SequencedCollection<String> getInvalidSlots() {
        return invalidSlots;
    }

    @Override
    public void addInfo(ListTooltipAdder adder, Item.TooltipContext ctx, TooltipFlag type) {
        var state = this.canPerformAction();

        SequencedCollection<String> slots = switch (this.canPerformAction()) {
            case INVALID -> getInvalidSlots();
            case VALID, IRRELEVANT -> getValidSlots();
        };

        if (!slots.isEmpty()) {
            var slotNames = ComponentUtils.formatList(
                slots,
                Accessories.translation("tooltip.validator.component.entry_separator"),
                slotName -> {
                    return (Objects.equals(slotName, this.slotName) || Objects.equals(slotName, "any") ? state : ValidationState.IRRELEVANT)
                        .asColorBuilder()
                        .withArgs(Component.translatable(SlotType.toTranslationKey(slotName)));
                }
            );

            adder.add(Accessories.translationWithArgs("tooltip.validator.component.advanced", this.canPerformAction().formatedName()).withArgs(slotNames));
        } else {
            adder.add(Accessories.translation("tooltip.validator.component.simple", this.canPerformAction().formatedName()));
        }
    }

}
