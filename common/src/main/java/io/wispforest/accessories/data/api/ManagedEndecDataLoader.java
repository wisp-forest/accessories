package io.wispforest.accessories.data.api;

import com.google.common.collect.BiMap;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import net.minecraft.resources.Identifier;
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

public abstract class ManagedEndecDataLoader<V, D> extends EndecDataLoader<D> implements SyncedDataHelper<SequencedBiMap<Identifier, V>>, LookupDataLoader<V> {

    private final SequencedBiMap<Identifier, V> server = SequencedBiMap.of(LinkedHashMap::new);
    private final SequencedBiMap<Identifier, V> client = SequencedBiMap.of(LinkedHashMap::new);

    private final Endec<V> valueEndec;
    private final Endec<SequencedBiMap<Identifier, V>> mapEndec;

    protected ManagedEndecDataLoader(Identifier id, String type, Endec<V> valueEndec, Endec<D> dataEndec, PackType packType) {
        this(id, type, valueEndec, dataEndec, packType, false);
    }

    protected ManagedEndecDataLoader(Identifier id, String type, Endec<V> valueEndec, Endec<D> dataEndec, PackType packType, boolean requiresRegistries) {
        this(id, type, valueEndec, dataEndec, packType, SerializationContext.empty(), requiresRegistries);
    }

    protected ManagedEndecDataLoader(Identifier id, String type, Endec<V> valueEndec, Endec<D> dataEndec, PackType packType, Set<Identifier> dependencies) {
        this(id, type, valueEndec, dataEndec, packType, SerializationContext.empty(), false, dependencies);
    }

    protected ManagedEndecDataLoader(Identifier id, String type, Endec<V> valueEndec, Endec<D> dataEndec, PackType packType, SerializationContext context, boolean requiresRegistries) {
        this(id, type, valueEndec, dataEndec, packType, context, requiresRegistries, Set.of());
    }

    protected ManagedEndecDataLoader(Identifier id, String type, Endec<V> valueEndec, Endec<D> dataEndec, PackType packType, SerializationContext context, boolean requiresRegistries, Set<Identifier> dependencies) {
        super(id, type, dataEndec, packType, context, requiresRegistries, dependencies);

        this.valueEndec = valueEndec;
        this.mapEndec = biMapEndec(value -> SequencedBiMap.of(LinkedHashMap::new), Identifier::toString, Identifier::tryParse, valueEndec);
    }

    public static <V, D> ManagedEndecDataLoader<V, D> of(Identifier id, String type, Endec<V> valueEndec, Endec<D> dataEndec, PackType packType, Function<Map<Identifier, D>, Map<Identifier, V>> mapFrom) {
        return new ManagedEndecDataLoader<V, D>(id, type, valueEndec, dataEndec, packType){
            @Override
            public Map<Identifier, V> mapFrom(Map<Identifier, D> rawData) {
                return mapFrom.apply(rawData);
            }
        };
    }

    @Override
    public Map<Identifier, V> getEntries(boolean isClientSide) {
        return Collections.unmodifiableMap(isClientSide ? client : server);
    }

    @Override
    @Nullable
    public V getEntry(Identifier id, boolean isClientSide) {
        return (isClientSide ? client : server).get(id);
    }

    @Override
    public Identifier getId(V t, boolean isClientSide) {
        return (isClientSide ? client : server).inverse().get(t);
    }

    //--

    public abstract Map<Identifier, V> mapFrom(Map<Identifier, D> rawData);

    protected void onSync() {}

    @Override
    public final SequencedBiMap<Identifier, V> getServerData() {
        return this.server;
    }

    @Override
    public final Endec<SequencedBiMap<Identifier, V>> syncDataEndec() {
        return this.mapEndec;
    }

    @Override
    public final void onReceivedData(SequencedBiMap<Identifier, V> data) {
        this.client.clear();
        this.client.putAll(data);

        this.onSync();
    }

    //--

    @Override
    protected void apply(Map<Identifier, D> loadedObjects, ResourceManager resourceManager, ProfilerFiller profiler) {
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
