package io.wispforest.accessories.client.gui.components;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.impl.option.PlayerOptions;
import io.wispforest.owo.ui.base.BaseParentComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import org.apache.commons.lang3.IntegerRange;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static io.wispforest.accessories.client.gui.components.ComponentUtils.BACKGROUND_SLOT_RENDERING_SURFACE;

public class ScrollableAccessoriesLayout extends AccessoriesContainingLayout<ScrollableAccessoriesLayout.RawScrollData> {

    private IntegerRange alternativeCheckRange;

    protected ScrollableAccessoriesLayout(AccessoriesScreen screen, RawScrollData data) {
        super(screen, data);

        this.alternativeCheckRange = IntegerRange.of(0, Math.min(data.maxRowCount, data.totalRowCount));
    }

    @Override
    public int getCurrentWidth() {
        return super.getCurrentWidth() + (layoutData.showScrollbar() ? 11 : 0);
    }

    @Override
    public int getMaxPossibleWidth() {
        return super.getMaxPossibleWidth() + 11;
    }

    //--

    @Nullable
    public static ScrollableAccessoriesLayout createOrNull(AccessoriesScreen screen) {
        var data = buildPages(screen);

        if(data == null) return null;

        return new ScrollableAccessoriesLayout(screen, data);
    }

    @Nullable
    private static ScrollableAccessoriesLayout.RawScrollData buildPages(AccessoriesScreen screen) {
        var menu = screen.getMenu();
        var slots = menu.getVisibleAccessoriesSlots();

        var sideBySide = screen.getDefaultedData(PlayerOptions.SIDE_BY_SIDE_SLOTS);

        var maxColumnCount = screen.getDefaultedData(PlayerOptions.COLUMN_AMOUNT);
        var maxRowCount = 7;

        var totalRowCount = (int) Math.ceil(((slots.size()) / 2f) / maxColumnCount);

        if (totalRowCount <= 0) return null;

        //--

        var baseLayoutGroup = createBaseLayoutGroup(screen, slots, totalRowCount, maxColumnCount, 0, sideBySide);

        baseLayoutGroup.accessoriesLayout().surface(BACKGROUND_SLOT_RENDERING_SURFACE);
        baseLayoutGroup.cosmeticLayout().surface(BACKGROUND_SLOT_RENDERING_SURFACE);

        return new RawScrollData(baseLayoutGroup, totalRowCount, maxColumnCount, maxRowCount, (maxColumnCount - baseLayoutGroup.totalColumnCount()) * 18, sideBySide);
    }

    public record RawScrollData(BaseLayoutGroup baseLayoutGroup, int totalRowCount, int maxColumnCount, int maxRowCount, int basePadding, boolean sideBySide) implements LayoutData {
        public boolean showScrollbar() {
            return totalRowCount > maxRowCount;
        }

        @Override
        public int width() {
            return LayoutData.super.width();
        }
    }

    //--

    @Override
    protected BaseLayoutGroup getCurrentGroup() {
        return this.layoutData.baseLayoutGroup();
    }

    @Override
    protected void buildLayout(RawScrollData layoutData) {
        var minimumLayoutHeight = (layoutData.height()) /* + (this.slotPages.size() > 1 ? (3 + 20) : 0)*/ + (2 * 6);

        var baseGroupData = layoutData.baseLayoutGroup();

        FlowLayout fullLayout;

        if (layoutData.sideBySide()) {
            fullLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content()).gap(3);

            fullLayout.id("side_by_side_holder");

            if (screen.showCosmeticState()) {
                fullLayout.child(baseGroupData.cosmeticLayout());
            }

            fullLayout.child(baseGroupData.accessoriesLayout());
        } else {
            fullLayout = screen.showCosmeticState() ? baseGroupData.cosmeticLayout() : baseGroupData.accessoriesLayout();
        }

        var paddingValue = (layoutData.showScrollbar() ? 3 : 0);

        fullLayout.padding(this.screen.getDefaultedData(PlayerOptions.MAIN_WIDGET_POSITION) ? Insets.left(paddingValue) : Insets.right(paddingValue));

        Component innerAccessoriesLayout;

        var width = layoutData.width();

        if (screen.showCosmeticState() && layoutData.sideBySide()) width = (width * 2) + 3;

        if(layoutData.showScrollbar()) {
            innerAccessoriesLayout = new ExtendedScrollContainer<>(ScrollContainer.ScrollDirection.VERTICAL, Sizing.fixed(width + 8 + 3), Sizing.fixed(layoutData.height()), fullLayout)
                    .strictMouseScrolling(!Accessories.config().screenOptions.allowSlotScrolling())
                    .oppositeScrollbar(this.screen.getDefaultedData(PlayerOptions.MAIN_WIDGET_POSITION))
                    .scrolledToCallback((container, prevOffset, scrollOffset) -> {
                        if(Objects.equals(prevOffset, scrollOffset)) return;

                        int rowIndex;

                        if(prevOffset - scrollOffset < 0) {
                            rowIndex = (int) Math.floor(scrollOffset / 18);
                        } else {
                            rowIndex = (int) Math.ceil(scrollOffset / 18);
                        }

                        var maximumRangeValue = Math.min(rowIndex + layoutData.maxRowCount(), layoutData.totalRowCount()) - 1;

                        this.alternativeCheckRange = IntegerRange.of(rowIndex, maximumRangeValue);
                    })
                    .scrollbarThiccness(8)
                    .scrollbar(ScrollContainer.Scrollbar.vanilla())
                    .fixedScrollbarLength(16)
                    .scrollStep(18)
                    .scrollbar(ComponentUtils.getScrollbarRenderer())
                    .id("accessories_scroll_container");
        } else {
            innerAccessoriesLayout = Containers.verticalFlow(Sizing.content(), Sizing.content()).child(fullLayout);
        }

