package io.wispforest.accessories.compat.jei;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@JeiPlugin
public class AccessoriesClientJEIPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return Accessories.of("main");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(AccessoriesScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(AccessoriesScreen screen) {
                var leftPos = screen.leftPos();
                var topPos = screen.topPos();

                var bl = screen.getMenu().showingSlots();

                var x = leftPos - screen.getPanelWidth() - (bl ? 15 : 0);
                var y = topPos;

                var width = screen.getPanelWidth() + (bl ? 15 : 0) + 176;
                var height = screen.getPanelHeight();

                return List.of(new Rect2i(x, y, width, height));
            }
        });

        registration.addGuiContainerHandler(AccessoriesExperimentalScreen.class, new IGuiContainerHandler<AccessoriesExperimentalScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(AccessoriesExperimentalScreen screen) {
                return screen.getComponentRectangles().stream()
                        .map(rectangle -> new Rect2i(rectangle.x(), rectangle.y(), rectangle.width(), rectangle.height()))
                        .toList();
            }
        });
    }
}
