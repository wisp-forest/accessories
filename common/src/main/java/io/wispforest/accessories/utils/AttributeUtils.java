package io.wispforest.accessories.utils;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;

public class AttributeUtils {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void addTransientAttributeModifiers(LivingEntity livingEntity, AccessoryAttributeBuilder attributes) {
        if(attributes.isEmpty()) return;

        var attributeMap = livingEntity.getAttributes();
        var capability = livingEntity.accessoriesCapability();

        if (capability == null) return;

        var containers = capability.getContainers();

        for (var entry : attributes.getSlotModifiers().asMap().entrySet()) {
            var container = containers.get(entry.getKey());

            if (container == null) continue;

            for (var modifier :  entry.getValue()) {
                if (!container.hasModifier(modifier.id())) container.addTransientModifier(modifier);
            }
        }

        for (var entry : attributes.getAttributeModifiers(true).asMap().entrySet()) {
            var instance = attributeMap.getInstance(entry.getKey());

            if (instance == null) continue;

            for (var modifier : entry.getValue()) {
                if (!instance.hasModifier(modifier.id())) instance.addTransientModifier(modifier);
            }
        }
    }

    public static void removeTransientAttributeModifiers(LivingEntity livingEntity, AccessoryAttributeBuilder attributes) {
        if(attributes.isEmpty()) return;

        var attributeMap = livingEntity.getAttributes();
        var capability = livingEntity.accessoriesCapability();

        var containers = capability.getContainers();

        for (var entry : attributes.getSlotModifiers().asMap().entrySet()) {
            var container = containers.get(entry.getKey());

            if (container == null) continue;

            for (var attributeModifier : entry.getValue()) container.removeModifier(attributeModifier.id());
        }

        for (var entry : attributes.getAttributeModifiers(true).asMap().entrySet()) {
            var instance = attributeMap.getInstance(entry.getKey());

            if (instance == null) continue;

            for (var attributeModifier : entry.getValue()) instance.removeModifier(attributeModifier.id());
        }
    }

    public static final StructEndec<AttributeModifier> ATTRIBUTE_MODIFIER_ENDEC = StructEndecBuilder.of(
            MinecraftEndecs.IDENTIFIER.fieldOf("id", AttributeModifier::id),
            Endec.DOUBLE.fieldOf("amount", AttributeModifier::amount),
            EndecUtils.forEnumStringRepresentable(AttributeModifier.Operation.class).fieldOf("operation", AttributeModifier::operation),
            AttributeModifier::new
    );
}
