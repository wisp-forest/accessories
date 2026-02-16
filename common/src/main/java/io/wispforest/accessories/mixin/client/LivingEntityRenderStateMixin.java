package io.wispforest.accessories.mixin.client;

import io.wispforest.accessories.pond.AccessoriesRenderStateAPImpl;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.context.ContextKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements AccessoriesRenderStateAPImpl {
    @Unique
    private final Reference2ObjectMap<ContextKey<?>, Object> keyToData = new Reference2ObjectOpenHashMap<>();

    @Override
    public Reference2ObjectMap<ContextKey<?>, Object> contextKeyToContextData() {
        return keyToData;
    }
}
