package io.wispforest.accessories.client.gui.components;

import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.accessories.mixin.client.owo.BaseComponentAccessor;
import io.wispforest.accessories.pond.owo.MutableBoundingArea;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.util.Observable;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class GriddedAccessoriesComponent extends FlowLayout implements AccessoriesContainingComponent {

    private final Map<Integer, PageLayouts> slotPages;

    private final Observable<Integer> pageIndex = Observable.of(0);

    private final AccessoriesExperimentalScreen screen;

    protected GriddedAccessoriesComponent(AccessoriesExperimentalScreen screen, Pair<Map<Integer, PageLayouts>, Vector2i> componentData) {
        super(Sizing.content(), Sizing.content(), Algorithm.VERTICAL);

        this.setupID();

        this.screen = screen;
        this.slotPages = componentData.first();

        var minimumDimensions = componentData.second();

        this.build(minimumDimensions.x, minimumDimensions.y);
    }

    @Nullable
    public static GriddedAccessoriesComponent createOrNull(AccessoriesExperimentalScreen screen) {
        var data = buildPages(screen);

        var slotPages = data.first();

        if(slotPages.isEmpty()) return null;

        return new GriddedAccessoriesComponent(screen, data);
    }

    private static Pair<Map<Integer, PageLayouts>, Vector2i> buildPages(AccessoriesExperimentalScreen screen) {
        Map<Integer, PageLayouts> slotPages = new LinkedHashMap<>();

        var menu = screen.getMenu();

        var slots = menu.slots;

        var pageStartingSlotIndex = menu.startingAccessoriesSlot + menu.addedArmorSlots;

        var gridSize = 6;

        var maxColumnCount = menu.owner().accessoriesHolder().columnAmount();
        var maxRowCount = gridSize;

        var totalRowCount = (int) Math.ceil(((slots.size() - pageStartingSlotIndex) / 2f) / maxColumnCount);

        int pageCount;

        if(totalRowCount <= 7) {
            pageCount = (int) 1;
            maxRowCount = 7;
        } else {
            pageCount = (int) Math.ceil(totalRowCount / (float) maxRowCount);
        }

        var minimumWidth = maxColumnCount * 18;
        var minimumHeight = maxRowCount * 18;

        //if(totalRowCount < maxRowCount) maxColumnCount = totalRowCount;

        if(totalRowCount > 0) {
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                if (pageIndex != 0) pageStartingSlotIndex += (maxRowCount * maxColumnCount * 2);

                var rowCount = (totalRowCount < 0) ? maxRowCount + totalRowCount : maxRowCount;

                totalRowCount -= maxRowCount;

                var cosmeticGridLayout = Containers.grid(Sizing.content(), Sizing.content(), rowCount, maxColumnCount);
                var accessoriesGridLayout = Containers.grid(Sizing.content(), Sizing.content(), rowCount, maxColumnCount);

                var buttons = new ArrayList<io.wispforest.owo.ui.core.Component>();

                rowLoop:
                for (int row = 0; row < rowCount; row++) {
                    var colStartingIndex = pageStartingSlotIndex + (row * (maxColumnCount * 2));

                    for (int col = 0; col < maxColumnCount; col++) {
                        var cosmetic = colStartingIndex + (col * 2);
                        var accessory = cosmetic + 1;

                        if (accessory >= slots.size() || cosmetic >= slots.size()) break rowLoop;

                        //this.enableSlot(cosmetic);
                        if (pageIndex == 0) screen.enableSlot(accessory);

                        //--

                        var btnPosition = Positioning.absolute(15, -1);

                        var cosmeticToggleBtn = ComponentUtils.ofSlot((AccessoriesBasedSlot) slots.get(accessory))
                                .zIndex(360)
                                .sizing(Sizing.fixed(5))
                                .positioning(btnPosition);

                        buttons.add(cosmeticToggleBtn);

                        var cosmeticSlot = Containers.verticalFlow(Sizing.fixed(18), Sizing.fixed(18))
                                .child(
                                        screen.slotAsComponent(cosmetic)
                                                .isBatched(true)
                                                .margins(Insets.of(1))
                                ).child(cosmeticToggleBtn)
                                .allowOverflow(true);

                        var cosmeticArea = ((MutableBoundingArea) cosmeticSlot);

                        cosmeticArea.addInclusionZone(cosmeticToggleBtn);
                        cosmeticArea.deepRecursiveChecking(true);

                        //--

                        var accessoryToggleBtn = ComponentUtils.ofSlot((AccessoriesBasedSlot) slots.get(accessory))
                                .zIndex(360)
                                .sizing(Sizing.fixed(5))
                                .positioning(btnPosition);

                        buttons.add(accessoryToggleBtn);

                        var accessorySlot = Containers.verticalFlow(Sizing.fixed(18), Sizing.fixed(18))
                                .child(
                                        screen.slotAsComponent(accessory)
                                                .isBatched(true)
                                                .margins(Insets.of(1))
//                                                .zIndex(5)
                                ).child(accessoryToggleBtn)
                                .allowOverflow(true);

                        var accessoryArea = ((MutableBoundingArea) accessorySlot);

                        accessoryArea.addInclusionZone(accessoryToggleBtn);
                        accessoryArea.deepRecursiveChecking(true);

                        //--

                        cosmeticGridLayout.child(cosmeticSlot, row, col);
                        accessoriesGridLayout.child(accessorySlot, row, col);
                    }
                }

                ((MutableBoundingArea<GridLayout>) accessoriesGridLayout)
                        .deepRecursiveChecking(true)
                        .surface(screen.FULL_SLOT_RENDERING)
                        .allowOverflow(true);

                ((MutableBoundingArea<GridLayout>) cosmeticGridLayout)
                        .deepRecursiveChecking(true)
                        .surface(screen.FULL_SLOT_RENDERING)
                        .allowOverflow(true);

                slotPages.put(pageIndex, new PageLayouts(accessoriesGridLayout, cosmeticGridLayout, buttons));
            }
        }

        return Pair.of(slotPages, new Vector2i(minimumWidth, minimumHeight));
    }

    public void build(int minimumWidth, int minimumHeight) {
        var minimumLayoutHeight = (minimumHeight) + (this.slotPages.size() > 1 ? (3 + 20 + (2 * 6)) : 0);

        var accessoriesMainLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(3)
                .child(
                        ((MutableBoundingArea<FlowLayout>) Containers.verticalFlow(Sizing.content(), Sizing.content()))
                                .deepRecursiveChecking(true)
                                .child(getCurrentPageDefaulted().getLayout(screen.showCosmeticState))
                                .allowOverflow(true)
                                .id("grid_container")
                )
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .surface(Surface.PANEL)
                .allowOverflow(true)
                .padding(Insets.of(6))
                .id("accessories_layout");

        ((MutableBoundingArea) accessoriesMainLayout).deepRecursiveChecking(true);

        if (this.slotPages.size() <= 1) {
            this.child(accessoriesMainLayout);

            return;
        }

        var pageLabel = Components.label(Component.literal((pageIndex.get() + 1) + "/" + slotPages.size()));

        pageIndex.observe(integer -> pageLabel.text(Component.literal((pageIndex.get() + 1) + "/" + slotPages.size())));

        var titleBar = Containers.horizontalFlow(Sizing.fixed(minimumWidth), Sizing.fixed(20))
                .child(
                        Components.button(Component.literal("<"), btn -> switchPage(pageIndex.get() - 1))
                                .configure((ButtonComponent btn) -> {
                                    btn.mouseScroll().subscribe((mouseX, mouseY, amount) -> {
                                        switchPage((int) Math.round(pageIndex.get() + amount));

                                        return true;
                                    });
                                })
                                .sizing(Sizing.fixed(10), Sizing.fixed(14))
                                .margins(Insets.of(2, 2, 0, 2))
                )
                .child(
                        Components.button(Component.literal(">"), btn -> switchPage(pageIndex.get() + 1))
                                .configure((ButtonComponent btn) -> {
                                    btn.mouseScroll().subscribe((mouseX, mouseY, amount) -> {
                                        switchPage((int) Math.round(pageIndex.get() + amount));

                                        return true;
                                    });
                                })
                                .sizing(Sizing.fixed(10), Sizing.fixed(14))
                                .margins(Insets.of(2, 2, 0, 0))
                )
                .child(
                        Containers.horizontalFlow(Sizing.expand(100), Sizing.content())
                                .child(pageLabel.color(Color.ofFormatting(ChatFormatting.DARK_GRAY)))
                                .horizontalAlignment(HorizontalAlignment.CENTER)
                )
                //.child(cosmeticToggleButton)
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .verticalAlignment(VerticalAlignment.CENTER)
                .id("page_title_bar");

        accessoriesMainLayout.child(0, titleBar);

        this.allowOverflow(true)
                .sizing(Sizing.content(), Sizing.fixed(minimumLayoutHeight));

        this.child(accessoriesMainLayout);
        this.child(Containers.verticalFlow(Sizing.content(), Sizing.expand()));

        ((MutableBoundingArea) this).deepRecursiveChecking(true);
    }

    public void switchPage(int nextPageIndex) {
        switchPage(nextPageIndex, screen.showCosmeticState);
    }

    public void switchPage(int nextPageIndex, boolean showCosmeticState)  {
        if(nextPageIndex >= 0 && nextPageIndex < slotPages.size()) {
            var prevPageIndex = pageIndex.get();

            pageIndex.set(nextPageIndex);

            var lastGrid = slotPages.get(prevPageIndex).getLayout(showCosmeticState);
            var activeGrid = slotPages.get(nextPageIndex).getLayout(showCosmeticState);

            updateSlots(lastGrid, activeGrid);

            var titleBarComponent = this.childById(FlowLayout.class, "page_title_bar");

            var gridContainer = titleBarComponent.parent().childById(FlowLayout.class, "grid_container");

            gridContainer.clearChildren();

            gridContainer.child(activeGrid);
        }
    }

    private PageLayouts getCurrentPage() {
        return this.slotPages.get(this.pageIndex.get());
    }

    private PageLayouts getCurrentPageDefaulted() {
        return this.slotPages.getOrDefault(this.pageIndex.get(), PageLayouts.DEFAULT);
    }

    @Override
    public void onCosmeticToggle(boolean showCosmeticState) {
        var titleBarComponent = this.childById(FlowLayout.class, "page_title_bar");

        var gridContainer = titleBarComponent.parent().childById(FlowLayout.class, "grid_container");

        gridContainer.clearChildren();

        var pageLayouts = this.getCurrentPageDefaulted();

        var lastGrid = pageLayouts.getLayout(!showCosmeticState);
        var activeGrid = pageLayouts.getLayout(showCosmeticState);

        updateSlots(lastGrid, activeGrid);

        gridContainer.child(activeGrid);
    }

    @Override
    public @Nullable Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
        for (var child : getCurrentPageDefaulted().cosmeticToggleButtons()) {
            if(!((BaseComponentAccessor)child).accessories$IsMounted()) continue;

            if (child.isInBoundingBox(mouseX, mouseY)) return false;
        }

        return null;
    }

    private void updateSlots(ParentComponent prevComp, ParentComponent newComp) {
        ComponentUtils.recursiveSearch(prevComp, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> screen.disableSlot(slotComponent.index()));
        ComponentUtils.recursiveSearch(newComp, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> screen.enableSlot(slotComponent.index()));
    }
}
