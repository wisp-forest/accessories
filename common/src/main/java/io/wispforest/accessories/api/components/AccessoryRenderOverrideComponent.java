package io.wispforest.accessories.api.components;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;

public record AccessoryRenderOverrideComponent(boolean defaultRenderOverride) {

    public static final AccessoryRenderOverrideComponent DEFAULT = new AccessoryRenderOverrideComponent(false);

    public static final Endec<AccessoryRenderOverrideComponent> ENDEC = StructEndecBuilder.of(
            Endec.BOOLEAN.fieldOf("defaultRenderOverride", AccessoryRenderOverrideComponent::defaultRenderOverride),
            AccessoryRenderOverrideComponent::new
    );
}
