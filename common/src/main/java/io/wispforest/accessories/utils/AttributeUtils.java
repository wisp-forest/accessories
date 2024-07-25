package io.wispforest.accessories.utils;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.endec.MinecraftEndecs;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.StructEndecBuilder;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
                    .filter(modifier -> !container.hasModifier(modifier.getId()))
                    .forEach(container::addTransientModifier);
        });

        attributes.getAttributeModifiers(true).asMap().forEach((holder, modifiers) -> {
            var instance = attributeMap.getInstance(holder);

            if(instance == null) return;

            modifiers.stream()
                    .filter(modifier -> !instance.hasModifier(modifier))
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
                    .map(AttributeModifier::getId)
                    .forEach(container::removeModifier);
        });

        attributes.getAttributeModifiers(true).asMap().forEach((holder, modifiers) -> {
            var instance = attributeMap.getInstance(holder);

            if(instance == null) return;

            modifiers.stream()
                    .map(AttributeModifier::getId)
                    .forEach(instance::removeModifier);
        });
    }

    public static final StructEndec<AttributeModifier> ATTRIBUTE_MODIFIER_ENDEC = StructEndecBuilder.of(
            BuiltInEndecs.UUID.fieldOf("uuid", AttributeModifier::getId),
            Endec.STRING.fieldOf("name", AttributeModifier::getName),
            Endec.DOUBLE.fieldOf("amount", AttributeModifier::getAmount),
            Endec.forEnum(AttributeModifier.Operation.class).fieldOf("operation", AttributeModifier::getOperation),
            AttributeModifier::new
    );

    public static ResourceLocation getLocation(String name) {
        var location = ResourceLocation.tryParse(name);

        if(location == null) location = Accessories.of(name);

        return location;
    }

    public static Pair<String, UUID> getModifierData(ResourceLocation location) {
        var name = location.toString();
        var id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));

        return Pair.of(name, id);
    }
}
