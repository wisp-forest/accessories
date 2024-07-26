package io.wispforest.cclayer.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(NetworkInstance.class)
public interface NetworkInstanceAccessor {
    @Invoker("<init>")
    static NetworkInstance createNetworkInstance(ResourceLocation channelName, Supplier<String> networkProtocolVersion, Predicate<String> clientAcceptedVersions, Predicate<String> serverAcceptedVersions) {
        throw new UnsupportedOperationException();
    }
}