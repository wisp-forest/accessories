package io.wispforest.accessories.mixin.client.model;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.accessories.pond.ModelPartLoadingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderers.class)
public abstract class EntityRenderersMixin {

    @WrapOperation(method = {
        "method_32174",
        "lambda$createEntityRenderers$2" // Dev Neoforge
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRendererProvider;create(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;)Lnet/minecraft/client/renderer/entity/EntityRenderer;"),
        expect = 1, require = 1, allow = 1)
    private static <T extends Entity> EntityRenderer<T> accessories$attemptToSaveRoot1(EntityRendererProvider<T> instance, EntityRendererProvider.Context context, Operation<EntityRenderer<T>> original) {
        var renderer = original.call(instance, context);

        // Attempts to clear queue in case issues with saving root model part goes wrong
        ((ModelPartLoadingHelper) context.getModelSet()).accessories$clearQueue();

        return renderer;
    }

    @WrapOperation(method = {
        "method_32175",
        "lambda$createPlayerRenderers$3" // Dev Neoforge
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRendererProvider;create(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;)Lnet/minecraft/client/renderer/entity/EntityRenderer;"),
        expect = 1, require = 1, allow = 1)
    private static <T extends Entity> EntityRenderer<T> accessories$attemptToSaveRoot2(EntityRendererProvider<T> instance, EntityRendererProvider.Context context, Operation<EntityRenderer<T>> original) {
        var renderer = original.call(instance, context);

        // Attempts to clear queue in case issues with saving root model part goes wrong
        ((ModelPartLoadingHelper) context.getModelSet()).accessories$clearQueue();

        return renderer;
    }
}
