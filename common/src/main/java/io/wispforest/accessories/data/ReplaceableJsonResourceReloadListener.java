package io.wispforest.accessories.data;

import com.google.gson.*;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class ReplaceableJsonResourceReloadListener extends SimplePreparableReloadListener<Map<ResourceLocation, JsonObject>> {

    private final Logger logger;
    private final Gson gson;

    protected final String directory;

    protected ReplaceableJsonResourceReloadListener(Gson gson, Logger logger, String directory){
        this.logger = logger;
        this.gson = gson;

        this.directory = directory;
    }

    @Override
    protected Map<ResourceLocation, JsonObject> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        var fileToIdConverter = FileToIdConverter.json(directory);

        var output = new HashMap<ResourceLocation, JsonObject>();

        for(var entry : fileToIdConverter.listMatchingResourceStacks(resourceManager).entrySet()) {
            var filePath = entry.getKey();
            var resourceLocation = fileToIdConverter.fileToId(entry.getKey());

            for (Resource resource : entry.getValue()) {
                try(Reader reader = resource.openAsReader()) {
                    var jsonElement = GsonHelper.fromJson(gson, reader, JsonElement.class);

                    if(!(jsonElement instanceof JsonObject jsonObject)){
                        logger.warn("File was found not to be parsed as a valid JsonObject, it will be skipped: [Location: " + filePath + "]");
                        continue;
                    }

                    if(output.containsKey(resourceLocation)){
                        var jsonObject2 = output.get(resourceLocation).getAsJsonObject();

                        //TODO: SHOULD THIS OVERWRITE ENTRIES OR REPLACE THE OBJECT????
                        if(GsonHelper.getAsBoolean(jsonObject, "replace")){
                            jsonObject.asMap().forEach(jsonObject2::add);
                        }
                    } else {
                        output.put(resourceLocation, jsonObject);
                    }
                } catch (IllegalArgumentException | IOException | JsonParseException var14) {
                    logger.error("Couldn't parse data file {} from {}", resourceLocation, resourceLocation, var14);
                }
            }
        }

        return output;
    }

    public <T> void decodeJsonArray(JsonArray jsonArray, String name, ResourceLocation location, Function<JsonElement, @Nullable T> decoder, Consumer<T> consumer){
        for (var element : jsonArray) {
            if(!element.isJsonPrimitive()) {
                logger.warn("Unable to parse " + name + " as it is not a valid Json Primitive! [Location: " + location + "]");
                continue;
            }

            var value = decoder.apply(element);

            if(value == null) {
                logger.warn("Unable to parse " + name + " as it is not a valid ResourceLocation! [Location: " + location + ", Value: " + element.getAsString() + "]");
                continue;
            }

            consumer.accept(value);
        }
    }

    @Nullable
    protected <T> T safeHelper(BiFunction<JsonObject, String, T> func, JsonObject object, String key, ResourceLocation location){
        return safeHelper(func, object, key, null, location);
    }

    protected <T> T safeHelper(BiFunction<JsonObject, String, T> func, JsonObject object, String key, T defaultValue, ResourceLocation location){
        if(!object.has(key)) return defaultValue;

        try {
            return func.apply(object, key);
        } catch (Exception e){
            logger.warn("Unable to deserialize value for the given file: [Location: " + location + ", Field: " + key + "]", e);
        }

        return defaultValue;
    }
}
