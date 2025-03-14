package io.wispforest.accessories.commands;

import com.google.gson.*;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesLoaderInternals;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.client.RenderingFunction;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryCustomRendererComponent;
import io.wispforest.accessories.api.components.AccessorySlotValidationComponent;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.format.gson.GsonDeserializer;
import io.wispforest.endec.format.gson.GsonEndec;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TempUtilCommands {
    private static final Map<UUID, Boolean> shouldRecreateRenderStack = new ConcurrentHashMap<>();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setLenient()
            .create();

    public static boolean shouldKeepCreatingStack(Player player) {
        if (!Accessories.DEBUG) return false;

        return shouldRecreateRenderStack.getOrDefault(player.getUUID(), false);
    }

    public static final Endec<Map<String, JsonElement>> VALUE_REFERENCES = EndecUtils.structifyEndec("references", GsonEndec.INSTANCE.mapOf())
            .catchErrors((ctx, serializer, exception) -> new HashMap<>());

    public static final Endec<RenderingFunction.Compound> RENDERING_FUNCTIONS = RenderingFunction.Compound.ENDEC
            .catchErrors((ctx, serializer, exception) -> new RenderingFunction.Compound(List.of(), null));

    public static void createRenderStack(Player player) {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) return;

        var capability = AccessoriesCapability.get(player);
        var entryRef = capability.getFirstEquipped(stack1 -> stack1.has(AccessoriesDataComponents.CUSTOM_RENDERER));

        SlotReference reference;

        ItemStack stack = Items.BEDROCK.getDefaultInstance();
        stack.set(AccessoriesDataComponents.SLOT_VALIDATION, new AccessorySlotValidationComponent(
                Set.of("any"),
                Set.of()
        ));

        if (entryRef != null) {
            reference = entryRef.reference();
        } else {
            reference = SlotReference.of(player, List.copyOf(capability.getContainers().keySet()).getFirst(), 0);
        }

        List<RenderingFunction> renderingFunctions;

        var fileTargetData = decode(AccessoriesLoaderInternals.getConfigPath(), "accessories_custom_renderer_update_hook.json");

        if (!fileTargetData.has("file")) return;

        var jsonData = decode(AccessoriesLoaderInternals.getConfigPath(), fileTargetData.getAsJsonPrimitive("file").getAsString());

        var references = VALUE_REFERENCES.decodeFully(GsonDeserializer::of, jsonData);

        renderingFunctions = List.of(RENDERING_FUNCTIONS.decodeFully(GsonDeserializer::of, resolveReferences(references, jsonData)));

        if (renderingFunctions.isEmpty()) return;

        var newComponent = new AccessoryCustomRendererComponent(renderingFunctions);

        stack.set(AccessoriesDataComponents.CUSTOM_RENDERER, newComponent);

        boolean updateOccured = false;

        if (!reference.getStack().has(AccessoriesDataComponents.CUSTOM_RENDERER)) {
            updateOccured = true;
        } else if (!newComponent.equals(reference.getStack().get(AccessoriesDataComponents.CUSTOM_RENDERER))) {
            updateOccured = true;
        }

        if (updateOccured) {
            reference.setStack(stack);
        }

        shouldRecreateRenderStack.put(player.getUUID(), true);
    }

    public static JsonObject decode(Path path, String fileName) {
        try {
            var file = path.resolve(fileName);

            if (file.toFile().exists()) {
                var data = Files.readString(path.resolve(fileName));

                var jsonData = GSON.fromJson(data, JsonObject.class);

                if (jsonData != null) return jsonData;
            }
        } catch (Throwable ignored) {}

        return new JsonObject();
    }

    public static JsonElement resolveReferences(Map<String, JsonElement> references, JsonElement jsonElement) {
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

        return jsonElement;
    }


}
