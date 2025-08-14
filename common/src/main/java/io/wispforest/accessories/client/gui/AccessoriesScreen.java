package io.wispforest.accessories.client.gui;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.client.AccessoriesFunkyRenderingState;
import io.wispforest.accessories.client.DrawUtils;
import io.wispforest.accessories.client.gui.utils.Line3d;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.impl.core.ExpandedSimpleContainer;
import io.wispforest.accessories.impl.option.PlayerOptions;
import io.wispforest.accessories.impl.slot.SlotGroupImpl;
import io.wispforest.accessories.menu.AccessoriesInternalSlot;
import io.wispforest.accessories.menu.variants.AccessoriesMenu;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.impl.option.PlayerOption;
import io.wispforest.accessories.networking.holder.SyncOptionChange;
import io.wispforest.accessories.networking.server.MenuScroll;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.apache.commons.lang3.Range;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.glfw.GLFW;

import java.lang.Math;
import java.util.*;

public class AccessoriesScreen extends AbstractContainerScreen<AccessoriesMenu> implements ContainerScreenExtension, AccessoriesScreenBase<AccessoriesMenu> {

    private static final ResourceLocation SLOT = Accessories.of("textures/gui/theme/light/slot.png");

    private static final ResourceLocation ACCESSORIES_INVENTORY_LOCATION = Accessories.of("textures/gui/container/accessories_inventory.png");

    private static final ResourceLocation BACKGROUND_PATCH = Accessories.of("background_patch");

    private static final ResourceLocation SCROLL_BAR_PATCH = Accessories.of("scroll_bar_patch");
    private static final ResourceLocation SCROLL_BAR = Accessories.of("scroll_bar");

    private static final ResourceLocation HORIZONTAL_TABS = Accessories.of("textures/gui/container/horizontal_tabs_small.png");

    private static final WidgetSprites SPRITES_12X12 = new WidgetSprites(Accessories.of("widget/12x12/button"), Accessories.of("widget/12x12/button_disabled"), Accessories.of("widget/12x12/button_highlighted"));
    public static final WidgetSprites SPRITES_8X8 = new WidgetSprites(Accessories.of("widget/8x8/button"), Accessories.of("widget/8x8/button_disabled"), Accessories.of("widget/8x8/button_highlighted"));

    private static final ResourceLocation BACk_ICON = Accessories.of("widget/back");

    private static final ResourceLocation LINE_HIDDEN = Accessories.of("widget/line_hidden");
    private static final ResourceLocation LINE_SHOWN = Accessories.of("widget/line_shown");

    private static final ResourceLocation UNUSED_SLOTS_HIDDEN = Accessories.of("widget/unused_slots_hidden");
    private static final ResourceLocation UNUSED_SLOTS_SHOWN = Accessories.of("widget/unused_slots_shown");

    private final Map<AccessoriesInternalSlot, ToggleButton> cosmeticButtons = new LinkedHashMap<>();

    private int currentTabPage = 1;

    private int scrollBarHeight = 0;

    private boolean isScrolling = false;

    public AccessoriesScreen(AccessoriesMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, Component.empty());

