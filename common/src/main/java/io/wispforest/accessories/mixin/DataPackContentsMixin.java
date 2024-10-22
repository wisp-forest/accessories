package io.wispforest.accessories.mixin;

import io.wispforest.accessories.data.EntitySlotLoader;
import net.minecraft.server.ReloadableServerResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Copied 1:1 from https://github.com/FabricMC/fabric/blob/625ef353552d973b6ed26c720dfa892e064afeef/fabric-resource-conditions-api-v1/src/main/java/net/fabricmc/fabric/mixin/resource/conditions/DataPackContentsMixin.java#L41
@Mixin(ReloadableServerResources.class)
public class DataPackContentsMixin {
    @Inject(method = "updateStaticRegistryTags", at = @At("TAIL"))
    private void removeLoadedTags(CallbackInfo ci) {
        EntitySlotLoader.INSTANCE.buildEntryMap();
    }
}
