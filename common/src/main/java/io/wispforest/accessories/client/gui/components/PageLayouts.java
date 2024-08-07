package io.wispforest.accessories.client.gui.components;

import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;

import java.util.List;

public record PageLayouts(GridLayout accessoriesLayout, GridLayout cosmeticLayout, List<Component> cosmeticToggleButtons) {
    public static final PageLayouts DEFAULT = new PageLayouts(
            Containers.grid(Sizing.content(), Sizing.content(), 0, 0),
            Containers.grid(Sizing.content(), Sizing.content(), 0, 0),
            List.of());

    public GridLayout getLayout(boolean isCosmetic) {
        return isCosmetic ? cosmeticLayout : accessoriesLayout;
    }
}
