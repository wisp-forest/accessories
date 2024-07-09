package io.wispforest.accessories.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.format.bytebuf.ByteBufDeserializer;
import io.wispforest.endec.format.bytebuf.ByteBufSerializer;
import io.wispforest.endec.format.gson.GsonSerializer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.BiFunction;
import java.util.function.Function;

public record RecordArgumentTypeInfo<A extends ArgumentType<?>, T>(StructEndec<T> endec, Function<A, T> toTemplate, BiFunction<CommandBuildContext, T, A> fromTemplate) implements ArgumentTypeInfo<A, RecordArgumentTypeInfo.RecordInfoTemplate<A, T>> {

    public static <A extends ArgumentType<?>> RecordArgumentTypeInfo<A, Void> of(Function<CommandBuildContext, A> argTypeConstructor) {
        return new RecordArgumentTypeInfo<>(EndecUtils.structUnit(() -> null), a -> null, (commandBuildContext, unused) -> argTypeConstructor.apply(commandBuildContext));
    }

    @Override
    public void serializeToNetwork(RecordInfoTemplate<A, T> template, FriendlyByteBuf buffer) {
        endec.encodeFully(() -> ByteBufSerializer.of(buffer), template.data());
    }

    @Override
    public RecordInfoTemplate<A, T> deserializeFromNetwork(FriendlyByteBuf buffer) {
        return new RecordInfoTemplate<>(this, endec.decodeFully(ByteBufDeserializer::of, buffer), fromTemplate);
    }

    @Override
    public void serializeToJson(RecordInfoTemplate<A, T> template, JsonObject json) {
        json.asMap().putAll(((JsonObject) endec.encodeFully(GsonSerializer::of, template.data())).asMap());
    }

    @Override
    public RecordInfoTemplate<A, T> unpack(A argument) {
        return new RecordInfoTemplate<>(this, toTemplate.apply(argument), fromTemplate);
    }

    public record RecordInfoTemplate<A extends ArgumentType<?>, T>(ArgumentTypeInfo<A, ?> type, T data, BiFunction<CommandBuildContext, T, A> fromTemplate) implements Template<A> {
        @Override public A instantiate(CommandBuildContext ctx) { return fromTemplate.apply(ctx, data()); }
    }
}
