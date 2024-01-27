package io.wispforest.accessories.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ToggleButton extends Button {

    public static final WidgetSprites SPRITES = new WidgetSprites(new ResourceLocation("widget/button"), new ResourceLocation("widget/button_disabled"), new ResourceLocation("widget/button_highlighted"));

    private boolean toggled = false;

    private final int zIndex;

    private final Consumer<ToggleButton> onRender;

    protected ToggleButton(int x, int y, int zIndex, int width, int height, Component message, OnPress onPress, CreateNarration createNarration, Consumer<ToggleButton> onRender) {
        super(x, y, width, height, message, onPress, createNarration);

        this.zIndex = zIndex;
        this.onRender = onRender;
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

        var pose = guiGraphics.pose();

        pose.pushPose();
        pose.translate(0, 0, zIndex);

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        guiGraphics.blitSprite(SPRITES.get(this.toggled(), this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = this.active ? 16777215 : 10526880;
        this.renderString(guiGraphics, Minecraft.getInstance().font, i | Mth.ceil(this.alpha * 255.0F) << 24);

        pose.popPose();
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
