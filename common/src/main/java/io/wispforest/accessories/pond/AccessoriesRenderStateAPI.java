package io.wispforest.accessories.pond;

import io.wispforest.accessories.api.AccessoriesStorageLookup;
import io.wispforest.accessories.api.client.AccessoriesRenderStateKeys;
import io.wispforest.accessories.api.client.AccessoryRenderState;
import io.wispforest.accessories.api.client.RenderStateStorage;
import io.wispforest.accessories.api.slot.SlotPath;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.UUID;

public interface AccessoriesRenderStateAPI extends RenderStateStorage {
    @Nullable
    default AccessoriesStorageLookup getStorageLookup() {
        return getStateData(AccessoriesRenderStateKeys.STORAGE_LOOKUP);
    }

    default UUID getEntityUUIDForState() {
        return getStateData(AccessoriesRenderStateKeys.ENTITY_UUID);
    }

    default float getEntityPartialTicksForState() {
        return getStateData(AccessoriesRenderStateKeys.PARTIAL_TICKS);
    }

    default int getEntityIdForState() {
        return getStateData(AccessoriesRenderStateKeys.ENTITY_ID);
    }
}
