package io.wispforest.accessories.fabric.mixin;

import io.wispforest.accessories.fabric.AccessoriesInternalsImpl;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.tags.TagManager;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

//Copy of Mixin found within FabricAPI found here https://github.com/FabricMC/fabric/blob/41bc64cd617f03d49ecc4a4f7788cb65d465415c/fabric-resource-conditions-api-v1/src/main/java/net/fabricmc/fabric/mixin/resource/conditions/TagManagerLoaderMixin.java
@Mixin(TagManager.class)
public abstract class TagManagerMixin {
    @Shadow
    private List<TagManager.LoadResult<?>> results;

    // lambda body inside thenAcceptAsync, in the reload method
    @Dynamic
    @Inject(method = "method_40098(Ljava/util/List;Ljava/lang/Void;)V", at = @At("RETURN"))
    private void hookApply(List<?> list, Void void_, CallbackInfo ci) {
        AccessoriesInternalsImpl.setTags(results);
    }
}
