package io.wispforest.accessories.compat.config.client.components;


import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.ui.component.ListOptionContainer;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.parsing.UIModel;
import io.wispforest.owo.ui.util.UISounds;
import io.wispforest.owo.util.ReflectionUtils;
import net.minecraft.ChatFormatting;

public class StructListOptionContainer<T> extends ListOptionContainer<T> {

    private final UIModel uiModel;

    public StructListOptionContainer(UIModel uiModel, Option option) {
        super(option);

        this.uiModel = uiModel;

        this.refreshOptions();
    }

    @Override
    protected void refreshOptions() {
        if (uiModel == null) return;

        this.collapsibleChildren.clear();

        var listType = (Class<T>) ReflectionUtils.getTypeArgument(this.backingOption.backingField().field().getGenericType(), 0);
        for (int i = 0; i < this.backingList.size(); i++) {
            var container = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            container.verticalAlignment(VerticalAlignment.CENTER);

            int optionIndex = i;
            final var label = Components.label(TextOps.withFormatting("- ", ChatFormatting.GRAY));

            label.margins(Insets.left(6)); //10

            if (!this.backingOption.detached()) {
                label.cursorStyle(CursorStyle.HAND);
                label.mouseEnter().subscribe(() -> label.text(TextOps.withFormatting("x ", ChatFormatting.GRAY)));
                label.mouseLeave().subscribe(() -> label.text(TextOps.withFormatting("- ", ChatFormatting.GRAY)));
                label.mouseDown().subscribe((mouseX, mouseY, button) -> {
                    this.backingList.remove(optionIndex);
                    this.refreshResetButton();
                    this.refreshOptions();
                    UISounds.playInteractionSound();

                    return true;
                });
            }

            var option = backingList.get(i);

            if (option instanceof String) {
                backingList.set(i, (T) ConfigurableStructLayout.ReflectOps.defaultConstruct(listType));
            }

            var labelContainer = Containers.verticalFlow(Sizing.fixed(19), Sizing.content());

            labelContainer.child(label);

            container.child(labelContainer);
            container.child(ConfigurableStructLayout.of(listType, backingList.get(i), uiModel, this.backingOption, true));

            this.collapsibleChildren.add(container);
        }

        this.contentLayout.<FlowLayout>configure(layout -> {
            layout.clearChildren();
            if (this.expanded) layout.children(this.collapsibleChildren);
        });

        this.refreshResetButton();
    }
}
