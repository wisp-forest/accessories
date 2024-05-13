package dev.emi.trinkets.compat;

import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketEnums;
import io.wispforest.accessories.Accessories;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public class WrappedSlotType extends SlotType {

    private final io.wispforest.accessories.api.slot.SlotType slotType;

    public WrappedSlotType(io.wispforest.accessories.api.slot.SlotType slotType){
        super("", "", 0, 0, new ResourceLocation(""), Set.of(), Set.of(Accessories.of("tag"), Accessories.of("compound")), Set.of(), null);

        this.slotType = slotType;
    }

    @Override
    public String getGroup() {
        return "";
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
}
