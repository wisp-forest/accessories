package io.wispforest.accessories.data;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.client.rendering.CustomDataRenderer;
import io.wispforest.accessories.api.client.rendering.RenderingFunction;
import io.wispforest.accessories.utils.HashUtils;
import io.wispforest.accessories.utils.ManagedEndecDataLoader;
import io.wispforest.endec.format.gson.GsonDeserializer;
import io.wispforest.owo.Owo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.util.*;

@ApiStatus.Experimental
public class CustomRendererLoader extends ManagedEndecDataLoader<CustomDataRenderer> {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    private ResourceLocation constantResolveTarget = null;

    public static final CustomRendererLoader INSTANCE = new CustomRendererLoader();

    private final Map<ResourceLocation, RenderingFunction.Compound> resolvedClient = new HashMap<>();
    private final Map<ResourceLocation, RenderingFunction.Compound> resolvedServer = new HashMap<>();

    protected CustomRendererLoader() {
        super(Accessories.of("custom_renderer_loader"), "custom_renderer", CustomDataRenderer.ENDEC);
    }

    @Nullable
    public static RenderingFunction getOrResolveRenderer(ResourceLocation id, Map<String, JsonElement> references, Level level) {
        return getOrResolveRenderer(id, references, level.isClientSide());
    }

    @Nullable
    public static RenderingFunction getOrResolveRenderer(ResourceLocation id, Map<String, JsonElement> references, boolean isClientSide) {
        return INSTANCE.getOrResolveRendererInitial(new ArrayDeque<>(), id, references, isClientSide);
    }

    // TODO: DUE TO THE DESIRE TO ALLOW FOR SOME REWORKING OF THE RENDERS BASED ON PASSED REFERENCE DATA, THIS NEEDS A DIFFERENT WAY TO CACHE CAUSE CURRENTLY CACHES ONLY BY ID
    @Nullable
    public static RenderingFunction getOrResolveRenderer(CustomDataRenderer dataRenderer, boolean isClientSide) {
        if (!dataRenderer.rendererId().equals(CustomDataRenderer.NO_RENDERER_SELECTED)) {
            return CustomRendererLoader.getOrResolveRenderer(dataRenderer.rendererId(), Map.of(), isClientSide);
        } else if(dataRenderer.renderingFunctions() != null) {
            return CustomRendererLoader.INSTANCE.resolveRawData(new ArrayDeque<>(), Accessories.of("generated"), dataRenderer, new HashMap<>(), isClientSide);
        }

        return null;
    }

    private boolean alwaysResolveFlag = false;

    private final Set<ResourceLocation> missingRenderersClient = new HashSet<>();
    private final Set<ResourceLocation> missingRenderersServer = new HashSet<>();

    @Override
    protected void onSync() {
        this.missingRenderersClient.clear();
        this.missingRenderersServer.clear();

        this.resolvedClient.clear();
        this.resolvedServer.clear();
    }

    @Nullable
    private RenderingFunction.Compound getOrResolveRendererInitial(Deque<ResourceLocation> currentResolveTree, ResourceLocation id, Map<String, JsonElement> references, boolean isClientSide) {
        references = new HashMap<>(references);

        RenderingFunction.Compound function = null;
        boolean shouldResetFlagOnResolve = false;

        if (Objects.equals(constantResolveTarget, id)) {
            if (!alwaysResolveFlag) {
                alwaysResolveFlag = true;
                shouldResetFlagOnResolve = true;
            }
        } else if(!alwaysResolveFlag) {
            function = (isClientSide ? resolvedClient : resolvedServer).get(id);
        }

        if (function == null) {
            function = resolveRenderer(currentResolveTree, id, references, isClientSide);

            (isClientSide ? resolvedClient : resolvedServer).put(id, function);
        }

        if (shouldResetFlagOnResolve) alwaysResolveFlag = false;


        return function;
    }

    private RenderingFunction.Compound resolveRenderer(Deque<ResourceLocation> currentResolveTree, ResourceLocation id, Map<String, JsonElement> references, boolean isClientSide) {
        currentResolveTree.push(id);

        CustomDataRenderer rawRenderer = null;

        if (alwaysResolveFlag) rawRenderer = this.getDataFromId(id, isClientSide);
        if (rawRenderer == null) rawRenderer = getEntry(id, isClientSide);

        if (rawRenderer == null) {
            var errorSet = (isClientSide ? missingRenderersClient : missingRenderersServer);

            if (!errorSet.contains(id)) {
                LOGGER.error("Unable to resolve renderer [{}] as it was not found within Custom Renderer Registry!", id);

                errorSet.add(id);
            }

            return null;
        }

        var function = resolveRawData(currentResolveTree, id, rawRenderer, references, isClientSide);

        currentResolveTree.pop();

        return function;
    }

