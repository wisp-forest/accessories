package io.wispforest.accessories.api.data.providers.slot;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.data.providers.BaseDataProvider;
import io.wispforest.accessories.api.slot.ExtraSlotTypeProperties;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class SlotBuilder {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final boolean replace;

    private final String name;
    private final boolean uniqueSlot;

    private ResourceLocation icon = null;
    private Integer order = null;

    private Integer baseAmount = null;
    private Integer offsetAmount = 0;

    private final Set<ResourceLocation> validators = new HashSet<>();
    private DropRule dropRule = null;

    public SlotBuilder(String name, boolean replace) {
        this.replace = replace;

        this.name = name;

        this.uniqueSlot = UniqueSlotHandling.isUniqueSlot(name);
    }

    public SlotBuilder icon(ResourceLocation value) {
        this.icon = value;
        return this;
    }

    public SlotBuilder order(Integer value) {
        this.order = value;
        return this;
    }

    public SlotBuilder amount(int value) {
        if (this.uniqueSlot && !ExtraSlotTypeProperties.getProperty(this.name, false).allowResizing()) {
            LOGGER.error("[SlotDataProvider] An attempt to adjust the amount for a given Unique slot even though resizing is not allow! [Slot: {}]", this.name);

            return this;
        }

        this.baseAmount = value;

        return this;
    }

    public SlotBuilder addAmount(int value) {
        if (this.uniqueSlot && !ExtraSlotTypeProperties.getProperty(this.name, false).allowResizing()) {
            LOGGER.error("[SlotDataProvider] An attempt to adjust the amount for a given Unique slot even though resizing is not allow! [Slot: {}]", this.name);

            return this;
        }

        this.offsetAmount += value;

        return this;
    }

    public SlotBuilder subtractAmount(int value) {
        if (this.uniqueSlot && !ExtraSlotTypeProperties.getProperty(this.name, false).allowResizing()) {
            LOGGER.error("[SlotDataProvider] An attempt to adjust the amount for a given Unique slot even though resizing is not allow! [Slot: {}]", this.name);

            return this;
        }

        this.offsetAmount -= value;

        return this;
    }

    public SlotBuilder validator(ResourceLocation validator) {
        if (this.uniqueSlot && !ExtraSlotTypeProperties.getProperty(this.name, false).strictMode()) {
            LOGGER.error("[SlotDataProvider] An attempt to adjust the validators for a given Unique slot even though strict mode is enabled! [Slot: {}]", this.name);

            return this;
        }

        this.validators.add(validator);
        return this;
    }

    public SlotBuilder dropRule(DropRule value) {
        this.dropRule = value;
        return this;
    }

    public RawSlotType create() {
        var defaultedBaseAmount = Optional.ofNullable(this.baseAmount).map(i -> Math.max(i, 0));

        if (this.offsetAmount != 0) {
            defaultedBaseAmount.or(() -> Optional.of(1)).map(integer -> integer + this.offsetAmount);
        }

        return new RawSlotType(
                this.name,
                this.replace ? Optional.of(true) : Optional.empty(),
                Optional.ofNullable(this.icon),
                Optional.ofNullable(this.order),
                defaultedBaseAmount,
                this.validators.isEmpty() ? Optional.of(this.validators) : Optional.empty(),
                Optional.ofNullable(this.dropRule)
        );
    }
}
