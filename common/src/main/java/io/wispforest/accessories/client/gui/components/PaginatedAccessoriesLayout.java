package io.wispforest.accessories.client.gui.components;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.impl.option.PlayerOptions;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.util.Observable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static io.wispforest.accessories.client.gui.components.ComponentUtils.BACKGROUND_SLOT_RENDERING_SURFACE;

public class PaginatedAccessoriesLayout extends AccessoriesContainingLayout<PaginatedAccessoriesLayout.RawPageData> {

    private Observable<Integer> pageIndex;

    private PaginatedAccessoriesLayout(AccessoriesScreen screen, RawPageData data) {
        super(screen, data);
    }

    @Nullable
    public static PaginatedAccessoriesLayout createOrNull(AccessoriesScreen screen) {
        var data = buildPageData(screen);

        if(data == null) return null;

        return new PaginatedAccessoriesLayout(screen, data);
    }

    @Nullable
    private static RawPageData buildPageData(AccessoriesScreen screen) {
        var menu = screen.getMenu();
        var slots = menu.getVisibleAccessoriesSlots();

        var sideBySide = screen.getDefaultedData(PlayerOptions.SIDE_BY_SIDE_SLOTS);

        var maxColumnCount = screen.getDefaultedData(PlayerOptions.COLUMN_AMOUNT);
        var maxRowCount = 6;

        var totalRowCount = (int) Math.ceil((slots.size() / 2f) / maxColumnCount);

        if (totalRowCount <= 0) return null;

        //--

        int pageCount = 1;

        if(totalRowCount > 7) {
            pageCount = (int) Math.ceil(totalRowCount / (float) maxRowCount);
        } else {
            maxRowCount = 7;
        }

        var pageStartingSlotIndex = 0;

        Map<Integer, BaseLayoutGroup> slotPages = new LinkedHashMap<>();

        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            if (pageIndex != 0) pageStartingSlotIndex += (maxRowCount * maxColumnCount * 2);

            var rowCount = (totalRowCount < 0) ? maxRowCount + totalRowCount : maxRowCount;

            totalRowCount -= maxRowCount;

            slotPages.put(pageIndex, createBaseLayoutGroup(screen, slots, rowCount, maxColumnCount, pageStartingSlotIndex, sideBySide));
        }

