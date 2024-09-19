package io.wispforest.accessories.api.components;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import org.jetbrains.annotations.Nullable;

public record AccessoryRenderOverrideComponent(@Nullable Boolean defaultRenderOverride) {

    public static final AccessoryRenderOverrideComponent DEFAULT = new AccessoryRenderOverrideComponent(null);

    public static final Endec<AccessoryRenderOverrideComponent> ENDEC = StructEndecBuilder.of(
            Endec.BOOLEAN.optionalFieldOf("default_render_override", AccessoryRenderOverrideComponent::defaultRenderOverride, () -> null),
            AccessoryRenderOverrideComponent::new
    );
}