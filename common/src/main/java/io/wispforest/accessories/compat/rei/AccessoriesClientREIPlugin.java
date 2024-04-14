package io.wispforest.accessories.compat.rei;

import io.wispforest.accessories.client.gui.AccessoriesScreen;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;

import java.util.List;

public class AccessoriesClientREIPlugin implements REIClientPlugin {
    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(AccessoriesScreen.class, screen -> {
            var leftPos = screen.leftPos();
            var topPos = screen.topPos();

            var bl = screen.getMenu().showingSlots();

            var x = leftPos - screen.getPanelWidth() - (bl ? 15 : 0);
            var y = topPos;

            var width = screen.getPanelWidth() + (bl ? 15 : 0) + 176;
            var height = screen.getPanelHeight();

            return List.of(new Rectangle(x, y, width, height));
        });
    }
}
