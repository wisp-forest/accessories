package io.wispforest.accessories.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.client.GuiGraphicsUtils;
import io.wispforest.accessories.client.gui.components.ComponentUtils;
import io.wispforest.accessories.client.gui.components.InventoryEntityComponent;
import io.wispforest.accessories.menu.SlotTypeAccessible;
import io.wispforest.accessories.menu.variants.AccessoriesExperimentalMenu;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import io.wispforest.accessories.pond.owo.ExclusiveBoundingArea;
import io.wispforest.accessories.pond.owo.MutableBoundingArea;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.util.Observable;
import io.wispforest.owo.util.pond.OwoSlotExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AccessoriesExperimentalScreen extends BaseOwoHandledScreen<FlowLayout, AccessoriesExperimentalMenu> implements AccessoriesScreenBase, ContainerScreenExtension {

    public AccessoriesExperimentalScreen(AccessoriesExperimentalMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);

        this.inventoryLabelX = 42069;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    protected SlotComponent slotAsComponent(int index) {
        return new ExtendedSlotComponent(index);
    }

    //--

    public static final Set<Integer> changedSlots = new HashSet<>();

    @Override
    protected void disableSlot(int index) {
        super.disableSlot(index);

        changedSlots.add(index);
    }

    @Override
    protected void disableSlot(Slot slot) {
        super.disableSlot(slot);

        changedSlots.add(slot.index);
    }

    @Override
    protected void enableSlot(int index) {
        super.enableSlot(index);

        changedSlots.add(index);
    }

    @Override
    protected void enableSlot(Slot slot) {
        super.enableSlot(slot);

        changedSlots.add(slot.index);
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if(!changedSlots.isEmpty()) {
            var slots = this.getMenu().slots;

            var changes = changedSlots.stream()
                    .map(i -> (i < slots.size()) ? slots.get(i) : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(slot -> slot.index, slot -> ((OwoSlotExtension)slot).owo$getDisabledOverride()));

            this.getMenu().sendMessage(new AccessoriesExperimentalMenu.ToggledSlots(changes));

            changedSlots.clear();
        }
    }

    @Override
    public final LivingEntity targetEntityDefaulted() {
        var targetEntity = this.menu.targetEntity();

        return (targetEntity != null) ? targetEntity : this.minecraft.player;
    }

    //--

    public List<io.wispforest.owo.ui.core.Component> hoverComponents = new ArrayList<>();

    @Override
    protected void build(FlowLayout rootComponent) {
        hoverComponents.clear();
        changedSlots.clear();

        rootComponent.allowOverflow(true)
                .surface(Surface.VANILLA_TRANSLUCENT);
        //rootComponent.surface(SLOT_RENDERING_SURFACE);

        var baseChildren = new ArrayList<io.wispforest.owo.ui.core.Component>();

        //--

        for (int i = 0; i < this.getMenu().slots.size(); i++) {
            this.disableSlot(i);
        }

        var menu = this.getMenu();

        var slots = this.getMenu().slots;

        //--

        {
            var playerLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());

            playerLayout.allowOverflow(true);

            playerLayout.padding(Insets.of(6))
                    .surface(Surface.PANEL);

            int row = 0;

            var rowLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .configure((FlowLayout layout) -> {
                        layout.surface(SLOT_RENDERING_SURFACE)
                                .allowOverflow(true);
                    });

            int rowCount = 0;

            for (int i = 0; i < menu.startingAccessoriesSlot; i++) {
                var slotComponent = this.slotAsComponent(i);

                this.enableSlot(i);

                rowLayout.child(slotComponent.margins(Insets.of(1)));

                if(row >= 8) {
                    playerLayout.child(rowLayout);

                    rowLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                            .configure((FlowLayout layout) -> {
                                layout.surface(SLOT_RENDERING_SURFACE)
                                        .allowOverflow(true);
                            });

                    rowCount++;

                    if(rowCount == 3) rowLayout.margins(Insets.top(4));

                    row = 0;
                } else {
                    row++;
                }
            }

            //playerLayout.positioning(Positioning.relative(50, 70));

            baseChildren.add(playerLayout);
        }

        //--

        var armorAndEntityLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(2)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        {
            var armorsLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .horizontalAlignment(HorizontalAlignment.CENTER);

            var innerSpacingComponent = Containers.horizontalFlow(Sizing.fixed(60), Sizing.fixed(84));

            var outerLeftArmorLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            var outerRightArmorLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content());

            var armorSlotsLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());

            armorSlotsLayout.surface(SLOT_RENDERING_SURFACE)
                    .allowOverflow(true);

            outerLeftArmorLayout.child(armorSlotsLayout);

            var cosmeticArmorSlotsLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());

            cosmeticArmorSlotsLayout.surface(SLOT_RENDERING_SURFACE)
                    .allowOverflow(true);

            outerRightArmorLayout.child(cosmeticArmorSlotsLayout);

            for (int i = 0; i < 4; i++) {
                var armor = menu.startingAccessoriesSlot + (i * 2);
                var cosmeticArmor = armor + 1;

                this.enableSlot(armor);
                this.enableSlot(cosmeticArmor);

                var armorSlot = this.slotAsComponent(armor)
                        .margins(Insets.of(1));

                var cosmeticArmorSlot = this.slotAsComponent(cosmeticArmor)
                        .margins(Insets.of(1));

                armorSlotsLayout.child(armorSlot);
                cosmeticArmorSlotsLayout.child(cosmeticArmorSlot);
            }

            //outerArmorLayout.positioning(Positioning.relative(50, 20));

            outerLeftArmorLayout.surface(Surface.PANEL)
                    .padding(Insets.of(6));

            outerRightArmorLayout.surface(Surface.PANEL)
                    .padding(Insets.of(6));

            armorsLayout.child(outerLeftArmorLayout);

            armorsLayout.child(innerSpacingComponent);
            ((ExclusiveBoundingArea) armorsLayout).addExclusionZone(innerSpacingComponent);

            armorsLayout.child(outerRightArmorLayout);

            //--

            var entityContainer = Containers.verticalFlow(Sizing.content(), Sizing.fixed(131 + 12))
                    .child(
                            Containers.verticalFlow(Sizing.content(), Sizing.content())
                                    .child(
                                            new InventoryEntityComponent<>(Sizing.fixed(131), this.targetEntityDefaulted())
                                                    .scaleToFitVertically(true)
                                                    .allowMouseRotation(true)
                                                    .horizontalSizing(Sizing.fixed(108))
                                    )
                                    .surface(Surface.flat(Color.BLACK.argb()))
                    )
                    .child(
                            armorsLayout.positioning(Positioning.relative(50, 50))
                                    .zIndex(10)
                    )
                    .padding(Insets.of(6))
                    .surface(Surface.PANEL);

            armorAndEntityLayout.child(0, entityContainer);
        }

        baseChildren.add(armorAndEntityLayout);

        //--

        int screenState = 1;

        if(screenState == 0){
            var accessoriesStartingIndex = menu.startingAccessoriesSlot + 8;

            var columnCount = 6;

            var rowCount = (int) Math.ceil(((slots.size() - accessoriesStartingIndex) / 2f) / columnCount);

            var cosmeticGridLayout = Containers.grid(Sizing.content(), Sizing.content(), rowCount, columnCount);
            var accessoriesGridLayout = Containers.grid(Sizing.content(), Sizing.content(), rowCount, columnCount);

            rowLoop: for (int row = 0; row < rowCount; row++) {
                var colStartingIndex = accessoriesStartingIndex + (row * (columnCount * 2));

                for (int col = 0; col < columnCount; col++) {
                    var cosmetic = colStartingIndex + (col * 2);
                    var accessory = cosmetic + 1;

                    if(accessory >= slots.size() || cosmetic >= slots.size()) {
                        break rowLoop;
                    }

                    this.enableSlot(cosmetic);
                    this.enableSlot(accessory);

                    var cosmeticSlot = this.slotAsComponent(cosmetic)
                            .margins(Insets.of(1));

                    var accessorySlot = this.slotAsComponent(accessory)
                            .margins(Insets.of(1));

                    cosmeticGridLayout.child(cosmeticSlot, row, (columnCount - 1) - col);
                    accessoriesGridLayout.child(accessorySlot, row, col);
                }
            }

            accessoriesGridLayout.surface(SLOT_RENDERING_SURFACE)
                    .allowOverflow(true);

            cosmeticGridLayout.surface(SLOT_RENDERING_SURFACE)
                    .allowOverflow(true);

            var groupedContainer = Containers.horizontalFlow(Sizing.content(), Sizing.content());

            baseChildren.add(groupedContainer);

            groupedContainer.child(
                    Containers.verticalFlow(Sizing.content(), Sizing.content())
                            .child(
                                    Components.label(Component.literal("Accessories"))
                                            .color(Color.ofFormatting(ChatFormatting.BLACK))
                                            .margins(Insets.of(1, 2, 0, 0))
                            )
                            .child(accessoriesGridLayout)
                            .horizontalAlignment(HorizontalAlignment.CENTER)
                            .surface(Surface.PANEL)
                            .padding(Insets.of(6))
                            .positioning(Positioning.relative(35, 45)));

            groupedContainer.child(
                    Containers.verticalFlow(Sizing.content(), Sizing.content())
                            .child(
                                    Components.label(Component.literal("Cosmetics"))
                                            .color(Color.ofFormatting(ChatFormatting.BLACK))
                                            .margins(Insets.of(1, 2, 0, 0))
                            )
                            .child(cosmeticGridLayout)
                            .horizontalAlignment(HorizontalAlignment.CENTER)
                            .surface(Surface.PANEL)
                            .padding(Insets.of(6))
                            .positioning(Positioning.relative(65, 45)));
        } else {
            Map<Integer, PageLayouts> pages = new LinkedHashMap<>();

            var pageStartingSlotIndex = menu.startingAccessoriesSlot + 8;

            var gridSize = 6;

            var maxColumnCount = gridSize;
            var maxRowCount = gridSize;

            var minimumWidth = maxColumnCount * 18;
            var minimumHeight = maxRowCount * 18;

            var totalRowCount = (int) Math.ceil(((slots.size() - pageStartingSlotIndex) / 2f) / maxColumnCount);

            if(totalRowCount < maxRowCount) maxColumnCount = totalRowCount;

            var pageCount = (int) Math.ceil(totalRowCount / (float) maxRowCount);

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                if(pageIndex != 0) pageStartingSlotIndex += (maxRowCount * maxColumnCount * 2);

                totalRowCount -= maxRowCount;

                var rowCount = (totalRowCount < 0) ? maxRowCount + totalRowCount : maxRowCount;

                var cosmeticGridLayout = Containers.grid(Sizing.content(), Sizing.content(), rowCount, maxColumnCount);
                var accessoriesGridLayout = Containers.grid(Sizing.content(), Sizing.content(), rowCount, maxColumnCount);

                rowLoop: for (int row = 0; row < rowCount; row++) {
                    var colStartingIndex = pageStartingSlotIndex + (row * (maxColumnCount * 2));

                    for (int col = 0; col < maxColumnCount; col++) {
                        var cosmetic = colStartingIndex + (col * 2);
                        var accessory = cosmetic + 1;

                        if(accessory >= slots.size() || cosmetic >= slots.size()) {
                            break rowLoop;
                        }

                        //this.enableSlot(cosmetic);
                        if(pageIndex == 0) this.enableSlot(accessory);

                        //--

                        var btnPosition = Positioning.absolute(15, -1);

                        var cosmeticToggleBtn = ComponentUtils.ofSlot((AccessoriesBasedSlot) slots.get(accessory))
                                .zIndex(0)
                                .sizing(Sizing.fixed(5))
                                .positioning(btnPosition);

                        hoverComponents.add(cosmeticToggleBtn);

                        var cosmeticSlot = Containers.verticalFlow(Sizing.fixed(18), Sizing.fixed(18))
                                .child(
                                        this.slotAsComponent(cosmetic)
                                                .margins(Insets.of(1))
                                )
                                .child(
                                        cosmeticToggleBtn
                                ).allowOverflow(true);

                        var cosmeticArea = ((MutableBoundingArea) cosmeticSlot);

                        cosmeticArea.addInclusionZone(cosmeticToggleBtn);
                        cosmeticArea.deepRecursiveChecking(true);

                        //--

                        var accessoryToggleBtn = ComponentUtils.ofSlot((AccessoriesBasedSlot) slots.get(accessory))
                                .zIndex(2)
                                .sizing(Sizing.fixed(5))
                                .positioning(btnPosition);

                        hoverComponents.add(accessoryToggleBtn);

                        var accessorySlot = Containers.verticalFlow(Sizing.fixed(18), Sizing.fixed(18))
                                .child(
                                        this.slotAsComponent(accessory).margins(Insets.of(1))
                                                .zIndex(5)
                                )
                                .child(accessoryToggleBtn)
                                .allowOverflow(true)
                                .id("fucky");

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
                        .surface(SLOT_RENDERING_SURFACE)
                        .allowOverflow(true);

                ((MutableBoundingArea<GridLayout>) cosmeticGridLayout)
                        .deepRecursiveChecking(true)
                        .surface(SLOT_RENDERING_SURFACE)
                        .allowOverflow(true);

                pages.put(pageIndex, new PageLayouts(accessoriesGridLayout, cosmeticGridLayout));
            }

            var pageIndex = Observable.of(0);

            var showCosmetics = new MutableBoolean(false);

            var pageLabel = Components.label(Component.literal((pageIndex.get() + 1) + " / " + pages.size()));

            pageIndex.observe(integer -> pageLabel.text(Component.literal((pageIndex.get() + 1) + " / " + pages.size())));

            var titleBar = Containers.horizontalFlow(Sizing.fixed(minimumWidth), Sizing.content())
                    .child(
                            Components.button(Component.literal("<"), btn -> {
                                var nextPageIndex = pageIndex.get() - 1;

                                if(nextPageIndex >= 0) {
                                    var showCosmeticState = showCosmetics.booleanValue();

                                    var prevPageIndex = pageIndex.get();

                                    pageIndex.set(nextPageIndex);

                                    var lastGrid = pages.get(prevPageIndex).getLayout(showCosmeticState);
                                    var activeGrid = pages.get(nextPageIndex).getLayout(showCosmeticState);

                                    updateSlots(lastGrid, activeGrid);

                                    var titleBarComponent = btn.parent();

                                    var gridContainer = titleBarComponent.parent().childById(FlowLayout.class, "grid_container");

                                    gridContainer.clearChildren();

                                    gridContainer.child(activeGrid);
                                }
                            }).sizing(Sizing.fixed(14))
                                    .margins(Insets.of(2))
                    )
                    .child(
                            Components.button(Component.literal(">"), btn -> {
                                var nextPageIndex = pageIndex.get() + 1;

                                if(nextPageIndex < pages.size()) {
                                    var showCosmeticState = showCosmetics.booleanValue();

                                    var prevPageIndex = pageIndex.get();

                                    pageIndex.set(nextPageIndex);

                                    var lastGrid = pages.get(prevPageIndex).getLayout(showCosmeticState);
                                    var activeGrid = pages.get(nextPageIndex).getLayout(showCosmeticState);

                                    updateSlots(lastGrid, activeGrid);

                                    var titleBarComponent = btn.parent();

                                    var gridContainer = titleBarComponent.parent().childById(FlowLayout.class, "grid_container");

                                    gridContainer.clearChildren();

                                    gridContainer.child(activeGrid);
                                }
                            }).sizing(Sizing.fixed(14))
                    )
                    .child(
                            Containers.horizontalFlow(Sizing.expand(100), Sizing.content())
                                    .child(pageLabel.color(Color.ofFormatting(ChatFormatting.DARK_GRAY)))
                                    .horizontalAlignment(HorizontalAlignment.CENTER)
                    )
                    .child(
                            Components.button(Component.literal(""), btn -> {
                                var titleBarComponent = btn.parent();

                                var showCosmeticState = !showCosmetics.booleanValue();

                                showCosmetics.setValue(showCosmeticState);

                                var gridContainer = titleBarComponent.parent().childById(FlowLayout.class, "grid_container");

                                gridContainer.clearChildren();

                                var pageLayouts = pages.getOrDefault(pageIndex.get(), PageLayouts.DEFAULT);

                                var lastGrid = pageLayouts.getLayout(!showCosmeticState);
                                var activeGrid = pageLayouts.getLayout(showCosmeticState);

                                updateSlots(lastGrid, activeGrid);

                                gridContainer.child(activeGrid);
                            }).renderer((context, button, delta) -> {
                                ButtonComponent.Renderer.VANILLA.draw(context, button, delta);

                                var showCosmeticState = showCosmetics.booleanValue();

                                var textureAtlasSprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                                        .apply(!showCosmeticState ? Accessories.of("gui/slot/cosmetic") : Accessories.of("gui/slot/charm"));

                                var color = (!showCosmeticState ? Color.BLUE.interpolate(Color.WHITE, 0.1f) : Color.BLACK);

                                var red = color.red();
                                var green = color.green();
                                var blue = color.blue();

                                var shard = RenderStateShard.TRANSLUCENT_TRANSPARENCY;

                                shard.setupRenderState();

                                context.blit(button.x() + 2, button.y() + 2, 0, 16,16, textureAtlasSprite, red, green, blue, 0.9f);

                                shard.clearRenderState();
                            }).sizing(Sizing.fixed(20))
                    )
                    .horizontalAlignment(HorizontalAlignment.RIGHT)
                    .verticalAlignment(VerticalAlignment.CENTER);

            var outerCheatLayout = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.fixed((minimumHeight) + 3 + 20 + (2 * 6)))
                    .allowOverflow(true);

            var accessoriesMainLayout = Containers.verticalFlow(Sizing.content(), Sizing.content())
                    .gap(3)
                    .child(titleBar)
                    .child(
                            ((MutableBoundingArea<FlowLayout>)Containers.verticalFlow(Sizing.content(), Sizing.content()))
                                    .deepRecursiveChecking(true)
                                    .child(pages.getOrDefault(pageIndex.get(), PageLayouts.DEFAULT).getLayout(false))
                                    .allowOverflow(true)
                                    .id("grid_container")
                    )
                    .horizontalAlignment(HorizontalAlignment.RIGHT)
                    .surface(Surface.PANEL.and((context, component) -> {
                        var x = component.x();
                        var y = component.y();

                        var width = component.width();
                        var height = component.height();

                        var outerRadius = 3;

                        var color = Color.BLUE.interpolate(Color.WHITE, 0.3f);

                        if(showCosmetics.booleanValue()) {
                            context.drawRectOutline(
                                    x + outerRadius + 1,
                                    y + outerRadius + 1,
                                    width - (outerRadius * 2) - 2,
                                    height - (outerRadius * 2) - 2,
                                    color.argb());
                        }
                    }))
                    .allowOverflow(true)
                    .padding(Insets.of(6))
                    .id("deez_nuts");
                    //.positioning(Positioning.relative(50, 45));

            ((MutableBoundingArea) accessoriesMainLayout).deepRecursiveChecking(true);

            outerCheatLayout.child(accessoriesMainLayout);
            outerCheatLayout.child(Containers.verticalFlow(Sizing.content(), Sizing.expand()));

            ((MutableBoundingArea) outerCheatLayout).deepRecursiveChecking(true);

            armorAndEntityLayout.child(0, outerCheatLayout);

            //baseChildren.add(accessoriesMainLayout);
        }

        var baseLayout = Containers.verticalFlow(Sizing.content(), Sizing.fill())
                .gap(2)
                .children(baseChildren.reversed());

        baseLayout.horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .positioning(Positioning.relative(50, 50));

        rootComponent.child(baseLayout);
    }

    @Override
    @Nullable
    public Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
        for (var child : hoverComponents) {
            if (child.isInBoundingBox(mouseX, mouseY)) return false;
        }

        return ContainerScreenExtension.super.isHovering_Logical(slot, mouseX, mouseY);
    }

    @Override
    @Nullable
    public Boolean isHovering_Rendering(Slot slot, double mouseX, double mouseY) {
        if(slot instanceof SlotTypeAccessible) return false;

        return ContainerScreenExtension.super.isHovering_Rendering(slot, mouseX, mouseY);
    }

    @Override
    public @Nullable Boolean shouldRenderSlot(Slot slot, double mouseX, double mouseY) {
//        if(true) return null;
        if(slot instanceof SlotTypeAccessible) return false;

        return ContainerScreenExtension.super.shouldRenderSlot(slot, mouseX, mouseY);
    }

    public void updateSlots(ParentComponent prevComp, ParentComponent newComp) {
        recursiveSearch(prevComp, ExtendedSlotComponent.class, slotComponent -> this.disableSlot(slotComponent.index()));
        recursiveSearch(newComp, ExtendedSlotComponent.class, slotComponent -> this.enableSlot(slotComponent.index()));
    }

    public static <C extends io.wispforest.owo.ui.core.Component> void recursiveSearch(ParentComponent parentComponent, Class<C> target, Consumer<C> action) {
        for (var child : parentComponent.children()) {
            if(target.isInstance(child)) {
                action.accept((C) child);
            } else if(child instanceof ParentComponent childParent) {
                recursiveSearch(childParent, target, action);
            }
        }
    }

    @Override
    public void onHolderChange(String key) {
//        switch (key) {
//            case "cosmetic" -> updateCosmeticToggleButton();
//        }
    }

    private static Component cosmeticsToggleTooltip(boolean value) {
        return createToggleTooltip("slot.cosmetics", value);
    }

    private static Component createToggleTooltip(String type, boolean value) {
        var key = type + ".toggle." + (!value ? "show" : "hide");

        return Component.translatable(Accessories.translation(key));
    }

    private static final ResourceLocation SLOT = Accessories.of("textures/gui/slot.png");

    public class ExtendedSlotComponent extends SlotComponent {

        protected final int index;

        protected ExtendedSlotComponent(int index) {
            super(index);

            this.index = index;
        }

        public Slot slot() {
            return this.slot;
        }

        public int index() {
            return index;
        }

        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            var screen = AccessoriesExperimentalScreen.this;

            var hoveredSlot = screen.hoveredSlot;

            if(slot() instanceof SlotTypeAccessible || true) {
                this.didDraw = true;

                int i = screen.leftPos;
                int j = screen.topPos;

                boolean bl = true;

                //RenderStateShard.LEQUAL_DEPTH_TEST.setupRenderState();

                if(bl) {
                    context.push();
                    context.translate((float) i, (float) j, 0.0F);

                    screen.renderSlot(context, slot());

                    context.pop();

                    RenderSystem.disableDepthTest();

                }

                if (this.slot.equals(hoveredSlot) && hoveredSlot.isHighlightable()) {
                    context.push();
                    //context.translate(0, 0, 8);

                    context.fillGradient(RenderType.guiOverlay(), x(), y(), x() + 16, y() + 16, -2130706433, -2130706433, 0);

                    context.pop();

                    //RenderSystem.disableDepthTest();
                }
            } else {
                super.draw(context, mouseX, mouseY, partialTicks, delta);
            }
        }
    }

    private static final Surface SLOT_RENDERING_SURFACE = (context, component) -> {
        var slotComponents = new ArrayList<AccessoriesExperimentalScreen.ExtendedSlotComponent>();

        recursiveSearch(component, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponents::add);

        context.push();
        context.translate(component.x(), component.y(), 0);

        GuiGraphicsUtils.batched(context, SLOT, slotComponents, (bufferBuilder, poseStack, slotComponent) -> {
            var slot = slotComponent.slot();

            if (!slot.isActive()) return;

            GuiGraphicsUtils.blit(bufferBuilder, poseStack, slotComponent.x() - component.x() - 1, slotComponent.y() - component.y() - 1, 18);
        });

        context.pop();
    };

    public record PageLayouts(GridLayout accessoriesLayout, GridLayout cosmeticLayout) {
        private static final PageLayouts DEFAULT = new PageLayouts(
                Containers.grid(Sizing.content(), Sizing.content(), 0, 0),
                Containers.grid(Sizing.content(), Sizing.content(), 0, 0));

        public GridLayout getLayout(boolean isCosmetic) {
            return isCosmetic ? cosmeticLayout : accessoriesLayout;
        }
    }
}
