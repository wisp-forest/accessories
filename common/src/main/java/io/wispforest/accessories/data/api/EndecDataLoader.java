package io.wispforest.accessories.data.api;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.wispforest.accessories.mixin.SimpleJsonResourceReloadListenerAccessor;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.RegistriesAttribute;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;


// TODO: 1.21.4 ADJUSTMENTS SHOULD BE MADE TO USE LESS DIRECT CODE ANYWAYS
public abstract class EndecDataLoader<T> extends SimpleJsonResourceReloadListener<T> implements IdentifiedResourceReloadListener {

    protected final String type;

    protected final ResourceLocation id;
    protected final Endec<T> endec;

    protected final Set<ResourceLocation> dependencies;

    protected final SerializationContext context;

    protected final boolean requiresRegistries;

    private Function<PreparableReloadListener.SharedState, HolderLookup.@Nullable Provider> registriesAccess = sharedState -> null;

    protected EndecDataLoader(ResourceLocation id, String type, Endec<T> endec, PackType packType) {
        this(id, type, endec, packType, false);
    }

    protected EndecDataLoader(ResourceLocation id, String type, Endec<T> endec, PackType packType, Set<ResourceLocation> value) {
        this(id, type, endec, packType, SerializationContext.empty(),false, value);
    }

    protected EndecDataLoader(ResourceLocation id, String type, Endec<T> endec, PackType packType, boolean requiresRegistries) {
        this(id, type, endec, packType, SerializationContext.empty(), requiresRegistries);
    }

    protected EndecDataLoader(ResourceLocation id, String type, Endec<T> endec, PackType packType, SerializationContext context, boolean requiresRegistries) {
        this(id, type, endec, packType, context, requiresRegistries, Set.of());
    }

    protected EndecDataLoader(ResourceLocation id, String type, Endec<T> endec, PackType packType, SerializationContext context, boolean requiresRegistries, Set<ResourceLocation> value) {
        super(new DelegatingCodec<>(endec.toString(), endec), FileToIdConverter.json(type));

        this.id = id;
        this.type = type;
        this.endec = endec;
        this.context = context;
        this.requiresRegistries = requiresRegistries;
        this.dependencies = value;

        this.registerForType(packType);

        if (packType.equals(PackType.SERVER_DATA) && this instanceof SyncedDataHelper<?> syncedDataLoader) {
            SyncedDataHelperManager.registerLoader(syncedDataLoader);
        }
    }

    public void setRegistriesAccess(Function<PreparableReloadListener.SharedState, HolderLookup.@Nullable Provider> registriesAccess) {
        this.registriesAccess = registriesAccess;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public Set<ResourceLocation> getDependencyIds() {
        return this.dependencies;
    }

    @Override
    public void prepareSharedState(SharedState sharedState) {
        super.prepareSharedState(sharedState);

        var ctx = this.context;

        if (this.requiresRegistries) {
            var registries = registriesAccess.apply(sharedState);

            Objects.requireNonNull(registries, "Can not add the registries to endec context for the ManagedEndecDataLoader: " + this.getId());

            ctx = ctx.withAttributes(RegistriesAttribute.fromInfoGetter(new RegistryOps.HolderLookupAdapter(registries)));
        }

        getCodec().setCodec(ctx);
    }

    @ApiStatus.Internal
    private DelegatingCodec<T> getCodec() {
        return ((DelegatingCodec<T>) ((SimpleJsonResourceReloadListenerAccessor<T>) this).accessories$getCodec());
    }

    @Override
    protected Map<ResourceLocation, T> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        var entries = super.prepare(resourceManager, profiler);

        getCodec().resetCodec();

        return entries;
    }

    private static class DelegatingCodec<T> implements Codec<T> {

        private final String name;
        private final Endec<T> endec;

        @Nullable
        private Codec<T> codec = null;

        private DelegatingCodec(String name, Endec<T> endec) {
            this.name = name;
            this.endec = endec;
        }

        void setCodec(SerializationContext ctx) {
            this.codec = CodecUtils.toCodec(this.endec, ctx);
        }

        void resetCodec() {
            this.codec = null;
        }

        Codec<T> getOrThrow() {
            if (codec == null) throw new IllegalStateException("Unable to get codec as such has yet to be setup with the proper context!");

            return codec;
        }

        @Override
        public <S> DataResult<Pair<T, S>> decode(final DynamicOps<S> ops, final S input) {
            return this.getOrThrow().decode(ops, input);
        }

        @Override
        public <S> DataResult<S> encode(final T input, final DynamicOps<S> ops, final S prefix) {
            return this.getOrThrow().encode(input, ops, prefix);
        }

        @Override
        public String toString() {
            return "RecursiveCodec[" + name + ']';
        }
    }
}
