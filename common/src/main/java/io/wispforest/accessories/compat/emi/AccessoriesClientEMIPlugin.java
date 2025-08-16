package io.wispforest.accessories.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import io.wispforest.accessories.client.gui.AccessoriesScreen;

@EmiEntrypoint
public class AccessoriesClientEMIPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addExclusionArea(AccessoriesScreen.class, (screen, consumer) -> {
            screen.getComponentRectangles().forEach(rectangle -> {
                consumer.accept(new Bounds(rectangle.x(), rectangle.y(), rectangle.width(), rectangle.height()));
            });
        });
    }
}
