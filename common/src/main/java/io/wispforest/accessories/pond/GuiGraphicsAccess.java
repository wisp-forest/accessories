package io.wispforest.accessories.pond;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

public interface GuiGraphicsAccess {
    GuiRenderState accessories$guiRenderState();
    GuiGraphicsExtractor.ScissorStack accessories$scissorStack();
}
