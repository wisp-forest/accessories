package io.wispforest.accessories.data;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.renderers.AccessoryRenderer;
import io.wispforest.accessories.api.client.rendering.RenderingFunction;
import io.wispforest.accessories.api.client.rendering.RenderingFunction.DeferredRenderer;
import io.wispforest.accessories.api.client.rendering.RenderingFunction.RawRenderer;
import io.wispforest.accessories.data.api.SimpleManagedEndecDataLoader;
import io.wispforest.accessories.utils.HashUtils;
import io.wispforest.accessories.utils.ServerInstanceHolder;
import io.wispforest.endec.format.gson.GsonDeserializer;
import io.wispforest.owo.Owo;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.util.*;

@ApiStatus.Experimental
public class CustomRendererLoader extends SimpleManagedEndecDataLoader<RawRenderer> {

    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    private ResourceLocation constantResolveTarget = null;

    public static final CustomRendererLoader CLIENT_OVERRIDES = new CustomRendererLoader(PackType.CLIENT_RESOURCES);
    public static final CustomRendererLoader PRIMARY = new CustomRendererLoader(PackType.SERVER_DATA);

    private final Map<UUID, RenderingFunction.Compound> resolvedClient = new HashMap<>();
    private final Map<UUID, RenderingFunction.Compound> resolvedServer = new HashMap<>();

    protected CustomRendererLoader(PackType packType) {
        super(Accessories.of("rendering_renderer"), "accessories/render/renderer", RawRenderer.ENDEC, packType);
    }

    @Nullable
    public static Either<AccessoryRenderer, RenderingFunction> getOrResolveRenderer(DeferredRenderer dataRenderer, boolean isClientSide) {
        var renderer = AccessoriesRendererRegistry.getRenderer(dataRenderer.rendererId());
        if (renderer != null) return Either.left(renderer);

        var resolvedRenderer = CustomRendererLoader.getOrResolveDeferredRenderer(dataRenderer, isClientSide);
        if (resolvedRenderer != null) return Either.right(resolvedRenderer);

        return null;
    }

    public static RenderingFunction getOrResolveDeferredRenderer(DeferredRenderer deferredRenderer, boolean isClientSide) {
        var result = CLIENT_OVERRIDES.getOrResolveRendererInitial(deferredRenderer, isClientSide, true);

        if (result != null) return result;

        return PRIMARY.getOrResolveRendererInitial(deferredRenderer, isClientSide, false);
    }