    @Nullable
    private RenderingFunction.Compound resolveRawData(Deque<ResourceLocation> currentResolveTree, ResourceLocation id, CustomDataRenderer rawData, Map<String, JsonElement> references, boolean isClientSide) {
        rawData.references().forEach(references::putIfAbsent);

        if (!rawData.rendererId().equals(CustomDataRenderer.NO_RENDERER_SELECTED)) {
            if (currentResolveTree.contains(rawData.rendererId())) {
                currentResolveTree.push(rawData.rendererId());

                LOGGER.error("Recursive loop of Renderer Referencing, unable to resolve such! [{}]", currentResolveTree);

                currentResolveTree.pop();

                return null;
            }

            var renderingFunc = resolveRenderer(currentResolveTree, rawData.rendererId(), references, isClientSide);

            if (renderingFunc != null && rawData.firstPersonArmTarget() != null) {
                renderingFunc = new RenderingFunction.Compound(renderingFunc.renderingFunctions(), rawData.firstPersonArmTarget());
            }

            return renderingFunc;
        } else if(rawData.renderingFunctions() != null) {
            var renderers = new ArrayList<RenderingFunction>();

            for (var rawRenderingFunc : rawData.renderingFunctions()) {
                try {
                    rawRenderingFunc = resolveReferencesForCopy(references, rawRenderingFunc);

                    var renderingFunc = RenderingFunction.ENDEC.decodeFully(GsonDeserializer::of, rawRenderingFunc);

                    if (renderingFunc instanceof CustomDataRenderer renderer) {
                        renderingFunc = resolveRawData(currentResolveTree, id.withPrefix("."), renderer, references, isClientSide);

                        if (renderingFunc == null) {
                            LOGGER.warn("Unable to resolve inner renderer [{}] for [{}] as it was not found within Custom Renderer Registry!", renderer.rendererId(), id);

                            continue;
                        }
                    }

                    renderers.add(renderingFunc);
                } catch (Exception e) {
                    errorIfDifferent(id, e, () -> {
                        LOGGER.error("Unable to decode the a given Render Function with [{}] due the following error: ", id);
                        minimalErroring(e);
                    });
                }
            }

            var armTarget = rawData.firstPersonArmTarget();

            return new RenderingFunction.Compound(Collections.unmodifiableList(renderers), armTarget != null ? armTarget : RenderingFunction.ArmTarget.NONE);
        }

        return null;
    }

    private static JsonElement resolveReferencesForCopy(Map<String, JsonElement> references, JsonElement jsonElement) {
        var copy = jsonElement.deepCopy();

        resolveReferences(references, copy);

        return copy;
    }

    private static void resolveReferences(Map<String, JsonElement> references, JsonElement jsonElement) {
        if (jsonElement instanceof JsonObject jsonObject) {
            for (var entry : jsonObject.asMap().entrySet()) {
                var key = entry.getKey();
                var childElement = entry.getValue();

                if (childElement instanceof JsonObject innerJsonObject) {
                    resolveReferences(references, innerJsonObject);
                } else if (childElement instanceof JsonArray innerJsonArray) {
                    resolveReferences(references, innerJsonArray);
                } else if (childElement instanceof JsonPrimitive jsonPrimitive && jsonPrimitive.isString()) {
                    var possibleReference = jsonPrimitive.getAsString();

                    if (possibleReference.matches("#.*") && references.containsKey(possibleReference)) {
                        jsonObject.add(key, references.get(possibleReference));
                    }
                }
            }
        } else if (jsonElement instanceof JsonArray jsonArray) {
            var list = jsonArray.asList();
            for (int i = 0; i < list.size(); i++) {
                var childElement = list.get(i);

                if (childElement instanceof JsonObject innerJsonObject) {
                    resolveReferences(references, innerJsonObject);
                } else if (childElement instanceof JsonArray innerJsonArray) {
                    resolveReferences(references, innerJsonArray);
                } else if (childElement instanceof JsonPrimitive jsonPrimitive && jsonPrimitive.isString()) {
                    var possibleReference = jsonPrimitive.getAsString();

                    if (possibleReference.matches("#.*") && references.containsKey(possibleReference)) {
                        jsonArray.set(i, references.get(possibleReference));
                    }
                }
            }
        }

    }

    //--

    @ApiStatus.Internal
    public static void constantFileResolving(MinecraftServer server, ResourceLocation id) {
        if (server.isDedicatedServer() && Accessories.DEBUG) return;

        INSTANCE.constantResolveTarget = id;
    }

    public static boolean isConstantResolveTarget() {
        return INSTANCE.constantResolveTarget != null;
    }

    @Nullable
    protected CustomDataRenderer getDataFromId(ResourceLocation id, boolean isClientSide) {
        var fileId = FileToIdConverter.json(this.type).idToFile(id);
        ResourceManager resource = getResourceManager(isClientSide);

        if (resource != null) {
            try {
                Reader reader = resource.openAsReader(fileId);
                JsonElement element;
                try {
                    element = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                } catch (Throwable var13) {
                    try {
                        reader.close();
                    } catch (Throwable var12) {
                        var13.addSuppressed(var12);
                    }

                    throw var13;
                }

                reader.close();

                return this.endec.decodeFully(GsonDeserializer::of, element);
            } catch (IllegalArgumentException | IOException | JsonParseException e) {
                errorIfDifferent(id, e, () -> {
                    LOGGER.error("Couldn't parse data file {} from {}", id, fileId);
                    minimalErroring(e);
                });
            }
        }

        return null;
    }

    private static final Cache<ResourceLocation, Integer> ERROR_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(30))
            .maximumSize(3000)
            .build();

    private void minimalErroring(Throwable throwable) {
        if (!alwaysResolveFlag) {
            LOGGER.error("", throwable);

            return;
        }

        if (throwable.getCause() != null) minimalErroring(throwable.getCause());

        LOGGER.error(throwable.getMessage());
    }

    private void errorIfDifferent(ResourceLocation id, Throwable e, Runnable runnable) {
        if (!alwaysResolveFlag) {
            runnable.run();
            return;
        }

        var prevErrorHash = ERROR_CACHE.getIfPresent(id);
        var hash = HashUtils.getHash(e);

        if (!Objects.equals(hash, prevErrorHash)) {
            ERROR_CACHE.put(id, hash);
            runnable.run();
        }
    }

    // TODO: I KNOW ITS UNSAFEISH!!!!
    private @NotNull ResourceManager getResourceManager(boolean isClientSide) {
        if (!isClientSide) return Owo.currentServer().getResourceManager();

        return getClientManger();
    }

    @Environment(EnvType.CLIENT)
    private ResourceManager getClientManger() {
        return Minecraft.getInstance().getResourceManager();
    }

    public static void init() {}
}
