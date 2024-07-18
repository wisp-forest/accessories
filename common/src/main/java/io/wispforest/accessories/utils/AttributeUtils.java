package io.wispforest.accessories.utils;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.endec.MinecraftEndecs;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.slf4j.Logger;

public class AttributeUtils {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void addTransientAttributeModifiers(LivingEntity livingEntity, AccessoryAttributeBuilder attributes) {
        if(attributes.isEmpty()) return;

        var attributeMap = livingEntity.getAttributes();
        var capability = livingEntity.accessoriesCapability();

        var containers = capability.getContainers();

        attributes.getSlotModifiers().asMap().forEach((s, modifiers) -> {
            var container = containers.get(s);

            if(container == null) return;

            modifiers.stream()
                    .filter(modifier -> !container.hasModifier(modifier.id()))
                    .forEach(container::addTransientModifier);
        });

        attributes.getAttributeModifiers(true).asMap().forEach((holder, modifiers) -> {
            var instance = attributeMap.getInstance(holder);

            if(instance == null) return;

            modifiers.stream()
                    .filter(modifier -> !instance.hasModifier(modifier.id()))
                    .forEach(instance::addTransientModifier);
        });
    }

    public static void removeTransientAttributeModifiers(LivingEntity livingEntity, AccessoryAttributeBuilder attributes) {
        if(attributes.isEmpty()) return;

        var attributeMap = livingEntity.getAttributes();
        var capability = livingEntity.accessoriesCapability();

        var containers = capability.getContainers();

        attributes.getSlotModifiers().asMap().forEach((s, modifiers) -> {
            var container = containers.get(s);

            if(container == null) return;

            modifiers.stream()
                    .map(AttributeModifier::id)
                    .forEach(container::removeModifier);
        });

        attributes.getAttributeModifiers(true).asMap().forEach((holder, modifiers) -> {
            var instance = attributeMap.getInstance(holder);

            if(instance == null) return;

            modifiers.stream()
                    .map(AttributeModifier::id)
                    .forEach(instance::removeModifier);
        });
    }

    public static final StructEndec<AttributeModifier> ATTRIBUTE_MODIFIER_ENDEC = StructEndecBuilder.of(
            MinecraftEndecs.IDENTIFIER.fieldOf("id", AttributeModifier::id),
            Endec.DOUBLE.fieldOf("amount", AttributeModifier::amount),
            MinecraftEndecs.forEnumStringRepresentable(AttributeModifier.Operation.class).fieldOf("operation", AttributeModifier::operation),
            AttributeModifier::new
    );
}
