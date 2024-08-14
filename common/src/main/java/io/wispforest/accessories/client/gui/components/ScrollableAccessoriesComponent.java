package io.wispforest.accessories.client.gui.components;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.accessories.pond.owo.ComponentExtension;
import io.wispforest.accessories.pond.owo.MutableBoundingArea;
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

        var slots = menu.slots;

        var pageStartingSlotIndex = menu.startingAccessoriesSlot() + menu.addedArmorSlots();

        var gridSize = 7;

        var maxColumnCount = menu.owner().accessoriesHolder().columnAmount();
        var maxRowCount = gridSize;

        var totalRowCount = (int) Math.ceil(((slots.size() - pageStartingSlotIndex) / 2f) / maxColumnCount);

        if (totalRowCount <= 0) return null;

        var minimumWidth = maxColumnCount * 18;
        var minimumHeight = maxRowCount * 18;

        var fullAccessoriesLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                //.allowOverflow(true)
                .id("accessories_container");

        //((MutableBoundingArea<FlowLayout>) fullAccessoriesLayout).deepRecursiveChecking(false);
        //TODO: WAY TO MUCH PERFORMANCE DEGRADATION WITH ENABLE BUT BUTTONS BREAK WITHOUT IT

        var fullCosmeticsLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                //.allowOverflow(true)
                .id("cosmetics_container");

        //((MutableBoundingArea<FlowLayout>) fullCosmeticsLayout).deepRecursiveChecking(false);
        //TODO: WAY TO MUCH PERFORMANCE DEGRADATION WITH ENABLE BUT BUTTONS BREAK WITHOUT IT

        var alternativeButtonChecks = new ArrayList<PositionedRectangle>();

        for (int row = 0; row < totalRowCount; row++) {
            var colStartingIndex = pageStartingSlotIndex + (row * (maxColumnCount * 2));

            var accessoriesRowLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .surface(screen.FULL_SLOT_RENDERING)
                    //.allowOverflow(true)
                    .id("row_" + row);

            //((MutableBoundingArea<FlowLayout>) accessoriesRowLayout).deepRecursiveChecking(true);

            var cosmeticRowLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .surface(screen.FULL_SLOT_RENDERING)
                    //.allowOverflow(true)
                    .id("row_" + row);

            //((MutableBoundingArea<FlowLayout>) cosmeticRowLayout).deepRecursiveChecking(true);

            var rowButtons = new ArrayList<PositionedRectangle>();

            var overMaxSlots = false;

            for (int col = 0; col < maxColumnCount; col++) {
                var cosmetic = colStartingIndex + (col * 2);
                var accessory = cosmetic + 1;

                if (accessory >= slots.size() || cosmetic >= slots.size()) {
                    overMaxSlots = true;

                    break;
                }

                //this.enableSlot(cosmetic);
//                if(row < maxRowCount)
                screen.hideSlot(accessory);
                screen.hideSlot(cosmetic);

                screen.enableSlot(screen.showCosmeticState() ? cosmetic : accessory);
                screen.disableSlot(screen.showCosmeticState() ? accessory : cosmetic);

                //--

                var accessoryComponentData = ComponentUtils.slotAndToggle((AccessoriesBasedSlot) slots.get(accessory), screen::slotAsComponent);

                accessoriesRowLayout.child(accessoryComponentData.first());

                if (row < maxRowCount && screen.showCosmeticState()) rowButtons.add(accessoryComponentData.second());

                //--

                var cosmeticComponentData = ComponentUtils.slotAndToggle((AccessoriesBasedSlot) slots.get(cosmetic), screen::slotAsComponent);

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

        fullLayout.padding(this.screen.leftPositioned() ? Insets.left(paddingValue) : Insets.right(paddingValue));

        BaseParentComponent innerAccessoriesLayout;

        if(showScrollBar) {
            innerAccessoriesLayout = new ExtendedScrollContainer<>(ScrollContainer.ScrollDirection.VERTICAL, Sizing.fixed(minimumWidth + 8 + 3), Sizing.fixed(minimumHeight), fullLayout)
                    .strictMouseScrolling(!Accessories.getConfig().clientData.allowSlotScrolling)
                    .oppositeScrollbar(this.screen.leftPositioned())
                    .scrolledToCallback((container, prevOffset, scrollOffset) -> {
                        if(prevOffset == scrollOffset) return;

                        int prevRowIndex;
                        int rowIndex;

                        if(prevOffset - scrollOffset < 0) {
                            prevRowIndex = (int) Math.floor(prevOffset / 18);
                            rowIndex = (int) Math.floor(scrollOffset / 18);
                        } else {
                            prevRowIndex = (int) Math.ceil(prevOffset / 18);
                            rowIndex = (int) Math.ceil(scrollOffset / 18);
                        }

                        if(container.child() instanceof FlowLayout layout) {
                            this.alternativeButtonChecks.clear();

//                            var prevRow = (ParentComponent) layout.children().get(prevRowIndex);
//
//                            ComponentUtils.recursiveSearch(prevRow, ButtonComponent.class, btn -> {
//                                this.visibleButtons.remove(btn);
//                                //((ComponentExtension)(btn)).allowIndividualOverdraw(false);
//                            });
//
//                            ComponentUtils.recursiveSearch(prevRow, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, (slotComponent) -> {
//                                //this.screen.disableSlot(slotComponent.index());
//                            });

                            var maxRange = Math.min(rowIndex + this.maxRowCount, this.totalRowCount - 1);

                            for (int i = rowIndex; i < maxRange; i++) {
                                var visibleRow = layout.childById(ParentComponent.class, "row_" + i);

                                var rowButtons = new ArrayList<PositionedRectangle>();

                                ComponentUtils.recursiveSearch(visibleRow, ButtonComponent.class, rowButtons::add);

                                this.alternativeButtonChecks.add(CollectedPositionedRectangle.of(visibleRow, rowButtons));

//                                ComponentUtils.recursiveSearch(visibleRow, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, (slotComponent) -> {
//                                    this.screen.enableSlot(slotComponent.index());
//                                });
                            }
                        }
                    })
                    .scrollbarThiccness(8)
                    .scrollbar(ScrollContainer.Scrollbar.vanilla())
                    .fixedScrollbarLength(16)
                    .scrollStep(18);
                    //.margins(Insets.right(-3)).padding(Insets.right(-3));
                    //.allowOverflow(true);
        } else {
            innerAccessoriesLayout = Containers.verticalFlow(Sizing.content(), Sizing.content()).child(fullLayout);
        }

        innerAccessoriesLayout.id("inner_accessories_container");
                //.allowOverflow(true)

        //((MutableBoundingArea<BaseParentComponent>) innerAccessoriesLayout).deepRecursiveChecking(true);

        var accessoriesMainLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(3)
                .child(innerAccessoriesLayout)
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .surface(Surface.PANEL)
                //.allowOverflow(true)
                .padding(Insets.of(6))
                .id("accessories_layout");

        //((MutableBoundingArea) accessoriesMainLayout).deepRecursiveChecking(true);

        //((MutableBoundingArea) this).deepRecursiveChecking(true);

        if (showScrollBar) {
            this.child(accessoriesMainLayout);

            return;
        }

//        var pageLabel = Components.label(net.minecraft.network.chat.Component.literal((pageIndex.get() + 1) + "/" + slotPages.size()));
//
//        pageIndex.observe(integer -> pageLabel.text(net.minecraft.network.chat.Component.literal((pageIndex.get() + 1) + "/" + slotPages.size())));

//        var titleBar = Containers.horizontalFlow(Sizing.fixed(minimumWidth), Sizing.fixed(20))
//                .child(
//                        Components.button(Component.literal("<"), btn -> switchPage(pageIndex.get() - 1))
//                                .configure((ButtonComponent btn) -> {
//                                    btn.mouseScroll().subscribe((mouseX, mouseY, amount) -> {
//                                        switchPage((int) Math.round(pageIndex.get() + amount));
//
//                                        return true;
//                                    });
//                                })
//                                .sizing(Sizing.fixed(10), Sizing.fixed(14))
//                                .margins(Insets.of(2, 2, 0, 2))
//                )
//                .child(
//                        Components.button(Component.literal(">"), btn -> switchPage(pageIndex.get() + 1))
//                                .configure((ButtonComponent btn) -> {
//                                    btn.mouseScroll().subscribe((mouseX, mouseY, amount) -> {
//                                        switchPage((int) Math.round(pageIndex.get() + amount));
//
//                                        return true;
//                                    });
//                                })
//                                .sizing(Sizing.fixed(10), Sizing.fixed(14))
//                                .margins(Insets.of(2, 2, 0, 0))
//                )
//                .child(
//                        Containers.horizontalFlow(Sizing.expand(100), Sizing.content())
//                                .child(pageLabel.color(Color.ofFormatting(ChatFormatting.DARK_GRAY)))
//                                .horizontalAlignment(HorizontalAlignment.CENTER)
//                )
//                //.child(cosmeticToggleButton)
//                .horizontalAlignment(HorizontalAlignment.RIGHT)
//                .verticalAlignment(VerticalAlignment.CENTER)
//                .id("page_title_bar");

//        accessoriesMainLayout.child(0, titleBar);

        //this.allowOverflow(true);
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
            var currentOffset = scrollContainer.currentScrollOffset();

            //rowIndex = (int) Math.round(currentOffset / 18);

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
                this.screen.disableSlot(slotComponent.index());
            });
        }

        //prevLayout.remove();
        prevLayout.parent().queue(() -> {
            var currentParent = prevLayout.parent();

            if (currentParent != null) currentParent.removeChild(prevLayout);
        });

        var newLayout = showCosmeticState ? this.fullCosmeticsLayout : this.fullAccessoriesLayout;

        var paddingValue = (showScrollbar ? 3 : 0);

        newLayout.padding(this.screen.leftPositioned() ? Insets.left(paddingValue) : Insets.right(paddingValue));

        childSetter.accept(newLayout);

        for (int i = startingRowIndex; i < startingRowIndex + this.totalRowCount; i++) {
            var newRow = newLayout.childById(ParentComponent.class, "row_" + i);

            if(i < maxRowCount) {
                var rowButtons = new ArrayList<PositionedRectangle>();

                ComponentUtils.recursiveSearch(newRow, ButtonComponent.class, rowButtons::add);

                this.alternativeButtonChecks.add(CollectedPositionedRectangle.of(newRow, rowButtons));
            }

            ComponentUtils.recursiveSearch(newRow, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, (slotComponent) -> {
                this.screen.enableSlot(slotComponent.index());
            });
        }
    }

    @Override
    public @Nullable Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
        for (var child : this.alternativeButtonChecks) {
            //if(!((BaseComponentAccessor)child).accessories$IsMounted()) continue;

            if (child.isInBoundingBox(mouseX, mouseY)) return false;
        }

        return null;
    }
}
