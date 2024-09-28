package io.wispforest.accessories.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.accessories.client.gui.AccessoriesScreen;

@EmiEntrypoint
public class AccessoriesClientEMIPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addExclusionArea(AccessoriesScreen.class, (screen, consumer) -> {
            var leftPos = screen.leftPos();
            var topPos = screen.topPos();

            var bl = screen.getMenu().showingSlots();

            var x = leftPos - screen.getPanelWidth() - (bl ? 15 : 0);
            var y = topPos;

            var width = screen.getPanelWidth() + (bl ? 15 : 0) + 176;
            var height = screen.getPanelHeight();

            consumer.accept(new Bounds(x, y, width, height));
        });

        registry.addExclusionArea(AccessoriesExperimentalScreen.class, (screen, consumer) -> {
            screen.getComponentRectangles().forEach(rectangle -> {
                consumer.accept(new Bounds(rectangle.x(), rectangle.y(), rectangle.width(), rectangle.height()));
            });
        });
    }
}
