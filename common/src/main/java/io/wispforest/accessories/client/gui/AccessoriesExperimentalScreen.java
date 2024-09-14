package io.wispforest.accessories.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.client.GuiGraphicsUtils;
import io.wispforest.accessories.client.gui.components.*;
import io.wispforest.accessories.data.SlotGroupLoader;
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
import io.wispforest.owo.ui.util.NinePatchTexture;
import io.wispforest.owo.util.pond.OwoSlotExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    //--

    public final Map<Integer, Boolean> changedSlots = new HashMap<>();

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

        if(state != null && !state) return;

        hideSlot(index);

        this.changedSlots.put(index, true);
    }

    @Override
    public void enableSlot(Slot slot) {
        enableSlot(slot.index);
    }

    @Override
    public void enableSlot(int index) {
        super.enableSlot(index);

        var state = this.changedSlots.getOrDefault(index, null);

        if(state != null && state) return;

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

    @Override
    protected void containerTick() {
        super.containerTick();

        if(this.changedSlots.isEmpty()) return;

        var slots = this.getMenu().slots;

        var changes = this.changedSlots.keySet().stream()
                .map(i -> (i < slots.size()) ? slots.get(i) : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(slot -> slot.index, slot -> ((OwoSlotExtension)slot).owo$getDisabledOverride()));

        this.getMenu().sendMessage(new AccessoriesExperimentalMenu.ToggledSlots(changes));

        this.changedSlots.clear();
    }

    @Override
    public final LivingEntity targetEntityDefaulted() {
        var targetEntity = this.menu.targetEntity();

        return (targetEntity != null) ? targetEntity : this.minecraft.player;
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
        changedSlots.clear();

        rootComponent.allowOverflow(true)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .surface(Surface.VANILLA_TRANSLUCENT);
        //rootComponent.surface(SLOT_RENDERING_SURFACE);

        var baseChildren = new ArrayList<io.wispforest.owo.ui.core.Component>();

        //--

        for (int i = 0; i < this.getMenu().slots.size(); i++) {
            this.disableSlot(i);
        }

        var menu = this.getMenu();

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
                                                        }).tooltip(createToggleTooltip("advanced_options", true, this.showAdvancedOptions()))
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
                                                        }).tooltip(createToggleTooltip("crafting_grid", true, this.showCraftingGrid()))
                                                        .sizing(Sizing.fixed(16))
                                                        .margins(Insets.of(1))
                                                        .id("crafting_grid_button")
                                        )
                                        .child(
                                                Components.spacer()
                                                        .sizing(Sizing.fixed(18))
                                        )
                                        .child(
                                                Components.spacer()
                                                        .sizing(Sizing.fixed(4))
                                        )
                                        .child(Containers.verticalFlow(Sizing.content(), Sizing.content())
                                                        .child(
                                                                Containers.verticalFlow(Sizing.content(), Sizing.content())
                                                                        .child(
                                                                                this.slotAsComponent(offHandIndex)
                                                                                        .margins(Insets.of(1))
                                                                        )
                                                                        .surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                                                        ).surface(Surface.PANEL)
                                                        //.padding(Insets.of(6))
                                                        //.margins(Insets.of(0, -6, 0, -6))
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
//                        .gap(3)
                        .padding(Insets.of(6))
                        .surface(Surface.PANEL)
        );


        //--

        var armorAndEntityLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(2)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .id("armor_entity_layout");

        {
            var armorsLayout = (FlowLayout) Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .configure((FlowLayout component) -> {
                        component.mouseScroll().subscribe((mouseX, mouseY, amount) -> true);
                    })
                    .gap(68)
                    .horizontalAlignment(HorizontalAlignment.CENTER)
                    .positioning(Positioning.relative(50, 50))
                    .zIndex(10);

            var innerSpacingComponent = Containers.horizontalFlow(Sizing.fixed(60), Sizing.fixed(0));

            var outerLeftArmorLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            var outerRightArmorLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content());

            var armorSlotsLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());

            armorSlotsLayout.surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                    .allowOverflow(true);

            outerLeftArmorLayout.child(armorSlotsLayout);

            var cosmeticArmorSlotsLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());

            cosmeticArmorSlotsLayout.surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                    .allowOverflow(true);

            outerRightArmorLayout.child(cosmeticArmorSlotsLayout);

            for (int i = 0; i < menu.addedArmorSlots() / 2; i++) {
                var armor = menu.startingAccessoriesSlot() + (i * 2);
                var cosmeticArmor = armor + 1;

                this.enableSlot(armor);
                this.enableSlot(cosmeticArmor);

                var armorSlot = this.slotAsComponent(armor)
                        .margins(Insets.of(1));

                var cosmeticArmorSlot = ComponentUtils.slotAndToggle((AccessoriesBasedSlot) this.menu.slots.get(cosmeticArmor), false, this::slotAsComponent).left();

                armorSlotsLayout.child(armorSlot);
                cosmeticArmorSlotsLayout.child(cosmeticArmorSlot);
            }

            //outerArmorLayout.positioning(Positioning.relative(50, 20));

            outerLeftArmorLayout
                    .configure((FlowLayout component) -> {
                        component.mouseScroll().subscribe((mouseX, mouseY, amount) -> true);
                    })
                    .surface(Surface.PANEL)
                    .padding(Insets.of(6));

            outerRightArmorLayout
                    .configure((FlowLayout component) -> {
                        component.mouseScroll().subscribe((mouseX, mouseY, amount) -> true);
                    })
                    .surface(Surface.PANEL)
                    .padding(Insets.of(6));

            armorsLayout.child(outerLeftArmorLayout);

            armorsLayout.child(innerSpacingComponent);
