package io.wispforest.accessories.client.gui;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.gui.components.AbstractButton;

public interface AbstractButtonExtension {
    Event<ButtonEvents.AdjustRendering> getRenderingEvent();

    default <B extends AbstractButton> B adjustRendering(ButtonEvents.AdjustRendering event) {
        this.getRenderingEvent().register(event);

        return (B) this;
    }
}
