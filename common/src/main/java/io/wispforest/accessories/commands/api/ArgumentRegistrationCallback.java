package io.wispforest.accessories.commands.api;

import com.mojang.brigadier.arguments.ArgumentType;
import io.wispforest.accessories.commands.api.core.RecordArgumentTypeInfo;
import net.minecraft.resources.ResourceLocation;

public interface ArgumentRegistrationCallback {
    <A extends ArgumentType<?>, T> RecordArgumentTypeInfo<A, T> register(ResourceLocation location, Class<A> clazz, RecordArgumentTypeInfo<A, T> info);
}
