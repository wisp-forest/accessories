package io.wispforest.accessories.api.menu;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class AccessoriesSlotGenerator {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final AccessoriesCapability capability;

    private final Consumer<Slot> slotConsumer;

    private final List<SlotType> slotTypes;

    private int horizontalPadding = 0, verticalPadding = 0;
    private int startX, startY;

    private AccessoriesSlotGenerator(Consumer<Slot> slotConsumer, int startX, int startY, List<SlotType> slotTypes, AccessoriesCapability capability) {
        this.slotConsumer = slotConsumer;

        this.startX = startX;
        this.startY = startY;

        this.slotTypes = slotTypes;

        this.capability = capability;
    }

    @Nullable
    public AccessoriesSlotGenerator of(Consumer<Slot> slotConsumer, int startX, int startY, LivingEntity livingEntity, SlotType... slotTypes) {
        var capability = livingEntity.accessoriesCapability().orElse(null);

        if(capability == null) return null;

        var validEntitySlotTypes = EntitySlotLoader.getEntitySlots(livingEntity).values();

        var validSlotTypes = new ArrayList<SlotType>();

        for (var slotType : slotTypes) {
            if(!validEntitySlotTypes.contains(slotType)){
                LOGGER.error("Unable to create Accessory Slot due to the given LivingEntity not having the given SlotType bound to such! [EntityType: " + livingEntity.getType() + ", SlotType: " + slotType.name() + "]");

                continue;
            }

            validSlotTypes.add(slotType);
        }

        return new AccessoriesSlotGenerator(slotConsumer, startX, startY, validSlotTypes, capability);
    }

    public AccessoriesSlotGenerator padding(int value) {
        this.horizontalPadding = value;
        this.verticalPadding = value;

        return this;
    }

    public AccessoriesSlotGenerator horizontalPadding(int value) {
        this.horizontalPadding = value;

        return this;
    }

    public AccessoriesSlotGenerator verticalPadding(int value) {
        this.verticalPadding = value;

        return this;
    }

    public void row() {
        var containers = capability.getContainers();

        int xOffset = this.horizontalPadding / 2;
        int yOffset = this.verticalPadding / 2;

        for (var slotType : slotTypes) {
            var container = capability.tryAndGetContainer(slotType).orElse(null);

            if(container == null){
                LOGGER.error("Unable to locate the given container for the passed slotType. [Type:" + slotType.name() + "]");

                continue;
            }

            for (int i = 0; i < container.getSize(); i++) {
                slotConsumer.accept(new AccessoriesBasedSlot(container, container.getAccessories(), i, this.startX + xOffset, this.startY + yOffset));

                xOffset += (this.horizontalPadding / 2) + 18;
            }
        }
    }

    public void column() {
        var containers = capability.getContainers();

        int xOffset = this.horizontalPadding / 2;
        int yOffset = this.verticalPadding / 2;

        for (var slotType : slotTypes) {
            var container = capability.tryAndGetContainer(slotType).orElse(null);

            if(container == null){
                LOGGER.error("Unable to locate the given container for the passed slotType. [Type:" + slotType.name() + "]");

                continue;
            }

            for (int i = 0; i < container.getSize(); i++) {
                slotConsumer.accept(new AccessoriesBasedSlot(container, container.getAccessories(), i, this.startX + xOffset, this.startY + yOffset));

                yOffset += (this.verticalPadding / 2) + 18;
            }
        }
    }
}
