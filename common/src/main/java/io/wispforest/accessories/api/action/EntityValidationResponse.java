package io.wispforest.accessories.api.action;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class EntityValidationResponse extends ActionResponseBase {
    private final List<EntityType<?>> validEntityTypes;
    private final EntityType<?> targetEntityType;

    public EntityValidationResponse(List<EntityType<?>> validEntityTypes, EntityType<?> targetEntityType) {
        super(validEntityTypes.contains(targetEntityType));

        this.validEntityTypes = validEntityTypes;
        this.targetEntityType = targetEntityType;
    }

    @Override
    public void gatherReason(Consumer<Component> messageAdditionCallback, Item.TooltipContext ctx, TooltipFlag type) {
        var baseMessage = canPerformAction
            ? Component.literal("Such creature is valid for this Accessory.")
            : Component.literal("Such creature is not valid for this Accessory.");

        if (type.isAdvanced() || type.hasShiftDown()) {
            baseMessage.append(
                Component.literal(" The Valid Entities are: ")
                    .append(ComponentUtils.formatList(validEntityTypes, Component.literal(","), EntityType::getDescription))
            );
        }

        messageAdditionCallback.accept(baseMessage);
    }

    public List<EntityType<?>> validEntityTypes() {
        return validEntityTypes;
    }

    public EntityType<?> targetEntityType() {
        return targetEntityType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof EntityValidationResponse that)) return false;

        return Objects.equals(this.validEntityTypes, that.validEntityTypes) &&
            Objects.equals(this.targetEntityType, that.targetEntityType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validEntityTypes, targetEntityType);
    }

    @Override
    public String toString() {
        return "EntityValidation[" +
            "validEntityTypes=" + validEntityTypes + ", " +
            "targetEntityType=" + targetEntityType + ", " +
            "canPerformAction=" + canPerformAction + "]";
    }
}
