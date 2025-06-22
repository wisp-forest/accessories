package io.wispforest.accessories.client.gui.components;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.accessories.impl.option.PlayerOptions;
import io.wispforest.accessories.mixin.client.AbstractSliderButtonAccessor;
import io.wispforest.accessories.mixin.client.owo.DiscreteSliderComponentAccessor;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.impl.option.PlayerOption;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

public class AccessoriesScreenSettingsLayout extends FlowLayout {

    private final AccessoriesExperimentalScreen screen;

    private int maxWidth = 162;
    private int columnAmount = 1;

    public AccessoriesScreenSettingsLayout(AccessoriesExperimentalScreen screen) {
        super(Sizing.content(), Sizing.content(), Algorithm.VERTICAL);

        this.screen = screen;

        this.buildLayout();
    }

    private void buildLayout() {
        List<Component> children = new ArrayList<>();

        children.add(
                wrapAsSettings(PlayerOptions.COLUMN_AMOUNT,
                        Components.discreteSlider(Sizing.fixed(45), getMinimumColumnAmount(), getMaximumColumnAmount())
                                .configure((DiscreteSliderComponent slider) -> {
                                    slider.onChanged().subscribe(value -> {
                                        AccessoriesNetworking.sendToServer(PlayerOptions.COLUMN_AMOUNT.toPacket((int) value));

                                        this.screen.setOption(PlayerOptions.COLUMN_AMOUNT, (int) value);

                                        screen.rebuildAccessoriesComponent();
                                    });
                                })
                                .snap(true)
                                .setFromDiscreteValue(this.screen.getOption(PlayerOptions.COLUMN_AMOUNT))
                                .scrollStep(1f / (18 - getMinimumColumnAmount()))
                ));

        children.add(
                wrapAsSettings(PlayerOptions.WIDGET_TYPE,
                        Components.button(
                                        widgetTypeToggleMessage(this.screen.getOption(PlayerOptions.WIDGET_TYPE), false),
                                        btn -> {
                                            var newWidget = this.screen.getOption(PlayerOptions.WIDGET_TYPE) + 1;

                                            if(newWidget > 2) newWidget = 1;

                                            AccessoriesNetworking.sendToServer(PlayerOptions.WIDGET_TYPE.toPacket(newWidget));

                                            this.screen.setOption(PlayerOptions.WIDGET_TYPE, newWidget);

                                            this.onHolderChange(PlayerOptions.WIDGET_TYPE);
                                        })
                                .renderer(ComponentUtils.getButtonRenderer())
                                .tooltip(widgetTypeToggleMessage(this.screen.getOption(PlayerOptions.WIDGET_TYPE), true))
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.SHOW_UNUSED_SLOTS,
                        (option, newValue) -> AccessoriesNetworking.sendToServer(option.toPacket(newValue))
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.SIDE_BY_SIDE_SLOTS,
                        (option, newValue) -> {
                            AccessoriesNetworking.sendToServer(option.toPacket(newValue));

                            screen.rebuildAccessoriesComponent();
                        }
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.SIDE_BY_SIDE_ENTITY,
                        (option, newValue) -> {
                            AccessoriesNetworking.sendToServer(option.toPacket(newValue));

                            screen.rebuildEntityComponent();
                        }
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.MAIN_WIDGET_POSITION,
                        (option, newValue) -> {
                            AccessoriesNetworking.sendToServer(option.toPacket(newValue));

                            this.screen.component(InventoryEntityComponent.class, "entity_rendering_component")
                                    .startingRotation(newValue ? -45 : 45);
                        }
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.SHOW_GROUP_FILTER,
                        (option, newValue) -> {
                            AccessoriesNetworking.sendToServer(option.toPacket(newValue));

                            screen.rebuildSideBarOptions();
                        }
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.SIDE_WIDGET_POSITION,
                        (option, newValue) -> {
                            AccessoriesNetworking.sendToServer(option.toPacket(newValue));

                            screen.rebuildAccessoriesComponent();
                        }
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.ENTITY_CENTERED,
                        (option, newValue) -> {
                            AccessoriesNetworking.sendToServer(option.toPacket(newValue));

                            screen.rebuildAccessoriesComponent();
                        }
                ));

        children.add(
                ofSettingsToggle("dark_mode_toggle",
                        () -> Accessories.config().screenOptions.isDarkMode(),
                        bl -> Accessories.config().screenOptions.isDarkMode(bl)
                ));

        children.add(
                ofSettingsToggle("show_equipped_stack_slot_type",
                        () -> Accessories.config().screenOptions.showEquippedStackSlotType(),
                        bl -> Accessories.config().screenOptions.showEquippedStackSlotType(bl)
                ));

        children.add(
                ofSettingsToggle("entity_look_at_cursor",
                        () -> Accessories.config().screenOptions.entityLooksAtMouseCursor(),
                        bl -> {
                            Accessories.config().screenOptions.entityLooksAtMouseCursor(bl);

                            var component = this.screen.component(InventoryEntityComponent.class, "entity_rendering_component");

                            component.lookAtCursor(bl);
                        }
                ));

        var baseOptionPanel = Containers.grid(Sizing.fixed(maxWidth), Sizing.content(), (int) Math.ceil(children.size() / (float) columnAmount), columnAmount)
                .configure((GridLayout component) -> {
                    component
                            .verticalAlignment(VerticalAlignment.CENTER)
                            .horizontalAlignment(HorizontalAlignment.CENTER)
                            .padding(Insets.horizontal(3).withTop(1).withBottom(3));
                });

        for (int i = 0; i < children.size(); i++) {
            var row = i / columnAmount;
            var column = i % columnAmount;

            var child = children.get(i);

            baseOptionPanel.child(child, row, column);

            if (!(i + columnAmount >= children.size())) {
                child.margins(Insets.bottom(2));
            } else if (columnAmount > 1 && i + 1 >= children.size() && column == 0) {
                baseOptionPanel.child(createBaseParent(), row, 1);
            }
        }

        this.child(Containers.verticalScroll(Sizing.expand(), Sizing.expand(), baseOptionPanel));
    }