        this.titleLabelX = 97;
        this.inventoryLabelX = 42069;
    }

    private static final int upperPadding = 8;

    public int getPanelHeight() {
        return getPanelHeight(upperPadding);
    }

    public int getPanelHeight(int upperPadding) {
        return 14 + (Math.min(menu.totalSlots, 8) * 18) + upperPadding;
    }

    public int getPanelWidth() {
        int width = 8 + 18 + 18;

        if (menu.isCosmeticsOpen()) width += 18 + 2;

        if (!menu.overMaxVisibleSlots) width -= 12;

        return width;
    }

    public int getStartingPanelX() {
        int x = this.leftPos - ((menu.isCosmeticsOpen()) ? 72 : 52);

        if (!menu.overMaxVisibleSlots) x += 12;

        return x;
    }

    public int leftPos() {
        return this.leftPos;
    }

    public int topPos() {
        return this.topPos;
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        int x = getStartingPanelX() + 13;
        int y = this.topPos + 7 + upperPadding;

        int height = getPanelHeight() - 22;
        int width = 8;

        return this.menu.overMaxVisibleSlots && (mouseX >= x && mouseY >= y && mouseX < (x + width) && mouseY < (y + height));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var bl = super.mouseClicked(mouseX, mouseY, button);

        if (this.getFocused() instanceof Button) this.clearFocus();

        if (this.insideScrollbar(mouseX, mouseY)) {
            this.isScrolling = true;

            return true;
        }

        if (Accessories.config().screenOptions.showGroupTabs() && this.menu.maxScrollableIndex() > 0) {
            int x = getStartingPanelX();
            int y = this.topPos;

            for (var value : this.getGroups(x, y).values()) {
                if (!value.isInBounds((int) Math.round(mouseX), (int) Math.round(mouseY))) continue;

                var index = value.startingIndex;

                if (index > this.menu.maxScrollableIndex()) index = this.menu.maxScrollableIndex();

                if (index != this.menu.scrolledIndex) {
                    AccessoriesNetworking.sendToServer(new MenuScroll(index, false));

                    Minecraft.getInstance().getSoundManager()
                            .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }

                break;
            }
        }

        return bl;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.insideScrollbar(mouseX, mouseY) && button == GLFW.GLFW_MOUSE_BUTTON_1) this.isScrolling = false;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private final List<Vector3d> hoveredAccessoryPositons = new ArrayList<>();
    private final List<Line3d> linesToAccessoryPositions = new ArrayList<>();

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int leftPos = this.leftPos;
        int topPos = this.topPos;

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ACCESSORIES_INVENTORY_LOCATION,
                leftPos, topPos,
                0f, 0f,
                this.imageWidth, this.imageHeight,
                256, 256
        );

        //--

        var scissorStart = new Vector2i(leftPos + 26, topPos + 8);
        var scissorEnd = new Vector2i(leftPos + 26 + 124, topPos + 8 + 70);
        var size = new Vector2i((scissorEnd.x - scissorStart.x) / 2, scissorEnd.y - scissorStart.y);

        // --

        AccessoriesFunkyRenderingState.INSTANCE.wrapEntityRendering(scissorStart.x, scissorStart.y, scissorEnd.x, scissorEnd.y, primaryEntityWrapCall -> {
            primaryEntityWrapCall.accept(() -> {
                renderEntityInInventoryFollowingMouseRotated(guiGraphics, scissorStart, size, scissorStart, scissorEnd, mouseX, mouseY, 0);
            });

            renderEntityInInventoryFollowingMouseRotated(guiGraphics, new Vector2i(scissorStart).add(size.x, 0), size, scissorStart, scissorEnd, mouseX, mouseY, 180);
        });


//        HOVERED_SLOT_TYPE = null;

        //--

        guiGraphics.push()
            .translate(0.0F, 0.0F);

        int x = getStartingPanelX();
        int y = this.topPos;

        int height = getPanelHeight();
        int width = getPanelWidth();

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, AccessoriesScreen.BACKGROUND_PATCH, x + 6, y, width, height); //147

        if (menu.overMaxVisibleSlots) {
            //guiGraphics.blitSprite(AccessoriesScreen.SCROLL_BAR_PATCH, x + 13, y + 7 + upperPadding, 8, height - 22);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, AccessoriesScreen.SCROLL_BAR_PATCH, x + 13, y + 7 + upperPadding, 8, height - 22);
        }

        guiGraphics.pop();

        //--

        guiGraphics.push()
            .translate(-1, -1);

