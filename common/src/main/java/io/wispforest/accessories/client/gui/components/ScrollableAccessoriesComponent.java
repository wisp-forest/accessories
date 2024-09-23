package io.wispforest.accessories.client.gui.components;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.owo.ui.base.BaseParentComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ScrollableAccessoriesComponent extends FlowLayout implements AccessoriesContainingComponent {

    private final int totalRowCount;
    private final int maxRowCount;

    private final FlowLayout fullAccessoriesLayout;
    private final FlowLayout fullCosmeticsLayout;

    private final List<PositionedRectangle> alternativeButtonChecks;

    private final AccessoriesExperimentalScreen screen;

    private final boolean showScrollbar;

    protected ScrollableAccessoriesComponent(AccessoriesExperimentalScreen screen, Data componentData) {
        super(Sizing.content(), Sizing.content(), Algorithm.VERTICAL);

        this.setupID();

        this.screen = screen;

        this.totalRowCount = componentData.totalRowCount();
        this.maxRowCount = componentData.maxRowCount();

        this.fullAccessoriesLayout = componentData.fullAccessoriesLayout();
        this.fullCosmeticsLayout = componentData.fullCosmeticsLayout();

        this.alternativeButtonChecks = componentData.alternativeButtonChecks();

        this.showScrollbar = componentData.showScrollbar();

        var minimumDimensions = componentData.minimumDimensions();

        this.build(minimumDimensions.x, minimumDimensions.y, componentData.showScrollbar());
    }

    @Nullable
    public static ScrollableAccessoriesComponent createOrNull(AccessoriesExperimentalScreen screen) {
        var data = buildPages(screen);

        if(data == null) return null;

        return new ScrollableAccessoriesComponent(screen, data);
    }

    public record Data(FlowLayout fullAccessoriesLayout, FlowLayout fullCosmeticsLayout, int totalRowCount, int maxRowCount, List<PositionedRectangle> alternativeButtonChecks, Vector2i minimumDimensions, Boolean showScrollbar) {}

    @Nullable
    private static Data buildPages(AccessoriesExperimentalScreen screen) {
        var menu = screen.getMenu();

        var slots = menu.getVisibleAccessoriesSlots();

        var gridSize = 7;

        var maxColumnCount = menu.owner().accessoriesHolder().columnAmount();
        var maxRowCount = gridSize;

        var totalRowCount = (int) Math.ceil(((slots.size()) / 2f) / maxColumnCount);

        if (totalRowCount <= 0) return null;

        var minimumWidth = maxColumnCount * 18;
        var minimumHeight = maxRowCount * 18;

        var fullAccessoriesLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .id("accessories_container");

        var fullCosmeticsLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .id("cosmetics_container");

        var alternativeButtonChecks = new ArrayList<PositionedRectangle>();

        for (int row = 0; row < totalRowCount; row++) {
            var colStartingIndex = (row * (maxColumnCount * 2));

            var accessoriesRowLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .surface(screen.FULL_SLOT_RENDERING)
                    .id("row_" + row);

            var cosmeticRowLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .surface(screen.FULL_SLOT_RENDERING)
                    .id("row_" + row);

            var rowButtons = new ArrayList<PositionedRectangle>();

            var overMaxSlots = false;

            for (int col = 0; col < maxColumnCount; col++) {
                var cosmetic = colStartingIndex + (col * 2);
                var accessory = cosmetic + 1;

                if (accessory >= slots.size() || cosmetic >= slots.size()) {
                    overMaxSlots = true;

                    break;
                }

                var cosmeticSlot = (AccessoriesBasedSlot) slots.get(cosmetic);
                var accessorySlot = (AccessoriesBasedSlot) slots.get(accessory);

                screen.hideSlot(cosmeticSlot);
                screen.hideSlot(accessorySlot);

                screen.enableSlot(screen.showCosmeticState() ? cosmeticSlot : accessorySlot);
                screen.disableSlot(screen.showCosmeticState() ? accessorySlot : cosmeticSlot);

                //--

                var accessoryComponentData = ComponentUtils.slotAndToggle(accessorySlot, screen::slotAsComponent);

                accessoriesRowLayout.child(accessoryComponentData.first());

                if (row < maxRowCount && screen.showCosmeticState()) rowButtons.add(accessoryComponentData.second());

                //--

                var cosmeticComponentData = ComponentUtils.slotAndToggle(cosmeticSlot, screen::slotAsComponent);

                cosmeticRowLayout.child(cosmeticComponentData.first());

                if (row < maxRowCount && !screen.showCosmeticState()) rowButtons.add(cosmeticComponentData.second());
            }

            if(!rowButtons.isEmpty()) {
                alternativeButtonChecks.add(CollectedPositionedRectangle.of(screen.showCosmeticState() ? cosmeticRowLayout : accessoriesRowLayout, rowButtons));
            }

            fullAccessoriesLayout.child(accessoriesRowLayout);
            fullCosmeticsLayout.child(cosmeticRowLayout);

            if(overMaxSlots) break;
        }

        return new Data(fullAccessoriesLayout, fullCosmeticsLayout, totalRowCount, maxRowCount, alternativeButtonChecks, new Vector2i(minimumWidth, minimumHeight), totalRowCount > 8);
    }

    public void build(int minimumWidth, int minimumHeight, boolean showScrollBar) {
        var minimumLayoutHeight = (minimumHeight) /* + (this.slotPages.size() > 1 ? (3 + 20) : 0)*/ + (2 * 6);

        var fullLayout = screen.showCosmeticState() ? fullCosmeticsLayout : fullAccessoriesLayout;

        var paddingValue = (showScrollBar ? 3 : 0);

        fullLayout.padding(this.screen.mainWidgetPosition() ? Insets.left(paddingValue) : Insets.right(paddingValue));

        BaseParentComponent innerAccessoriesLayout;

        if(showScrollBar) {
            innerAccessoriesLayout = new ExtendedScrollContainer<>(ScrollContainer.ScrollDirection.VERTICAL, Sizing.fixed(minimumWidth + 8 + 3), Sizing.fixed(minimumHeight), fullLayout)
                    .strictMouseScrolling(!Accessories.getConfig().clientData.allowSlotScrolling)
                    .oppositeScrollbar(this.screen.mainWidgetPosition())
                    .scrolledToCallback((container, prevOffset, scrollOffset) -> {
                        if(Objects.equals(prevOffset, scrollOffset)) return;

                        int rowIndex;

                        if(prevOffset - scrollOffset < 0) {
                            rowIndex = (int) Math.floor(scrollOffset / 18);
                        } else {
                            rowIndex = (int) Math.ceil(scrollOffset / 18);
                        }

                        if(container.child() instanceof FlowLayout layout) {
                            this.alternativeButtonChecks.clear();

                            var maxRange = Math.min(rowIndex + this.maxRowCount, this.totalRowCount - 1);

                            for (int i = rowIndex; i < maxRange; i++) {
                                var visibleRow = layout.childById(ParentComponent.class, "row_" + i);

                                var rowButtons = new ArrayList<PositionedRectangle>();

                                ComponentUtils.recursiveSearch(visibleRow, ButtonComponent.class, rowButtons::add);

                                this.alternativeButtonChecks.add(CollectedPositionedRectangle.of(visibleRow, rowButtons));
                            }
                        }
                    })
                    .scrollbarThiccness(8)
                    .scrollbar(ScrollContainer.Scrollbar.vanilla())
                    .fixedScrollbarLength(16)
                    .scrollStep(18)
                    .scrollbar(ComponentUtils.getScrollbarRenderer());
        } else {
            innerAccessoriesLayout = Containers.verticalFlow(Sizing.content(), Sizing.content()).child(fullLayout);
        }

        innerAccessoriesLayout.id("inner_accessories_container");

        var accessoriesMainLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(3)
                .child(innerAccessoriesLayout)
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .surface(ComponentUtils.getPanelSurface())
                //.allowOverflow(true)
                .padding(Insets.of(6))
                .id("accessories_layout");

        if (showScrollBar) {
            this.child(accessoriesMainLayout);

            return;
        }

        this.sizing(Sizing.content(), Sizing.fixed(minimumLayoutHeight));

        this.child(accessoriesMainLayout);
        this.child(Containers.verticalFlow(Sizing.content(), Sizing.expand()));
    }

    @Override
    public void onCosmeticToggle(boolean showCosmeticState) {
        this.alternativeButtonChecks.clear();

        var container = this.childById(BaseParentComponent.class, "inner_accessories_container");

        Consumer<FlowLayout> childSetter;

        int startingRowIndex = 0;

        if(container instanceof ExtendedScrollContainer<?> scrollContainer){
            childSetter = layout -> ((ExtendedScrollContainer<FlowLayout>) scrollContainer).child(layout);
        } else if(container instanceof FlowLayout flowLayout) {
            childSetter = flowLayout::child;
        } else {
            return;
        }

        ParentComponent prevLayout = showCosmeticState ? this.fullAccessoriesLayout : this.fullCosmeticsLayout ;

        for (int i = startingRowIndex; i < startingRowIndex + this.totalRowCount; i++) {
            var oldRow = prevLayout.childById(ParentComponent.class, "row_" + i);

            ComponentUtils.recursiveSearch(oldRow, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, (slotComponent) -> {
                this.screen.disableSlot(slotComponent.slot());
            });
        }

        prevLayout.parent().queue(() -> {
            var currentParent = prevLayout.parent();

            if (currentParent != null) currentParent.removeChild(prevLayout);
        });

        var newLayout = showCosmeticState ? this.fullCosmeticsLayout : this.fullAccessoriesLayout;

        var paddingValue = (showScrollbar ? 3 : 0);

        newLayout.padding(this.screen.mainWidgetPosition() ? Insets.left(paddingValue) : Insets.right(paddingValue));

        childSetter.accept(newLayout);

        for (int i = startingRowIndex; i < startingRowIndex + this.totalRowCount; i++) {
            var newRow = newLayout.childById(ParentComponent.class, "row_" + i);

            if(i < maxRowCount) {
                var rowButtons = new ArrayList<PositionedRectangle>();

                ComponentUtils.recursiveSearch(newRow, ButtonComponent.class, rowButtons::add);

                this.alternativeButtonChecks.add(CollectedPositionedRectangle.of(newRow, rowButtons));
            }

            ComponentUtils.recursiveSearch(newRow, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, (slotComponent) -> {
                this.screen.enableSlot(slotComponent.slot());
            });
        }
    }

    @Override
    public @Nullable Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
        for (var child : this.alternativeButtonChecks) {
            if (child.isInBoundingBox(mouseX, mouseY)) return false;
        }

        return null;
    }
}
