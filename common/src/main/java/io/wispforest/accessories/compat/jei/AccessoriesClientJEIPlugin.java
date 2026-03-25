package io.wispforest.accessories.compat.jei;

// JEI integration disabled - no JEI available for 1.21.11 yet
// TODO: Re-enable when JEI is updated for 1.21.11

/*
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

import java.util.List;

@JeiPlugin
public class AccessoriesClientJEIPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Accessories.of("main");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(AccessoriesScreen.class, new IGuiContainerHandler<AccessoriesScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(AccessoriesScreen screen) {
                return screen.getComponentRectangles().stream()
                        .map(rectangle -> new Rect2i(rectangle.x(), rectangle.y(), rectangle.width(), rectangle.height()))
                        .toList();
            }
        });
    }
}
*/
