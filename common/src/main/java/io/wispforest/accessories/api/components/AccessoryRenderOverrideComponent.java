package io.wispforest.accessories.api.components;

import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.fabricmc.fabric.api.util.TriState;

public record AccessoryRenderOverrideComponent(TriState defaultRenderOverride) {

    public static final AccessoryRenderOverrideComponent DEFAULT = new AccessoryRenderOverrideComponent(TriState.DEFAULT);

    public static final Endec<AccessoryRenderOverrideComponent> ENDEC = StructEndecBuilder.of(
            EndecUtils.TRI_STATE_ENDEC.fieldOf("default_render_override",AccessoryRenderOverrideComponent::defaultRenderOverride),
            AccessoryRenderOverrideComponent::new
    );
}
