package io.wispforest.accessories.client.gui;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.gui.components.PixelPerfectTextureComponent;
import io.wispforest.accessories.compat.config.ScreenType;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ScreenVariantSelectionScreen extends BaseOwoScreen<FlowLayout> {

    private ScreenType screenType = ScreenType.NONE;

    private final Consumer<AccessoriesMenuVariant> variantConsumer;

    public ScreenVariantSelectionScreen(Consumer<AccessoriesMenuVariant> variantConsumer) {
        this.variantConsumer = variantConsumer;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.child(
                Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(
                                Containers.verticalFlow(Sizing.fixed(262), Sizing.content())
                                        .child(
                                                Containers.verticalFlow(Sizing.content(), Sizing.content())
                                                        .child(
                                                                Components.label(Accessories.translation("selection_screen.message"))
                                                                        .horizontalTextAlignment(HorizontalAlignment.CENTER)
                                                        )
                                                        .horizontalAlignment(HorizontalAlignment.CENTER)
                                                        .padding(Insets.of(3))
                                        )
                                        .child(
                                                Components.box(Sizing.fixed(256), Sizing.fixed(1))
                                                        .margins(Insets.vertical(2))
                                        )
                                        .child(
                                                Containers.horizontalScroll(Sizing.fixed(256), Sizing.content(),
                                                        Containers.horizontalFlow(Sizing.content(), Sizing.content())
                                                                .child(
                                                                        Containers.verticalFlow(Sizing.fixed(120), Sizing.fixed(110))
                                                                                .child(
                                                                                        Components.button(Component.literal("Original"), btn -> {
                                                                                                    this.screenType = ScreenType.ORIGINAL;
                                                                                                    setConfigAndOpenScreen();
                                                                                                })
                                                                                                .horizontalSizing(Sizing.fixed(80))
                                                                                                .verticalSizing(Sizing.fixed(16))
                                                                                                .margins(Insets.of(1))
                                                                                )
                                                                                .child(
                                                                                        new PixelPerfectTextureComponent(
                                                                                                Accessories.of("textures/gui/original_gui_image_crop.png"),
                                                                                                992,
                                                                                                709,
                                                                                                Sizing.fixed(992 / 9),
                                                                                                Sizing.fixed(709 / 9)
                                                                                        )
                                                                                )
                                                                                .margins(Insets.of(3))
                                                                                .padding(Insets.of(3))
                                                                                .surface(Surface.outline(Color.BLACK.interpolate(Color.WHITE, 0.1f).argb()))
                                                                                .horizontalAlignment(HorizontalAlignment.CENTER)
                                                                ).child(
                                                                        Containers.verticalFlow(Sizing.fixed(120), Sizing.fixed(110))
                                                                                .child(
                                                                                        Components.button(Component.literal("Experimental"), btn -> {
                                                                                                    this.screenType = ScreenType.EXPERIMENTAL_V1;
                                                                                                    setConfigAndOpenScreen();
                                                                                                })
                                                                                                .horizontalSizing(Sizing.fixed(80))
                                                                                                .verticalSizing(Sizing.fixed(16))
                                                                                                .margins(Insets.of(1))
                                                                                )
                                                                                .child(
                                                                                        new PixelPerfectTextureComponent(
                                                                                                Accessories.of("textures/gui/new_gui_image_crop.png"),
                                                                                                1185,
                                                                                                1373,
                                                                                                Sizing.fixed(1185 / 16),
                                                                                                Sizing.fixed(1373 / 16)
                                                                                        )
                                                                                )
                                                                                .margins(Insets.of(3))
                                                                                .padding(Insets.of(3))
                                                                                .surface(Surface.outline(Color.BLACK.interpolate(Color.WHITE, 0.1f).argb()))
                                                                                .horizontalAlignment(HorizontalAlignment.CENTER)
                                                                ).gap(3)
                                                )
                                        )
                                        .horizontalAlignment(HorizontalAlignment.CENTER)
                                        .surface(Surface.PANEL_INSET)
                                        .padding(Insets.of(3))
                        )
                        .gap(3)
                        .surface(Surface.PANEL)
                        .padding(Insets.of(6))
                        .positioning(Positioning.relative(50, 50))
        );

        rootComponent.surface(Surface.VANILLA_TRANSLUCENT);
    }

    private void setConfigAndOpenScreen() {
        Accessories.config().screenOptions.selectedScreenType(this.screenType);

        if(screenType != ScreenType.NONE) this.variantConsumer.accept(AccessoriesMenuVariant.getVariant(this.screenType));

        this.onClose();
    }
}
