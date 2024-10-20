package io.wispforest.accessories.api.components;

import io.wispforest.accessories.Accessories;
import io.wispforest.owo.serialization.CodecUtils;
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
            builder -> builder.persistent(CodecUtils.toCodec(AccessoryNestContainerContents.ENDEC, BASE_CTX))
                    .networkSynchronized(CodecUtils.toPacketCodec(AccessoryNestContainerContents.ENDEC))
    );

    public static final DataComponentType<AccessoryRenderOverrideComponent> RENDER_OVERRIDE = register(Accessories.of("render_override"),
            builder -> builder.persistent(CodecUtils.toCodec(AccessoryRenderOverrideComponent.ENDEC, BASE_CTX))
                    .networkSynchronized(CodecUtils.toPacketCodec(AccessoryRenderOverrideComponent.ENDEC))
    );

    public static final DataComponentType<AccessoryRenderTransformations> RENDER_TRANSFORMATIONS = register(Accessories.of("render_transformations"),
            builder -> builder.persistent(CodecUtils.toCodec(AccessoryRenderTransformations.ENDEC, BASE_CTX))
                    .networkSynchronized(CodecUtils.toPacketCodec(AccessoryRenderTransformations.ENDEC))
    );

    public static final DataComponentType<AccessorySlotValidationComponent> SLOT_VALIDATION = register(Accessories.of("slot_validation"),
            builder -> builder.persistent(CodecUtils.toCodec(AccessorySlotValidationComponent.ENDEC, BASE_CTX))
                    .networkSynchronized(CodecUtils.toPacketCodec(AccessorySlotValidationComponent.ENDEC))
    );

    public static final DataComponentType<AccessoryItemAttributeModifiers> ATTRIBUTES = register(Accessories.of("attributes"),
            builder -> builder.persistent(CodecUtils.toCodec(AccessoryItemAttributeModifiers.ENDEC, BASE_CTX))
                    .networkSynchronized(CodecUtils.toPacketCodec(AccessoryItemAttributeModifiers.ENDEC))
    );

    public static final DataComponentType<AccessoryStackSizeComponent> STACK_SIZE = register(Accessories.of("stack_size"),
            builder -> builder.persistent(CodecUtils.toCodec(AccessoryStackSizeComponent.ENDEC, BASE_CTX))
                    .networkSynchronized(CodecUtils.toPacketCodec(AccessoryStackSizeComponent.ENDEC))
    );

    private static <T> DataComponentType<T> register(ResourceLocation string, UnaryOperator<DataComponentType.Builder<T>> unaryOperator) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, string, ((DataComponentType.Builder)unaryOperator.apply(DataComponentType.builder())).build());
    }

    @ApiStatus.Internal
    public static void init() {}
}
