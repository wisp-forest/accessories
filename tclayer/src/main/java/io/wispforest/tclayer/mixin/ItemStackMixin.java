package io.wispforest.tclayer.mixin;

import dev.emi.trinkets.api.TrinketConstants;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.Accessories;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.stream.Stream;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract Item getItem();

    @Shadow public abstract Stream<TagKey<Item>> getTags();

    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At(value = "HEAD"), cancellable = true)
    private void redirectTags(TagKey<Item> tag, CallbackInfoReturnable<Boolean> cir){
        var namespace = tag.location().getNamespace();

        if(!namespace.equals(Accessories.MODID) && !namespace.equals(TrinketConstants.MOD_ID)) return;

        var path = tag.location().getPath();

        var isInTag = this.getItem().builtInRegistryHolder().is(tag);
        if(namespace.equals(TrinketConstants.MOD_ID)) {
            var filteredPath = TrinketConstants.filterGroup(path);

            var accessoryTag = TagKey.create(Registries.ITEM, new ResourceLocation(Accessories.MODID, WrappingTrinketsUtils.trinketsToAccessories_Slot(filteredPath)));

            isInTag = this.getItem().builtInRegistryHolder().is(accessoryTag) || this.getItem().builtInRegistryHolder().is(tag);
        }

        var filteredPath = TrinketConstants.filterGroup(path);

        var trinketsPath = WrappingTrinketsUtils.accessoriesToTrinkets_Slot(filteredPath);

        for (TagKey<Item> itemTagKey : this.getTags().toList()) {
            var namespace1 = itemTagKey.location().getNamespace();
            var path1 = itemTagKey.location().getPath();

            if((namespace1.equals(TrinketConstants.MOD_ID) && path1.contains(trinketsPath)) || (namespace1.equals(Accessories.MODID) && path1.contains(filteredPath))) {
                isInTag = isInTag || this.getItem().builtInRegistryHolder().is(itemTagKey);

                if (isInTag) break;
            }
        }

        if(isInTag) cir.setReturnValue(true);
    }
}
