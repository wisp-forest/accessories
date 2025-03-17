package io.wispforest.accessories.api.data.providers;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public abstract class BaseDataProvider<O extends BaseDataProvider.DataOutput> implements DataProvider {

    protected static final Logger LOGGER = LogUtils.getLogger();

    private final PackOutput.PathProvider pathProvider;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public BaseDataProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> completableFuture) {
        this.pathProvider = packOutput.createPathProvider(target(), type());
        this.registries = completableFuture;
    }

    public PackOutput.PathProvider pathProvider() {
        return this.pathProvider;
    }

    @Override
    public final CompletableFuture<?> run(CachedOutput cachedOutput) {
        return this.registries.thenCompose(provider -> this.run(cachedOutput, provider));
    }

    private CompletableFuture<?> run(CachedOutput cachedOutput, HolderLookup.Provider provider) {
        var output = buildOutput(cachedOutput, provider);

        this.buildData(provider, output);

        return CompletableFuture.allOf(output.futures().toArray(CompletableFuture[]::new));
    }

    public abstract String getName();

    protected abstract String type();

    protected abstract PackOutput.Target target();

    protected abstract O buildOutput(CachedOutput cachedOutput, HolderLookup.Provider provider);

    protected abstract void buildData(HolderLookup.Provider provider, O output);

    public interface DataOutput {
        Collection<CompletableFuture<?>> futures();
    }
}
