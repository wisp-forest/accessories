package io.wispforest.accessories.mixin;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.pond.ContextedFileToIdConverter;
import io.wispforest.accessories.pond.ReplaceableJsonResourceReloadListener;
import io.wispforest.accessories.utils.JsonUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(SimpleJsonResourceReloadListener.class)
public abstract class SimpleJsonResourceReloadListenerMixin implements ReplaceableJsonResourceReloadListener {
    @Unique
    private boolean allowReplacementLoading = false;

    @Override
    public void accessories$allowReplacementLoading(boolean value) {
        this.allowReplacementLoading = value;
    }

    @Override
    public boolean accessories$allowReplacementLoading() {
        return this.allowReplacementLoading;
    }

    @WrapOperation(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/SimpleJsonResourceReloadListener;scanDirectory(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/FileToIdConverter;Lcom/mojang/serialization/DynamicOps;Lcom/mojang/serialization/Codec;Ljava/util/Map;)V"))
    private <T> void checkIfReplaceScan(ResourceManager resourceManager, FileToIdConverter converter, DynamicOps<JsonElement> ops, Codec<T> codec, Map<ResourceLocation, T> output, Operation<Void> original) {
        // TODO: REPLACE WITH INJECTION INTO ORIGINAL SCAN??

        if (this.allowReplacementLoading) {
            converter = ((ContextedFileToIdConverter) converter)
                    .setData(Accessories.of("allow_replacement_loading"), true);
        }

        original.call(resourceManager, converter, ops, codec, output);
    }

    @WrapOperation(method = "scanDirectory(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/FileToIdConverter;Lcom/mojang/serialization/DynamicOps;Lcom/mojang/serialization/Codec;Ljava/util/Map;)V",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/FileToIdConverter;listMatchingResources(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;"))
    private static Map<ResourceLocation, Resource> listReplacedResources(FileToIdConverter instance, ResourceManager resourceManager, Operation<Map<ResourceLocation, Resource>> original) {
        if (instance instanceof ContextedFileToIdConverter ctx && ctx.<Boolean>getDataOrDefault(Accessories.of("allow_replacement_loading"), false)) {
            return JsonUtils.scanDirectoryWithReplace(resourceManager, instance);
        }

        return original.call(instance, resourceManager);
    }
}
