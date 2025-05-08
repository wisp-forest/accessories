package io.wispforest.accessories.utils;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonUtils {

    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    private static final Logger LOGGER = LogUtils.getLogger();

    public static <T> void scanDirectoryWithReplace(ResourceManager resourceManager, ResourceKey<? extends Registry<T>> registryKey, DynamicOps<JsonElement> ops, Codec<T> codec, Map<ResourceLocation, T> output) {
        scanDirectoryWithReplace(resourceManager, FileToIdConverter.registry(registryKey), ops, codec, output);
    }

    public static <T> void scanDirectoryWithReplace(ResourceManager resourceManager, FileToIdConverter fileToIdConverter, DynamicOps<JsonElement> ops, Codec<T> codec, Map<ResourceLocation, T> output) {
        var outputJson = new LinkedHashMap<ResourceLocation, JsonObject>();

        for(var entry : fileToIdConverter.listMatchingResourceStacks(resourceManager).entrySet()) {
            var filePath = entry.getKey();
            var resourceLocation = fileToIdConverter.fileToId(entry.getKey());

            for (Resource resource : entry.getValue()) {
                try(Reader reader = resource.openAsReader()) {
                    var jsonElement = GsonHelper.fromJson(GSON, reader, JsonElement.class);

                    if(!(jsonElement instanceof JsonObject jsonObject)){
                        LOGGER.warn("File was found not to be parsed as a valid JsonObject, it will be skipped: [Location: " + filePath + "]");
                        continue;
                    }

                    if(outputJson.containsKey(resourceLocation)){
                        var jsonObject2 = outputJson.get(resourceLocation).getAsJsonObject();

                        //TODO: SHOULD THIS OVERWRITE ENTRIES OR REPLACE THE OBJECT????
                        if(GsonHelper.getAsBoolean(jsonObject, "replace")){
                            jsonObject.asMap().forEach(jsonObject2::add);
                        }
                    } else {
                        outputJson.put(resourceLocation, jsonObject);
                    }
                } catch (IllegalArgumentException | IOException | JsonParseException var14) {
                    LOGGER.error("Couldn't parse data file {} from {}", resourceLocation, resourceLocation, var14);
                }
            }
        }

        for (Map.Entry<ResourceLocation, JsonObject> entry : outputJson.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            ResourceLocation resourceLocation2 = fileToIdConverter.fileToId(resourceLocation);

            try {
                codec.parse(ops, entry.getValue()).ifSuccess(object -> {
                    if (output.putIfAbsent(resourceLocation2, object) != null) {
                        throw new IllegalStateException("Duplicate data file ignored with ID " + resourceLocation2);
                    }
                }).ifError(error -> LOGGER.error("Couldn't parse data file '{}' from '{}': {}", resourceLocation2, resourceLocation, error));
            } catch (IllegalArgumentException | JsonParseException var14) {
                LOGGER.error("Couldn't parse data file '{}' from '{}'", resourceLocation2, resourceLocation, var14);
            }
        }
    }

    public static <T> Map<ResourceLocation, Resource> scanDirectoryWithReplace(ResourceManager resourceManager, FileToIdConverter fileToIdConverter) {
        var outputResources = new LinkedHashMap<ResourceLocation, Pair<JsonObject, Resource>>();

        for(var entry : fileToIdConverter.listMatchingResourceStacks(resourceManager).entrySet()) {
            var filePath = entry.getKey();
            var resourceLocation = fileToIdConverter.fileToId(entry.getKey());

            for (Resource resource : entry.getValue()) {
                try(Reader reader = resource.openAsReader()) {
                    var jsonElement = GsonHelper.fromJson(GSON, reader, JsonElement.class);

                    if(!(jsonElement instanceof JsonObject jsonObject)){
                        LOGGER.warn("File was found not to be parsed as a valid JsonObject, it will be skipped: [Location: " + filePath + "]");
                        continue;
                    }

                    if(outputResources.containsKey(resourceLocation)){
                        var jsonObject2 = outputResources.get(resourceLocation).first().getAsJsonObject();

                        //TODO: SHOULD THIS OVERWRITE ENTRIES OR REPLACE THE OBJECT????
                        if(GsonHelper.getAsBoolean(jsonObject, "replace")){
                            jsonObject.asMap().forEach(jsonObject2::add);
                        }
                    } else {
                        outputResources.put(resourceLocation, Pair.of(jsonObject, resource));
                    }
                } catch (IllegalArgumentException | IOException | JsonParseException var14) {
                    LOGGER.error("Couldn't parse data file {} from {}", resourceLocation, resourceLocation, var14);
                }
            }
        }

        return outputResources.entrySet().stream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().second(),
                        (object, object2) -> object,
                        LinkedHashMap::new
                )
        );
    }
}