        innerAccessoriesLayout
                .id("inner_accessories_container");

        var accessoriesMainLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(3)
                .child(innerAccessoriesLayout)
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .surface(ComponentUtils.getPanelSurface())
                .padding(Insets.of(7))
                .id("accessories_layout");

        if (layoutData.showScrollbar()) {
            this.child(accessoriesMainLayout);

            return;
        }

        this.sizing(Sizing.content(), Sizing.fixed(minimumLayoutHeight));

        this.child(accessoriesMainLayout);
        this.child(Containers.verticalFlow(Sizing.content(), Sizing.expand()));
    }

    @Override
    public void onCosmeticToggle(boolean showCosmeticState) {
        var baseGroupData = layoutData.baseLayoutGroup();

        if (this.layoutData.sideBySide) {
            if (showCosmeticState) {
                ParentComponent newLayout = baseGroupData.cosmeticLayout();

                for (int i = 0; i < layoutData.totalRowCount(); i++) {
                    var newRow = newLayout.childById(ParentComponent.class, "row_" + i);

                    ComponentUtils.recursiveSearchSlots(newRow, (slotComponent) -> {
                        this.screen.enableSlot(slotComponent.slot());
                    });
                }

                screen.component(FlowLayout.class, "side_by_side_holder").child(0, newLayout);
            } else {
                ParentComponent prevLayout = baseGroupData.cosmeticLayout();

                for (int i = 0; i < layoutData.totalRowCount(); i++) {
                    var oldRow = prevLayout.childById(ParentComponent.class, "row_" + i);

                    ComponentUtils.recursiveSearchSlots(oldRow, (slotComponent) -> {
                        this.screen.disableSlot(slotComponent.slot());
                        this.screen.hideSlot(slotComponent.slot());
                    });
                }

                prevLayout.parent().removeChild(prevLayout);
            }

            var scrollContainer = this.childById(ParentComponent.class, "inner_accessories_container");

            if (scrollContainer instanceof ExtendedScrollContainer) {
                var width = layoutData.width();

                if (showCosmeticState) width = (width * 2) + 3;

                scrollContainer.horizontalSizing(Sizing.fixed(width + 8 + 3));
            }

            screen.setupPadding();
        } else {
            var container = this.childById(BaseParentComponent.class, "inner_accessories_container");

            Consumer<FlowLayout> childSetter;

            if(container instanceof ExtendedScrollContainer<?> scrollContainer){
                childSetter = layout -> ((ExtendedScrollContainer<FlowLayout>) scrollContainer).child(layout);
            } else if(container instanceof FlowLayout flowLayout) {
                childSetter = component -> {
                    flowLayout.clearChildren();

                    flowLayout.child(component);
                };
            } else {
                return;
            }

            ParentComponent prevLayout = showCosmeticState ? baseGroupData.accessoriesLayout() : baseGroupData.cosmeticLayout();

            for (int i = 0; i < layoutData.totalRowCount(); i++) {
                var oldRow = prevLayout.childById(ParentComponent.class, "row_" + i);

                ComponentUtils.recursiveSearchSlots(oldRow, (slotComponent) -> {
                    this.screen.disableSlot(slotComponent.slot());
                });
            }

            var newLayout = showCosmeticState ? baseGroupData.cosmeticLayout() : baseGroupData.accessoriesLayout();

            var paddingValue = (layoutData.showScrollbar() ? 3 : 0);

            newLayout.padding(this.screen.getDefaultedData(PlayerOptions.MAIN_WIDGET_POSITION) ? Insets.left(paddingValue) : Insets.right(paddingValue));

            childSetter.accept(newLayout);

            for (int i = 0; i < layoutData.totalRowCount(); i++) {
                var newRow = newLayout.childById(ParentComponent.class, "row_" + i);

                ComponentUtils.recursiveSearchSlots(newRow, (slotComponent) -> {
                    this.screen.enableSlot(slotComponent.slot());
                });
            }
        }

        updatePadding();
    }

    @Override
    protected Iterable<PositionedRectangle> getAlternativeChecks(boolean showingCosmetics) {
        var layoutGroup = this.layoutData.baseLayoutGroup();

        var min = this.alternativeCheckRange.getMinimum();
        var max = this.alternativeCheckRange.getMaximum();

        return getSubList(layoutGroup.getAlternativeChecks(!layoutData.sideBySide && showingCosmetics), min, max);
    }

    private <T> List<T> getSubList(List<T> list, int fromIndex, int toIndex) {
        if (toIndex >= list.size() || fromIndex == toIndex) return List.of();

        return list.subList(fromIndex, toIndex);
    }
}
