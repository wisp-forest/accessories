package io.wispforest.accessories.neoforge;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.impl.AccessoriesInternals;
import io.wispforest.accessories.networking.AccessoriesNetworkHandler;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class AccessoriesAccessImpl {

    public static Optional<AccessoriesCapability> getCapability(LivingEntity livingEntity){
        return Optional.ofNullable(AccessoriesForge.CAPABILITY.getCapability(livingEntity, null));
    }

    public static AccessoriesHolder getHolder(LivingEntity livingEntity){
        return livingEntity.getData(AccessoriesForge.HOLDER_ATTACHMENT_TYPE);
    }

    public static void modifyHolder(LivingEntity livingEntity, UnaryOperator<AccessoriesHolder> modifier){
        var holder = getHolder(livingEntity);

        holder = modifier.apply(holder);

        livingEntity.setData(AccessoriesForge.HOLDER_ATTACHMENT_TYPE, holder);
    }

    public static AccessoriesNetworkHandler getNetworkHandler(){
        return AccessoriesForgeNetworkHandler.INSTANCE;
    }

    public static AccessoriesInternals getInternal(){
        return AccessoriesForgeInternals.INSTANCE;
    }

    public static <T> Optional<Collection<Holder<T>>> getHolder(TagKey<T> tagKey){
        return currentContext.map(iContext -> iContext.getTag(tagKey));
    }

    private static Optional<ICondition.IContext> currentContext = Optional.empty();

    public static void setContext(@Nullable ICondition.IContext context){
        currentContext = Optional.ofNullable(context);
    }

}
