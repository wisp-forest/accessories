package io.wispforest.accessories.fabric.mixin;

import io.wispforest.accessories.fabric.AccessoriesInternalsImpl;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

// Copied 1:1 from https://github.com/FabricMC/fabric/blob/625ef353552d973b6ed26c720dfa892e064afeef/fabric-resource-conditions-api-v1/src/main/java/net/fabricmc/fabric/mixin/resource/conditions/DataPackContentsMixin.java#L41
@Mixin(ReloadableServerResources.class)
public class DataPackContentsMixin {
//    @Inject(
//            method = "loadResources",
//            at = @At("HEAD")
//    )
//    private static void hookReload(ResourceManager resourceManager, LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess, List<Registry.PendingTags<?>> list, FeatureFlagSet featureFlagSet, Commands.CommandSelection commandSelection, int i, Executor executor, Executor executor2, CallbackInfoReturnable<CompletableFuture<ReloadableServerResources>> cir) {
//        AccessoriesInternalsImpl.setTags(list);
//    }
//
//    @Inject(
//            method = "updateStaticRegistryTags",
//            at = @At("TAIL")
//    )
//    private void removeLoadedTags(CallbackInfo ci) {
//        Objects.requireNonNull(AccessoriesInternalsImpl.LOADED_TAGS.getAndSet(null), "loaded tags not reset");
//    }
}
