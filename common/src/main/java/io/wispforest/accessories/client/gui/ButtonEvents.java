package io.wispforest.accessories.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.resources.ResourceLocation;

public class ButtonEvents {

    public static <B extends AbstractButton> B adjustRendering(B button, AdjustRendering event){
        ((AbstractButtonExtension) button).getRenderingEvent().register(event);

        return button;
    }

    public interface AdjustRendering {
        boolean render(AbstractButton button, GuiGraphics instance, ResourceLocation sprite, int x, int y, int width, int height);
    }
}
