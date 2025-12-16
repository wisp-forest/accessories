package io.wispforest.accessories.data;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.data.api.EndecDataLoader;
import io.wispforest.endec.Endec;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

import java.util.Map;

public class RendererBindingLoader extends EndecDataLoader<Map<Item, Identifier>> {

    public static final RendererBindingLoader LOADER = new RendererBindingLoader();

    private RendererBindingLoader() {
        super(Accessories.of("rendering_binding"), "accessories/render/binding", Endec.map(
                item -> BuiltInRegistries.ITEM.getKey(item).toString(),
                itemId -> BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId)),
                MinecraftEndecs.IDENTIFIER), PackType.CLIENT_RESOURCES);
    }

    @Override
    protected void apply(Map<Identifier, Map<Item, Identifier>> rawData, ResourceManager resourceManager, ProfilerFiller profiler) {
        rawData.forEach((location, data) -> AccessoriesRendererRegistry.setDataLoadedItemToRenderer(data));
    }
}
