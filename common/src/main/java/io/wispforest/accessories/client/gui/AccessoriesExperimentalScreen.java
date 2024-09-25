package io.wispforest.accessories.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.api.slot.ExtraSlotTypeProperties;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.client.GuiGraphicsUtils;
import io.wispforest.accessories.client.gui.components.*;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.menu.AccessoriesInternalSlot;
import io.wispforest.accessories.menu.SlotTypeAccessible;
import io.wispforest.accessories.menu.variants.AccessoriesExperimentalMenu;
import io.wispforest.accessories.mixin.client.AbstractContainerScreenAccessor;
import io.wispforest.accessories.mixin.client.owo.DiscreteSliderComponentAccessor;
import io.wispforest.accessories.networking.holder.HolderProperty;
import io.wispforest.accessories.networking.holder.SyncHolderChange;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import io.wispforest.owo.mixin.ui.SlotAccessor;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.util.pond.OwoSlotExtension;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.Logger;
import oshi.util.tuples.Triplet;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import static io.wispforest.accessories.client.gui.components.ComponentUtils.BACKGROUND_SLOT_RENDERING_SURFACE;

public class AccessoriesExperimentalScreen extends BaseOwoHandledScreen<FlowLayout, AccessoriesExperimentalMenu> implements AccessoriesScreenBase, ContainerScreenExtension {

    private static final Logger LOGGER = LogUtils.getLogger();

    public AccessoriesExperimentalScreen(AccessoriesExperimentalMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);

