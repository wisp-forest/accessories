package io.wispforest.accessories.api.components;

import io.wispforest.accessories.Accessories;
import io.wispforest.endec.SerializationAttributes;
import io.wispforest.endec.SerializationContext;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.UnaryOperator;

public class AccessoriesDataComponents {

    private static final SerializationContext BASE_CTX = SerializationContext.attributes(SerializationAttributes.HUMAN_READABLE);

    public static final DataComponentType<AccessoryNestContainerContents> NESTED_ACCESSORIES = register(Accessories.of("nested_accessories"),
            builder -> builder.endec(AccessoryNestContainerContents.ENDEC, BASE_CTX)
    );

    public static final DataComponentType<AccessorySlotValidationComponent> SLOT_VALIDATION = register(Accessories.of("slot_validation"),
            builder -> builder.endec(AccessorySlotValidationComponent.ENDEC, BASE_CTX)
    );

    public static final DataComponentType<AccessoryItemAttributeModifiers> ATTRIBUTES = register(Accessories.of("attributes"),
            builder -> builder.endec(AccessoryItemAttributeModifiers.ENDEC, BASE_CTX)
    );

    public static final DataComponentType<AccessoryStackSettings> STACK_SETTINGS = register(Accessories.of("stack_settings"),
            builder -> builder.endec(AccessoryStackSettings.ENDEC, BASE_CTX)
    );

    public static final DataComponentType<AccessoryCustomRendererComponent> CUSTOM_RENDERER = register(Accessories.of("custom_renderer"),
            builder -> builder.endec(AccessoryCustomRendererComponent.ENDEC, BASE_CTX)
    );

    public static final DataComponentType<AccessoryMobEffectsComponent> MOB_EFFECTS = register(Accessories.of("mob_effects"),
            builder -> builder.endec(AccessoryMobEffectsComponent.ENDEC, BASE_CTX)
    );

    private static <T> DataComponentType<T> register(ResourceLocation string, UnaryOperator<DataComponentType.Builder<T>> unaryOperator) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, string, ((DataComponentType.Builder)unaryOperator.apply(DataComponentType.builder())).build());
    }

    @ApiStatus.Internal
    public static void init() {}
}