    @Nullable
    public static RenderingFunction getOrResolveRawRenderer(RawRenderer dataRenderer, boolean isClientSide) {
        var result = CLIENT_OVERRIDES.resolveRawData(new ArrayDeque<>(), Accessories.of("generated"), dataRenderer, new HashMap<>(), isClientSide);

        if (result != null) return result;

        return CustomRendererLoader.PRIMARY.resolveRawData(new ArrayDeque<>(), Accessories.of("generated"), dataRenderer, new HashMap<>(), isClientSide);
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

    @Override
    public Map<ResourceLocation, RawRenderer> mapFrom(Map<ResourceLocation, RawRenderer> rawData) {
        this.resolvedServer.clear();
        this.missingRenderersServer.clear();

        return super.mapFrom(rawData);
    }

    @Nullable
    private RenderingFunction.Compound getOrResolveRendererInitial(DeferredRenderer deferredRenderer, boolean isClientSide, boolean allowMissing) {
        Deque<ResourceLocation> currentResolveTree = new ArrayDeque<>();
        var references = new HashMap<>(deferredRenderer.references());

        RenderingFunction.Compound function = null;
        boolean shouldResetFlagOnResolve = false;

        var uuid = deferredRenderer.getUUID();

        if (Objects.equals(constantResolveTarget, deferredRenderer.rendererId())) {
            if (!alwaysResolveFlag) {
                alwaysResolveFlag = true;
                shouldResetFlagOnResolve = true;
            }
        } else if(!alwaysResolveFlag) {
            function = (isClientSide ? resolvedClient : resolvedServer).get(uuid);
        }

        if (function == null) {
            function = resolveRenderer(currentResolveTree, deferredRenderer.rendererId(), references, isClientSide, allowMissing);

            (isClientSide ? resolvedClient : resolvedServer).put(uuid, function);
        }

        if (shouldResetFlagOnResolve) alwaysResolveFlag = false;


        return function;
    }

    private RenderingFunction.Compound resolveRenderer(Deque<ResourceLocation> currentResolveTree, ResourceLocation id, Map<String, JsonElement> references, boolean isClientSide, boolean allowMissing) {
        currentResolveTree.push(id);

        RawRenderer rawRenderer = null;

        if (alwaysResolveFlag) rawRenderer = this.getDataFromId(id, isClientSide);
        if (rawRenderer == null) rawRenderer = getEntry(id, isClientSide);

        if (rawRenderer == null) {
            if (allowMissing) {
                var errorSet = (isClientSide ? missingRenderersClient : missingRenderersServer);

                if (!errorSet.contains(id)) {
                    LOGGER.error("Unable to resolve renderer [{}] as it was not found within Custom Renderer Registry!", id);

                    errorSet.add(id);
                }
            }

            return null;
        }

        var function = resolveRawData(currentResolveTree, id, rawRenderer, references, isClientSide);

        currentResolveTree.pop();

        return function;
    }

    @Nullable
    private RenderingFunction.Compound resolveRawData(Deque<ResourceLocation> currentResolveTree, ResourceLocation id, RenderingFunction function, Map<String, JsonElement> references, boolean isClientSide) {
        if (function instanceof RawRenderer data) {
            data.references().forEach(references::putIfAbsent);

            if(data.renderingFunctions() != null) {
                var renderers = new ArrayList<RenderingFunction>();

                for (var rawRenderingFunc : data.renderingFunctions()) {
                    try {
                        rawRenderingFunc = resolveReferencesForCopy(references, rawRenderingFunc);

                        var renderingFunc = RenderingFunction.ENDEC.decodeFully(GsonDeserializer::of, rawRenderingFunc);

                        if (renderingFunc instanceof DeferredRenderer renderer) {
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

                var armTarget = data.firstPersonArmTarget();

                return new RenderingFunction.Compound(Collections.unmodifiableList(renderers), armTarget != null ? armTarget : RenderingFunction.ArmTarget.NONE);
            }
        } else if (function instanceof DeferredRenderer renderer) {
            renderer.references().forEach(references::putIfAbsent);

            if (!renderer.rendererId().equals(AccessoriesRendererRegistry.NO_RENDERER_ID)) {
                if (currentResolveTree.contains(renderer.rendererId())) {
                    currentResolveTree.push(renderer.rendererId());

                    LOGGER.error("Recursive loop of Renderer Referencing, unable to resolve such! [{}]", currentResolveTree);

                    currentResolveTree.pop();

                    return null;
                }

                var renderingFunc = resolveRenderer(currentResolveTree, renderer.rendererId(), references, isClientSide, false);

                if (renderingFunc != null && renderer.firstPersonArmTarget() != null) {
                    renderingFunc = new RenderingFunction.Compound(renderingFunc.renderingFunctions(), renderer.firstPersonArmTarget());
                }

                return renderingFunc;
            }
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

        PRIMARY.constantResolveTarget = id;
    }

    public static boolean isConstantResolveTarget() {
        return PRIMARY.constantResolveTarget != null;
    }

    @Nullable
    protected RenderingFunction.RawRenderer getDataFromId(ResourceLocation id, boolean isClientSide) {
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
        if (!isClientSide) return ServerInstanceHolder.getInstance().getResourceManager();

        return getClientManger();
    }

    //@Environment(EnvType.CLIENT)
    private ResourceManager getClientManger() {
        return Minecraft.getInstance().getResourceManager();
    }
}
