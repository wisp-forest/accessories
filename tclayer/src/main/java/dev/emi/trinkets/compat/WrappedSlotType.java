package dev.emi.trinkets.compat;

import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketEnums;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.data.SlotGroupLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public class WrappedSlotType extends SlotType {

    private final io.wispforest.accessories.api.slot.SlotType slotType;
    private String slotGroup = "";

    public WrappedSlotType(io.wispforest.accessories.api.slot.SlotType slotType, boolean isClientSide){
        super("", "", 0, 0, new ResourceLocation(""), Set.of(), Set.of(), Set.of(), null);

        var groups = SlotGroupLoader.INSTANCE.getGroups(isClientSide, false);

        for (var group : groups) {
            if(group.slots().contains(slotType.name())) {
                this.slotGroup = WrappingTrinketsUtils.accessoriesToTrinkets_Group(group.name());

                break;
            }
        }

        this.slotType = slotType;
    }

    @Override
    public String getGroup() {
        return this.slotGroup;
    }

    @Override
    public String getName() {
        return WrappingTrinketsUtils.accessoriesToTrinkets_Slot(slotType.name());
    }

    @Override
    public int getOrder() {
        return slotType.order();
    }

    @Override
    public int getAmount() {
        return slotType.amount();
    }

    @Override
    public ResourceLocation getIcon() {
        return slotType.icon();
    }

    @Override
    public TrinketEnums.DropRule getDropRule() {
        return TrinketEnums.convert(slotType.dropRule());
    }

    public MutableComponent getTranslation() {
        return Component.translatable("trinkets.slot." + this.getGroup() + "." + this.getName());
    }

    @Override
    public Set<ResourceLocation> getQuickMovePredicates() {
        return this.slotType.validators();
    }

    @Override
    public Set<ResourceLocation> getValidatorPredicates() {
        return this.slotType.validators();
    }

    @Override
    public Set<ResourceLocation> getTooltipPredicates() {
        return this.slotType.validators();
    }
}
