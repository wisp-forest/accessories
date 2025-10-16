package io.wispforest.accessories.data.api;

import com.google.common.collect.BiMap;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

public abstract class ManagedEndecDataLoader<V, D> extends EndecDataLoader<D> implements SyncedDataHelper<SequencedBiMap<ResourceLocation, V>>, LookupDataLoader<V> {

    private final SequencedBiMap<ResourceLocation, V> server = SequencedBiMap.of(LinkedHashMap::new);
    private final SequencedBiMap<ResourceLocation, V> client = SequencedBiMap.of(LinkedHashMap::new);

    private final Endec<V> valueEndec;
    private final Endec<SequencedBiMap<ResourceLocation, V>> mapEndec;

    protected ManagedEndecDataLoader(ResourceLocation id, String type, Endec<V> valueEndec, Endec<D> dataEndec, PackType packType) {
        this(id, type, valueEndec, dataEndec, packType, false);
    }

    protected ManagedEndecDataLoader(ResourceLocation id, String type, Endec<V> valueEndec, Endec<D> dataEndec, PackType packType, boolean requiresRegistries) {
        this(id, type, valueEndec, dataEndec, packType, SerializationContext.empty(), requiresRegistries);
    }

    protected ManagedEndecDataLoader(ResourceLocation id, String type, Endec<V> valueEndec, Endec<D> dataEndec, PackType packType, Set<ResourceLocation> dependencies) {
        this(id, type, valueEndec, dataEndec, packType, SerializationContext.empty(), false, dependencies);
    }

    protected ManagedEndecDataLoader(ResourceLocation id, String type, Endec<V> valueEndec, Endec<D> dataEndec, PackType packType, SerializationContext context, boolean requiresRegistries) {
        this(id, type, valueEndec, dataEndec, packType, context, requiresRegistries, Set.of());
    }

    protected ManagedEndecDataLoader(ResourceLocation id, String type, Endec<V> valueEndec, Endec<D> dataEndec, PackType packType, SerializationContext context, boolean requiresRegistries, Set<ResourceLocation> dependencies) {
        super(id, type, dataEndec, packType, context, requiresRegistries, dependencies);

        this.valueEndec = valueEndec;
        this.mapEndec = biMapEndec(value -> SequencedBiMap.of(LinkedHashMap::new), ResourceLocation::toString, ResourceLocation::tryParse, valueEndec);
    }

    public static <V, D> ManagedEndecDataLoader<V, D> of(ResourceLocation id, String type, Endec<V> valueEndec, Endec<D> dataEndec, PackType packType, Function<Map<ResourceLocation, D>, Map<ResourceLocation, V>> mapFrom) {
        return new ManagedEndecDataLoader<V, D>(id, type, valueEndec, dataEndec, packType){
            @Override
            public Map<ResourceLocation, V> mapFrom(Map<ResourceLocation, D> rawData) {
                return mapFrom.apply(rawData);
            }
        };
    }

    @Override
    public Map<ResourceLocation, V> getEntries(boolean isClientSide) {
        return Collections.unmodifiableMap(isClientSide ? client : server);
    }

    @Override
    @Nullable
    public V getEntry(ResourceLocation id, boolean isClientSide) {
        return (isClientSide ? client : server).get(id);
    }

    @Override
    public ResourceLocation getId(V t, boolean isClientSide) {
        return (isClientSide ? client : server).inverse().get(t);
    }

    //--

    public abstract Map<ResourceLocation, V> mapFrom(Map<ResourceLocation, D> rawData);

    protected void onSync() {}

    @Override
    public final SequencedBiMap<ResourceLocation, V> getServerData() {
        return this.server;
    }

    @Override
    public final Endec<SequencedBiMap<ResourceLocation, V>> syncDataEndec() {
        return this.mapEndec;
    }

    @Override
    public final void onReceivedData(SequencedBiMap<ResourceLocation, V> data) {
        this.client.clear();
        this.client.putAll(data);

        this.onSync();
    }

    //--

    @Override
    protected void apply(Map<ResourceLocation, D> loadedObjects, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.server.clear();
        this.server.putAll(mapFrom(loadedObjects));
    }

    @ApiStatus.Internal
    private static <K, V, M extends BiMap<K, V>> Endec<M> biMapEndec(IntFunction<M> biMapFactory, Function<K, String> keyToString, Function<String, K> stringToKey, Endec<V> valueEndec) {
        return Endec.of((ctx, serializer, map) -> {
            try (var mapState = serializer.map(ctx, valueEndec, map.size())) {
                map.forEach((k, v) -> mapState.entry(keyToString.apply(k), v));
            }
        }, (ctx, deserializer) -> {
            var mapState = deserializer.map(ctx, valueEndec);

            var map = biMapFactory.apply(mapState.estimatedSize());
            mapState.forEachRemaining(entry -> map.put(stringToKey.apply(entry.getKey()), entry.getValue()));

            return map;
        });
    }
}
