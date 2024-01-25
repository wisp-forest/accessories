package io.wispforest.accessories.fabric;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.*;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AccessoriesAPIImpl extends AccessoriesAPI {

    public static final AccessoriesAPIImpl INSTANCE = new AccessoriesAPIImpl();

    public final EntityApiLookup<AccessoriesCapability, Void> CAPABILITY = EntityApiLookup.get(Accessories.of("capability"), AccessoriesCapability.class, Void.class);

    private final Map<Item, Accessory> REGISTER = new HashMap<>();

    private final Map<ResourceLocation, SlotBasedPredicate> PREDICATE_REGISTRY = new HashMap<>();

    //--

    @Override
    public Optional<AccessoriesCapability> getCapability(LivingEntity livingEntity) {
        return Optional.ofNullable(CAPABILITY.find(livingEntity, null));
    }

    @Override
    public void registerAccessory(Item item, Accessory accessory) {
        this.REGISTER.put(item, accessory);
    }

    @Override
    public Optional<Accessory> getAccessory(Item item) {
        return Optional.ofNullable(this.REGISTER.get(item));
    }

    //--

    @Override
    public Optional<SlotBasedPredicate> getPredicate(ResourceLocation location) {
        return Optional.ofNullable(PREDICATE_REGISTRY.get(location));
    }

    @Override
    public void registerPredicate(ResourceLocation location, SlotBasedPredicate predicate) {
        if(PREDICATE_REGISTRY.containsKey(location)) {
            LOGGER.warn("[AccessoriesAPI]: A SlotBasedPredicate attempted to be registered but a duplicate entry existed already! [Id: " + location + "]");

            return;
        }

        PREDICATE_REGISTRY.put(location, predicate);
    }
}
