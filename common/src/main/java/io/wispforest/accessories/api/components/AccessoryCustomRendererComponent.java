package io.wispforest.accessories.api.components;

import io.wispforest.accessories.api.client.rendering.RenderingFunction;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Experimental
public record AccessoryCustomRendererComponent(List<RenderingFunction> renderingFunctions) {

    public static final AccessoryCustomRendererComponent EMPTY = new AccessoryCustomRendererComponent(List.of());

    public static final Endec<AccessoryCustomRendererComponent> ENDEC = StructEndecBuilder.of(
            RenderingFunction.ENDEC.listOf().fieldOf("rendering_functions", AccessoryCustomRendererComponent::renderingFunctions),
            AccessoryCustomRendererComponent::new
    );
}