        this.inventoryLabelX = 42069;
    }

    //--

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    public List<PositionedRectangle> getComponentRectangles() {
        return this.uiAdapter.rootComponent.children().stream().map(component -> (PositionedRectangle) component).toList();
    }

    @Override
    public <C extends io.wispforest.owo.ui.core.Component> C component(Class<C> expectedClass, String id) {
        return super.component(expectedClass, id);
    }

    private final Map<Integer, Boolean> changedSlots = new HashMap<>();

    @Override
    protected void containerTick() {
        super.containerTick();

        if (this.changedSlots.isEmpty()) return;

        var slots = this.getMenu().slots;

        var changes = this.changedSlots.entrySet().stream()
                .filter(entry -> entry.getKey() < slots.size())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.getMenu().sendMessage(new AccessoriesExperimentalMenu.ToggledSlots(changes));

        this.changedSlots.clear();
    }

    public void hideSlot(int index) {
        hideSlot(this.menu.slots.get(index));
    }

    public void hideSlot(Slot slot) {
        ((SlotAccessor) slot).owo$setX(-300);
        ((SlotAccessor) slot).owo$setY(-300);
    }

    @Override
    public void disableSlot(Slot slot) {
        disableSlot(slot.index);
    }

    @Override
    public void disableSlot(int index) {
        super.disableSlot(index);

        var state = this.changedSlots.getOrDefault(index, null);

        if (state != null && state) return;

        hideSlot(index);

        this.changedSlots.put(index, true);
    }

    public void disableSlots(int ...index) {
        for (int i : index) disableSlot(i);
    }

    @Override
    public void enableSlot(Slot slot) {
        enableSlot(slot.index);
    }

    @Override
    public void enableSlot(int index) {
        super.enableSlot(index);

        var state = this.changedSlots.getOrDefault(index, null);

        if (state != null && !state) return;

        this.changedSlots.put(index, false);
    }

    @Override
    protected boolean isSlotEnabled(int index) {
        return isSlotEnabled(this.menu.slots.get(index));
    }

    @Override
    protected boolean isSlotEnabled(Slot slot) {
        return !((OwoSlotExtension) slot).owo$getDisabledOverride();
    }

    //--

    @Override
    public final LivingEntity targetEntityDefaulted() {
        var targetEntity = this.menu.targetEntity();

        return (targetEntity != null) ? targetEntity : this.minecraft.player;
    }

    @Override
    public Slot getHoveredSlot() {
        return this.hoveredSlot;
    }

    @Override
    @Nullable
    public Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
        var accessories = this.topComponent;

        return (accessories != null) ? accessories.isHovering_Logical(slot, mouseX, mouseY) : null;
    }

    @Override
    @Nullable
    public Boolean isHovering_Rendering(Slot slot, double mouseX, double mouseY) {
        return (slot instanceof SlotTypeAccessible) ? Boolean.FALSE : null;
    }

    @Override
    public @Nullable Boolean shouldRenderSlot(Slot slot) {
        return (slot instanceof SlotTypeAccessible) ? Boolean.FALSE : null;
    }

    @Override
    public void onClose() {
        var selectedGroups = this.getMenu().selectedGroups().stream()
                .map(SlotGroup::name)
                .collect(Collectors.toSet());

        AccessoriesInternals.getNetworkHandler()
                .sendToServer(SyncHolderChange.of(HolderProperty.FILTERED_GROUPS, selectedGroups));

        super.onClose();
    }

    //--

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveredSlot instanceof AccessoriesInternalSlot) {
            AccessoriesScreenBase.FORCE_TOOLTIP_LEFT.setValue(this.mainWidgetPosition());
        }

        guiGraphics.push()
                .translate(0,0,100);

        super.renderTooltip(guiGraphics, x, y);

        guiGraphics.pop();

        AccessoriesScreenBase.FORCE_TOOLTIP_LEFT.setValue(false);
    }

    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        var tooltipData = getTooltipFromItem(this.minecraft, itemStack);

        if (Accessories.config().screenOptions.showEquippedStackSlotType()
                && this.menu.getCarried().isEmpty()
                && this.hoveredSlot != null
                && this.hoveredSlot.hasItem()
                && this.hoveredSlot instanceof AccessoriesBasedSlot slot && ExtraSlotTypeProperties.getProperty(slot.slotName(), true).allowTooltipInfo()) {
            var hoveredStack = this.hoveredSlot.getItem();

            if (itemStack == hoveredStack) {
                tooltipData.add(Component.empty());
                tooltipData.add(
                        Component.translatable(Accessories.translationKey("tooltip.currently_equipped_in"))
                                .withStyle(ChatFormatting.GRAY)
                                .append(
                                        Component.translatable(slot.slotType().translation())
                                                .withStyle(ChatFormatting.BLUE)
                                )
                );
            }
        }

        return tooltipData;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);

        //--

        if (getHoveredSlot() != null && getHoveredSlot() instanceof AccessoriesInternalSlot slot && slot.isActive() && !slot.getItem().isEmpty()) {
            if (NOT_VERY_NICE_POSITIONS.containsKey(slot.accessoriesContainer.getSlotName() + slot.getContainerSlot())) {
                ACCESSORY_POSITIONS.add(NOT_VERY_NICE_POSITIONS.get(slot.accessoriesContainer.getSlotName() + slot.getContainerSlot()));

                var positionKey = slot.accessoriesContainer.getSlotName() + slot.getContainerSlot();
                var vec = NOT_VERY_NICE_POSITIONS.getOrDefault(positionKey, null);

                if (!slot.isCosmetic && vec != null && (Accessories.config().screenOptions.hoveredOptions.line())) {
                    var start = new Vector3d(slot.x + this.leftPos + 17, slot.y + this.topPos + 9, 5000);
                    var vec3 = vec.add(0, 0, 5000);

                    ACCESSORY_LINES.add(Pair.of(start, vec3));}
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (Accessories.config().screenOptions.hoveredOptions.clickbait()) {
            ACCESSORY_POSITIONS.forEach(pos -> guiGraphics.blitSprite(Accessories.of("highlight/clickbait"), (int) pos.x - 128, (int) pos.y - 128, 450, 256, 256));
            ACCESSORY_POSITIONS.clear();
        }

        if (!ACCESSORY_LINES.isEmpty() || Accessories.config().screenOptions.hoveredOptions.line()) {
            var buf = guiGraphics.bufferSource().getBuffer(RenderType.LINES);
            var lastPose = guiGraphics.pose().last();

            for (Pair<Vector3d, Vector3d> line : ACCESSORY_LINES) {
                var endPoint = line.second();

                if (endPoint.x == 0 || endPoint.y == 0) continue;

                var normalVec = endPoint.sub(line.first(), new Vector3d()).normalize().get(new Vector3f());

                double segments = Math.max(10, ((int) (line.first().distance(line.second()) * 10)) / 100);
                segments *= 2;

                var movement = (System.currentTimeMillis() / (segments * 1000) % 1);
                var delta = movement % (2 / (segments)) % segments;

                var firstVec = line.first().get(new Vector3f());

                if (delta > 0.05) {
                    buf.addVertex(firstVec)
                            .setColor(255, 255, 255, 255)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            //.uv2(LightTexture.FULL_BLOCK)
                            .setNormal(lastPose, normalVec.x, normalVec.y, normalVec.z);
                    //.endVertex();

                    var pos = new Vector3d(
                            Mth.lerp(delta - 0.05, line.first().x, line.second().x),
                            Mth.lerp(delta - 0.05, line.first().y, line.second().y),
                            Mth.lerp(delta - 0.05, line.first().z, line.second().z)
                    ).get(new Vector3f());

                    buf.addVertex(pos)
                            .setColor(255, 255, 255, 255)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            //.uv2(LightTexture.FULL_BLOCK)
                            .setNormal(lastPose, normalVec.x, normalVec.y, normalVec.z);
                    //.endVertex();
                }
                for (int i = 0; i < segments / 2; i++) {
                    var delta1 = ((i * 2) / segments + movement) % 1;
                    var delta2 = ((i * 2 + 1) / segments + movement) % 1;

                    var pos1 = new Vector3d(
                            Mth.lerp(delta1, line.first().x, line.second().x),
                            Mth.lerp(delta1, line.first().y, line.second().y),
                            Mth.lerp(delta1, line.first().z, line.second().z)
                    ).get(new Vector3f());
                    var pos2 = (delta2 > delta1 ? new Vector3d(
                            Mth.lerp(delta2, line.first().x, line.second().x),
                            Mth.lerp(delta2, line.first().y, line.second().y),
                            Mth.lerp(delta2, line.first().z, line.second().z)
                    ) : line.second()).get(new Vector3f());

                    buf.addVertex(pos1)
                            .setColor(255, 255, 255, 255)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            //.setUv2(LightTexture.FULL_BLOCK)
                            .setNormal(lastPose, normalVec.x, normalVec.y, normalVec.z);
                    //.endVertex();
                    buf.addVertex(pos2)
                            .setColor(255, 255, 255, 255)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            //.setUv2(LightTexture.FULL_BLOCK)
                            .setNormal(lastPose, normalVec.x, normalVec.y, normalVec.z);
                    //.endVertex();
                }
            }

            minecraft.renderBuffers().bufferSource().endBatch(RenderType.LINES);

            ACCESSORY_LINES.clear();
        }
    }

    //--

    private boolean showCosmeticState = false;

    public void showCosmeticState(boolean value) {
        this.showCosmeticState = value;
    }

    public boolean showCosmeticState() {
        return showCosmeticState;
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.changedSlots.clear();

        rootComponent.allowOverflow(true)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .surface(Surface.VANILLA_TRANSLUCENT);

        var baseChildren = new ArrayList<io.wispforest.owo.ui.core.Component>();

        //--

        this.getMenu().slots.forEach(this::disableSlot);

        var menu = this.getMenu();

        SlotGroupLoader.getValidGroups(this.getMenu().targetEntityDefaulted()).keySet().stream()
                .filter(group -> this.getHolderValue(AccessoriesHolder::filteredGroups, Set.of(), "filteredGroups").contains(group.name()))
                .forEach(menu::addSelectedGroup);

        //--

        var playerInv = ComponentUtils.createPlayerInv(5, menu.startingAccessoriesSlot(), this::slotAsComponent, this::enableSlot);

        this.showAdvancedOptions(false);

        var offHandIndex = this.getMenu().startingAccessoriesSlot() - (this.getMenu().includeSaddle() ? 2 : 1);

        this.enableSlot(offHandIndex);

        baseChildren.add(
                Containers.horizontalFlow(Sizing.content(), Sizing.content())//Sizing.fixed(195 + 39), Sizing.fixed(88) : [39, 60]
                        .child(
                                Containers.verticalFlow(Sizing.fixed(162), Sizing.fixed(76))
                                        .child(playerInv)
                                        .margins(Insets.right(3))
                                        .id("bottom_component_holder")
                        ).child(
                                Containers.verticalFlow(Sizing.content(), Sizing.content()) // Sizing.expand()
                                        .child(
                                                Components.button(createToggleTooltip("advanced_options", false, this.showAdvancedOptions()), btn -> {
                                                            this.showAdvancedOptions(!this.showAdvancedOptions());

                                                            btn.setMessage(createToggleTooltip("advanced_options", false, this.showAdvancedOptions()));
                                                            btn.tooltip(createToggleTooltip("advanced_options", true, this.showAdvancedOptions()));

                                                            this.swapBottomComponentHolder();
                                                        })
                                                        .renderer(ComponentUtils.getButtonRenderer())
                                                        .tooltip(createToggleTooltip("advanced_options", true, this.showAdvancedOptions()))
                                                        .sizing(Sizing.fixed(16))
                                                        .margins(Insets.of(1))
                                        )
                                        .child(
                                                Components.button(createToggleTooltip("crafting_grid", false, this.showCraftingGrid()), btn -> {
                                                            AccessoriesInternals.getNetworkHandler()
                                                                    .sendToServer(SyncHolderChange.of(HolderProperty.CRAFTING_GRID_PROP, this.menu.owner(), bl -> !bl));

                                                            this.showCraftingGrid(!this.showCraftingGrid());

                                                            btn.setMessage(createToggleTooltip("crafting_grid", false, this.showCraftingGrid()));
                                                            btn.tooltip(createToggleTooltip("crafting_grid", true, this.showCraftingGrid()));

                                                            this.toggleCraftingGrid();
                                                        })
                                                        .renderer(ComponentUtils.getButtonRenderer())
                                                        .tooltip(createToggleTooltip("crafting_grid", true, this.showCraftingGrid()))
                                                        .sizing(Sizing.fixed(16))
                                                        .margins(Insets.of(1))
                                                        .id("crafting_grid_button")
                                        )
                                        .child(Components.spacer().sizing(Sizing.fixed(18)))
                                        .child(Components.spacer().sizing(Sizing.fixed(4)))
                                        .child(Containers.verticalFlow(Sizing.content(), Sizing.content())
                                                        .child(
                                                                Containers.verticalFlow(Sizing.content(), Sizing.content())
                                                                        .child(this.slotAsComponent(offHandIndex).margins(Insets.of(1)))
                                                                        .surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                                                        ).surface(ComponentUtils.getPanelSurface())
                                                        .zIndex(10)
                                        )
                        )
                        .child(
                                Containers.verticalFlow(Sizing.content(), Sizing.content())
                                        .configure((FlowLayout component) -> {
                                            if (this.showCraftingGrid()) {
                                                component.margins(Insets.left(3));
                                                component.child(ComponentUtils.createCraftingComponent(0, 4, this::slotAsComponent, this::enableSlot, true));
                                            }
                                        })
                                        .id("crafting_grid_layout")
                        )
                        .padding(Insets.of(6))
                        .surface(ComponentUtils.getPanelSurface())
        );


        //--

        var armorAndEntityLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(2)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .id("armor_entity_layout");

        {
            var armorSlotsLayout = Containers.verticalFlow(Sizing.content(), Sizing.content())
                    .configure((FlowLayout layout) -> {
                        layout.surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                                .allowOverflow(true);
                    });

            var outerLeftArmorLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .child(armorSlotsLayout);

            var cosmeticArmorSlotsLayout = Containers.verticalFlow(Sizing.content(), Sizing.content())
                            .configure((FlowLayout layout) -> {
                                layout.surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                                        .allowOverflow(true);
                            });

            var outerRightArmorLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .child(cosmeticArmorSlotsLayout);

            for (int i = 0; i < menu.addedArmorSlots() / 2; i++) {
                var armor = menu.startingAccessoriesSlot() + (i * 2);
                var cosmeticArmor = armor + 1;

                this.enableSlot(armor);
                this.enableSlot(cosmeticArmor);

                armorSlotsLayout.child(this.slotAsComponent(armor).margins(Insets.of(1)));
                cosmeticArmorSlotsLayout.child(ComponentUtils.slotAndToggle((AccessoriesBasedSlot) this.menu.slots.get(cosmeticArmor), false, this::slotAsComponent).left());
            }

            //--

            var entityComponentSize = 126;

            var entityContainer = Containers.stack(Sizing.content(), Sizing.fixed(entityComponentSize + 12))
                    .child(
                            Containers.verticalFlow(Sizing.content(), Sizing.content())
                                    .child(
                                            InventoryEntityComponent.of(Sizing.fixed(entityComponentSize), Sizing.fixed(108), this.getMenu().targetEntityDefaulted())
                                                    .renderWrapping((component, renderCall) -> {
                                                        AccessoriesScreenBase.SCISSOR_BOX.set(component.x(), component.y(), component.x() + component.width(), component.y() + component.height());

                                                        AccessoriesScreenBase.togglePositionCollection();

                                                        AccessoriesScreenBase.IS_RENDERING_UI_ENTITY.setValue(true);
                                                        AccessoriesScreenBase.IS_RENDERING_LINE_TARGET.setValue(true);

                                                        renderCall.run();

                                                        AccessoriesScreenBase.IS_RENDERING_LINE_TARGET.setValue(false);
                                                        AccessoriesScreenBase.IS_RENDERING_UI_ENTITY.setValue(false);

                                                        AccessoriesScreenBase.COLLECT_ACCESSORY_POSITIONS.setValue(false);

                                                        //AccessoriesScreenBase.SCISSOR_BOX.set(0, 0, 0, 0);
                                                    })
                                                    .startingRotation(this.mainWidgetPosition() ? -45 : 45)
                                                    .scaleToFit(true)
                                                    .allowMouseRotation(true)
                                                    .id("entity_rendering_component")
                                    )
                                    .surface(Surface.flat(Color.BLACK.argb()))
                    )
                    .child(
                            outerLeftArmorLayout
                                    .configure((FlowLayout component) -> component.mouseScroll().subscribe((mouseX, mouseY, amount) -> true))
                                    .surface(ComponentUtils.getPanelSurface())
                                    .padding(Insets.of(6))
                                    .margins(Insets.left(-6))
                                    .positioning(Positioning.relative(0, 40))
                                    .zIndex(10)
                    )
                    .child(
                            outerRightArmorLayout
                                    .configure((FlowLayout component) -> component.mouseScroll().subscribe((mouseX, mouseY, amount) -> true))
                                    .surface(ComponentUtils.getPanelSurface())
                                    .padding(Insets.of(6))
                                    .margins(Insets.right(-6))
                                    .positioning(Positioning.relative(100, 40))
                                    .zIndex(10)
                    )
                    .child(
                            Components.button(Component.literal(""), (btn) -> {
                                this.minecraft.setScreen(new InventoryScreen(minecraft.player));
                            }).renderer((context, btn, delta) -> {
                                        ComponentUtils.getButtonRenderer().draw(context, btn, delta);

                                        context.push();

                                        var BACK_ICON = Accessories.config().screenOptions.isDarkMode()
                                                ? Accessories.of("widget/back_dark")
                                                : Accessories.of("widget/back");

                                        context.blitSprite(BACK_ICON, btn.x() + 1, btn.y() + 1, 8, 8);

                                        context.pop();
                                    })
                                    .tooltip(Component.translatable(Accessories.translationKey("back.screen")))
                                    .positioning(Positioning.relative(100, 0))
                                    .margins(Insets.of(1, 0, 0, 1))
                                    .sizing(Sizing.fixed(10))
                    )
                    .padding(Insets.of(6))
                    .surface(ComponentUtils.getPanelSurface());

            if(this.getMenu().includeSaddle()) {
                var saddleIndex = this.getMenu().startingAccessoriesSlot() - 1;

                this.enableSlot(saddleIndex);

                ((StackLayout) entityContainer).child(
                        Containers.verticalFlow(Sizing.content(), Sizing.content())
                                .child(
                                        Containers.verticalFlow(Sizing.content(), Sizing.content())
                                                .child(
                                                        this.slotAsComponent(saddleIndex)
                                                                .margins(Insets.of(1))
                                                )
                                                .surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                                ).surface(ComponentUtils.getPanelSurface())
                                .padding(Insets.of(6))
                                .positioning(Positioning.relative(0, 100))
                                .margins(Insets.of(0, -6, -6, 0))
                                .zIndex(10)
                        );
            }

            armorAndEntityLayout.child(entityContainer);
        }

        baseChildren.add(armorAndEntityLayout);

        var accessoriesComponent = createAccessoriesComponent();

        if(accessoriesComponent != null) {
            armorAndEntityLayout.child((this.mainWidgetPosition() ? 0 : 1), accessoriesComponent); //1,
        }

        if(accessoriesComponent != null || !this.getMenu().selectedGroups().isEmpty()) {
            var sideBarOptionsComponent = createSideBarOptions();

            if (sideBarOptionsComponent != null) {
                if (this.sideWidgetPosition() == this.mainWidgetPosition()) {
                    armorAndEntityLayout.child(0, sideBarOptionsComponent);
                } else {
                    armorAndEntityLayout.child(sideBarOptionsComponent);
                }
            }
        }

        //--

        var baseLayout = Containers.verticalFlow(Sizing.content(), Sizing.content())
                .gap(2)
                .children(baseChildren.reversed());

        baseLayout.horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .positioning(Positioning.relative(50, 50));

        //--

        rootComponent.child(baseLayout);
    }

    @Nullable
    private AccessoriesContainingComponent topComponent = null;

    public void rebuildAccessoriesComponent() {
        var columnAmountSlider = this.uiAdapter.rootComponent.childById(DiscreteSliderComponent.class, "column_amount_slider");

        if(columnAmountSlider != null) {
            var previousValue = columnAmountSlider.discreteValue();

            var newMinimum = getMinimumColumnAmount();

            ((DiscreteSliderComponentAccessor) columnAmountSlider).accessories$setMin(newMinimum);

            var newValue = Math.max((int) Math.round(previousValue), newMinimum);

            columnAmountSlider.setFromDiscreteValue(newValue);

            ((DiscreteSliderComponentAccessor) columnAmountSlider).accessories$updateMessage();

            this.columnAmount(newValue);
        }

        this.getMenu().getAccessoriesSlots().forEach(this::disableSlot);

        //--

        var armorAndEntityComp = this.uiAdapter.rootComponent.childById(FlowLayout.class, "armor_entity_layout");

        for (var child : List.copyOf(armorAndEntityComp.children())) {
            if (AccessoriesContainingComponent.defaultID().equals(child.id()) && child instanceof AccessoriesContainingComponent accessories) {
                var parent = accessories.parent();

                if (parent != null) {
                    ComponentUtils.recursiveSearch(parent, ExtendedSlotComponent.class, slotComponent -> this.hideSlot(slotComponent.slot()));

                    parent.removeChild(accessories);
                }
            }
        }

        var accessoriesComp = createAccessoriesComponent();

        if (accessoriesComp != null) {
            if(this.mainWidgetPosition()) {
                armorAndEntityComp.child(0, accessoriesComp);
            } else {
                armorAndEntityComp.child(accessoriesComp);
            }

            swapOrCreateSideBarComponent();
        } else {
            if(this.getMenu().selectedGroups().isEmpty()) {
                var sideBarOptionsComponent = armorAndEntityComp.childById(io.wispforest.owo.ui.core.Component.class, "accessories_toggle_panel");

                if (sideBarOptionsComponent != null) {
                    var parent = sideBarOptionsComponent.parent();

                    if (parent != null) parent.removeChild(sideBarOptionsComponent);
                }
            } else {
                swapOrCreateSideBarComponent();
            }
        }
    }

    public void swapOrCreateSideBarComponent() {
        if (this.topComponent == null && this.getMenu().selectedGroups().isEmpty()) return;

        var armorAndEntityComp = this.uiAdapter.rootComponent.childById(FlowLayout.class, "armor_entity_layout");

        var sideBarOptionsComponent = armorAndEntityComp.childById(FlowLayout.class, "accessories_toggle_panel");

        armorAndEntityComp.removeChild(sideBarOptionsComponent);

        sideBarOptionsComponent = createSideBarOptions();

        if (this.sideWidgetPosition() == this.mainWidgetPosition()) {
            armorAndEntityComp.child(0, sideBarOptionsComponent);
        } else {
            armorAndEntityComp.child(sideBarOptionsComponent);
        }
    }

    private int getMinimumColumnAmount() {
        return (this.widgetType() == 2) ? 1 : 3;
    }

    @Nullable
    private AccessoriesContainingComponent createAccessoriesComponent() {
        this.topComponent = (this.widgetType() == 2)
                ? ScrollableAccessoriesComponent.createOrNull(this)
                : GriddedAccessoriesComponent.createOrNull(this);

        return this.topComponent;
    }

    private FlowLayout createSideBarOptions() {
        var cosmeticToggleButton = Components.button(Component.literal(""), btn -> {
                    showCosmeticState(!showCosmeticState());

                    btn.tooltip(createToggleTooltip("slot_cosmetics", false, showCosmeticState()));

                    var component = this.uiAdapter.rootComponent.childById(AccessoriesContainingComponent.class, AccessoriesContainingComponent.defaultID());

                    if(component != null) component.onCosmeticToggle(showCosmeticState());
                }).renderer((context, button, delta) -> {
                    ComponentUtils.getButtonRenderer().draw(context, button, delta);

                    var textureAtlasSprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                            .apply(!showCosmeticState() ? Accessories.of("gui/slot/cosmetic") : Accessories.of("gui/slot/charm"));

                    var color = (!showCosmeticState() ? Color.WHITE.interpolate(Color.BLACK, 0.3f) : Color.BLACK);

                    var red = color.red();
                    var green = color.green();
                    var blue = color.blue();

                    if(!showCosmeticState()) {
                        GuiGraphicsUtils.drawWithSpectrum(context, button.x() + 2, button.y() + 2, 0, 16, 16, textureAtlasSprite, 1f);
                        context.blit(button.x() + 2, button.y() + 2, 0, 16, 16, textureAtlasSprite, red, green, blue, 0.4f);
                    } else {
                        context.blit(button.x() + 2, button.y() + 2, 0, 16, 16, textureAtlasSprite, red, green, blue, 0.9f);
                    }
                }).sizing(Sizing.fixed(20))
                .tooltip(createToggleTooltip("slot_cosmetics", false, showCosmeticState()));

        var accessoriesTogglePanel = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .child(cosmeticToggleButton.margins(Insets.of(2, 2, 2, 2)))
                .gap(1)
                .padding(Insets.of(6))
                .surface(ComponentUtils.getPanelSurface().and(ComponentUtils.getPanelWithInset(6)))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .id("accessories_toggle_panel");

        var groupFilterComponent = createGroupFilters();

        if(groupFilterComponent != null) accessoriesTogglePanel.child(groupFilterComponent);

        return accessoriesTogglePanel;
    }

    @Nullable
    private io.wispforest.owo.ui.core.Component baseFilterLayout = null;

    @Nullable
    private io.wispforest.owo.ui.core.Component createGroupFilters() {
        if (!this.showGroupFilters()) return null;

        var groups = new ArrayList<>(SlotGroupLoader.getValidGroups(this.getMenu().targetEntityDefaulted()).keySet());

        var usedSlots = this.getMenu().getUsedSlots();

        if (groups.isEmpty()) return null;

        var groupButtons = groups.stream()
                .map(group -> {
                    var groupSlots = group.slots()
                            .stream()
                            .filter(slotName -> !UniqueSlotHandling.isUniqueSlot(slotName))
                            .map(slotName -> SlotTypeLoader.getSlotType(this.targetEntityDefaulted(), slotName))
                            .filter(Objects::nonNull)
                            .filter(slotType -> {
                                var capability = this.targetEntityDefaulted().accessoriesCapability();

                                if (capability == null) return false;

                                var container = capability.getContainer(slotType);

                                if (container == null) return false;

                                return container.getSize() > 0;
                            })
                            .collect(Collectors.toSet());

                    if (groupSlots.isEmpty() || (usedSlots != null && groupSlots.stream().noneMatch(usedSlots::contains))) return null;

                    return ComponentUtils.groupToggleBtn(this, group);
                })
                .filter(Objects::nonNull)
                .toList();

        if (groupButtons.isEmpty()) return null;

        var baseButtonLayout = (ParentComponent) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .children(groupButtons)
                .gap(1);

        if(groupButtons.size() > 5) {
            baseButtonLayout = new ExtendedScrollContainer<>(
                    ScrollContainer.ScrollDirection.VERTICAL,
                    Sizing.fixed(18 + 6),
                    Sizing.fixed((5 * 14) + (5) + (2 * 2) - 1),
                    baseButtonLayout.padding(!this.mainWidgetPosition() ? Insets.of(2, 2, 1, 1) : Insets.of(2, 2, 2, 0))
            ).oppositeScrollbar(this.sideWidgetPosition() == this.mainWidgetPosition())
                    .customClippingInsets(Insets.of(1))
                    .scrollToAfterLayout((this.baseFilterLayout instanceof ExtendedScrollContainer<?> prevContainer) ? prevContainer.getProgress() : 0.0f)
                    .scrollbarThiccness(7)
                    .scrollbar(ComponentUtils.getScrollbarRenderer())
                    .fixedScrollbarLength(16);
        } else {
            baseButtonLayout.margins(Insets.bottom(2));
        }

        this.baseFilterLayout = baseButtonLayout;

        return new ExtendedCollapsibleContainer(Sizing.content(), Sizing.content(), this.isGroupFiltersOpen())
                .configure((ExtendedCollapsibleContainer component) -> {
                    component.onToggled().subscribe(b -> {
                        AccessoriesInternals.getNetworkHandler()
                                .sendToServer(SyncHolderChange.of(HolderProperty.GROUP_FILTER_OPEN_PROP, b));

                        this.isGroupFiltersOpen(b);
                    });
                })
                .child(
                        Components.button(Component.empty(), btn -> {
                                    this.getMenu().selectedGroups().clear();
                                    this.rebuildAccessoriesComponent();
                                }).renderer((context, button, delta) -> {
                                    ComponentUtils.getButtonRenderer().draw(context, button, delta);

                                    RenderSystem.depthMask(false);

                                    var color = Color.WHITE;

                                    RenderSystem.setShaderColor(color.red(), color.green(), color.blue(), 1f);
                                    RenderSystem.enableBlend();
                                    RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

                                    context.blit(Accessories.of("textures/gui/reset_icon.png"), button.x() + 3 + 3, button.y() + 3, 0, 0, 0, 8, 8, 8, 8);

                                    RenderSystem.depthMask(true);
                                    RenderSystem.disableBlend();
                                }).sizing(Sizing.fixed(14))
                                .horizontalSizing(Sizing.fixed(20))
                                .margins(Insets.bottom(1))
                                .tooltip(Component.translatable(Accessories.translationKey("reset.group_filter")))
                )
                .child(baseButtonLayout)
                .id("group_filter_component");
    }

    private void swapBottomComponentHolder() {
        var holder = this.uiAdapter.rootComponent.childById(FlowLayout.class, "bottom_component_holder");

        holder.clearChildren();

        if(this.showAdvancedOptions()) {
            for (int i = 0; i < menu.startingAccessoriesSlot() - (this.getMenu().includeSaddle() ? 2 : 1); i++) this.disableSlot(i);

            holder.child(createOptionsComponent());
        } else {
            holder.child(ComponentUtils.createPlayerInv(5, menu.startingAccessoriesSlot(), this::slotAsComponent, this::enableSlot));
        }
    }

    private void toggleCraftingGrid() {
        var holder = this.uiAdapter.rootComponent.childById(FlowLayout.class, "crafting_grid_layout");

        holder.clearChildren();

        if(this.showCraftingGrid()) {
            holder.margins(Insets.left(3));

            holder.child(ComponentUtils.createCraftingComponent(0, 4, this::slotAsComponent, this::enableSlot, true));
        } else {
            holder.margins(Insets.of(0));

            this.disableSlots(0, 1, 2, 3, 4);
        }
    }

    private io.wispforest.owo.ui.core.Component createOptionsComponent() {
        var baseOptionPanel = Containers.grid(Sizing.fixed(162), Sizing.content(), 4, 2)
                .configure((GridLayout component) -> {
                    component.surface(ComponentUtils.getInsetPanelSurface())
                            .verticalAlignment(VerticalAlignment.CENTER)
                            .horizontalAlignment(HorizontalAlignment.CENTER)
                            .padding(Insets.of(3));
                });

        //--

        baseOptionPanel.child(
                createConfigComponent("unused_slots",
                        this::showUnusedSlots,
                        bl -> AccessoriesInternals.getNetworkHandler().sendToServer(SyncHolderChange.of(HolderProperty.UNUSED_PROP, bl))
                ).margins(Insets.bottom(3)),
                0, 0);

        baseOptionPanel.child(
                Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(Components.label(Accessories.translation("column_amount_slider.label")))
                        .child(Components.discreteSlider(Sizing.fixed(45), getMinimumColumnAmount(), 18)
                                .configure((DiscreteSliderComponent slider) -> {
                                    slider.onChanged().subscribe(value -> {
                                        AccessoriesInternals.getNetworkHandler()
                                                .sendToServer(SyncHolderChange.of(HolderProperty.COLUMN_AMOUNT_PROP, (int) value));

                                        this.columnAmount((int) value);

                                        rebuildAccessoriesComponent();
                                    });
                                })
                                .snap(true)
                                .setFromDiscreteValue(this.columnAmount())
                                .scrollStep(1f / (18 - getMinimumColumnAmount()))
                                .tooltip(Accessories.translation("column_amount_slider.tooltip"))
                                .id("column_amount_slider")
                                .horizontalSizing(Sizing.fixed(74))
                        )
                        .margins(Insets.bottom(3)),
                0, 1);

        baseOptionPanel.child(
                Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(Components.label(Accessories.translation("widget_type.label")))
                        .child(
                                Components.button(
                                        widgetTypeToggleMessage(this.widgetType(), false),
                                        btn -> {
                                            var newWidget = this.widgetType() + 1;

                                            if(newWidget > 2) newWidget = 1;

                                            AccessoriesInternals.getNetworkHandler()
                                                    .sendToServer(SyncHolderChange.of(HolderProperty.WIDGET_TYPE_PROP, newWidget));

                                            this.widgetType(newWidget);

                                            updateWidgetTypeToggleButton();
                                        })
                                        .renderer(ComponentUtils.getButtonRenderer())
                                        .tooltip(widgetTypeToggleMessage(this.widgetType(), true))
                                        .id("widget_type_toggle")
                                        .horizontalSizing(Sizing.fixed(74))
                        ).margins(Insets.bottom(3)),
                1, 0);

        baseOptionPanel.child(
                createConfigComponent("main_widget_position",
                        this::mainWidgetPosition,
                        bl -> {
                            AccessoriesInternals.getNetworkHandler()
                                    .sendToServer(SyncHolderChange.of(HolderProperty.MAIN_WIDGET_POSITION_PROP, bl));

                            this.mainWidgetPosition(bl);

                            this.uiAdapter.rootComponent.childById(InventoryEntityComponent.class, "entity_rendering_component")
                                    .startingRotation(this.mainWidgetPosition() ? -45 : 45);
                        }
                ).margins(Insets.bottom(3)),
                1, 1);

        baseOptionPanel.child(
                createConfigComponent("group_filter",
                        this::showGroupFilters,
                        bl -> {
                            AccessoriesInternals.getNetworkHandler().sendToServer(SyncHolderChange.of(HolderProperty.GROUP_FILTER_PROP, bl));

                            this.showGroupFilters(bl);

                            if(this.showGroupFilters()) {
                                var panel = this.uiAdapter.rootComponent.childById(FlowLayout.class, "accessories_toggle_panel");

                                if(panel != null) {
                                    var groupFilter = createGroupFilters();

                                    if (groupFilter != null) panel.child(groupFilter);
                                }
                            } else {
                                var component = this.uiAdapter.rootComponent.childById(io.wispforest.owo.ui.core.Component.class, "group_filter_component");

                                if (component != null) component.remove();
                            }
                        }
                ).margins(Insets.bottom(3)),
                2, 0);

        baseOptionPanel.child(
                createConfigComponent("side_widget_position",
                        this::sideWidgetPosition,
                        bl -> {
                            AccessoriesInternals.getNetworkHandler()
                                    .sendToServer(SyncHolderChange.of(HolderProperty.SIDE_WIDGET_POSITION_PROP, bl));

                            this.sideWidgetPosition(bl);

                            this.swapOrCreateSideBarComponent();
                        }
                ).margins(Insets.bottom(3)),
                2, 1);

        baseOptionPanel.child(
                createConfigComponent("dark_mode_toggle",
                        () -> Accessories.config().screenOptions.isDarkMode(),
                        bl -> Accessories.config().screenOptions.isDarkMode(bl)
                ),
                3, 0);

        baseOptionPanel.child(
                createConfigComponent("show_equipped_stack_slot_type",
                        () -> Accessories.config().screenOptions.showEquippedStackSlotType(),
                        bl -> Accessories.config().screenOptions.showEquippedStackSlotType(bl)
                ),
                3, 1);

        return Containers.verticalScroll(Sizing.expand(), Sizing.expand(), baseOptionPanel);
    }

    private io.wispforest.owo.ui.core.Component createConfigComponent(String type, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Containers.verticalFlow(Sizing.content(), Sizing.content())
                .child(Components.label(Accessories.translation(type + ".label")))
                .child(
                        Components.button(
                                        createToggleTooltip(type, false, getter.get()),
                                        btn -> {
                                            var newValue = !getter.get();

                                            setter.accept(newValue);

                                            btn.setMessage(createToggleTooltip(type, false, newValue));
                                            btn.tooltip(createToggleTooltip(type, true, newValue));
                                        })
                                .renderer(ComponentUtils.getButtonRenderer())
                                .tooltip(createToggleTooltip(type, true, getter.get()))
                                .id(type)
                                .horizontalSizing(Sizing.fixed(74))
                );
    }

    @Override
    public void onHolderChange(String key) {
        switch (key) {
            case "unused_slots" -> updateToggleButton("unused_slots", this::showUnusedSlots, () -> {
                this.getMenu().updateUsedSlots();

                Accessories.config().screenOptions.showUnusedSlots(this.showUnusedSlots());

                this.rebuildAccessoriesComponent();
            });
            case "group_filter" -> updateToggleButton("group_filter", this::showGroupFilters, this::rebuildAccessoriesComponent);
            case "main_widget_position" -> updateToggleButton("main_widget_position", this::mainWidgetPosition, () -> {
                this.rebuildAccessoriesComponent();
                this.swapOrCreateSideBarComponent();
            });
            case "side_widget_position" -> updateToggleButton("side_widget_position", this::sideWidgetPosition, this::swapOrCreateSideBarComponent);
        }
    }

    private void updateToggleButton(String baseId, Supplier<Boolean> getter, Runnable runnable) {
        var btn = this.uiAdapter.rootComponent.childById(ButtonComponent.class, baseId);

        var value = getter.get();

        btn.setMessage(createToggleTooltip(baseId, false, value));
        btn.tooltip(createToggleTooltip(baseId, true, value));

        runnable.run();
    }

    private void updateWidgetTypeToggleButton() {
        var btn = this.uiAdapter.rootComponent.childById(ButtonComponent.class, "widget_type_toggle");

        var value = this.widgetType();

        btn.setMessage(widgetTypeToggleMessage(value, false));
        btn.tooltip(widgetTypeToggleMessage(value, true));

        this.rebuildAccessoriesComponent();
    }

    private static Component widgetTypeToggleMessage(int value, boolean isTooltip) {
        var type = value == 2 ? "scrollable" : "paginated";

        return Accessories.translation("widget_type." + type + (isTooltip ? ".tooltip" : ""));
    }

    private static Component createToggleTooltip(String type, boolean isTooltip, boolean value) {
        return Accessories.translation(type + ".toggle." + (value ? "enabled" : "disabled") + (isTooltip ? ".tooltip" : ""));
    }

    //--

    public ExtendedSlotComponent slotAsComponent(int index) {
        return new ExtendedSlotComponent(this, index);
    }

    public class ExtendedSlotComponent extends SlotComponent {

        private boolean isBatched = false;

        protected final AccessoriesExperimentalScreen screen;
        protected int index;

        protected ExtendedSlotComponent(AccessoriesExperimentalScreen screen, int index) {
            super(index);

            this.screen = screen;
            this.index = index;

            this.didDraw = true;

            if (slot() instanceof AccessoriesBasedSlot accessoriesBasedSlot) {
                this.tooltip(accessoriesBasedSlot.getTooltipData());
            }
        }

        public final Slot slot() {
            return this.slot;
        }

        public final boolean isBatched() {
            return this.isBatched;
        }

        public ExtendedSlotComponent isBatched(boolean value) {
            this.isBatched = value;

            return this;
        }

        @Override
        public void dismount(DismountReason reason) {
            super.dismount(reason);

            if(reason == DismountReason.REMOVED) screen.hideSlot(slot);
        }

        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            if(!(slot() instanceof SlotTypeAccessible)) {
                super.draw(context, mouseX, mouseY, partialTicks, delta);

                return;
            }

            this.didDraw = true;

            if (isBatched) return;

            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();

            renderSlot(context);

            renderCosmeticOverlay(context, false);

            renderHover(context, () -> screen.hoveredSlot);
        }

        public void renderSlot(OwoUIDrawContext context) {
            int i = screen.leftPos;
            int j = screen.topPos;

            context.push();
            context.translate((float) i, (float) j, 0);

            screen.forceRenderSlot(context, slot());

            context.pop();
        }

        public void renderCosmeticOverlay(OwoUIDrawContext context, boolean externalBatching) {
            if (!(slot() instanceof SlotTypeAccessible slotTypeAccessible) || !slotTypeAccessible.isCosmeticSlot()) return;

            context.push();
            context.translate(0.0F, 0.0F, 101.0F);

            if(externalBatching) {
                GuiGraphicsUtils.drawRectOutlineWithSpectrumWithoutRecord(context, this.x(), this.y(), 0, 16, 16, 0.35f, true);
            } else {
                GuiGraphicsUtils.drawRectOutlineWithSpectrum(context, this.x(), this.y(), 0, 16, 16, 0.35f, true);
            }

            context.pop();
        }

        public void renderHover(OwoUIDrawContext context, Supplier<Slot> hoverSlot) {
            var hoveredSlot = hoverSlot.get();

            if (this.slot.equals(hoveredSlot) && hoveredSlot.isHighlightable()) {
                context.push();

                context.fillGradient(RenderType.gui(), x(), y(), x() + 16, y() + 16, -2130706433, -2130706433, 330);

                context.pop();
            }
        }
    }

    //--
    // Below is a copy of minecraft code for rendering slots and items stacks in a batched method for performance

    private final Surface SLOT_RENDERING_SURFACE = (context, component) -> {
        var validComponents = new ArrayList<ExtendedSlotComponent>();

        ComponentUtils.recursiveSearch(component, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> {
            if(!slotComponent.isBatched() || !(slotComponent.slot() instanceof SlotTypeAccessible)) return;

            validComponents.add(slotComponent);
        });

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        context.push();
        context.translate(0, 0, 100.0F);

        //--

        Map<Slot, Triplet<ItemStack, Boolean, @Nullable String>> slotStateData = new HashMap<>();
        Map<Slot, Boolean> allBl2s = new HashMap<>();

        //--

        var accessor = (AbstractContainerScreenAccessor) this;

        //--

        int i = this.leftPos;
        int j = this.topPos;

        context.push();
        context.translate(i, j, 0);

        safeBatching(context, hasDrawCallOccur -> {
            for (var slotComponent : validComponents) {
                var slot = slotComponent.slot();

                var data = slotStateData.computeIfAbsent(slot, this::getRenderStack);

                if (data == null) continue;

                allBl2s.put(slot, renderSlotTexture(context, slot, data.getA()));

                hasDrawCallOccur.setValue(true);
            }
        });

        //--

        safeBatching(context, hasDrawCallOccur -> {
            for (var slotComponent : validComponents) {
                var slot = slotComponent.slot();

                var data = slotStateData.computeIfAbsent(slot, this::getRenderStack);

                if (data == null) return;

                var itemStack = data.getA();

                var bl2 = allBl2s.getOrDefault(slot, false)
                        || (slot == accessor.accessories$getClickedSlot() && !accessor.accessories$getDraggingItem().isEmpty() && !accessor.accessories$isSplittingStack());

                if (!bl2) {
                    int slotX = slot.x;
                    int slotY = slot.y;

                    if (data.getB()) {
                        context.fill(slotX, slotY, slotX + 16, slotY + 16, -2130706433);
                    }

                    int k = slot.x + slot.y * this.imageWidth;

                    if (slot.isFake()) {
                        context.renderFakeItem(itemStack, slotX, slotY, k);
                    } else {
                        context.renderItem(itemStack, slotX, slotY, k);
                    }

                    context.renderItemDecorations(this.font, itemStack, slotX, slotY, data.getC());
                }

                hasDrawCallOccur.setValue(true);
            }
        });

        context.pop();

        safeBatching(context, hasDrawCallOccur -> {
            for (var slotComponent : validComponents) {
                slotComponent.renderCosmeticOverlay(context, true);

                hasDrawCallOccur.setValue(true);
            }
        });

        context.pop();

        validComponents.forEach(slotComponent -> slotComponent.renderHover(context, () -> this.hoveredSlot));
    };

    private static boolean renderSlotTexture(GuiGraphics context, Slot slot, ItemStack itemStack) {
        if (itemStack.isEmpty() /*&& slot.isActive()*/) {
            com.mojang.datafixers.util.Pair<ResourceLocation, ResourceLocation> pair = slot.getNoItemIcon();

            if (pair != null) {
                TextureAtlasSprite textureAtlasSprite = Minecraft.getInstance().getTextureAtlas(pair.getFirst()).apply(pair.getSecond());

                context.blit(slot.x, slot.y, 0, 16, 16, textureAtlasSprite);

                return true;
            }
        }

        return false;
    }

    public final Surface FULL_SLOT_RENDERING = BACKGROUND_SLOT_RENDERING_SURFACE.and(SLOT_RENDERING_SURFACE);

    //--

    @Nullable
    private Triplet<ItemStack, Boolean, @Nullable String> getRenderStack(Slot slot) {
        var accessor = (AbstractContainerScreenAccessor) this;

        var itemStack = slot.getItem();

        var bl = false;

        var itemStack2 = this.menu.getCarried();

        String string = null;

        if (slot == accessor.accessories$getClickedSlot() && !accessor.accessories$getDraggingItem().isEmpty() && accessor.accessories$isSplittingStack() && !itemStack.isEmpty()) {
            itemStack = itemStack.copyWithCount(itemStack.getCount() / 2);
        } else if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !itemStack2.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) return null;

            if (AbstractContainerMenu.canItemQuickReplace(slot, itemStack2, true) && this.menu.canDragTo(slot)) {
                bl = true;

                int maxSlotStackSize = Math.min(itemStack2.getMaxStackSize(), slot.getMaxStackSize(itemStack2));
                int currentSlotStackSize = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();

                int newStackSize = AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots, accessor.accessories$getQuickCraftingType(), itemStack2) + currentSlotStackSize;

                if (newStackSize > maxSlotStackSize) {
                    newStackSize = maxSlotStackSize;
                    string = ChatFormatting.YELLOW.toString() + maxSlotStackSize;
                }

                itemStack = itemStack2.copyWithCount(newStackSize);
            } else {
                this.quickCraftSlots.remove(slot);
                accessor.accessories$recalculateQuickCraftRemaining();
            }
        }

        return new Triplet<>(itemStack, bl, string);
    }

    //--

    private <T> T getHolderValue(Function<AccessoriesHolder, T> getter, T defaultValue, String valueType) {
        return Optional.ofNullable(AccessoriesHolder.get(this.menu.owner()))
                .map(getter)
                .orElseGet(() -> {
                    LOGGER.warn("[AccessoriesScreen] Unable to get the given holder value '{}' for the given owner: {}", valueType, this.menu.owner().getName());

                    return defaultValue;
                });
    }

    private <T> void setHolderValue(BiFunction<AccessoriesHolder, T, AccessoriesHolder> setter, T value, String valueType) {
        var holder = AccessoriesHolder.get(this.menu.owner());

        if(holder == null) {
            LOGGER.warn("[AccessoriesScreen] Unable to set the given holder value '{}' for the given owner: {}", valueType, this.menu.owner().getName());

            return;
        }

        setter.apply(holder, value);
    }

    private int widgetType() {
        return this.getHolderValue(AccessoriesHolder::widgetType, 1, "widgetType");
    }

    private void widgetType(int type) {
        this.setHolderValue(AccessoriesHolder::widgetType, type, "widgetType");
    }

    private int columnAmount() {
        return this.getHolderValue(AccessoriesHolder::columnAmount, 1, "columnAmount");
    }

    private void columnAmount(int type) {
        this.setHolderValue(AccessoriesHolder::columnAmount, type, "columnAmount");
    }

    public boolean mainWidgetPosition() {
        return this.getHolderValue(AccessoriesHolder::mainWidgetPosition, false, "mainWidgetPosition");
    }

    private void mainWidgetPosition(boolean value) {
        this.setHolderValue(AccessoriesHolder::mainWidgetPosition, value, "mainWidgetPosition");
    }

    public boolean showGroupFilters() {
        return this.getHolderValue(AccessoriesHolder::showGroupFilter, false, "showGroupFilter");
    }

    private void showGroupFilters(boolean value) {
        this.setHolderValue(AccessoriesHolder::showGroupFilter, value, "showGroupFilter");
    }

    public boolean isGroupFiltersOpen() {
        return this.getHolderValue(AccessoriesHolder::isGroupFiltersOpen, false, "isGroupFiltersOpen");
    }

    private void isGroupFiltersOpen(boolean value) {
        this.setHolderValue(AccessoriesHolder::isGroupFiltersOpen, value, "isGroupFiltersOpen");
    }

    private boolean showUnusedSlots() {
        return this.getHolderValue(AccessoriesHolder::showUnusedSlots, false, "showUnusedSlots");
    }

    private void showUnusedSlots(boolean value) {
        this.setHolderValue(AccessoriesHolder::showUnusedSlots, value, "showUnusedSlots");
    }

    private boolean showAdvancedOptions() {
        return this.getHolderValue(AccessoriesHolder::showAdvancedOptions, false, "showAdvancedOptions");
    }

    private void showAdvancedOptions(boolean value) {
        this.setHolderValue(AccessoriesHolder::showAdvancedOptions, value, "showAdvancedOptions");
    }

    private boolean sideWidgetPosition() {
        return this.getHolderValue(AccessoriesHolder::sideWidgetPosition, false, "sideWidgetPosition");
    }

    private void sideWidgetPosition(boolean value) {
        this.setHolderValue(AccessoriesHolder::sideWidgetPosition, value, "sideWidgetPosition");
    }

    public boolean showCraftingGrid() {
        return this.getHolderValue(AccessoriesHolder::showCraftingGrid, false, "showCraftingGrid");
    }

    public void showCraftingGrid(boolean value) {
        this.setHolderValue(AccessoriesHolder::showCraftingGrid, value, "showCraftingGrid");
    }

    //--

    public static void safeBatching(OwoUIDrawContext context, Consumer<MutableBoolean> drawCallback) {
        context.recordQuads();

        var hasDrawCallOccur = new MutableBoolean(false);

        drawCallback.accept(hasDrawCallOccur);

        try {
            if(hasDrawCallOccur.booleanValue()) context.submitQuads();
        } catch (Exception e) {
            var test = "this is for debugging only really!";
        }
    }
}
