package io.wispforest.accessories.api.components;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.accessories.endec.format.nbt.NbtDeserializer;
import io.wispforest.accessories.endec.format.nbt.NbtEndec;
import io.wispforest.accessories.endec.format.nbt.NbtSerializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttribute;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.KeyedEndec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class AccessoriesDataComponents {

    private static final Logger LOGGER = LogUtils.getLogger();

    /*
     * {
     *   "id": "",
     *   "count": 1,
     *   "components": {
     *     "accessories:nested_accessories": {
     *       //..
     *     },
     *     "accessories:render_override": {
     *       //..
     *     },
     *     "accessories:slot_validation": {
     *       //..
     *     },
     *     "accessories:attributes": {
     *       //..
     *     },
     *     "accessories:stack_size": {
     *       //..
     *     },
     *   }
     * }
     */

    //Accessories.of("nested_accessories")
    public static final KeyedEndec<AccessoryNestContainerContents> NESTED_ACCESSORIES = AccessoryNestContainerContents.ENDEC
            .keyed(Accessories.of("nested_accessories").toString(), AccessoryNestContainerContents.EMPTY);

    //Accessories.of("render_override")
    public static final KeyedEndec<AccessoryRenderOverrideComponent> RENDER_OVERRIDE = AccessoryRenderOverrideComponent.ENDEC
            .keyed(Accessories.of("render_override").toString(), AccessoryRenderOverrideComponent.DEFAULT);

    //Accessories.of("slot_validation")
    public static final KeyedEndec<AccessorySlotValidationComponent> SLOT_VALIDATION = AccessorySlotValidationComponent.ENDEC
            .keyed(Accessories.of("slot_validation").toString(), AccessorySlotValidationComponent.EMPTY);

    //Accessories.of("attributes")
    public static final KeyedEndec<AccessoryItemAttributeModifiers> ATTRIBUTES = AccessoryItemAttributeModifiers.ENDEC
            .keyed(Accessories.of("attributes").toString(), AccessoryItemAttributeModifiers.EMPTY);

    //Accessories.of("stack_size")
    public static final KeyedEndec<AccessoryStackSizeComponent> STACK_SIZE = AccessoryStackSizeComponent.ENDEC
            .keyed(Accessories.of("stack_size").toString(), AccessoryStackSizeComponent.DEFAULT);

    public static final Endec<Map<ResourceLocation, Tag>> COMPONENTS_ENDEC = NbtEndec.COMPOUND.xmap(compoundTag -> {
        var map = new HashMap<ResourceLocation, Tag>();

        for (var key : compoundTag.getAllKeys()) {
            var location = ResourceLocation.tryParse(key);

            if (location != null) map.put(location, compoundTag.get(key));
        }

        return map;
    }, map -> {
        var compound = new CompoundTag();

        map.forEach((location, tag) -> compound.put(location.toString(), tag));

        return compound;
    });

    public static final KeyedEndec<Map<ResourceLocation, Tag>> COMPONENTS_KEY = COMPONENTS_ENDEC.keyed("components", HashMap::new);

    @ApiStatus.Internal
    public static void init() {}

    public static <T> boolean has(KeyedEndec<T> keyedEndec, ItemStack stack) {
        var tag = stack.getTag();

        if (tag != null) {
            var location = ResourceLocation.tryParse(keyedEndec.key());

            if (location != null) return new NbtMapCarrier(tag).get(COMPONENTS_KEY).containsKey(location);
        }

        return false;
    }

    public static <T> T readOrDefault(KeyedEndec<T> keyedEndec, ItemStack stack) {
        var tag = stack.getTag();

        if (tag != null) {
            var location = ResourceLocation.tryParse(keyedEndec.key());

            if (location != null) {
                var carrier = new NbtMapCarrier(tag);

                var components = carrier
                        .get(COMPONENTS_KEY);

                var data = components.get(location);

                if (data != null) {
                    try {
                        return keyedEndec.endec().decodeFully(SerializationContext.attributes(new StackAttribute(stack.copy())), NbtDeserializer::of, data);
                    } catch (Exception e) {
                        LOGGER.warn("Unable to read the given component from the given stack: [Key: {}]", keyedEndec.key(), e);

                        components.remove(location);

                        carrier.put(SerializationContext.attributes(new StackAttribute(stack.copy())), COMPONENTS_KEY, components);
                    }
                }
            }
        }

        return keyedEndec.defaultValue();
    }

    public static <T> void write(KeyedEndec<T> keyedEndec, ItemStack stack, T data) {
        var carrier = new NbtMapCarrier(stack.getOrCreateTag());

        var components = carrier.get(COMPONENTS_KEY);

        var location = ResourceLocation.tryParse(keyedEndec.key());

        if (location != null) {
            try {
                components.put(location, keyedEndec.endec().encodeFully(NbtSerializer::of, data));
            } catch (Exception e) {
                LOGGER.warn("Unable to write the given component from the given stack: [Key: {}]", keyedEndec.key(), e);
            }
        }

        carrier.put(COMPONENTS_KEY, components);
    }

    public static <T> void update(KeyedEndec<T> keyedEndec, ItemStack stack, UnaryOperator<T> operator) {
        write(keyedEndec, stack, operator.apply(readOrDefault(keyedEndec, stack)));
    }

    //--

    public record StackAttribute(ItemStack stack) implements SerializationAttribute.Instance {

        public static final SerializationAttribute.WithValue<StackAttribute> INSTANCE = SerializationAttribute.withValue("accessories:stack");

        @Override public SerializationAttribute attribute() { return INSTANCE; }
        @Override public Object value() { return this; }
    }
}
