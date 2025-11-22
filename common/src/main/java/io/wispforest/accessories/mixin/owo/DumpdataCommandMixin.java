package io.wispforest.accessories.mixin.owo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.wispforest.owo.command.debug.DumpdataCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.RegistryOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DumpdataCommand.class)
public abstract class DumpdataCommandMixin {
    @WrapOperation(method = "executeItem", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;encodeStart(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"))
    private static DataResult wrapWithRegistries(Codec instance, DynamicOps dynamicOps, Object object, Operation<DataResult> original, @Local(argsOnly = true) CommandContext<CommandSourceStack> context) {
        return original.call(instance, RegistryOps.create(dynamicOps, context.getSource().registryAccess()), object);
    }
}
