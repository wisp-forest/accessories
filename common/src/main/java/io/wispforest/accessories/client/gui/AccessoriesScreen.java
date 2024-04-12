package io.wispforest.accessories.client.gui;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import io.wispforest.accessories.impl.SlotGroupImpl;
import io.wispforest.accessories.networking.server.MenuScroll;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Range;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.glfw.GLFW;

import java.lang.Math;
import java.util.*;

public class AccessoriesScreen extends EffectRenderingInventoryScreen<AccessoriesMenu> implements ContainerScreenExtension {

    public static final ResourceLocation SLOT_FRAME = Accessories.of("textures/gui/slot.png");

    public static final ResourceLocation ACCESSORIES_INVENTORY_LOCATION = Accessories.of("textures/gui/container/accessories_inventory.png");

    protected static final ResourceLocation BACKGROUND_PATCH = Accessories.of("background_patch");

    protected static final ResourceLocation SCROLL_BAR_PATCH = Accessories.of("scroll_bar_patch");
    protected static final ResourceLocation SCROLL_BAR = Accessories.of("scroll_bar");

    protected static final ResourceLocation HORIZONTAL_TABS = Accessories.of("textures/gui/container/horizontal_tabs_small.png");

    @Nullable
    public static String HOVERED_SLOT_TYPE = null;
    public static Vector4i SCISSOR_BOX = new Vector4i();

    public static boolean IS_RENDERING_PLAYER = false;
    public static final Map<String, Vec3> NOT_VERY_NICE_POSITIONS = new HashMap<>();

    public static boolean FORCE_TOOLTIP_LEFT = false;

    private final List<Pair<Vec3, Vec3>> accessoryLines = new ArrayList<>();

    private final Map<AccessoriesInternalSlot, ToggleButton> cosmeticButtons = new LinkedHashMap<>();

    private int currentTabPage = 1;

    private int scrollBarHeight = 0;

    private boolean isScrolling = false;

    public AccessoriesScreen(AccessoriesMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, Component.empty());

        this.titleLabelX = 97;
        this.inventoryLabelX = 42069;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var bl = super.mouseClicked(mouseX, mouseY, button);

        if (this.getFocused() instanceof Button) this.clearFocus();

        if (this.insideScrollbar(mouseX, mouseY)) {
            this.isScrolling = true;

            return true;
        }