    private int getEntryWidth() {
        return (maxWidth - 14) / columnAmount;
    }

    private Component ofSettingsToggle(PlayerOption<Boolean> playerOption, BiConsumer<PlayerOption<Boolean>, Boolean> onChange) {
        return ofSettingsToggle(playerOption.name(), () -> this.screen.getOption(playerOption), newValue -> {
            this.screen.setOption(playerOption, newValue);
            onChange.accept(playerOption, newValue);
        });
    }

    private Component ofSettingsToggle(String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return wrapAsSettings(
                name,
                Components.button(
                                createToggleText(name, false, getter.get()),
                                btn -> {
                                    var newValue = !getter.get();

                                    btn.setMessage(createToggleText(name, false, newValue));
                                    btn.tooltip(createToggleText(name, true, newValue));

                                    setter.accept(newValue);
                                })
                        .renderer(ComponentUtils.getButtonRenderer())
                        .tooltip(createToggleText(name, true, getter.get()))
        );
    }

    private Component wrapAsSettings(PlayerOption<?> playerOption, Component component) {
        return wrapAsSettings(playerOption.name(), component);
    }

    private Component wrapAsSettings(String name, Component component) {
        return createBaseParent()
                .gap(0)
                .child(Components.label(Accessories.translation(name + ".label")).margins(Insets.left(1)))
                .child(component.id(name).horizontalSizing(Sizing.fixed(getEntryWidth())).verticalSizing(Sizing.fixed(14)));
    }

    private FlowLayout createBaseParent() {
        return Containers.verticalFlow(Sizing.fixed(getEntryWidth()), Sizing.fixed(23));
    }

    private static net.minecraft.network.chat.Component widgetTypeToggleMessage(int value, boolean isTooltip) {
        var type = value == 2 ? "scrollable" : "paginated";

        return Accessories.translation("widget_type." + type + (isTooltip ? ".tooltip" : ""));
    }

    private static net.minecraft.network.chat.Component createToggleText(String type, boolean isTooltip, boolean value) {
        return Accessories.translation(type + ".toggle." + (value ? "enabled" : "disabled") + (isTooltip ? ".tooltip" : ""));
    }

    //--