//        for (Slot slot : this.menu.slots) {
//            if (!(slot.container instanceof ExpandedSimpleContainer) || !slot.isActive()) continue;
//
//            if (slot instanceof AccessoriesInternalSlot accessoriesSlot && !accessoriesSlot.getItem().isEmpty()) {
//                var positionKey = accessoriesSlot.accessoriesContainer.getSlotName() + accessoriesSlot.getContainerSlot();
//
//                var vec = NOT_VERY_NICE_POSITIONS.getOrDefault(positionKey, null);
//
//                if (!accessoriesSlot.isCosmetic && vec != null && (menu.areLinesShown())) {
//                    var start = new Vector3d(slot.x + this.leftPos + 17, slot.y + this.topPos + 9, 5000);
//                    var vec3 = vec.add(0, 0, 5000);
//
//                    this.accessoryLines.add(Pair.of(start, vec3));}
//            }
//        }

        this.menu.slots.forEach(slot -> {
            if (!(slot.container instanceof ExpandedSimpleContainer) || !slot.isActive()) return;

            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SLOT, slot.x + this.leftPos, slot.y + this.topPos, 0f, 0f,18, 18, 18, 18);
        });

        if (getHoveredSlot() != null && getHoveredSlot() instanceof AccessoriesInternalSlot slot && slot.isActive() && !slot.getItem().isEmpty()) {
            var positions = AccessoriesFunkyRenderingState.INSTANCE.getNotVeryNicePositions();

            if (positions.containsKey(slot.accessoriesContainer.getSlotName() + slot.getContainerSlot())) {
                hoveredAccessoryPositons.add(positions.get(slot.accessoriesContainer.getSlotName() + slot.getContainerSlot()));

                var positionKey = slot.accessoriesContainer.getSlotName() + slot.getContainerSlot();
                var vec = positions.getOrDefault(positionKey, null);

                if (!slot.isCosmetic && vec != null && (Accessories.config().screenOptions.hoveredOptions.line())) {
                    var start = new Vector3d(slot.x + this.leftPos + 17, slot.y + this.topPos + 9, 5000);
                    var vec3 = vec.add(0, 0, 5000);

                    linesToAccessoryPositions.add(new Line3d(start, vec3));
                }
            }
        }

        guiGraphics.pop();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (insideScrollbar(mouseX, mouseY) || (Accessories.config().screenOptions.allowSlotScrolling() && this.hoveredSlot instanceof AccessoriesInternalSlot)) {
            int index = (int) Math.max(Math.min(-scrollY + this.menu.scrolledIndex, this.menu.maxScrollableIndex()), 0);

            if (index != menu.scrolledIndex) {
                AccessoriesNetworking.sendToServer(new MenuScroll(index, false));

                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isScrolling) {
            int patchYOffset = this.topPos + 7 + upperPadding;
            int height = getPanelHeight();

            this.menu.smoothScroll = Mth.clamp((float) (mouseY - patchYOffset) / (height - 22f), 0.0f, 1.0f); //(menu.smoothScroll + (dragY / (getPanelHeight(upperPadding) - 24)))

            int index = Math.round(this.menu.smoothScroll * this.menu.maxScrollableIndex());

            if (index != menu.scrolledIndex) {
                AccessoriesNetworking.sendToServer(new MenuScroll(index, true));

                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for (var cosmeticButton : this.cosmeticButtons.values()) {
            cosmeticButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        int x = getStartingPanelX();
        int y = this.topPos;

        int panelHeight = getPanelHeight();

        if (this.menu.overMaxVisibleSlots) {
            int startingY = y + upperPadding + 8;

            startingY += this.menu.smoothScroll * (panelHeight - 24 - this.scrollBarHeight);

            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, AccessoriesScreen.SCROLL_BAR, x + 14, startingY, 6, this.scrollBarHeight);
        }

        //--

        if (Accessories.config().screenOptions.showGroupTabs()) {
            for (var entry : getGroups(x, y).entrySet()) {
                var group = entry.getKey();
                var pair = entry.getValue();

                var vector = pair.dimensions();

                int v = (pair.isSelected()) ? vector.w : vector.w * 3;

                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, HORIZONTAL_TABS, vector.x, vector.y, 0, v, vector.z, vector.w, 19, vector.w * 4); //32,128

                guiGraphics.push()
                    .translate(vector.x + 3, vector.y + 3)
                    .translate(1, 1);

                if (pair.isSelected) guiGraphics.translate(2, 0);

                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, group.icon(), 0, 0, 8, 8);

                guiGraphics.pop();
            }
        }

        //--

        if (Accessories.config().screenOptions.hoveredOptions.clickbait()) {
            hoveredAccessoryPositons.forEach(pos -> guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, Accessories.of("highlight/clickbait"), (int) pos.x - 128, (int) pos.y - 128, 100, 256, 256));
            hoveredAccessoryPositons.clear();
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);



        if (!linesToAccessoryPositions.isEmpty() && Accessories.config().screenOptions.hoveredOptions.line()) {
//            guiGraphics.drawSpecial(multiBufferSource -> {
//                var buf = multiBufferSource.getBuffer(RenderType.LINES);
//                var lastPose = guiGraphics.pose().last();
//
//                for (Line3d line : linesToAccessoryPositions) {
//                    var normalVec = line.p2().sub(line.p1(), new Vector3d()).normalize().get(new Vector3f());
//
//                    double segments = Math.max(10, ((int) (line.p1().distance(line.p2()) * 10)) / 100);
//                    segments *= 2;
//
//                    var movement = (System.currentTimeMillis() / (segments * 1000) % 1);
//                    var delta = movement % (2 / (segments)) % segments;
//
//                    var firstVec = line.p1().get(new Vector3f());
//
//                    if (delta > 0.05) {
//                        DrawUtils.addToVertexBuffer(buf, firstVec, lastPose, normalVec);
//                        DrawUtils.addToVertexBuffer(buf, line.lerpPoint(delta - 0.05), lastPose, normalVec);
//                    }
//
//                    for (int i = 0; i < segments / 2; i++) {
//                        var delta1 = ((i * 2) / segments + movement) % 1;
//                        var delta2 = ((i * 2 + 1) / segments + movement) % 1;
//
//                        var pos1 = line.lerpPoint(delta1);
//                        var pos2 = (delta2 > delta1 ? line.lerpPoint(delta2) : line.p2().get(new Vector3f()));
//
//                        DrawUtils.addToVertexBuffer(buf, pos1, lastPose, normalVec);
//                        DrawUtils.addToVertexBuffer(buf, pos2, lastPose, normalVec);
//                    }
//                }
//
//                minecraft.renderBuffers().bufferSource().endBatch(RenderType.LINES);
//
//                linesToAccessoryPositions.clear();
//            });
        }
    }

    private Button cosmeticToggleButton = null;

    private Button unusedSlotsToggleButton = null;

    private Button tabUpButton = null;
    private Button tabDownButton = null;

    @Override
    protected void init() {
        if (!menu.isValidMenu()) {
            Minecraft.getInstance().setScreen(
                    new ErrorScreen(
                            Component.literal("Accessories Screen Opening Error!"),
                            Component.literal("Unable to open Accessories Screen due to desync with the Server!")
                    ));

            return;
        }

        super.init();

        this.currentTabPage = 1;

        this.cosmeticButtons.clear();

        this.addRenderableWidget(
                Button.builder(Component.empty(), (btn) -> this.switchToBaseInventory())
                        .bounds(this.leftPos + 141, this.topPos + 9, 8, 8)
                        .tooltip(Tooltip.create(Component.translatable(Accessories.translationKey("back.screen"))))
                        .build()).adjustRendering((button, guiGraphics, sprite, x, y, width, height) -> {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES_8X8.get(button.active, button.isHoveredOrFocused()), x, y, width, height, ARGB.white(/*button.alpha*/1.0f));

            guiGraphics.pop()
                .translate(0.5, 0.5);

            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACk_ICON, x, y, width - 1, height - 1);

            guiGraphics.pop();

            return true;
        });

        var cosmeticsOpen = this.menu.isCosmeticsOpen();

        this.cosmeticToggleButton = this.addRenderableWidget(
                Button.builder(Component.empty(), (btn) -> {
                            AccessoriesNetworking
                                    .sendToServer(SyncOptionChange.of(PlayerOptions.SHOW_COSMETIC_SLOTS, this.getMenu().owner(), bl -> !bl));
                        })
                        .tooltip(cosmeticsToggleTooltip(cosmeticsOpen))
                        .bounds(this.leftPos - 27 + (cosmeticsOpen ? -20 : 0), this.topPos + 7, (cosmeticsOpen ? 38 : 18), 6)
                        .build());

        var btnOffset = this.topPos + 7;

        this.unusedSlotsToggleButton = this.addRenderableWidget(
                Button.builder(Component.empty(), (btn) -> {
                            AccessoriesNetworking
                                    .sendToServer(SyncOptionChange.of(PlayerOptions.SHOW_UNUSED_SLOTS, this.getMenu().owner(), bl -> !bl));
                        })
                        .tooltip(unusedSlotsToggleButton(this.menu.areUnusedSlotsShown()))
                        .bounds(this.leftPos + 154, btnOffset, 12, 12)
                        .build()).adjustRendering((button, guiGraphics, sprite, x, y, width, height) -> {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES_12X12.get(button.active, button.isHoveredOrFocused()), x, y, width, height, ARGB.white(/*button.alpha*/1.0f));
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, (this.menu.areUnusedSlotsShown() ? UNUSED_SLOTS_SHOWN : UNUSED_SLOTS_HIDDEN), x, y, width, height);

            return true;
        });

        btnOffset += 15;

