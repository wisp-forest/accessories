package io.wispforest.accessories.api.components;

import io.wispforest.accessories.api.client.rendering.RenderingFunction;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@ApiStatus.Experimental
public record AccessoryCustomRendererComponent(@Nullable List<RenderingFunction> renderingFunctions, @Nullable Boolean defaultRenderOverride, boolean disableDefaultTranslations) {
    public static final AccessoryCustomRendererComponent EMPTY = new AccessoryCustomRendererComponent(null, null, false);

    public static final Endec<AccessoryCustomRendererComponent> ENDEC = StructEndecBuilder.of(
            RenderingFunction.ENDEC.listOf().optionalFieldOf("rendering_functions", AccessoryCustomRendererComponent::renderingFunctions, () -> null),
            Endec.BOOLEAN.optionalFieldOf("default_render_override", AccessoryCustomRendererComponent::defaultRenderOverride, () -> null),
            Endec.BOOLEAN.optionalFieldOf("disable_default_translations", AccessoryCustomRendererComponent::disableDefaultTranslations, () -> false),
            AccessoryCustomRendererComponent::new
    );
}
