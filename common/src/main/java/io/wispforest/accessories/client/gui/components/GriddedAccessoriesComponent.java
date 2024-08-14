package io.wispforest.accessories.client.gui.components;

import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.accessories.pond.owo.ComponentExtension;
import io.wispforest.accessories.pond.owo.MutableBoundingArea;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
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
import java.util.List;
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

        var pageStartingSlotIndex = menu.startingAccessoriesSlot() + menu.addedArmorSlots();

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

        if (totalRowCount > 0) {
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                if (pageIndex != 0) pageStartingSlotIndex += (maxRowCount * maxColumnCount * 2);

                var rowCount = (totalRowCount < 0) ? maxRowCount + totalRowCount : maxRowCount;

                totalRowCount -= maxRowCount;

                var accessoriesPageLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());
                var cosmeticsPageLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());

                var alternativeAccessoriesChecks = new ArrayList<PositionedRectangle>();
                var alternativeCosmeticButtons = new ArrayList<PositionedRectangle>();

                for (int row = 0; row < rowCount; row++) {
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

                    var accessoriesRowButtons = new ArrayList<PositionedRectangle>();
                    var cosmeticsRowButtons = new ArrayList<PositionedRectangle>();

                    var overMaxSlots = false;

                    for (int col = 0; col < maxColumnCount; col++) {
                        var cosmetic = colStartingIndex + (col * 2);
                        var accessory = cosmetic + 1;

                        if (accessory >= slots.size() || cosmetic >= slots.size()) {
                            overMaxSlots = true;

                            break;
                        }

                        //this.enableSlot(cosmetic);
                        screen.hideSlot(accessory);
                        screen.hideSlot(cosmetic);

                        screen.enableSlot(screen.showCosmeticState() ? cosmetic : accessory);
                        screen.disableSlot(screen.showCosmeticState() ? accessory : cosmetic);

                        //--

                        var accessoryComponentData = ComponentUtils.slotAndToggle((AccessoriesBasedSlot) slots.get(accessory), screen::slotAsComponent);

                        accessoriesRowLayout.child(accessoryComponentData.first());

                        accessoriesRowButtons.add(accessoryComponentData.second());

                        //--

                        var cosmeticComponentData = ComponentUtils.slotAndToggle((AccessoriesBasedSlot) slots.get(cosmetic), screen::slotAsComponent);

                        cosmeticRowLayout.child(cosmeticComponentData.first());

                        cosmeticsRowButtons.add(cosmeticComponentData.second());
                    }

                    accessoriesPageLayout.child(accessoriesRowLayout);
                    cosmeticsPageLayout.child(cosmeticRowLayout);

                    alternativeAccessoriesChecks.add(CollectedPositionedRectangle.of(accessoriesRowLayout, accessoriesRowButtons));
                    alternativeCosmeticButtons.add(CollectedPositionedRectangle.of(cosmeticRowLayout, cosmeticsRowButtons));

                    if(overMaxSlots) break;
                }

                //((MutableBoundingArea<FlowLayout>) accessoriesPageLayout).deepRecursiveChecking(true);
                        //.allowOverflow(true);

                //((MutableBoundingArea<FlowLayout>) cosmeticsPageLayout).deepRecursiveChecking(true);
                        //.allowOverflow(true);

                slotPages.put(pageIndex, new PageLayouts(accessoriesPageLayout, cosmeticsPageLayout, alternativeAccessoriesChecks, alternativeCosmeticButtons));
            }
        }

        return Pair.of(slotPages, new Vector2i(minimumWidth, minimumHeight));
    }

    public void build(int minimumWidth, int minimumHeight) {
        var minimumLayoutHeight = (minimumHeight) + (this.slotPages.size() > 1 ? (3 + 15 + (2 * 6)) : 0);

        var holder = Containers.verticalFlow(Sizing.content(), Sizing.content())
                .child(getCurrentPageDefaulted().getLayout(screen.showCosmeticState()))
                //.allowOverflow(true)
                .id("accessories_container_holder");

        //((MutableBoundingArea<FlowLayout>) holder).deepRecursiveChecking(true);

        var accessoriesMainLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(2)
                .child(holder)
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .surface(Surface.PANEL)
                //.allowOverflow(true)
                .padding(Insets.of(6))
                .id("accessories_layout");

        //((MutableBoundingArea) accessoriesMainLayout).deepRecursiveChecking(true);

        //((MutableBoundingArea) this).deepRecursiveChecking(true);

        if (this.slotPages.size() <= 1) {
            this.child(accessoriesMainLayout);

            return;
        }

        var pageLabel = Components.label(Component.literal((pageIndex.get() + 1) + "/" + slotPages.size()));

        pageIndex.observe(integer -> pageLabel.text(Component.literal((pageIndex.get() + 1) + "/" + slotPages.size())));

        var titleBar = Containers.horizontalFlow(Sizing.fixed(minimumWidth), Sizing.content())
                .child(
                        Components.button(Component.literal("<"), btn -> switchPage(pageIndex.get() - 1))
                                .configure((ButtonComponent btn) -> {
                                    btn.mouseScroll().subscribe((mouseX, mouseY, amount) -> {
                                        switchPage((int) Math.round(pageIndex.get() + amount));

                                        return true;
                                    });
                                })
                                .sizing(Sizing.fixed(10), Sizing.fixed(14))
                                .margins(Insets.of(0, 2, 0, 2))
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
                                .margins(Insets.of(0, 2, 0, 0))
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

        //this.allowOverflow(true)
        this.sizing(Sizing.content(), Sizing.fixed(minimumLayoutHeight));

        this.child(accessoriesMainLayout);
        this.child(Containers.verticalFlow(Sizing.content(), Sizing.expand()));
    }

    public void switchPage(int nextPageIndex) {
        switchPage(nextPageIndex, screen.showCosmeticState());
    }

    public void switchPage(int nextPageIndex, boolean showCosmeticState)  {
        if(nextPageIndex >= 0 && nextPageIndex < slotPages.size()) {
            var prevPageIndex = pageIndex.get();

            pageIndex.set(nextPageIndex);

            var lastGrid = slotPages.get(prevPageIndex).getLayout(showCosmeticState);
            var activeGrid = slotPages.get(nextPageIndex).getLayout(showCosmeticState);

            ComponentUtils.recursiveSearch(lastGrid, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> screen.hideSlot(slotComponent.index()));

            //updateSlots(lastGrid, activeGrid);

            var titleBarComponent = this.childById(FlowLayout.class, "page_title_bar");

            var gridContainer = titleBarComponent.parent().childById(FlowLayout.class, "accessories_container_holder");

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

        var gridContainer = titleBarComponent.parent().childById(FlowLayout.class, "accessories_container_holder");

        gridContainer.clearChildren();

        for (var pageLayout : this.slotPages.values()) {
            var lastGrid = pageLayout.getLayout(!showCosmeticState);

            updateDisabledStateSlots(
                    lastGrid,
                    pageLayout.getLayout(showCosmeticState));

            //ComponentUtils.recursiveSearch(lastGrid, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> screen.hideSlot(slotComponent.index()));
        }

        var activeGrid = getCurrentPageDefaulted().getLayout(showCosmeticState);

        gridContainer.child(activeGrid);
    }

    private CachedHoverCheck previousCheckResult = null;

    @Override
    public @Nullable Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
//        if(previousCheckResult != null) {
//            var result = previousCheckResult.isValid(mouseX, mouseY);
//
//            if(result == null) {
//                previousCheckResult = null;
//            } else if(result) {
//                return false;
//            }
//        }

        for (var child : getCurrentPageDefaulted().getAlternativeChecks(this.screen.showCosmeticState())) {
            //if(!((BaseComponentAccessor)child).accessories$IsMounted()) continue;

            if (child.isInBoundingBox(mouseX, mouseY)) return false;
        }

        return null;
    }

    private record CachedHoverCheck(double prevMouseX, double prevMouseY, boolean value){
        private Boolean isValid(double mouseX, double mouseY) {
            if(Math.abs(mouseX - prevMouseX) < 1 && Math.abs(mouseY - prevMouseY) < 1){
                return value;
            }

            return null;
        }
    }

    private void updateDisabledStateSlots(ParentComponent prevComp, ParentComponent newComp) {
        ComponentUtils.recursiveSearch(prevComp, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> screen.disableSlot(slotComponent.index()));
        ComponentUtils.recursiveSearch(newComp, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> screen.enableSlot(slotComponent.index()));
    }

    public record PageLayouts(FlowLayout accessoriesLayout, FlowLayout cosmeticLayout, List<PositionedRectangle> alternativeAccessoriesChecks, List<PositionedRectangle> alternativeCosmeticChecks) {
        public static final PageLayouts DEFAULT = new PageLayouts(
                Containers.verticalFlow(Sizing.content(), Sizing.content()),
                Containers.verticalFlow(Sizing.content(), Sizing.content()),
                List.of(),
                List.of());

        public FlowLayout getLayout(boolean isCosmetic) {
            return isCosmetic ? cosmeticLayout : accessoriesLayout;
        }

        public List<PositionedRectangle> getAlternativeChecks(boolean isCosmetic) {
            return isCosmetic ? alternativeCosmeticChecks : alternativeAccessoriesChecks;
        }
    }
}
