package io.wispforest.accessories.client.gui.components;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.impl.option.PlayerOption;
import io.wispforest.accessories.impl.option.PlayerOptions;
import io.wispforest.accessories.impl.option.PlayerOptionsAccess;
import io.wispforest.accessories.mixin.client.AbstractSliderButtonAccessor;
import io.wispforest.accessories.mixin.client.owo.DiscreteSliderComponentAccessor;
import io.wispforest.accessories.networking.AccessoriesNetworking;
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
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AccessoriesScreenSettingsLayout extends FlowLayout implements PlayerOptionsAccess {

    private final PlayerOptionsAccess optionAccess;
    private final ComponentAccess componentAccess;

    private Consumer<ChangeType> onChangeCallback = changeType -> {};
    private boolean shouldNetworkSync = true;
    private boolean updateLive = false;

    private int maxWidth = 162;
    private int columnAmount = 1;

    public AccessoriesScreenSettingsLayout(PlayerOptionsAccess optionAccess, ComponentAccess componentAccess) {
        super(Sizing.content(), Sizing.content(), Algorithm.VERTICAL);

        this.optionAccess = optionAccess;
        this.componentAccess = componentAccess;

        this.buildLayout();
    }

    public AccessoriesScreenSettingsLayout onChange(Consumer<ChangeType> onChangeCallback) {
        this.onChangeCallback = onChangeCallback;

        return this;
    }

    public AccessoriesScreenSettingsLayout shouldNetworkSync(boolean value) {
        this.shouldNetworkSync = value;

        return this;
    }

    public AccessoriesScreenSettingsLayout updateLive(boolean value) {
        this.updateLive = value;

        return this;
    }

    @Override
    public <T> Optional<T> getData(PlayerOption<T> option) {
        return optionAccess.getData(option);
    }

    @Override
    public <T> void setData(PlayerOption<T> option, T data) {
        optionAccess.setData(option, data);

        if (updateLive) onHolderChange(option);
    }

    public interface ComponentAccess {
        @Nullable
        <T extends io.wispforest.owo.ui.core.Component> T getComponent(Class<T> clazz, String id);

        default <T extends io.wispforest.owo.ui.core.Component> void adjustIfPresent(Class<T> clazz, String id, Consumer<T> callback) {
            var component = getComponent(clazz, id);

            if (component != null) callback.accept(component);
        }
    }

    public enum ChangeType {
        ACCESSORIES,
        ENTITY,
        SIDE_BAR,
        SLOTS
    }

    private <R extends Record> void syncToServer(R packet) {
        if (!shouldNetworkSync) return;

        AccessoriesNetworking.sendToServer(packet);
    }

    private void buildLayout() {
        List<Component> children = new ArrayList<>();

        children.add(
                wrapAsSettings(PlayerOptions.COLUMN_AMOUNT,
                        Components.discreteSlider(Sizing.fixed(45), getMinimumColumnAmount(), getMaximumColumnAmount())
                                .configure((DiscreteSliderComponent slider) -> {
                                    slider.snap(true)
                                        .setFromDiscreteValue(this.getDefaultedData(PlayerOptions.COLUMN_AMOUNT))
                                        .scrollStep(1f / (18 - getMinimumColumnAmount()));

                                    slider.onChanged().subscribe(value -> {
                                        syncToServer(PlayerOptions.COLUMN_AMOUNT.toPacket((int) value));

                                        this.setData(PlayerOptions.COLUMN_AMOUNT, (int) value);

                                        onChangeCallback.accept(ChangeType.ACCESSORIES);
                                    });
                                })
                ));

        children.add(
                wrapAsSettings(PlayerOptions.WIDGET_TYPE,
                        Components.button(
                                        widgetTypeToggleMessage(this.getDefaultedData(PlayerOptions.WIDGET_TYPE), false),
                                        btn -> {
                                            var newWidget = this.getDefaultedData(PlayerOptions.WIDGET_TYPE) + 1;

                                            if(newWidget > 2) newWidget = 1;

                                            syncToServer(PlayerOptions.WIDGET_TYPE.toPacket(newWidget));

                                            this.setData(PlayerOptions.WIDGET_TYPE, newWidget);

                                            this.onHolderChange(PlayerOptions.WIDGET_TYPE);
                                        })
                                .renderer(ComponentUtils.getButtonRenderer())
                                .tooltip(widgetTypeToggleMessage(this.getDefaultedData(PlayerOptions.WIDGET_TYPE), true))
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.SHOW_UNUSED_SLOTS,
                        (option, newValue) -> syncToServer(option.toPacket(newValue))
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.SIDE_BY_SIDE_SLOTS,
                        (option, newValue) -> {
                            syncToServer(option.toPacket(newValue));

                            onChangeCallback.accept(ChangeType.ACCESSORIES);
                        }
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.SIDE_BY_SIDE_ENTITY,
                        (option, newValue) -> {
                            syncToServer(option.toPacket(newValue));

                            onChangeCallback.accept(ChangeType.ENTITY);
                        }
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.MAIN_WIDGET_POSITION,
                        (option, newValue) -> {
                            syncToServer(option.toPacket(newValue));

                            componentAccess.adjustIfPresent(InventoryEntityComponent.class, "entity_rendering_component", c -> c.startingRotation(newValue ? -45 : 45));
                        }
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.SHOW_GROUP_FILTER,
                        (option, newValue) -> {
                            syncToServer(option.toPacket(newValue));

                            onChangeCallback.accept(ChangeType.SIDE_BAR);
                        }
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.SIDE_WIDGET_POSITION,
                        (option, newValue) -> {
                            syncToServer(option.toPacket(newValue));

                            onChangeCallback.accept(ChangeType.ACCESSORIES);
                        }
                ));

        children.add(
                ofSettingsToggle(PlayerOptions.ENTITY_CENTERED,
                        (option, newValue) -> {
                            syncToServer(option.toPacket(newValue));

                            onChangeCallback.accept(ChangeType.ACCESSORIES);
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

                            componentAccess.adjustIfPresent(InventoryEntityComponent.class, "entity_rendering_component", c -> c.lookAtCursor(bl));
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
        return ofSettingsToggle(playerOption.name(), () -> this.getDefaultedData(playerOption), newValue -> {
            this.setData(playerOption, newValue);
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
        var hasChangeOccurred = false;

        if (option.equals(PlayerOptions.SHOW_UNUSED_SLOTS)) {
            hasChangeOccurred = true;

            updateToggleButton(PlayerOptions.SHOW_UNUSED_SLOTS, Accessories.config().screenOptions::showUnusedSlots);

            onChangeCallback.accept(ChangeType.SLOTS);
        }

        if (option.equals(PlayerOptions.SHOW_GROUP_FILTER)) {
            updateToggleButton(PlayerOptions.SHOW_GROUP_FILTER);

            hasChangeOccurred = true;
        }

        if (option.equals(PlayerOptions.MAIN_WIDGET_POSITION)) {
            updateToggleButton(PlayerOptions.MAIN_WIDGET_POSITION);

            hasChangeOccurred = true;
        }

        if (option.equals(PlayerOptions.SIDE_WIDGET_POSITION)){
            updateToggleButton(PlayerOptions.SIDE_WIDGET_POSITION);

            hasChangeOccurred = true;
        }

        var updateMaxValue = option.equals(PlayerOptions.SIDE_BY_SIDE_SLOTS)
            || (this.getDefaultedData(PlayerOptions.SIDE_BY_SIDE_SLOTS) && option.equals(PlayerOptions.SIDE_BY_SIDE_ENTITY));

        if (updateMaxValue) {
            updateMaxValueColumnSlider(getMaximumColumnAmount());

            hasChangeOccurred = true;
        }

        if(option.equals(PlayerOptions.WIDGET_TYPE)) {
            updateMinValueColumnSlider(getMinimumColumnAmount());

            updateWidgetTypeToggleButton();

            hasChangeOccurred = true;
        }

        if (hasChangeOccurred) {
            onChangeCallback.accept(ChangeType.ACCESSORIES);
        }
    }

    public void updateMaxValueColumnSlider(int maxValue) {
        updateColumnSlider(null, maxValue);
    }

    public void updateMinValueColumnSlider(int minValue) {
        updateColumnSlider(minValue, null);
    }

    public void updateColumnSlider(@Nullable Integer minValue, @Nullable Integer maxValue) {
        var columnAmountSlider = this.childById(DiscreteSliderComponent.class, PlayerOptions.COLUMN_AMOUNT.name());

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
                this.setData(PlayerOptions.COLUMN_AMOUNT, (int) Math.round(newValue));
            }
        }
    }

    private boolean updateToggleButton(PlayerOption<Boolean> playerOption) {
        return updateToggleButton(playerOption, bl -> {});
    }

    private boolean updateToggleButton(PlayerOption<Boolean> playerOption, Consumer<Boolean> runnable) {
        return updateToggleButton(playerOption.name(), () -> this.getDefaultedData(playerOption), runnable);
    }

    private boolean updateToggleButton(String baseId, Supplier<Boolean> getter, Consumer<Boolean> runnable) {
        var btn = this.childById(ButtonComponent.class, baseId);

        var value = getter.get();

        btn.setMessage(createToggleText(baseId, false, value));
        btn.tooltip(createToggleText(baseId, true, value));

        runnable.accept(value);

        return true;
    }

    private void updateWidgetTypeToggleButton() {
        var btn = this.childById(ButtonComponent.class, "widget_type");

        var value = this.getDefaultedData(PlayerOptions.WIDGET_TYPE);

        btn.setMessage(widgetTypeToggleMessage(value, false));
        btn.tooltip(widgetTypeToggleMessage(value, true));
    }

    public int getMinimumColumnAmount() {
        return (this.getDefaultedData(PlayerOptions.WIDGET_TYPE) == 2) ? 1 : 3;
    }

    public int getMaximumColumnAmount() {
        return (this.getDefaultedData(PlayerOptions.SIDE_BY_SIDE_SLOTS)) ? (this.getDefaultedData(PlayerOptions.SIDE_BY_SIDE_ENTITY) ? 4 : 6) : 9;
    }
}
