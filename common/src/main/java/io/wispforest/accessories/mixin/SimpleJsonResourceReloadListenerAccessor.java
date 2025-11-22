package io.wispforest.accessories.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleJsonResourceReloadListener.class)
public interface SimpleJsonResourceReloadListenerAccessor<T> {

    @Accessor("codec")
    Codec<T> accessories$getCodec();
}