        if(Accessories.getConfig().clientData.showGroupTabs && this.menu.maxScrollableIndex > 0) {
            int x = getStartingPanelX();
            int y = this.topPos;

            var groups = this.getGroups(x, y);

            for (var value : groups.values()) {
                if (value.isInBounds((int) Math.round(mouseX), (int) Math.round(mouseY))) continue;

                var index = value.startingIndex;

                if (index > this.menu.maxScrollableIndex) index = this.menu.maxScrollableIndex;

                if (index != this.menu.scrolledIndex) {
                    AccessoriesInternals.getNetworkHandler().sendToServer(new MenuScroll(index, false));

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

    public final LivingEntity targetEntityDefaulted() {
        var targetEntity = this.menu.targetEntity();

        return (targetEntity != null) ? targetEntity : this.minecraft.player;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int leftPos = this.leftPos;
        int topPos = this.topPos;

        guiGraphics.blit(ACCESSORIES_INVENTORY_LOCATION, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight);

        //--

        var scissorStart = new Vector2i(leftPos + 26, topPos + 8);
        var scissorEnd = new Vector2i(leftPos + 26 + 124, topPos + 8 + 70);
        var size = new Vector2i((scissorEnd.x - scissorStart.x)/2, scissorEnd.y - scissorStart.y);

        IS_RENDERING_PLAYER = true;

        SCISSOR_BOX.set(scissorStart.x, scissorStart.y, scissorEnd.x, scissorEnd.y);

        if (hoveredSlot instanceof AccessoriesInternalSlot slot) {
            HOVERED_SLOT_TYPE = slot.container.getSlotName() + slot.getContainerSlot();
        }

        renderEntityInInventoryFollowingMouseRotated(guiGraphics, scissorStart, size, scissorStart, scissorEnd, mouseX, mouseY, 0);

        renderEntityInInventoryFollowingMouseRotated(guiGraphics, new Vector2i(scissorStart).add(size.x, 0), size, scissorStart, scissorEnd, mouseX, mouseY, 180);

        IS_RENDERING_PLAYER = false;

        HOVERED_SLOT_TYPE = null;

        //--

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 0);

        int x = getStartingPanelX();
        int y = this.topPos;

        int height = getPanelHeight();
        int width = getPanelWidth();

        guiGraphics.blitSprite(AccessoriesScreen.BACKGROUND_PATCH, x + 6, y, width, height); //147

        if (menu.overMaxVisibleSlots) {
            guiGraphics.blitSprite(AccessoriesScreen.SCROLL_BAR_PATCH, x + 13, y + 7 + upperPadding, 8, height - 22);
        }

        guiGraphics.pose().popPose();

        for (Slot slot : this.menu.slots) {
            if (!(slot.container instanceof ExpandedSimpleContainer) || !slot.isActive()) continue;

            var pose = guiGraphics.pose();

            pose.pushPose();

            pose.translate(-1, -1, 0);

            pose.pushPose();

            if (slot instanceof AccessoriesInternalSlot accessoriesSlot) {
                var positionKey = accessoriesSlot.container.getSlotName() + accessoriesSlot.getContainerSlot();

                var vec = NOT_VERY_NICE_POSITIONS.getOrDefault(positionKey, null);

                if (!accessoriesSlot.isCosmetic && vec != null && (menu.areLinesShown())) {
                    var start = new Vec3(slot.x + this.leftPos + 17, slot.y + this.topPos + 9, 5000);
                    var vec3 = vec.add(0, 0, 5000);

                    this.accessoryLines.add(Pair.of(start, vec3));
                }
            }

            pose.popPose();

            guiGraphics.blit(SLOT_FRAME, slot.x + this.leftPos, slot.y + this.topPos, 0, 0, 18, 18, 18, 18);

            pose.popPose();
        }
    }

    private static final int upperPadding = 8;

    private int getPanelHeight() {
        return getPanelHeight(upperPadding);
    }

    private int getPanelHeight(int upperPadding) {
        return 14 + (Math.min(menu.totalSlots, 8) * 18) + upperPadding;
    }

    private int getPanelWidth() {
        int width = 8 + 18 + 18;

        if (menu.isCosmeticsOpen()) width += 18 + 2;

        if (!menu.overMaxVisibleSlots) width -= 12;

        return width;
    }

    private int getStartingPanelX() {
        int x = this.leftPos - ((menu.isCosmeticsOpen()) ? 72 : 52);

        if (!menu.overMaxVisibleSlots) x += 12;

        return x;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (insideScrollbar(mouseX, mouseY) || (this.hoveredSlot != null && this.hoveredSlot instanceof AccessoriesInternalSlot)) {
            int index = (int) Math.max(Math.min(-scrollY + this.menu.scrolledIndex, this.menu.maxScrollableIndex), 0);

            if (index != menu.scrolledIndex) {
                AccessoriesInternals.getNetworkHandler().sendToServer(new MenuScroll(index, false));

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

            int index = Math.round(this.menu.smoothScroll * this.menu.maxScrollableIndex);

            if (index != menu.scrolledIndex) {
                AccessoriesInternals.getNetworkHandler().sendToServer(new MenuScroll(index, true));

                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        int x = getStartingPanelX() + 13;
        int y = this.topPos + 7 + upperPadding;

        int height = getPanelHeight() - 22;
        int width = 8;

        return this.menu.overMaxVisibleSlots && (mouseX >= x && mouseY >= y && mouseX < (x + width) && mouseY < (y + height));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        //this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
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

            guiGraphics.blitSprite(AccessoriesScreen.SCROLL_BAR, x + 14, startingY, 6, this.scrollBarHeight);
        }

        //--

        if(Accessories.getConfig().clientData.showGroupTabs) {
            for (var entry : getGroups(x, y).entrySet()) {
                var group = entry.getKey();
                var pair = entry.getValue();

                var vector = pair.dimensions();

                int v = (pair.isSelected()) ? vector.w : vector.w * 3;

                guiGraphics.blit(HORIZONTAL_TABS, vector.x, vector.y, 0, v, vector.z, vector.w, 19, vector.w * 4); //32,128

                var textureAtlasSprite = this.minecraft.getTextureAtlas(new ResourceLocation("textures/atlas/blocks.png")).apply(group.icon());

                var poseStack = guiGraphics.pose();

                poseStack.pushPose();

                poseStack.translate(vector.x + 3, vector.y + 3, 0);
                poseStack.translate(1, 1, 0);

                if (pair.isSelected) poseStack.translate(2, 0, 0);

                guiGraphics.blit(0, 0, 0, 8, 8, textureAtlasSprite);

                poseStack.popPose();
            }
        }

        //--

        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if(Accessories.getConfig().clientData.showLineRendering) {
            var buf = guiGraphics.bufferSource().getBuffer(RenderType.LINES);
            var normals = guiGraphics.pose().last().normal();

            for (Pair<Vec3, Vec3> line : this.accessoryLines) {
                var normalVec = line.second().subtract(line.first()).normalize().toVector3f();

                double segments = Math.max(10, ((int) (line.first().distanceTo(line.second()) * 10)) / 100);
                segments *= 2;

                var movement = (System.currentTimeMillis() / (segments * 1000) % 1);
                var delta = movement % (2 / (segments)) % segments;

                if (delta > 0.05) {
                    buf.vertex(line.first().x, line.first().y, line.first().z)
                            .color(255, 255, 255, 255)
                            .overlayCoords(OverlayTexture.NO_OVERLAY)
                            .uv2(LightTexture.FULL_BLOCK)
                            .normal(normals, normalVec.x, normalVec.y, normalVec.z)
                            .endVertex();

                    var pos = new Vec3(
                            Mth.lerp(delta - 0.05, line.first().x, line.second().x),
                            Mth.lerp(delta - 0.05, line.first().y, line.second().y),
                            Mth.lerp(delta - 0.05, line.first().z, line.second().z)
                    );

                    buf.vertex(pos.x, pos.y, pos.z)
                            .color(255, 255, 255, 255)
                            .overlayCoords(OverlayTexture.NO_OVERLAY)
                            .uv2(LightTexture.FULL_BLOCK)
                            .normal(normals, normalVec.x, normalVec.y, normalVec.z)
                            .endVertex();
                }
                for (int i = 0; i < segments / 2; i++) {
                    var delta1 = ((i * 2) / segments + movement) % 1;
                    var delta2 = ((i * 2 + 1) / segments + movement) % 1;

                    var pos1 = new Vec3(
                            Mth.lerp(delta1, line.first().x, line.second().x),
                            Mth.lerp(delta1, line.first().y, line.second().y),
                            Mth.lerp(delta1, line.first().z, line.second().z)
                    );
                    var pos2 = delta2 > delta1 ? new Vec3(
                            Mth.lerp(delta2, line.first().x, line.second().x),
                            Mth.lerp(delta2, line.first().y, line.second().y),
                            Mth.lerp(delta2, line.first().z, line.second().z)
                    ) : line.second();

                    buf.vertex(pos1.x, pos1.y, pos1.z)
                            .color(255, 255, 255, 255)
                            .overlayCoords(OverlayTexture.NO_OVERLAY)
                            .uv2(LightTexture.FULL_BLOCK)
                            .normal(normals, normalVec.x, normalVec.y, normalVec.z)
                            .endVertex();
                    buf.vertex(pos2.x, pos2.y, pos2.z)
                            .color(255, 255, 255, 255)
                            .overlayCoords(OverlayTexture.NO_OVERLAY)
                            .uv2(LightTexture.FULL_BLOCK)
                            .normal(normals, normalVec.x, normalVec.y, normalVec.z)
                            .endVertex();
                }
            }

            minecraft.renderBuffers().bufferSource().endBatch(RenderType.LINES);

            this.accessoryLines.clear();
        }
    }

    @Override
    public boolean handleComponentClicked(@Nullable Style style) {
        return super.handleComponentClicked(style);
    }

    private Button backButton = null;

    private Button cosmeticToggleButton = null;
    private Button linesToggleButton = null;

    private Button unusedSlotsToggleButton = null;

    private Button tabUpButton = null;
    private Button tabDownButton = null;

    @Override
    protected void init() {
        super.init();

        this.currentTabPage = 1;

        this.cosmeticButtons.clear();

        this.backButton = this.addRenderableWidget(
                Button.builder(Component.empty(), (btn) -> this.minecraft.setScreen(new InventoryScreen(minecraft.player)))
                        .bounds(this.leftPos + 141, this.topPos + 8, 8, 8)
                        .tooltip(Tooltip.create(Component.translatable(Accessories.translation("back.screen"))))
                        .build());

        this.cosmeticToggleButton = this.addRenderableWidget(
                Button.builder(Component.empty(), (btn) -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0))
                        .tooltip(cosmeticsToggleTooltip(this.menu.isCosmeticsOpen()))
                        .bounds(this.leftPos - 27, this.topPos + 7, 18, 6)
                        .build());

        this.unusedSlotsToggleButton = this.addRenderableWidget(
                Button.builder(Component.empty(), (btn) -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2)).tooltip(unusedSlotsToggleButton(this.menu.areUnusedSlotsShown()))
                        .bounds(this.leftPos + 154, this.topPos + 7, 12, 12)
                        .build());

        if(Accessories.getConfig().clientData.showLineRendering) {
            this.linesToggleButton = this.addRenderableWidget(
                    Button.builder(Component.empty(), (btn) -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1)).tooltip(linesToggleTooltip(this.menu.areLinesShown()))
                            .bounds(this.leftPos + 154, this.topPos + 7 + 15, 12, 12)
                            //.bounds(this.leftPos - (this.menu.isCosmeticsOpen() ? 59 : 39), this.topPos + 7, 8, 6)
                            .build());
        }

        int aceesoriesSlots = 0;

        for (Slot slot : this.menu.slots) {
            if (!(slot instanceof AccessoriesInternalSlot accessoriesSlot && !accessoriesSlot.isCosmetic)) continue;

            var slotButton = ToggleButton.toggleBuilder(Component.empty(), btn -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, slot.index)).onRender(btn -> {
                var bl = accessoriesSlot.container.shouldRender(accessoriesSlot.getContainerSlot());

                if (bl != btn.toggled()) {
                    btn.toggled(bl);
                    btn.setTooltip(toggleTooltip(bl));
                }
            }).tooltip(toggleTooltip(accessoriesSlot.container.shouldRender(accessoriesSlot.getContainerSlot())))
                    .zIndex(300)
                    .bounds(slot.x + this.leftPos + 13, slot.y + this.topPos - 2, 5, 5)
                    .build()
                    .toggled(accessoriesSlot.container.shouldRender(accessoriesSlot.getContainerSlot()));

            slotButton.visible = accessoriesSlot.isActive();
            slotButton.active = accessoriesSlot.isActive();

            cosmeticButtons.put(accessoriesSlot, this.addWidget(slotButton));

            aceesoriesSlots++;
        }

        if(tabPageCount() > 1) {
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

        this.menu.onScrollToEvent = this::updateAccessoryToggleButtons;

        this.scrollBarHeight = Mth.lerpInt(Math.min(aceesoriesSlots / 20f, 1.0f), 101, 31);

        if (this.scrollBarHeight % 2 == 0) this.scrollBarHeight++;
    }

    private void onTabPageChange(boolean isDown){
        if((this.currentTabPage <= 1 && isDown) || (this.currentTabPage > tabPageCount() && !isDown)) {
            return;
        }

        this.currentTabPage += (isDown) ? -1 : 1;

        var lowerLabel = "Page " + (this.currentTabPage - 1);
        var upperLabel = "Page " + (this.currentTabPage + 1);

        this.tabDownButton.setTooltip(Tooltip.create(Component.literal(lowerLabel)));
        this.tabUpButton.setTooltip(Tooltip.create(Component.literal(upperLabel)));

//        this.tabDownButton.setMessage(Component.literal(lowerLabel));
//        this.tabUpButton.setMessage(Component.literal(upperLabel));

        if(this.currentTabPage <= 1) {
            this.tabDownButton.active = false;
        } else if(!this.tabDownButton.active) {
            this.tabDownButton.active = true;
        }

        if(this.currentTabPage >= tabPageCount()) {
            this.tabUpButton.active = false;
        } else if(!this.tabUpButton.active) {
            this.tabUpButton.active = true;
        }
    }

    public void updateButtons(String name) {
        switch (name) {
            case "lines" -> updateLinesButton();
            case "cosmetic" -> updateCosmeticToggleButton();
            case "unused_slots" -> updateUnusedSlotToggleButton();
        }
    }

    public void updateLinesButton() {
        if(Accessories.getConfig().clientData.showLineRendering) {
            this.linesToggleButton.setTooltip(linesToggleTooltip(this.menu.areLinesShown()));
        }
    }

    public void updateCosmeticToggleButton() {
        this.cosmeticToggleButton.setTooltip(cosmeticsToggleTooltip(this.menu.isCosmeticsOpen()));
    }

    public void updateUnusedSlotToggleButton() {
        this.unusedSlotsToggleButton.setTooltip(unusedSlotsToggleButton(this.menu.areUnusedSlotsShown()));
        this.menu.reopenMenu();
        //AccessoriesClient.attemptToOpenScreen();
    }

    public void updateAccessoryToggleButtons(){
        for (var entry : cosmeticButtons.entrySet()) {
            var accessoriesSlot = entry.getKey();
            var btn = entry.getValue();

            if(!accessoriesSlot.isActive()){
                btn.active = false;
                btn.visible = false;
            } else {
                btn.setTooltip(toggleTooltip(accessoriesSlot.container.shouldRender(accessoriesSlot.getContainerSlot())));

                btn.setX(accessoriesSlot.x + this.leftPos + 13);
                btn.setY(accessoriesSlot.y + this.topPos - 2);

                btn.toggled(accessoriesSlot.container.shouldRender(accessoriesSlot.getContainerSlot()));

                btn.active = true;
                btn.visible = true;
            }
        }
    }

    private static Tooltip cosmeticsToggleTooltip(boolean value) {
        var key = "slot.cosmetics.toggle." + (!value ? "show" : "hide");

        return Tooltip.create(Component.translatable(Accessories.translation(key)));
    }

    private static Tooltip linesToggleTooltip(boolean value) {
        var key = "lines.toggle." + (!value ? "show" : "hide");

        return Tooltip.create(Component.translatable(Accessories.translation(key)));
    }

    private static Tooltip unusedSlotsToggleButton(boolean value) {
        var key = "unused_slots.toggle." + (!value ? "show" : "hide");

        return Tooltip.create(Component.translatable(Accessories.translation(key)));
    }

    private static Tooltip toggleTooltip(boolean value) {
        var key = "display.toggle." + (!value ? "show" : "hide");

        return Tooltip.create(Component.translatable(Accessories.translation(key)));
    }

    @Override
    public @Nullable Boolean isHovering(Slot slot, double mouseX, double mouseY) {
        for (var child : this.children()) {
            if (child instanceof ToggleButton btn && btn.isMouseOver(mouseX, mouseY)) return false;
        }

        return ContainerScreenExtension.super.isHovering(slot, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveredSlot instanceof AccessoriesInternalSlot slot) {
            FORCE_TOOLTIP_LEFT = true;

            if (slot.getItem().isEmpty() && slot.container.slotType() != null) {
                guiGraphics.renderTooltip(Minecraft.getInstance().font, slot.getTooltipData(), Optional.empty(), x, y);

                return;
            }
        }

        if(Accessories.getConfig().clientData.showGroupTabs) {
            int panelX = getStartingPanelX();
            int panelY = this.topPos;

            for (var entry : getGroups(panelX, panelY).entrySet()) {
                if (!entry.getValue().isInBounds(x, y)) continue;

                var tooltipData = new ArrayList<Component>();

                tooltipData.add(Component.translatable(entry.getKey().translation()));

                guiGraphics.renderTooltip(Minecraft.getInstance().font, tooltipData, Optional.empty(), x, y);

                break;
            }
        }

        super.renderTooltip(guiGraphics, x, y);

        FORCE_TOOLTIP_LEFT = false;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int x, int y, int mouseButton) {
        boolean flag = mouseX < (double) x || mouseY < (double) y || mouseX >= (double) (x + width) || mouseY >= (double) (y + height);
        boolean flag1 = (double) (x - 147) < mouseX && mouseX < (double) x && (double) y < mouseY && mouseY < (double) (y + height);
        return flag && !flag1;
    }

    public static int tabPageCount() {
        var groups = SlotGroupLoader.INSTANCE.getSharedGroups(true);

        return (int) Math.ceil(groups.size() / 9f);
    }

    // MAX 9
    private Map<SlotGroup, SlotGroupData> getGroups(int x, int y){
        Set<String> selectedGroup = new HashSet<>();

        int currentIndexOffset = 0;

        var groups = this.getMenu().validGroups.stream()
                .sorted(Comparator.comparingInt(SlotGroup::order).reversed())
                .toList();

        if(tabPageCount() > 1) {
            var lowerBound = (this.currentTabPage - 1) * 9;
            var upperBound = lowerBound + 9;

            if(upperBound > groups.size()) upperBound = groups.size();

            groups = groups.subList(lowerBound, upperBound);
        }

        var groupToIndex = new HashMap<SlotGroup, Integer>();

        var bottomIndex = this.menu.scrolledIndex;
        var upperIndex = bottomIndex + 8 - 1;

        var scrollRange = Range.between(bottomIndex, upperIndex, Integer::compareTo);

        for (var group : groups) {
            var groupSize = group.slots().stream()
                    .map(s -> SlotTypeLoader.getSlotType(Minecraft.getInstance().player.clientLevel, s))
                    .filter(Objects::nonNull)
                    .mapToInt(SlotType::amount)
                    .sum();

            var groupMinIndex = currentIndexOffset;
            var groupMaxIndex = groupMinIndex + groupSize - 1;

            var groupRange = Range.between(groupMinIndex, groupMaxIndex, Integer::compareTo);

            if(groupRange.isOverlappedBy(scrollRange)) {
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
            if((yOffset + height) > maxHeight) break;

            var selected = selectedGroup.contains(group.name());

            int xOffset = (selected) ? 0 : 2;

            groupValues.put(group, new SlotGroupData(new Vector4i(tabX + xOffset, tabY + yOffset, width - xOffset, height), selected, groupToIndex.get(group)));

            yOffset += height + 1;
        }

        return groupValues;
    }

    private static SlotGroupImpl copy(SlotGroup group) {
        return new SlotGroupImpl(group.name() + 1, group.order(), group.slots(), group.icon());
    }

    private record SlotGroupData(Vector4i dimensions, boolean isSelected, int startingIndex){
        private boolean isInBounds(int x, int y){
            return (x > dimensions.x) && (y > dimensions.y) && (x < dimensions.x + dimensions.z) && (y < dimensions.y + dimensions.w);
        }
    }

    //--

    private void renderEntityInInventoryFollowingMouseRotated(GuiGraphics guiGraphics, Vector2i pos, Vector2i size, Vector2i scissorStart, Vector2i scissorEnd, float mouseX, float mouseY, float rotation) {
        //30, 0.0625F, mousePos, targetEntity, 0
        int scale = 30;
        float yOffset = 0.0625F;
        var entity = this.targetEntityDefaulted();

        float f = (float) (pos.x + pos.x + size.x) / 2.0F;
        float g = (float) (pos.y + pos.y + size.y) / 2.0F;
        guiGraphics.enableScissor(scissorStart.x, scissorStart.y, scissorEnd.x, scissorEnd.y);
        float h = (float) Math.atan(((scissorStart.x + scissorStart.x + size.x )/2f - mouseX) / 40.0F);
        float i = (float) Math.atan(((scissorStart.y + scissorStart.y + size.y )/2f - mouseY) / 40.0F);
        Quaternionf quaternionf = (new Quaternionf()).rotateZ(3.1415927F).rotateY((float) (rotation * (Math.PI/180)));
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
        InventoryScreen.renderEntityInInventory(guiGraphics, f, g, scale, vector3f, quaternionf, quaternionf2, entity);
        entity.yBodyRot = j;
        entity.setYRot(k);
        entity.setXRot(l);
        entity.yHeadRotO = m;
        entity.yHeadRot = n;
        guiGraphics.disableScissor();
    }
}