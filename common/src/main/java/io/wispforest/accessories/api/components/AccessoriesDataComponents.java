package io.wispforest.accessories.api.components;

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

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class AccessoriesDataComponents {

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
        Map<ResourceLocation, Tag> map = new HashMap<>();

        for (String key : compoundTag.getAllKeys()) {
            var location = ResourceLocation.tryParse(key);

            if (location == null) continue;

            map.put(location, compoundTag.get(key));
        }

        return map;
    }, map -> {
        var compound = new CompoundTag();

        map.forEach((resourceLocation, tag) -> compound.put(resourceLocation.toString(), tag));

        return compound;
    });

    public static final KeyedEndec<Map<ResourceLocation, Tag>> COMPONENTS_KEY = COMPONENTS_ENDEC.keyed("components", HashMap::new);

    @ApiStatus.Internal
    public static void init() {
    }

    public static <T> boolean has(KeyedEndec<T> keyedEndec, ItemStack stack) {
        var tag = stack.getTag();

        if (tag == null) return false;

        var components = new NbtMapCarrier(tag).get(COMPONENTS_KEY);

        var location = ResourceLocation.tryParse(keyedEndec.key());

        return location != null && components.containsKey(location);
    }

    public static <T> T readOrDefault(KeyedEndec<T> keyedEndec, ItemStack stack) {
        var tag = stack.getTag();

        if (tag == null) return keyedEndec.defaultValue();

        var components = new NbtMapCarrier(tag).get(COMPONENTS_KEY);

        var location = ResourceLocation.tryParse(keyedEndec.key());

        if (location == null) return keyedEndec.defaultValue();

        var data = components.get(location);

        if (data == null) return keyedEndec.defaultValue();

        return keyedEndec.endec().decodeFully(SerializationContext.attributes(new StackAttribute(stack.copy())), NbtDeserializer::of, data);
    }

    public static <T> void write(KeyedEndec<T> keyedEndec, ItemStack stack, T data) {
        var carrier = new NbtMapCarrier(stack.getOrCreateTag());

        var components = carrier.get(COMPONENTS_KEY);

        var location = ResourceLocation.tryParse(keyedEndec.key());

        if (location != null) {
            components.put(location, keyedEndec.endec().encodeFully(NbtSerializer::of, data));
        }

        carrier.put(COMPONENTS_KEY, components);
    }

    public static <T> void update(KeyedEndec<T> keyedEndec, ItemStack stack, UnaryOperator<T> operator) {
        var value = readOrDefault(keyedEndec, stack);

        value = operator.apply(value);

        write(keyedEndec, stack, value);
    }

    //--

    public record StackAttribute(ItemStack stack) implements SerializationAttribute.Instance {

        public static final SerializationAttribute.WithValue<StackAttribute> INSTANCE = SerializationAttribute.withValue("accessories:stack");

        @Override
        public SerializationAttribute attribute() {
            return INSTANCE;
        }

        @Override
        public Object value() {
            return this;
        }
    }
}
