package io.wispforest.accessories.api.client.rendering;

import com.google.gson.JsonElement;
import io.wispforest.accessories.Accessories;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.format.gson.GsonEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// TODO: FIRST CHANGE FROM JSON TO EDM WHEN 1.21.4 and CACHE RESULTS OF CUSTOM renderingFunctions SOME HOW?
@ApiStatus.Experimental
public record CustomDataRenderer(ResourceLocation rendererId, Map<String, JsonElement> references,
                                 @Nullable List<JsonElement> renderingFunctions,
                                 @Nullable ArmTarget firstPersonArmTarget) implements RenderingFunction {
    public static final ResourceLocation NO_RENDERER_SELECTED = Accessories.of("none");

    public static final StructEndec<CustomDataRenderer> ENDEC = StructEndecBuilder.of(
            MinecraftEndecs.IDENTIFIER.optionalFieldOf("renderer_id", CustomDataRenderer::rendererId, () -> NO_RENDERER_SELECTED),
            GsonEndec.INSTANCE.mapOf().optionalFieldOf("references", CustomDataRenderer::references, HashMap::new),
            GsonEndec.INSTANCE.listOf().optionalFieldOf("rendering_functions", CustomDataRenderer::renderingFunctions, () -> null),
            Endec.forEnum(ArmTarget.class).optionalFieldOf("first_person_arm_target", CustomDataRenderer::firstPersonArmTarget, () -> null),
            CustomDataRenderer::new
    );

    @Override
    public Map<String, JsonElement> references() {
        return Collections.unmodifiableMap(references);
    }

    @Override
    public @Nullable List<JsonElement> renderingFunctions() {
        return renderingFunctions != null ? Collections.unmodifiableList(renderingFunctions) : null;
    }

    @Override
    public String key() {
        return "renderer";
    }
}
