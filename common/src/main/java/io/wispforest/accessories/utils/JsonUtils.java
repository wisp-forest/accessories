package io.wispforest.accessories.utils;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
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

    public record FileResourceData(ResourceLocation fileLocation, JsonObject obj, Resource resource){}

    public static <T> Map<ResourceLocation, Resource> scanDirectoryWithReplace(ResourceManager resourceManager, FileToIdConverter fileToIdConverter) {
        var outputResources = new LinkedHashMap<ResourceLocation, FileResourceData>();

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
                        var jsonObject2 = outputResources.get(resourceLocation).obj().getAsJsonObject();

                        //TODO: SHOULD THIS OVERWRITE ENTRIES OR REPLACE THE OBJECT????
                        if(GsonHelper.getAsBoolean(jsonObject, "replace")){
                            jsonObject.asMap().forEach(jsonObject2::add);
                        }
                    } else {
                        outputResources.put(resourceLocation, new FileResourceData(filePath, jsonObject, resource));
                    }
                } catch (IllegalArgumentException | IOException | JsonParseException var14) {
                    LOGGER.error("Couldn't parse data file {} from {}", resourceLocation, filePath, var14);
                }
            }
        }

        return outputResources.entrySet().stream().collect(
                Collectors.toMap(
                        entry -> entry.getValue().fileLocation(),
                        entry -> entry.getValue().resource(),
                        (object, object2) -> object,
                        LinkedHashMap::new
                )
        );
    }
}
