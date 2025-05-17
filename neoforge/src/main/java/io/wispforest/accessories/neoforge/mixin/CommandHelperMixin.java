package io.wispforest.accessories.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import io.wispforest.accessories.commands.api.core.ContextAwareLiteralArgumentBuilder;
import net.neoforged.neoforge.server.command.CommandHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CommandHelper.class)
public abstract class CommandHelperMixin {
    @WrapOperation(method = "toResult", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;literal(Ljava/lang/String;)Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;"))
    private static <S> LiteralArgumentBuilder<S> accessories$createContextAwareBuilder(String name, Operation<LiteralArgumentBuilder<S>> original, @Local(argsOnly = true) CommandNode<S> sourceNode) {
        return ContextAwareLiteralArgumentBuilder.builderFromNode(sourceNode).orElseGet(() -> original.call(name));
    }
}
