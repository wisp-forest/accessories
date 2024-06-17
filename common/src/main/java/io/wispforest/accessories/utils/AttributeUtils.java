package io.wispforest.accessories.utils;

import com.google.common.collect.Multimap;
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
}