//            ((ExclusiveBoundingArea) armorsLayout).addExclusionZone(innerSpacingComponent);

            armorsLayout.child(outerRightArmorLayout);

            //--

            var entityComponentSize = 126;

            var entityContainer = Containers.stack(Sizing.content(), Sizing.fixed(entityComponentSize + 12))
                    .child(
                            Containers.verticalFlow(Sizing.content(), Sizing.content())
                                    .child(
                                            InventoryEntityComponent.of(Sizing.fixed(entityComponentSize), Sizing.fixed(108), this.getMenu().targetEntityDefaulted())
                                                    .startingRotation(this.mainWidgetPosition() ? -45 : 45)
                                                    .scaleToFit(true)
                                                    .allowMouseRotation(true)
                                                    .id("entity_rendering_component")
                                    )
                                    .surface(Surface.flat(Color.BLACK.argb()))
                    )
                    .child(
                            outerLeftArmorLayout.positioning(Positioning.relative(0, 40))
                                    .margins(Insets.left(-6))
                                    .zIndex(10)
                    )
                    .child(
                            outerRightArmorLayout.positioning(Positioning.relative(100, 40))
                                    .margins(Insets.right(-6))
                                    .zIndex(10)
                    )
                    .child(
                            Components.button(Component.literal(""), (btn) -> {
                                this.minecraft.setScreen(new InventoryScreen(minecraft.player));
                            }).renderer((context, btn, delta) -> {
                                        ButtonComponent.Renderer.VANILLA.draw(context, btn, delta);

                                        context.push()/*.translate(1, 1, 0.0)*/;

                                        //--
//                                        var groupIcon = Accessories.of("gui/group/misc");
//
//                                        var textureAtlasSprite = Minecraft.getInstance()
//                                                .getTextureAtlas(ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png"))
//                                                .apply(groupIcon);
//
//                                        var color = Color.BLACK.interpolate(Color.WHITE, 0.4f);
//
//                                        RenderSystem.depthMask(false);
//
//                                        //RenderSystem.setShaderColor(color.red(), color.green(), color.blue(), 1f);
//                                        //RenderSystem.enableBlend();
//                                        //RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
//
//                                        context.blit(btn.x() + 2, btn.y() + 2, 2, 8, 8, textureAtlasSprite, color.red(), color.green(), color.blue(), 1f);
//
//                                        //RenderSystem.defaultBlendFunc();
//                                        //RenderSystem.disableBlend();
//                                        //RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
//
//                                        RenderSystem.depthMask(true);

                                        //--

                                        var BACK_ICON = Accessories.of("widget/back");

                                        context.blitSprite(BACK_ICON, btn.x() + 1, btn.y() + 1, 8, 8);

                                        //--

                                        context.pop();
                                    })
                                    .tooltip(Component.translatable(Accessories.translationKey("back.screen")))
                                    .positioning(Positioning.relative(100, 0))
                                    .margins(Insets.of(1, 0, 0, 1))
                                    .sizing(Sizing.fixed(10))
                    )
                    .padding(Insets.of(6))
                    .surface(Surface.PANEL);

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
                                ).surface(Surface.PANEL)
                                .padding(Insets.of(6))
                                .positioning(Positioning.relative(0, 100))
                                .margins(Insets.of(0, -6, -6, 0))
                                .zIndex(10)
                        );
            }

            //var entityDraggable = Containers.draggable(Sizing.content(), Sizing.content(), entityContainer);

            armorAndEntityLayout.child(entityContainer); //0,
        }

        baseChildren.add(armorAndEntityLayout);

        var accessoriesComponent = createAccessoriesComponent();

        if(accessoriesComponent != null) {
            armorAndEntityLayout.child((this.mainWidgetPosition() ? 0 : 1), accessoriesComponent); //1,

            var sideBarOptionsComponent = createSideBarOptions();

            if(sideBarOptionsComponent != null) {
                if(this.sideWidgetPosition()) {
                    if(this.mainWidgetPosition()) {
                        armorAndEntityLayout.child(0, sideBarOptionsComponent);
                    } else {
                        armorAndEntityLayout.child(sideBarOptionsComponent);
                    }
                } else {
                    if(this.mainWidgetPosition()) {
                        armorAndEntityLayout.child(sideBarOptionsComponent);
                    } else {
                        armorAndEntityLayout.child(0, sideBarOptionsComponent);
                    }
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

        for (var accessoriesSlot : this.getMenu().getAccessoriesSlots()) {
            this.disableSlot(accessoriesSlot);
        }

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

//                if(this.sideWidgetPosition()) {
//                    armorAndEntityComp.child(1, accessoriesComp);
//                } else {
//                    armorAndEntityComp.child(accessoriesComp);
//                }
            }

            swapOrCreateSideBarComponent();
        } else {
            var sideBarOptionsComponent = armorAndEntityComp.childById(io.wispforest.owo.ui.core.Component.class, "accessories_toggle_panel");

            var parent = sideBarOptionsComponent.parent();

            if (parent != null) parent.removeChild(sideBarOptionsComponent);
        }
    }

    public void swapOrCreateSideBarComponent() {
        var armorAndEntityComp = this.uiAdapter.rootComponent.childById(FlowLayout.class, "armor_entity_layout");

        var sideBarOptionsComponent = armorAndEntityComp.childById(io.wispforest.owo.ui.core.Component.class, "accessories_toggle_panel");

        if(sideBarOptionsComponent != null) {
            var parent = sideBarOptionsComponent.parent();

            if (parent != null) parent.removeChild(sideBarOptionsComponent);
        } else {
            sideBarOptionsComponent = createSideBarOptions();
        }

        if (this.sideWidgetPosition()) {
            if (this.mainWidgetPosition()) {
                armorAndEntityComp.child(0, sideBarOptionsComponent);
            } else {
                armorAndEntityComp.child(sideBarOptionsComponent);
            }
        } else {
            if (this.mainWidgetPosition()) {
                armorAndEntityComp.child(sideBarOptionsComponent);
            } else {
                armorAndEntityComp.child(0, sideBarOptionsComponent);
            }
        }
    }

    private int getMinimumColumnAmount() {
        var widgetType = this.widgetType();

        return switch (widgetType) {
            case 2 -> 1;
            default -> 3;
        };
    }

    @Nullable
    private AccessoriesContainingComponent createAccessoriesComponent() {
        var widgetType = this.widgetType();

        var component = switch (widgetType) {
            case 2 -> ScrollableAccessoriesComponent.createOrNull(this); // 2
            default -> GriddedAccessoriesComponent.createOrNull(this); // 1
        };

        this.topComponent = component;

        return component;
    }

    private io.wispforest.owo.ui.core.Component createSideBarOptions() {
        var cosmeticToggleButton = Components.button(Component.literal(""), btn -> {
                    showCosmeticState(!showCosmeticState());

                    btn.tooltip(createToggleTooltip("slot_cosmetics", false, showCosmeticState()));

                    var component = this.uiAdapter.rootComponent.childById(AccessoriesContainingComponent.class, AccessoriesContainingComponent.defaultID());

                    if(component != null) component.onCosmeticToggle(showCosmeticState());
                }).renderer((context, button, delta) -> {
                    ButtonComponent.Renderer.VANILLA.draw(context, button, delta);

                    var textureAtlasSprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                            .apply(!showCosmeticState() ? Accessories.of("gui/slot/cosmetic") : Accessories.of("gui/slot/charm"));

                    var color = (!showCosmeticState() ? Color.WHITE.interpolate(Color.BLACK, 0.3f) : Color.BLACK);

                    var red = color.red();
                    var green = color.green();
                    var blue = color.blue();

                    var shard = RenderStateShard.TRANSLUCENT_TRANSPARENCY;

                    //shard.setupRenderState();

                    if(!showCosmeticState()) {
                        GuiGraphicsUtils.drawWithSpectrum(context, button.x() + 2, button.y() + 2, 0, 16, 16, textureAtlasSprite, 1f);
                        context.blit(button.x() + 2, button.y() + 2, 0, 16, 16, textureAtlasSprite, red, green, blue, 0.4f);
                    } else {
                        context.blit(button.x() + 2, button.y() + 2, 0, 16, 16, textureAtlasSprite, red, green, blue, 0.9f);
                    }

                    //shard.clearRenderState();
                }).sizing(Sizing.fixed(20))
                .tooltip(createToggleTooltip("slot_cosmetics", false, showCosmeticState()));

        var accessoriesTogglePanel = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .child(
                        cosmeticToggleButton.margins(Insets.of(2, 2, 2, 2))
                )
//                .child(
//                        Components.box(Sizing.fixed(18), Sizing.fixed(1))
//                                .color(Color.WHITE)
//                )
                .gap(1)
                .padding(Insets.of(6))
                .surface(Surface.PANEL.and((context, component) -> {
                    NinePatchTexture.draw(OwoUIDrawContext.PANEL_INSET_NINE_PATCH_TEXTURE,
                            context, component.x() + 6, component.y() + 6, component.width() - 12, component.height() - 12);
                }))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .id("accessories_toggle_panel");

        var groupFilterComponent = createGroupFilters();

        if(groupFilterComponent != null) accessoriesTogglePanel.child(groupFilterComponent);

        return accessoriesTogglePanel;
    }

    @Nullable
    private io.wispforest.owo.ui.core.Component createGroupFilters() {
        if (!this.showGroupFilters()) return null;

        var groups = new ArrayList<>(SlotGroupLoader.getValidGroups(this.getMenu().targetEntityDefaulted()).keySet());

        if (groups.isEmpty()) return null;

        var groupButtons = groups.stream()
                .map(group -> ComponentUtils.groupToggleBtn(this, group))
                .toList();

        var baseButtonLayout = (ParentComponent) Containers.verticalFlow(Sizing.content(), Sizing.content())
                .children(groupButtons)
                .gap(1);

        if(groupButtons.size() > 5) {
            baseButtonLayout = new ExtendedScrollContainer<>(
                    ScrollContainer.ScrollDirection.VERTICAL,
                    Sizing.fixed(18 + 6),
                    Sizing.fixed((5 * 14) + (5 * 1) + (2 * 2) - 1),
                    baseButtonLayout.padding(!this.mainWidgetPosition() ? Insets.of(2, 2, 0, 2) : Insets.of(2, 2, 2, 0))
            ).oppositeScrollbar(!this.mainWidgetPosition())
                    .customClippingInsets(Insets.of(1))
                    .scrollbarThiccness(8)
                    .scrollbar(ScrollContainer.Scrollbar.vanilla())
                    .fixedScrollbarLength(16);
        }

        return new ExtendedCollapsibleContainer(Sizing.content(), Sizing.content(), false)
                .child(
                        Components.button(Component.empty(), btn -> {
                                    this.getMenu().selectedGroups().clear();
                                    this.rebuildAccessoriesComponent();
                                }).renderer((context, button, delta) -> {
                                    ButtonComponent.Renderer.VANILLA.draw(context, button, delta);

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
            this.disableSlot(0);
            this.disableSlot(1);
            this.disableSlot(2);
            this.disableSlot(3);
            this.disableSlot(4);
        }
    }

    private io.wispforest.owo.ui.core.Component createOptionsComponent() {
        var baseOptionPanel = Containers.grid(Sizing.fixed(162), Sizing.content(), 3, 2)
                .configure((GridLayout component) -> {
                    component.surface(Surface.PANEL_INSET)
                            .verticalAlignment(VerticalAlignment.CENTER)
                            .horizontalAlignment(HorizontalAlignment.CENTER)
                            .padding(Insets.of(3));
                });

        //--

        baseOptionPanel.child(
                Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(Components.label(Component.literal("Unused Slots:")))
                        .child(
                                Components.button(
                                                createToggleTooltip("unused_slots", false, this.showUnusedSlots()),
                                                btn -> {
                                                    AccessoriesInternals.getNetworkHandler()
                                                            .sendToServer(SyncHolderChange.of(HolderProperty.UNUSED_PROP, this.getMenu().owner(), bl -> !bl));
                                                }
                                        )
                                        .tooltip(createToggleTooltip("unused_slots", true, this.showUnusedSlots()))
                                        .horizontalSizing(Sizing.fixed(74))
                                        .id("unused_slots_toggle")
                        ).margins(Insets.bottom(3)),
                0, 0);

        {
            var min = getMinimumColumnAmount();
            var max = 18;

            var step = 1f / (max - min);

            var columnAmountSlider = Components.discreteSlider(Sizing.fixed(45), min, max)
                    .snap(true)
                    .setFromDiscreteValue(this.columnAmount())
                    .scrollStep(step);

            columnAmountSlider.tooltip(Accessories.translation("column_slider.tooltip"));

            columnAmountSlider.id("column_amount_slider");

            columnAmountSlider.onChanged().subscribe(value -> {
                AccessoriesInternals.getNetworkHandler()
                        .sendToServer(SyncHolderChange.of(HolderProperty.COLUMN_AMOUNT_PROP, (int) value));

                this.columnAmount((int) value);

                rebuildAccessoriesComponent();
            });

            baseOptionPanel.child(
                    Containers.verticalFlow(Sizing.content(), Sizing.content())
                            .child(Components.label(Component.literal("Column Amount:")))
                            .child(columnAmountSlider.horizontalSizing(Sizing.fixed(74)))
                            .margins(Insets.bottom(3)),
                    0, 1);
        }

        baseOptionPanel.child(
                Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(Components.label(Component.literal("Widget Variant:")))
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
                                                }).tooltip(widgetTypeToggleMessage(this.widgetType(), true))
                                        .id("widget_type_toggle")
                                        .horizontalSizing(Sizing.fixed(74))
                        ).margins(Insets.bottom(3)),
                1, 0);

        baseOptionPanel.child(
                Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(Components.label(Component.literal("Widget Position:")))
                        .child(
                                Components.button(
                                        createToggleTooltip("main_widget_position", false, this.mainWidgetPosition()),
                                        btn -> {
                                            AccessoriesInternals.getNetworkHandler()
                                                    .sendToServer(SyncHolderChange.of(HolderProperty.MAIN_WIDGET_POSITION_PROP, this.menu.owner(), bl -> !bl));

                                            this.mainWidgetPosition(!this.mainWidgetPosition());

                                            this.uiAdapter.rootComponent.childById(InventoryEntityComponent.class, "entity_rendering_component")
                                                    .startingRotation(this.mainWidgetPosition() ? -45 : 45);
                                        }).tooltip(createToggleTooltip("main_widget_position", true, this.mainWidgetPosition()))
                                .id("main_widget_position_toggle")
                                        .horizontalSizing(Sizing.fixed(74))
                        ).margins(Insets.bottom(3)),
                1, 1);

        baseOptionPanel.child(
                Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(Components.label(Component.literal("Group Filters:")))
                        .child(
                                Components.button(
                                                createToggleTooltip("group_filter", false, this.showGroupFilters()),
                                                btn -> {
                                                    AccessoriesInternals.getNetworkHandler()
                                                            .sendToServer(SyncHolderChange.of(HolderProperty.GROUP_FILTER_PROP, this.menu.owner(), bl -> !bl));

                                                    this.showGroupFilters(!this.showGroupFilters());

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

                                                }).tooltip(createToggleTooltip("group_filter", true, this.showGroupFilters()))
                                        .id("group_filter_toggle")
                                        .horizontalSizing(Sizing.fixed(74))
                        ),
                2, 0);

        baseOptionPanel.child(
                Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(Components.label(Component.literal("Side Position:")))
                        .child(
                                Components.button(
                                                createToggleTooltip("side_widget_position", false, this.sideWidgetPosition()),
                                                btn -> {
                                                    AccessoriesInternals.getNetworkHandler()
                                                            .sendToServer(SyncHolderChange.of(HolderProperty.SIDE_WIDGET_POSITION_PROP, this.menu.owner(), bl -> !bl));

                                                    this.sideWidgetPosition(!this.sideWidgetPosition());

                                                    this.swapOrCreateSideBarComponent();
                                                }).tooltip(createToggleTooltip("side_widget_position", true, this.sideWidgetPosition()))
                                        .id("side_widget_position_toggle")
                                        .horizontalSizing(Sizing.fixed(74))
                        ),
                2, 1);

        return Containers.verticalScroll(Sizing.expand(), Sizing.expand(), baseOptionPanel);
    }

    @Override
    @Nullable
    public Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
        var accessories = this.topComponent;

        if(accessories != null) {
            var result = accessories.isHovering_Logical(slot, mouseX, mouseY);

            if(result != null) return result;
        }

        return null;
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
    public void onHolderChange(String key) {
        switch (key) {
            case "unused_slots" -> updateToggleButton("unused_slots", this::showUnusedSlots, () -> {
                this.getMenu().updateUsedSlots();
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
        var btn = this.uiAdapter.rootComponent.childById(ButtonComponent.class, baseId + "_toggle");

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
        var type = switch (value) {
            case 2 -> "scrollable";
            case 1 -> "paginated";
            default -> "";
        };

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

        public final int index() {
            return index;
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

            if(reason == DismountReason.REMOVED) {
                ((SlotAccessor) slot).owo$setX(-300);
                ((SlotAccessor) slot).owo$setY(-300);
            }
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

    private final Surface SLOT_RENDERING_SURFACE = (context, component) -> {
        var validComponents = new ArrayList<ExtendedSlotComponent>();

        ComponentUtils.recursiveSearch(component, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> {
            if(!slotComponent.isBatched() || !(slotComponent.slot() instanceof SlotTypeAccessible)) return;

            validComponents.add(slotComponent);
        });

        context.push();

        {
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

                    if (data == null) return;

                    var itemStack = data.getA();

                    if (itemStack.isEmpty() /*&& slot.isActive()*/) {
                        var pair = slot.getNoItemIcon();

                        if (pair != null) {
                            var textureAtlasSprite = this.minecraft.getTextureAtlas(pair.getFirst()).apply(pair.getSecond());

                            context.blit(slot.x, slot.y, 0, 16, 16, textureAtlasSprite);

                            allBl2s.put(slot, true);
                        }
                    }

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

            //--

            context.pop();

        }

        safeBatching(context, hasDrawCallOccur -> {
            for (var slotComponent : validComponents) {
                slotComponent.renderCosmeticOverlay(context, true);

                hasDrawCallOccur.setValue(true);
            }
        });

        for (var slotComponent : validComponents) {
            slotComponent.renderHover(context, () -> this.hoveredSlot);
        }

        context.pop();
    };

    public final Surface FULL_SLOT_RENDERING = BACKGROUND_SLOT_RENDERING_SURFACE.and(SLOT_RENDERING_SURFACE);

    //--

    @Nullable
    public Triplet<ItemStack, Boolean, @Nullable String> getRenderStack(Slot slot) {
        var accessor = (AbstractContainerScreenAccessor) this;

        ItemStack itemStack = slot.getItem();

        boolean bl = false;

        ItemStack itemStack2 = this.menu.getCarried();

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