//        if (Accessories.getConfig().clientData.showLineRendering) {
//            this.linesToggleButton = this.addRenderableWidget(
//                    Button.builder(Component.empty(), (btn) -> {
//                                AccessoriesNetworking
//                                        .sendToServer(SyncHolderChange.of(HolderProperty.LINES_PROP, this.getMenu().owner(), bl -> !bl));
//                            })
//                            .bounds(this.leftPos + 154, btnOffset, 12, 12)
//                            //.bounds(this.leftPos - (this.menu.isCosmeticsOpen() ? 59 : 39), this.topPos + 7, 8, 6)
//                            .build()).adjustRendering((button, guiGraphics, sprite, x, y, width, height) -> {
//                guiGraphics.blitSprite(SPRITES_12X12.get(button.active, button.isHoveredOrFocused()), x, y, width, height);
//                guiGraphics.blitSprite((this.menu.areLinesShown() ? LINE_SHOWN : LINE_HIDDEN), x, y, width, height);
//
//                return true;
//            });
//        }

        int accessoriesSlots = 0;

        for (Slot slot : this.menu.slots) {
            if (!(slot instanceof AccessoriesInternalSlot accessoriesSlot && !accessoriesSlot.isCosmetic)) continue;

            var slotButton = ToggleButton.ofSlot(slot.x + this.leftPos + 13, slot.y + this.topPos - 2, 300, accessoriesSlot);

            slotButton.visible = accessoriesSlot.isActive();
            slotButton.active = accessoriesSlot.isActive();

            cosmeticButtons.put(accessoriesSlot, this.addWidget(slotButton));

            accessoriesSlots++;
        }

        if (tabPageCount() > 1) {
            this.tabDownButton = this.addRenderableWidget(
                    Button.builder(Component.literal("⬆"), button -> this.onTabPageChange(true))
                            .bounds(this.leftPos - 56, this.topPos - 11, 10, 10)
                            .build());

            this.tabDownButton.active = false;

            var height = getPanelHeight();

            this.tabUpButton = this.addRenderableWidget(
                    Button.builder(Component.literal("⬇"), button -> this.onTabPageChange(false))
                            .bounds(this.leftPos - 56, this.topPos + height + 0, 10, 10)
                            .build());

            this.tabUpButton.setTooltip(Tooltip.create(Component.literal("Page 2")));

            this.tabUpButton.active = tabPageCount() != 1;
        }

        this.menu.setScrollEvent(this::updateAccessoryToggleButtons);

        this.scrollBarHeight = Mth.lerpInt(Math.min(accessoriesSlots / 20f, 1.0f), 101, 31);

        if (this.scrollBarHeight % 2 == 0) this.scrollBarHeight++;
    }

    private void onTabPageChange(boolean isDown) {
        if ((this.currentTabPage <= 1 && isDown) || (this.currentTabPage > tabPageCount() && !isDown)) {
            return;
        }

        this.currentTabPage += (isDown) ? -1 : 1;

        var lowerLabel = "Page " + (this.currentTabPage - 1);
        var upperLabel = "Page " + (this.currentTabPage + 1);

        this.tabDownButton.setTooltip(Tooltip.create(Component.literal(lowerLabel)));
        this.tabUpButton.setTooltip(Tooltip.create(Component.literal(upperLabel)));

//        this.tabDownButton.setMessage(Component.literal(lowerLabel));
//        this.tabUpButton.setMessage(Component.literal(upperLabel));

        if (this.currentTabPage <= 1) {
            this.tabDownButton.active = false;
        } else if (!this.tabDownButton.active) {
            this.tabDownButton.active = true;
        }

        if (this.currentTabPage >= tabPageCount()) {
            this.tabUpButton.active = false;
        } else if (!this.tabUpButton.active) {
            this.tabUpButton.active = true;
        }
    }

    @Override
    public void onHolderChange(PlayerOption<?> option) {
        switch (option.name()) {
            case "lines" -> updateLinesButton();
            case "cosmetic" -> updateCosmeticToggleButton();
            case "unused_slots" -> updateUnusedSlotToggleButton();
        }
    }

    public void updateLinesButton() {
//        if (Accessories.getConfig().clientData.showLineRendering) {
//            this.linesToggleButton.setTooltip(linesToggleTooltip(this.menu.areLinesShown()));
//        }
    }

    public void updateCosmeticToggleButton() {
        var btn = this.cosmeticToggleButton;
        btn.setWidth(this.menu.isCosmeticsOpen() ? 38 : 18);
        btn.setX(btn.getX() + (this.menu.isCosmeticsOpen() ? -20 : 20));
        btn.setTooltip(cosmeticsToggleTooltip(this.menu.isCosmeticsOpen()));
    }

    public void updateUnusedSlotToggleButton() {
        this.unusedSlotsToggleButton.setTooltip(unusedSlotsToggleButton(this.menu.areUnusedSlotsShown()));
        this.menu.reopenMenu();
    }

    public void updateAccessoryToggleButtons() {
        for (var entry : cosmeticButtons.entrySet()) {
            var accessoriesSlot = entry.getKey();
            var btn = entry.getValue();

            if (!accessoriesSlot.isActive()) {
                btn.active = false;
                btn.visible = false;
            } else {
                btn.setTooltip(toggleTooltip(accessoriesSlot.accessoriesContainer.shouldRender(accessoriesSlot.getContainerSlot())));

                btn.setX(accessoriesSlot.x + this.leftPos + 13);
                btn.setY(accessoriesSlot.y + this.topPos - 2);

                btn.toggled(accessoriesSlot.accessoriesContainer.shouldRender(accessoriesSlot.getContainerSlot()));

                btn.active = true;
                btn.visible = true;
            }
        }
    }

    private static Tooltip cosmeticsToggleTooltip(boolean value) {
        return createToggleTooltip("slot.cosmetics", value);
    }

    private static Tooltip linesToggleTooltip(boolean value) {
        return createToggleTooltip("lines", value);
    }

    private static Tooltip unusedSlotsToggleButton(boolean value) {
        return createToggleTooltip("unused_slots", value);
    }

    private static Tooltip uniqueSlotsToggleButton(boolean value) {
        return createToggleTooltip("unique_slots", value);
    }

    private static Tooltip toggleTooltip(boolean value) {
        return createToggleTooltip("display", value);
    }

    private static Tooltip createToggleTooltip(String type, boolean value) {
        var key = type + ".toggle." + (!value ? "show" : "hide");

        return Tooltip.create(Component.translatable(Accessories.translationKey(key)));
    }

    @Override
    public @Nullable Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
        for (var child : this.children()) {
            if (child instanceof ToggleButton btn && btn.isMouseOver(mouseX, mouseY)) return false;
        }

        return ContainerScreenExtension.super.isHovering_Logical(slot, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveredSlot instanceof AccessoriesInternalSlot slot) {
            AccessoriesScreenBase.FORCE_TOOLTIP_LEFT.setValue(true);

            if (slot.getItem().isEmpty() && slot.accessoriesContainer.slotType() != null) {
                guiGraphics.setTooltipForNextFrame(Minecraft.getInstance().font, slot.getTooltipData(), Optional.empty(), x, y);

                return;
            }
        }

        if (Accessories.config().screenOptions.showGroupTabs()) {
            int panelX = getStartingPanelX();
            int panelY = this.topPos;

            for (var entry : getGroups(panelX, panelY).entrySet()) {
                if (!entry.getValue().isInBounds(x, y)) continue;

                var tooltipData = new ArrayList<Component>();
                var group = entry.getKey();

                tooltipData.add(Component.translatable(group.translation()));
                if (UniqueSlotHandling.isUniqueGroup(group.name(), true)) tooltipData.add(Component.literal(group.name()).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));

                guiGraphics.setTooltipForNextFrame(Minecraft.getInstance().font, tooltipData, Optional.empty(), x, y);

                break;
            }
        }

        super.renderTooltip(guiGraphics, x, y);

        AccessoriesScreenBase.FORCE_TOOLTIP_LEFT.setValue(false);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int x, int y, int mouseButton) {
        int leftPos = this.leftPos;
        int topPos = this.topPos;

        boolean insideMainPanel = (mouseX >= leftPos && mouseX <= leftPos + this.imageWidth)
                && (mouseY >= topPos && mouseY <= topPos + this.imageHeight);

        int sidePanelX = getStartingPanelX();
        int sidePanelY = topPos;

        boolean insideSidePanel = (mouseX >= sidePanelX && mouseX <= sidePanelX + this.getPanelWidth() + this.imageWidth)
                && (mouseY >= sidePanelY && mouseY <= sidePanelY + this.getPanelHeight());

        boolean insideGroupPanel = false;

        if (Accessories.config().screenOptions.showGroupTabs() && this.menu.maxScrollableIndex() > 0) {
            for (var value : this.getGroups(sidePanelX, sidePanelY).values()) {
                if (value.isInBounds((int) Math.round(mouseX), (int) Math.round(mouseY))) {
                    insideGroupPanel = true;
                    break;
                }
            }
        }

        return !(insideMainPanel || insideSidePanel || insideGroupPanel);
    }

    public static int tabPageCount() {
        var groups = SlotGroupLoader.INSTANCE.getGroups(true, true);

        return (int) Math.ceil(groups.size() / 9f);
    }

    // MAX 9
    private Map<SlotGroup, SlotGroupData> getGroups(int x, int y) {
        var groups = this.getMenu().validGroups().stream()
                .sorted(Comparator.comparingInt(SlotGroup::order).reversed())
                .toList();

        if (tabPageCount() > 1) {
            var lowerBound = (this.currentTabPage - 1) * 9;
            var upperBound = lowerBound + 9;

            if (upperBound > groups.size()) upperBound = groups.size();

            groups = groups.subList(lowerBound, upperBound);
        }

        var bottomIndex = this.menu.scrolledIndex;
        var upperIndex = bottomIndex + 8 - 1;

        var scrollRange = Range.between(bottomIndex, upperIndex, Integer::compareTo);

        var targetEntity = this.targetEntityDefaulted();
        var containers = targetEntity.accessoriesCapability().getContainers();

        var slotToSize = new HashMap<String, Integer>();

        for (var slotType : EntitySlotLoader.getEntitySlots(targetEntity).values()) {
            var usedSlots = this.getMenu().usedSlots();
            if (usedSlots != null && !usedSlots.contains(slotType)) continue;

            var container = containers.get(slotType.name());
            if(container == null) continue;

            slotToSize.put(slotType.name(), container.getAccessories().getContainerSize());
        }

        int currentIndexOffset = 0;

        var groupToIndex = new HashMap<SlotGroup, Integer>();
        var selectedGroup = new HashSet<String>();

        for (var group : groups) {
            var groupSize = slotToSize.entrySet().stream()
                    .filter(entry -> group.slots().contains(entry.getKey()))
                    .mapToInt(Map.Entry::getValue)
                    .sum();

            if (groupSize <= 0) continue;

            var groupMinIndex = currentIndexOffset;
            var groupMaxIndex = groupMinIndex + groupSize - 1;

            var groupRange = Range.between(groupMinIndex, groupMaxIndex, Integer::compareTo);

            if (groupRange.isOverlappedBy(scrollRange)) {
                selectedGroup.add(group.name());
            }

            groupToIndex.put(group, groupMinIndex);

            currentIndexOffset += groupSize;
        }

        int maxHeight = getPanelHeight() - 4;

        int width = 19;//32;
        int height = 16;//28;

        int tabY = y + 4;
        int tabX = x - (width - 10);

        int yOffset = 0;

        var groupValues = new HashMap<SlotGroup, SlotGroupData>();

        for (var group : groups) {
            if ((yOffset + height) > maxHeight) break;

            var selected = selectedGroup.contains(group.name());

            int xOffset = (selected) ? 0 : 2;

            var index = groupToIndex.get(group);

            if (index == null) continue;

            groupValues.put(group, new SlotGroupData(new Vector4i(tabX + xOffset, tabY + yOffset, width - xOffset, height), selected, index));

            yOffset += height + 1;
        }

        return groupValues;
    }

    private static SlotGroupImpl copy(SlotGroup group) {
        return new SlotGroupImpl(group.name() + 1, group.order(), group.slots(), group.icon());
    }

    private record SlotGroupData(Vector4i dimensions, boolean isSelected, int startingIndex) {
        private boolean isInBounds(int x, int y) {
            return (x > dimensions.x) && (y > dimensions.y) && (x < dimensions.x + dimensions.z) && (y < dimensions.y + dimensions.w);
        }
    }

    //--

    private void renderEntityInInventoryFollowingMouseRotated(GuiGraphics guiGraphics, Vector2i pos, Vector2i size, Vector2i scissorStart, Vector2i scissorEnd, float mouseX, float mouseY, float rotation) {
        int scale = 30;
        float yOffset = 0.0625F;
        var entity = this.targetEntityDefaulted();

        float f = (float) (pos.x + pos.x + size.x) / 2.0F;
        float g = (float) (pos.y + pos.y + size.y) / 2.0F;
        guiGraphics.enableScissor(scissorStart.x, scissorStart.y, scissorEnd.x, scissorEnd.y);
        float h = (float) Math.atan(((scissorStart.x + scissorStart.x + size.x) / 2f - mouseX) / 40.0F);
        float i = (float) Math.atan(((scissorStart.y + scissorStart.y + size.y) / 2f - mouseY) / 40.0F);
        Quaternionf quaternionf = (new Quaternionf()).rotateZ(3.1415927F).rotateY((float) (rotation * (Math.PI / 180)));
        Quaternionf quaternionf2 = (new Quaternionf()).rotateX(i * 20.0F * 0.017453292F);
        quaternionf.mul(quaternionf2);
        float j = entity.yBodyRot;
        float k = entity.getYRot();
        float l = entity.getXRot();
        float m = entity.yHeadRotO;
        float n = entity.yHeadRot;
        entity.yBodyRot = 180.0F + h * 30.0F;
        entity.setYRot(180.0F + h * 40.0F);
        entity.setXRot(-i * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        Vector3f vector3f = new Vector3f(0.0F, entity.getBbHeight() / 2.0F + yOffset, 0.0F);
//        InventoryScreen.renderEntityInInventory(guiGraphics, f, g, scale, vector3f, quaternionf, quaternionf2, entity);
        entity.yBodyRot = j;
        entity.setYRot(k);
        entity.setXRot(l);
        entity.yHeadRotO = m;
        entity.yHeadRot = n;
        guiGraphics.disableScissor();
    }

    public Slot getHoveredSlot() {
        return this.hoveredSlot;
    }
}