        return new RawPageData(slotPages, maxColumnCount, maxRowCount, sideBySide);
    }

    protected record RawPageData(Map<Integer, BaseLayoutGroup> pages, int maxColumnCount, int maxRowCount, boolean sideBySide) implements LayoutData {
    }

    @Override
    protected BaseLayoutGroup getCurrentGroup() {
        return this.getCurrentPageOrDefault();
    }

    @Override
    protected void buildLayout(RawPageData layoutData) {
        var minimumLayoutHeight = (layoutData.height()) + (layoutData.pages().size() > 1 ? (3 + 15 + (2 * 6)) : 0);

        var currentPage = getCurrentPageOrDefault();

        var holder = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .configure((FlowLayout layout) -> {
                    layout.gap(3)
                            .id("accessories_container_holder");
                });

        if (layoutData.sideBySide) {
            holder.child(currentPage.getLayout(false));

            if (screen.showCosmeticState()) {
                holder.child(0, currentPage.getLayout(screen.showCosmeticState()));
            }
        } else {
            holder.child(currentPage.getLayout(screen.showCosmeticState()));
        }

        var accessoriesMainLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(2)
                .child(holder)
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .surface(ComponentUtils.getPanelSurface().and(BACKGROUND_SLOT_RENDERING_SURFACE))
                .padding(Insets.of(7))
                .id("accessories_layout");

        if (layoutData.pages().size() <= 1) {
            this.child(accessoriesMainLayout);

            return;
        }

        var pageLabel = Components.label(Component.literal((pageIndex().get() + 1) + "/" + layoutData.pages().size()))
                .configure((LabelComponent labelComponent) -> {
                    ComponentUtils.addModeCheckHook(
                            Color.ofFormatting(ChatFormatting.DARK_GRAY), Color.ofFormatting(ChatFormatting.WHITE),
                            labelComponent,
                            component -> component.parent() != null,
                            LabelComponent::color
                    );
                });

        pageIndex().observe(integer -> pageLabel.text(Component.literal((pageIndex().get() + 1) + "/" + layoutData.pages().size())));

        var titleBar = Containers.horizontalFlow(Sizing.fixed(currentPage.width(screen.showCosmeticState(), layoutData.sideBySide())), Sizing.content())
                .child(
                        Containers.horizontalFlow(Sizing.expand(100), Sizing.content())
                                .child(pageLabel)
                                .horizontalAlignment(HorizontalAlignment.CENTER)
                )
                .child(
                        ComponentUtils.createIconButton(
                                btn -> {
                                    switchPage(-1);
                                },
                                10,
                                btn -> {
                                    btn.mouseScroll().subscribe((mouseX, mouseY, amount) -> {
                                        switchPage((int) Math.round(amount));

                                        return true;
                                    });
                                },
                                (btn) -> {
                                    return Accessories.of("textures/gui/accessories_back_icon" + (btn.isHovered() ? "_hovered" : "") + ".png");
                                }).margins(Insets.of(0, 0, 0, 2))
                )
                .child(
                        ComponentUtils.createIconButton(
                                btn -> {
                                    switchPage(1);
                                },
                                10,
                                btn -> {
                                    btn.mouseScroll().subscribe((mouseX, mouseY, amount) -> {
                                        switchPage((int) Math.round(amount));

                                        return true;
                                    });
                                },
                                (ctx, btn) -> {
                                    ctx.getMatrixStack()
                                        .rotateAbout((float) Math.toRadians(180), btn.getX() + (btn.width() / 2f), btn.getY() + (btn.height() / 2f));

                                    return Accessories.of("textures/gui/accessories_back_icon" + (btn.isHovered() ? "_hovered" : "") + ".png");
                                }).margins(Insets.of(0, 0, 0, 0))
                )
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .verticalAlignment(VerticalAlignment.CENTER)
                .id("page_title_bar");

        accessoriesMainLayout.child(0, titleBar);

        this.sizing(Sizing.content(), Sizing.fixed(minimumLayoutHeight));

        this.child(accessoriesMainLayout);
        this.child(Containers.verticalFlow(Sizing.content(), Sizing.expand()));
    }

    private void switchPage(int pageOffset) {
        switchPage(pageIndex().get() + pageOffset, screen.showCosmeticState());
    }

    private void switchPage(int nextPageIndex, boolean showCosmeticState)  {
        if(nextPageIndex >= 0 && nextPageIndex < layoutData.pages().size()) {
            var prevPageIndex = pageIndex().get();

            pageIndex().set(nextPageIndex);

            var prevGroup = layoutData.pages().get(prevPageIndex);
            var currentGroup = layoutData.pages.get(nextPageIndex);

            var titleBarComponent = this.childById(FlowLayout.class, "page_title_bar");
            var gridContainer = titleBarComponent.parent().childById(FlowLayout.class, "accessories_container_holder");

            var sideBySide = this.layoutData.sideBySide();

            titleBarComponent.horizontalSizing(Sizing.fixed(currentGroup.width(showCosmeticState, sideBySide)));

            Consumer<Boolean> consumer = isCosmetic -> {
                var lastGrid = prevGroup.getLayout(isCosmetic);
                var activeGrid = currentGroup.getLayout(isCosmetic);

                ComponentUtils.recursiveSearchSlots(lastGrid, slotComponent -> screen.hideSlot(slotComponent.slot()));

                gridContainer.removeChild(lastGrid);

                gridContainer.child(0, activeGrid);
            };

            if (sideBySide) {
                consumer.accept(false);

                if (showCosmeticState) {
                    consumer.accept(true);
                }
            } else {
                consumer.accept(showCosmeticState);
            }

            updatePadding();
        }
    }

    public Observable<Integer> pageIndex() {
        if (pageIndex == null) {
            pageIndex = Observable.of(0);
        }

        return pageIndex;
    }

    private BaseLayoutGroup getCurrentPageOrDefault() {
        return this.layoutData.pages().getOrDefault(this.pageIndex().get(), BaseLayoutGroup.DEFAULT);
    }

    @Override
    public void onCosmeticToggle(boolean showCosmeticState) {
        var gridContainer = this.childById(FlowLayout.class, "accessories_container_holder");

        var currentPage = getCurrentPageOrDefault();
        var sideBySide = layoutData.sideBySide;

        var titleBarComponent = this.childById(FlowLayout.class, "page_title_bar");

        if (titleBarComponent != null) titleBarComponent.horizontalSizing(Sizing.fixed(currentPage.width(showCosmeticState, sideBySide)));

        gridContainer.clearChildren();

        int pageIndex = pageIndex().get();

        int currentIndex = 0;

        for (var pageLayout : this.layoutData.pages().values()) {
            var selectedPage = pageIndex == currentIndex;

            if (sideBySide) {
               toggleSlotStates(pageLayout.getLayout(true), showCosmeticState, selectedPage);
            } else {
                var lastGrid = pageLayout.getLayout(!showCosmeticState);

                swapSlotStates(
                        lastGrid,
                        pageLayout.getLayout(showCosmeticState),
                        selectedPage
                );
            }


            currentIndex++;
        }

        if (sideBySide) {
            gridContainer.child(0, currentPage.accessoriesLayout());

            if (showCosmeticState) {
                gridContainer.child(0, currentPage.cosmeticLayout());
            }

            screen.setupPadding();
        } else {
            var activeGrid = currentPage.getLayout(showCosmeticState);

            gridContainer.child(activeGrid);
        }

        updatePadding();
    }

    @Override
    protected Iterable<PositionedRectangle> getAlternativeChecks(boolean showingCosmetics) {
        return this.getCurrentPageOrDefault().getAlternativeChecks(showingCosmetics);
    }

    private void swapSlotStates(ParentComponent prevComp, ParentComponent newComp, boolean selectedPage) {
        toggleSlotStates(prevComp, false, selectedPage);
        toggleSlotStates(newComp, true, selectedPage);
    }

    private void toggleSlotStates(ParentComponent component, boolean showSlots, boolean selectedPage) {
        if (!showSlots){
            ComponentUtils.recursiveSearchSlots(component, slotComponent -> screen.disableSlot(slotComponent.slot()));
        } else {
            ComponentUtils.recursiveSearchSlots(component, slotComponent -> {
                if (!selectedPage) screen.hideSlot(slotComponent.slot());
                screen.enableSlot(slotComponent.slot());
            });
        }
    }
}
