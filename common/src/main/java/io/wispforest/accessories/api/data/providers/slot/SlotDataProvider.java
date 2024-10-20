package io.wispforest.accessories.api.data.providers.slot;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import io.wispforest.accessories.api.data.providers.BaseDataProvider;
import io.wispforest.owo.serialization.CodecUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class SlotDataProvider extends BaseDataProvider<SlotDataProvider.SlotOutput> {

    private final Codec<RawSlotType> CODEC = CodecUtils.toCodec(RawSlotType.ENDEC);

    public SlotDataProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(packOutput, completableFuture);
    }

    public interface SlotOutput extends DataOutput {
        void accept(String namespace, RawSlotType slotType);
    }

    @Override
    protected abstract void buildData(HolderLookup.Provider provider, SlotOutput output);

    public SlotBuilder builder(ResourceLocation uniqueLocation) {
        return builder(uniqueLocation.toString());
    }

    public SlotBuilder builder(String slotName) {
        return new SlotBuilder(slotName, false);
    }

    //--

    @Override
    public final String getName() {
        return "SlotType";
    }

    @Override
    protected final String type() {
        return "accessories/slot";
    }

    @Override
    protected final PackOutput.Target target() {
        return PackOutput.Target.DATA_PACK;
    }

    @Override
    protected final SlotOutput buildOutput(CachedOutput cachedOutput, HolderLookup.Provider provider) {
        return new SlotOutput() {
            final Set<ResourceLocation> set = Sets.newHashSet();
            final List<CompletableFuture<?>> list = new ArrayList<>();

            @Override
            public void accept(String namespace, RawSlotType rawSlotType) {
                var location = ResourceLocation.fromNamespaceAndPath(namespace, rawSlotType.name().replace(":", "/"));

                if (!set.add(location)) throw new IllegalStateException("Duplicate SlotType: " + location);

                list.add(DataProvider.saveStable(cachedOutput, provider, CODEC, rawSlotType, SlotDataProvider.this.pathProvider().json(location)));
            }

            @Override
            public Collection<CompletableFuture<?>> futures() {
                return list;
            }
        };
    }
}
