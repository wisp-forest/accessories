package io.wispforest.accessories.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.server.SyncCosmeticToggle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ToggleButton extends Button {

    private static final WidgetSprites SPRITES = new WidgetSprites(
            ResourceLocation.withDefaultNamespace("widget/button"),
            ResourceLocation.withDefaultNamespace("widget/button_disabled"),
            ResourceLocation.withDefaultNamespace("widget/button_highlighted"));

    private boolean toggled = false;

    private final int zIndex;

    private final Consumer<ToggleButton> onRender;

    protected ToggleButton(int x, int y, int zIndex, int width, int height, Component message, OnPress onPress, CreateNarration createNarration, Consumer<ToggleButton> onRender) {
        super(x, y, width, height, message, onPress, createNarration);

        this.zIndex = zIndex;
        this.onRender = onRender;
    }

    public static ToggleButton ofSlot(int x, int y, int z, AccessoriesBasedSlot slot) {
        return ToggleButton.toggleBuilder(Component.empty(), btn -> {
                    AccessoriesNetworking.sendToServer(SyncCosmeticToggle.of(slot.entity.equals(Minecraft.getInstance().player) ? null : slot.entity, slot.accessoriesContainer.slotType(), slot.getContainerSlot()));
                }).onRender(btn -> {
                    var bl = slot.accessoriesContainer.shouldRender(slot.getContainerSlot());

                    if (bl == btn.toggled()) return;

                    btn.toggled(bl);
                    btn.setTooltip(accessoriesToggleTooltip(bl));
                }).tooltip(accessoriesToggleTooltip(slot.accessoriesContainer.shouldRender(slot.getContainerSlot())))
                .zIndex(z)
                .bounds(x, y, 5, 5)
                .build()
                .toggled(slot.accessoriesContainer.shouldRender(slot.getContainerSlot()));
    }

    private static Tooltip accessoriesToggleTooltip(boolean value) {
        var key = "display.toggle." + (!value ? "show" : "hide");

        return Tooltip.create(Component.translatable(Accessories.translationKey(key)));
    }

    public ToggleButton toggled(boolean value){
        this.toggled = value;

        return this;
    }

    public boolean toggled(){
        return this.toggled;
    }

    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    public static ToggleButton.Builder toggleBuilder(Component message, Button.OnPress onPress) {
        return new ToggleButton.Builder(message, onPress);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.onRender.accept(this);

        var minecraft = Minecraft.getInstance();
        guiGraphics.blitSprite(
                RenderType::guiTextured,
                SPRITES.get(this.toggled(), this.isHoveredOrFocused()),
                this.getX(),
                this.getY(),
                this.getWidth(),
                this.getHeight(),
                ARGB.white(this.alpha)
        );
        int i = this.active ? 16777215 : 10526880;
        this.renderString(guiGraphics, minecraft.font, i | Mth.ceil(this.alpha * 255.0F) << 24);
    }

    @Environment(EnvType.CLIENT)
    public static class Builder {
        private final Component message;
        private final Button.OnPress onPress;
        @Nullable
        private Tooltip tooltip;
        private int x;
        private int y;
        private int zIndex = 0;
        private int width = 150;
        private int height = 20;
        private Button.CreateNarration createNarration = Button.DEFAULT_NARRATION;

        private Consumer<ToggleButton> onRender = toggleButton -> {};

        public Builder(Component message, Button.OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public ToggleButton.Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public ToggleButton.Builder zIndex(int zIndex){
            this.zIndex = zIndex;

            return this;
        }

        public ToggleButton.Builder onRender(Consumer<ToggleButton> consumer){
            this.onRender = consumer;

            return this;
        }

        public ToggleButton.Builder width(int width) {
            this.width = width;
            return this;
        }

        public ToggleButton.Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public ToggleButton.Builder bounds(int x, int y, int width, int height) {
            return this.pos(x, y).size(width, height);
        }

        public ToggleButton.Builder tooltip(@Nullable Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public ToggleButton.Builder createNarration(Button.CreateNarration createNarration) {
            this.createNarration = createNarration;
            return this;
        }

        public ToggleButton build() {
            ToggleButton button = new ToggleButton(this.x, this.y, this.zIndex, this.width, this.height, this.message, this.onPress, this.createNarration, this.onRender);

            button.setTooltip(this.tooltip);
            return button;
        }
    }
}
