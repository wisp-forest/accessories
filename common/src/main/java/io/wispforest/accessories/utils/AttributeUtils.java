package io.wispforest.accessories.utils;

import com.google.common.collect.Multimap;
import io.wispforest.accessories.endec.MinecraftEndecs;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeUtils {
    public static void removeAttributes(LivingEntity livingEntity, Multimap<Holder<Attribute>, AttributeModifier> attributes) {
        var attributeMap = livingEntity.getAttributes();

        for (var attributeHolder : attributes.keySet()) {
            var instance = attributeMap.getInstance(attributeHolder);

            attributes.get(attributeHolder).forEach(instance::removeModifier);
        }
    }

    public static void addTransientAttributeModifiers(LivingEntity livingEntity, Multimap<Holder<Attribute>, AttributeModifier> attributes) {
        var attributeMap = livingEntity.getAttributes();

        for (var attributeHolder : attributes.keySet()) {
            var instance = attributeMap.getInstance(attributeHolder);

            attributes.get(attributeHolder).forEach(instance::addTransientModifier);
        }
    }

    public static final StructEndec<AttributeModifier> ATTRIBUTE_MODIFIER_ENDEC = StructEndecBuilder.of(
            MinecraftEndecs.IDENTIFIER.fieldOf("id", AttributeModifier::id),
            Endec.DOUBLE.fieldOf("amount", AttributeModifier::amount),
            Endec.forEnum(AttributeModifier.Operation.class).fieldOf("operation", AttributeModifier::operation),
            AttributeModifier::new
    );
}
