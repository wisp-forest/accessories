package io.wispforest.accessories.client.gui.components;

import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.impl.option.PlayerOptions;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.PositionedRectangle;
import io.wispforest.owo.ui.core.Size;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AccessoriesContainingLayout<D extends AccessoriesContainingLayout.LayoutData> extends FlowLayout {

    protected final AccessoriesScreen screen;
    protected final D layoutData;

    protected AccessoriesContainingLayout(AccessoriesScreen screen, D layoutData) {
        super(Sizing.content(), Sizing.content(), Algorithm.VERTICAL);

        this.screen = screen;
        this.layoutData = layoutData;

        this.id(defaultID());

        this.buildLayout(layoutData);

        updatePadding();
    }

    public int getCurrentWidth() {
        var currentGroup = getCurrentGroup();

        var slotsWidth = currentGroup.totalColumnCount() * 18;

        if (layoutData.sideBySide() && screen.showCosmeticState()) {
            slotsWidth = (slotsWidth * 2) + 3;
        }

        return slotsWidth + 14;
    }

    public int getMaxPossibleWidth() {
        var slotsWidth = layoutData.maxColumnCount() * 18;

        if (layoutData.sideBySide() && screen.showCosmeticState()) {
            slotsWidth = (slotsWidth * 2) + 3;
        }

        return slotsWidth + 14;
    }

    public int getPaddingOffset() {
        return getMaxPossibleWidth() - getCurrentWidth();
    }

    public void updatePadding() {
        var padding = getPaddingOffset();

        this.padding(this.screen.getDefaultedData(PlayerOptions.MAIN_WIDGET_POSITION) ? Insets.left(padding) : Insets.right(padding));
    }

    public static String defaultID() {
        return "outer_accessories_layout";
    }

    public abstract void onCosmeticToggle(boolean showCosmeticState);

    @Nullable
    public Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
        for (var child : getAlternativeChecks(this.screen.showCosmeticState())) {
            if (child.isInBoundingBox(mouseX, mouseY)) return false;
        }

        return null;
    }

    protected abstract Iterable<PositionedRectangle> getAlternativeChecks(boolean showingCosmetics);

    protected abstract void buildLayout(D layoutData);

    protected abstract BaseLayoutGroup getCurrentGroup();

    //--

    protected static BaseLayoutGroup createBaseLayoutGroup(AccessoriesScreen screen, List<Slot> slots, int totalRowCount, int maxColumnCount, int colStartingIndexOffset, boolean sideBySide) {
        var accessoriesLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());
        var cosmeticsLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());

        var accessoriesChecks = new ArrayList<PositionedRectangle>();
        var cosmeticChecks = new ArrayList<PositionedRectangle>();

        var totalColumnCount = maxColumnCount;

        for (int row = 0; row < totalRowCount; row++) {
            var colStartingIndex = colStartingIndexOffset + (row * (maxColumnCount * 2));

            var accessoriesRowLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .id("row_" + row);

            var cosmeticRowLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .id("row_" + row);

            var accessoriesRowButtons = new ArrayList<PositionedRectangle>();
            var cosmeticRowButtons = new ArrayList<PositionedRectangle>();

            var overMaxSlots = false;

            for (int col = 0; col < maxColumnCount; col++) {
                var cosmetic = colStartingIndex + (col * 2);
                var accessory = cosmetic + 1;

                if (accessory >= slots.size() || cosmetic >= slots.size()) {
                    overMaxSlots = true;

                    if (row == 0) {
                        totalColumnCount = col;
                    }

                    break;
                }

                var cosmeticSlot = (AccessoriesBasedSlot) slots.get(cosmetic);
                var accessorySlot = (AccessoriesBasedSlot) slots.get(accessory);

                screen.hideSlot(cosmeticSlot);
                screen.hideSlot(accessorySlot);

                if (sideBySide && screen.showCosmeticState()) {
                    screen.enableSlot(cosmeticSlot);
                    screen.enableSlot(accessorySlot);
                } else {
                    screen.enableSlot(screen.showCosmeticState() ? cosmeticSlot : accessorySlot);
                    screen.disableSlot(screen.showCosmeticState() ? accessorySlot : cosmeticSlot);
                }

                //--

                var accessoryComponentData = ComponentUtils.createSlotWithToggle(accessorySlot, screen::slotAsComponent);

                accessoriesRowLayout.child(accessoryComponentData.first());

                accessoriesRowButtons.add(accessoryComponentData.second());

                //--

                var cosmeticComponentData = ComponentUtils.createSlotWithToggle(cosmeticSlot, screen::slotAsComponent, !sideBySide);

                cosmeticRowLayout.child(cosmeticComponentData.first());

                if(cosmeticComponentData.second() != null) cosmeticRowButtons.add(cosmeticComponentData.second());
            }

            accessoriesLayout.child(accessoriesRowLayout);
            cosmeticsLayout.child(cosmeticRowLayout);

            accessoriesChecks.add(CollectedPositionedRectangle.of(accessoriesRowLayout, accessoriesRowButtons));

            var checks = CollectedPositionedRectangle.of(cosmeticRowLayout, cosmeticRowButtons);
            if (!checks.isEmpty()) cosmeticChecks.add(checks);

            if (overMaxSlots) break;
        }

        return new BaseLayoutGroup(accessoriesLayout, cosmeticsLayout, accessoriesChecks, cosmeticChecks, totalColumnCount);
    }

    public interface LayoutData {
        default int width() {
            return maxColumnCount() * 18;
        }

        default int height() {
            return maxRowCount() * 18;
        }

        int maxColumnCount();

        int maxRowCount();

        boolean sideBySide();
    }

    protected record BaseLayoutGroup(FlowLayout accessoriesLayout, FlowLayout cosmeticLayout, List<PositionedRectangle> accessoriesBtnChecks, List<PositionedRectangle> cosmeticBtnChecks, int totalColumnCount) {
        public static final BaseLayoutGroup DEFAULT = new BaseLayoutGroup(
                Containers.verticalFlow(Sizing.content(), Sizing.content()),
                Containers.verticalFlow(Sizing.content(), Sizing.content()),
                List.of(),
                List.of(),
                0);

        public FlowLayout getLayout(boolean isCosmetic) {
            return isCosmetic ? cosmeticLayout : accessoriesLayout;
        }

        public List<PositionedRectangle> getAlternativeChecks(boolean isCosmetic) {
            return isCosmetic ? cosmeticBtnChecks : accessoriesBtnChecks;
        }

        public int width(boolean isCosmetic, boolean sideBySide) {
            int baseWidth = 0;

            if (sideBySide) {
                baseWidth += getComponentWidth(false);

                if (isCosmetic) {
                    baseWidth += getComponentWidth(true) + 3;
                }
            } else {
                baseWidth += getComponentWidth(isCosmetic);
            }

            return baseWidth;
        }

        private int getComponentWidth(boolean isCosmetic) {
            var component = getLayout(isCosmetic);

            if (component.parent() == null) {
                component.inflate(Size.square(Integer.MAX_VALUE));
            }

            return component.width();
        }
    }
}
