package io.wispforest.accessories.api.components;

import io.wispforest.accessories.api.events.DropRule;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public record AccessoryStackSettings(
        DropRule dropRule,
        boolean canEquipFromDispenser,
        boolean canEquipFromUse,
        boolean useStackSize,
        int sizeOverride,
        Map<String, Component> slotBasedTooltips,
        Component extraTooltip
) {
    public static final AccessoryStackSettings DEFAULT = new AccessoryStackSettings(DropRule.DEFAULT, true, false, true, 1, new HashMap<>(), Component.empty());

    public static final StructEndec<AccessoryStackSettings> ENDEC = StructEndecBuilder.of(
            Endec.forEnum(DropRule.class).optionalFieldOf("drop_rule", AccessoryStackSettings::dropRule, () -> DropRule.DEFAULT),
            Endec.BOOLEAN.optionalFieldOf("can_equip_from_dispenser", AccessoryStackSettings::canEquipFromDispenser, false),
            Endec.BOOLEAN.optionalFieldOf("can_equip_from_use", AccessoryStackSettings::canEquipFromUse, false),
            Endec.BOOLEAN.optionalFieldOf("use_stack_size", AccessoryStackSettings::useStackSize, false),
            Endec.INT.optionalFieldOf("size_override", AccessoryStackSettings::sizeOverride, 1),
            MinecraftEndecs.TEXT.mapOf().optionalFieldOf("slot_based_tooltips", AccessoryStackSettings::slotBasedTooltips, HashMap::new),
            MinecraftEndecs.TEXT.optionalFieldOf("extra_tooltip", AccessoryStackSettings::extraTooltip, CommonComponents.EMPTY),
            AccessoryStackSettings::new
    );

    public AccessoryStackSettings useStackSize(boolean value) {
        return new AccessoryStackSettings(this.dropRule, this.canEquipFromDispenser, this.canEquipFromUse, value, 1, this.slotBasedTooltips, this.extraTooltip);
    }

    public AccessoryStackSettings sizeOverride(int value) {
        return new AccessoryStackSettings(this.dropRule, this.canEquipFromDispenser, this.canEquipFromUse, false, value, this.slotBasedTooltips, this.extraTooltip);
    }

    public Builder builderFrom() {
        return new Builder(this.dropRule, this.canEquipFromDispenser, this.canEquipFromUse, this.useStackSize, this.sizeOverride, this.slotBasedTooltips, this.extraTooltip);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DropRule dropRule = DropRule.DEFAULT;
        private boolean canEquipFromDispenser;
        private boolean canEquipFromUse = false;
        private boolean useStackSize = false;
        private int sizeOverride = 1;
        private Map<String, Component> slotBasedTooltips = new HashMap<>();
        private Component extraTooltip = Component.empty();

        private Builder() {}

        public Builder(DropRule dropRule, boolean canEquipFromDispenser, boolean canEquipFromUse, boolean useStackSize, int sizeOverride, Map<String, Component> slotBasedTooltips, Component extraTooltip) {
            this.dropRule = dropRule;
            this.canEquipFromDispenser = canEquipFromDispenser;
            this.canEquipFromUse = canEquipFromUse;
            this.useStackSize = useStackSize;
            this.sizeOverride = sizeOverride;
            this.slotBasedTooltips = slotBasedTooltips;
            this.extraTooltip = extraTooltip;
        }

        public Builder dropRule(DropRule dropRule) {
            this.dropRule = dropRule;
            return this;
        }

        public Builder canEquipFromUse(boolean canEquipFromUse) {
            this.canEquipFromUse = canEquipFromUse;
            return this;
        }

        public Builder useStackSize(boolean useStackSize) {
            this.useStackSize = useStackSize;
            return this;
        }

        public Builder sizeOverride(int sizeOverride) {
            this.sizeOverride = sizeOverride;
            return this;
        }

        public Builder slotBasedTooltip(String slot, Component tooltip) {
            this.slotBasedTooltips.put(slot, tooltip);
            return this;
        }

        public Builder slotBasedTooltips(Map<String, Component> slotBasedTooltips) {
            this.slotBasedTooltips = new HashMap<>(slotBasedTooltips);
            return this;
        }

        public Builder extraTooltip(Component extraTooltip) {
            this.extraTooltip = extraTooltip;
            return this;
        }

        public AccessoryStackSettings build() {
            return new AccessoryStackSettings(
                    dropRule,
                    canEquipFromDispenser,
                    canEquipFromUse,
                    useStackSize,
                    sizeOverride,
                    slotBasedTooltips,
                    extraTooltip
            );
        }
    }
}
