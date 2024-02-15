package io.wispforest.accessories.fabric;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.impl.AccessoriesInternals;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class AccessoriesAccessImpl {

    public static Optional<AccessoriesCapability> getCapability(LivingEntity livingEntity){
        return Optional.ofNullable(AccessoriesFabric.CAPABILITY.find(livingEntity, null));
    }

    public static AccessoriesHolder getHolder(LivingEntity livingEntity){
        return livingEntity.getAttachedOrCreate(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE);
    }

    public static void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolder> modifier){
        var holder = getHolder(livingEntity);

        holder = modifier.apply(holder);

        livingEntity.setAttached(AccessoriesFabric.HOLDER_ATTACHMENT_TYPE, holder);
    }

    public static AccessoriesNetworkHandler getNetworkHandler(){
        return AccessoriesFabricNetworkHandler.INSTANCE;
    }

    public static AccessoriesInternals getInternal(){
        return AccessoriesFabricInternals.INSTANCE;
    }

    public static <T> Optional<Collection<Holder<T>>> getHolder(TagKey<T> tagKey){
        var map = ResourceConditionsImpl.LOADED_TAGS.get();

        var tags = map.get(tagKey.registry());

        if(tags == null) return Optional.empty();

        var converted = (Collection<Holder<T>>) tags.get(tagKey.location()).stream().map(holder -> (Holder<T>) holder).toList();

        return Optional.of(converted);
    }
}
