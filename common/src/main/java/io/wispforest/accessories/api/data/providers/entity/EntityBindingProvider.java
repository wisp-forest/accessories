package io.wispforest.accessories.api.data.providers.entity;

import com.mojang.serialization.Codec;
import io.wispforest.accessories.api.data.providers.BaseDataProvider;
import io.wispforest.owo.serialization.CodecUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class EntityBindingProvider extends BaseDataProvider<EntityBindingProvider.EntityBindingOutput> {

    private final Codec<RawEntityBinding> CODEC = CodecUtils.toCodec(RawEntityBinding.ENDEC);

    public EntityBindingProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(packOutput, completableFuture);
    }

    @Override
    protected abstract void buildData(HolderLookup.Provider provider, EntityBindingOutput output);

    public EntityBindingBuilder builder() {
        return new EntityBindingBuilder(false);
    }

    //--

    @Override
    public final String getName() {
        return "SlotGroup";
    }

    @Override
    protected final String type() {
        return "accessories/group";
    }

    @Override
    protected final PackOutput.Target target() {
        return PackOutput.Target.DATA_PACK;
    }

    public interface EntityBindingOutput extends BaseDataProvider.DataOutput {
        void accept(ResourceLocation location, RawEntityBinding binding);
    }

    @Override
    protected EntityBindingOutput buildOutput(CachedOutput cachedOutput, HolderLookup.Provider provider) {
        return new EntityBindingOutput() {

            final List<CompletableFuture<?>> list = new ArrayList<>();

            @Override
            public void accept(ResourceLocation location, RawEntityBinding binding) {
                list.add(DataProvider.saveStable(cachedOutput, provider, CODEC, binding, EntityBindingProvider.this.pathProvider().json(location)));
            }

            @Override
            public Collection<CompletableFuture<?>> futures() {
                return List.of();
            }
        };
    }
}
