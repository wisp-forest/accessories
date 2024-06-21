package io.wispforest.accessories.api.components;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoryNest;
import io.wispforest.accessories.endec.CodecUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class AccessoriesDataComponents {

    public static final DataComponentType<AccessoryNestContainerContents> NESTED_ACCESSORIES = register(Accessories.of("nested_accessories"),
            builder -> builder.persistent(CodecUtils.ofEndec(AccessoryNestContainerContents.ENDEC))
                    .networkSynchronized(CodecUtils.packetCodec(AccessoryNestContainerContents.ENDEC))
                    .cacheEncoding()
    );

    public static final DataComponentType<AccessoryRenderOverrideComponent> RENDER_OVERRIDE = register(Accessories.of("render_override"),
            builder -> builder.persistent(CodecUtils.ofEndec(AccessoryRenderOverrideComponent.ENDEC))
                    .networkSynchronized(CodecUtils.packetCodec(AccessoryRenderOverrideComponent.ENDEC))
                    .cacheEncoding()
    );

    public static final DataComponentType<AccessorySlotValidationComponent> SLOT_VALIDATION = register(Accessories.of("slot_validation"),
            builder -> builder.persistent(CodecUtils.ofEndec(AccessorySlotValidationComponent.ENDEC))
                    .networkSynchronized(CodecUtils.packetCodec(AccessorySlotValidationComponent.ENDEC))
                    .cacheEncoding()
    );

    public static final DataComponentType<AccessoryItemAttributeModifiers> ATTRIBUTES = register(Accessories.of("attributes"),
            builder -> builder.persistent(CodecUtils.ofEndec(AccessoryItemAttributeModifiers.ENDEC))
                    .networkSynchronized(CodecUtils.packetCodec(AccessoryItemAttributeModifiers.ENDEC))
                    .cacheEncoding()
    );

    private static <T> DataComponentType<T> register(ResourceLocation string, UnaryOperator<DataComponentType.Builder<T>> unaryOperator) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, string, ((DataComponentType.Builder)unaryOperator.apply(DataComponentType.builder())).build());
    }

    public static void init() {}

    public static void adjustDefaultComponents(BiConsumer<Predicate<Item>, Consumer<AdditionCallback>> modifyCallback) {
//        modifyCallback.accept(item -> item instanceof AccessoryNest, (addConsumer) -> {
//            addConsumer.addTo(AccessoriesDataComponents.NESTED_ACCESSORIES, AccessoryNestContainerContents.EMPTY);
//        });
//
//        modifyCallback.accept(item -> true, (addConsumer) -> {
//            addConsumer.addTo(AccessoriesDataComponents.RENDER_OVERRIDE, AccessoryRenderOverrideComponent.DEFAULT);
//            addConsumer.addTo(AccessoriesDataComponents.SLOT_VALIDATION, AccessorySlotValidationComponent.EMPTY);
//            addConsumer.addTo(AccessoriesDataComponents.ATTRIBUTES, AccessoryItemAttributeModifiers.EMPTY);
//        });
    }

    public interface AdditionCallback {
        <T> void addTo(DataComponentType<T> type, T defaultValue);
    }
}
