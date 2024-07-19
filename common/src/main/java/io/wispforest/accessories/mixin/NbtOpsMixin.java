package io.wispforest.accessories.mixin;

import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NbtOps.class)
public class NbtOpsMixin {
    @Inject(method = "convertTo(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/nbt/Tag;)Ljava/lang/Object;", at = @At(value = "HEAD"), cancellable = true)
    private <U> void thisIsAStupidPatchToFixAOverallBigProblemWithCodecThatShouldBeSolvedHowEndecHasDoneSuch(DynamicOps<U> ops, Tag tag, CallbackInfoReturnable<U> cir){
        if(ops.empty() instanceof Tag) cir.setReturnValue((U) tag);
    }
}
