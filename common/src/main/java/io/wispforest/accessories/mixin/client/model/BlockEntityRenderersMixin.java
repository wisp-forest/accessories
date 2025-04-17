package io.wispforest.accessories.mixin.client.model;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.accessories.pond.ModelPartLoadingHelper;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockEntityRenderers.class)
public abstract class BlockEntityRenderersMixin {

    @WrapOperation(method = "method_32145", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRendererProvider;create(Lnet/minecraft/client/renderer/blockentity/BlockEntityRendererProvider$Context;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;"))
    private static <T extends BlockEntity> BlockEntityRenderer<T> accessories$attemptToSaveRoot(BlockEntityRendererProvider<T> instance, BlockEntityRendererProvider.Context context, Operation<BlockEntityRenderer<T>> original) {
        var renderer = original.call(instance, context);

        // Attempts to clear queue in case issues with saving root model part goes wrong
        ((ModelPartLoadingHelper) context.getModelSet()).accessories$clearQueue();

        return renderer;
    }
}
