package io.wispforest.accessories.neoforge.mixin.neoforge;

import net.minecraft.util.context.ContextKey;
import net.neoforged.neoforge.client.renderstate.BaseRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = BaseRenderState.class, remap = false)
public interface BaseRenderStateAccessor {
    @Accessor("extensions")
    Map<ContextKey<?>, Object> accessories$extensions();
}
