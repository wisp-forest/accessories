package io.wispforest.accessories.api.data.providers.group;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import io.wispforest.accessories.api.data.providers.BaseDataProvider;
import io.wispforest.accessories.api.data.providers.slot.SlotBuilder;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.owo.serialization.CodecUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract class GroupDataProvider extends BaseDataProvider<GroupDataProvider.GroupOutput> {

    private final Codec<RawSlotGroup> CODEC = CodecUtils.toCodec(RawSlotGroup.ENDEC);

    public GroupDataProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(packOutput, completableFuture);
    }

    public interface GroupOutput extends DataOutput {
        void accept(String namespace, RawSlotGroup slotType);
    }

    @Override
    protected abstract void buildData(HolderLookup.Provider provider, GroupOutput output);

    public SlotGroupBuilder builder(ResourceLocation uniqueLocation) {
        return builder(uniqueLocation.toString());
    }

    public SlotGroupBuilder builder(String groupName) {
        return new SlotGroupBuilder(groupName, false);
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

    @Override
    protected final GroupOutput buildOutput(CachedOutput cachedOutput, HolderLookup.Provider provider) {
        return new GroupOutput() {
            final Set<ResourceLocation> set = Sets.newHashSet();
            final List<CompletableFuture<?>> list = new ArrayList<>();

            @Override
            public void accept(String namespace, RawSlotGroup rawSlotType) {
                var location = ResourceLocation.fromNamespaceAndPath(namespace, rawSlotType.name().replace(":", "/"));

                if (!set.add(location)) throw new IllegalStateException("Duplicate Group: " + location);

                list.add(DataProvider.saveStable(cachedOutput, provider, CODEC, rawSlotType, GroupDataProvider.this.pathProvider().json(location)));
            }

            @Override
            public Collection<CompletableFuture<?>> futures() {
                return list;
            }
        };
    }
}