    public void onHolderChange(PlayerOption<?> option) {
        if (option.equals(PlayerOptions.SHOW_UNUSED_SLOTS)) {
            updateToggleButton(PlayerOptions.SHOW_UNUSED_SLOTS, (bl) -> {
                screen.getMenu().updateUsedSlots();

                Accessories.config().screenOptions.showUnusedSlots(bl);

                screen.rebuildAccessoriesComponent();
            });
        }

        if (option.equals(PlayerOptions.SHOW_GROUP_FILTER))
            updateToggleButton(PlayerOptions.SHOW_GROUP_FILTER, screen::rebuildAccessoriesComponent);
        if (option.equals(PlayerOptions.MAIN_WIDGET_POSITION))
            updateToggleButton(PlayerOptions.MAIN_WIDGET_POSITION, screen::rebuildAccessoriesComponent);
        if (option.equals(PlayerOptions.SIDE_WIDGET_POSITION))
            updateToggleButton(PlayerOptions.SIDE_WIDGET_POSITION, screen::rebuildAccessoriesComponent);

        var updateMaxValue = (option.equals(PlayerOptions.ENTITY_CENTERED) && this.screen.getOption(PlayerOptions.SIDE_BY_SIDE_SLOTS))
                || (option.equals(PlayerOptions.SIDE_BY_SIDE_SLOTS) && this.screen.getOption(PlayerOptions.ENTITY_CENTERED))
                || (option.equals(PlayerOptions.SIDE_BY_SIDE_ENTITY) && this.screen.getOption(PlayerOptions.SIDE_BY_SIDE_SLOTS));

        if (updateMaxValue) {
            updateMaxValueColumnSlider(getMaximumColumnAmount());

            screen.rebuildAccessoriesComponent();
        }

        if(option.equals(PlayerOptions.WIDGET_TYPE)) {
            updateMinValueColumnSlider(getMinimumColumnAmount());

            updateWidgetTypeToggleButton();
        }
    }

    public void updateMaxValueColumnSlider(int maxValue) {
        updateColumnSlider(null, maxValue);
    }

    public void updateMinValueColumnSlider(int minValue) {
        updateColumnSlider(minValue, null);
    }

    public void updateColumnSlider(@Nullable Integer minValue, @Nullable Integer maxValue) {
        var columnAmountSlider = this.screen.component(DiscreteSliderComponent.class, "column_amount");

        if(columnAmountSlider != null) {
            var previousValue = columnAmountSlider.discreteValue();

            var accessor = ((DiscreteSliderComponentAccessor) columnAmountSlider);

            if (minValue != null) {
                accessor.accessories$setMin(minValue);
            } else {
                minValue = (int) Math.round(columnAmountSlider.min());
            }

            if (maxValue != null) {
                accessor.accessories$setMax(maxValue);
            } else {
                maxValue = (int) Math.round(columnAmountSlider.max());
            }

            ((AbstractSliderButtonAccessor) columnAmountSlider).accessories$setValue(-1);

            columnAmountSlider.setFromDiscreteValue(Math.min(Math.max((int) Math.round(previousValue), minValue), maxValue));

            var newValue = columnAmountSlider.discreteValue();

            if (newValue != previousValue) {
                this.screen.setOption(PlayerOptions.COLUMN_AMOUNT, (int) Math.round(newValue));
            }
        }
    }

    private void updateToggleButton(PlayerOption<Boolean> playerOption, Consumer<Boolean> runnable) {
        updateToggleButton(playerOption.name(), () -> this.screen.getOption(playerOption), runnable);
    }

    private void updateToggleButton(PlayerOption<Boolean> playerOption, Runnable runnable) {
        updateToggleButton(playerOption.name(), () -> this.screen.getOption(playerOption), bl -> runnable.run());
    }

    private void updateToggleButton(String baseId, Supplier<Boolean> getter, Runnable runnable) {
        updateToggleButton(baseId, getter, bl -> runnable.run());
    }

    private void updateToggleButton(String baseId, Supplier<Boolean> getter, Consumer<Boolean> runnable) {
        var btn = this.screen.component(ButtonComponent.class, baseId);

        var value = getter.get();

        btn.setMessage(createToggleText(baseId, false, value));
        btn.tooltip(createToggleText(baseId, true, value));

        runnable.accept(value);
    }

    private void updateWidgetTypeToggleButton() {
        var btn = this.screen.component(ButtonComponent.class, "widget_type");

        var value = this.screen.getOption(PlayerOptions.WIDGET_TYPE);

        btn.setMessage(widgetTypeToggleMessage(value, false));
        btn.tooltip(widgetTypeToggleMessage(value, true));

        screen.rebuildAccessoriesComponent();
    }

    public int getMinimumColumnAmount() {
        return (this.screen.getOption(PlayerOptions.WIDGET_TYPE) == 2) ? 1 : 3;
    }

    public int getMaximumColumnAmount() {
        return (this.screen.getOption(PlayerOptions.SIDE_BY_SIDE_SLOTS)) ? (this.screen.getOption(PlayerOptions.SIDE_BY_SIDE_ENTITY) ? 4 : 6) : 9;
    }
}
