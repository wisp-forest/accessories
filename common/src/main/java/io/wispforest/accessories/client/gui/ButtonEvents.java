package io.wispforest.accessories.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.resources.Identifier;

public class ButtonEvents {

    public static <B extends AbstractButton> B adjustRendering(B button, AdjustRendering event){
        ((AbstractButtonExtension) button).getRenderingEvent().register(event);

        return button;
    }

    public interface AdjustRendering {
        boolean render(AbstractButton button, GuiGraphicsExtractor instance, Identifier sprite, int x, int y, int width, int height);
    }
}
