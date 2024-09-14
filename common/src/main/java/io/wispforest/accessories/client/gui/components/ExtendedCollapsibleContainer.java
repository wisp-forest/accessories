package io.wispforest.accessories.client.gui.components;

import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.minecraft.network.chat.Component;

public class ExtendedCollapsibleContainer extends CollapsibleContainer {
    public ExtendedCollapsibleContainer(Sizing horizontalSizing, Sizing verticalSizing, boolean expanded) {
        super(horizontalSizing, verticalSizing, Component.empty(), expanded);
        this.margins(Insets.top(0));

        this.configure((CollapsibleContainer component) -> {
            component.horizontalAlignment(HorizontalAlignment.CENTER);

            var titleLayout = component.titleLayout();

            titleLayout.padding(Insets.of(0));

            var spinyThing = titleLayout.children().get(1);

            spinyThing.margins(Insets.of(0, 2, 2, 0))
                    .sizing(Sizing.fixed(9));

            var contentLayout = component.children().get(1);

            if(contentLayout instanceof FlowLayout contentFlow) {
                contentFlow.surface(Surface.BLANK)
                        .padding(Insets.of(0))
                        .horizontalAlignment(HorizontalAlignment.CENTER);
                        //.margins(Insets.top(-2));
            }
        });

        this.onToggled().subscribe(nowExpanded -> {
            var contentLayout = this.children().get(1);

            if(contentLayout instanceof FlowLayout contentFlow) {
                var spinyThing = titleLayout.children().get(1);

                if (nowExpanded) {
                    contentFlow.margins(Insets.top(-2));

                    spinyThing.margins(Insets.of(0,2,2,0));
                } else {
                    contentFlow.margins(Insets.top(0));

                    spinyThing.margins(Insets.of(0,2,2,0));
                }
            }
        });
    }
}
