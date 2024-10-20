package io.wispforest.accessories.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.Accessories;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.AnimatableProperty;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.PositionedRectangle;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.resources.ResourceLocation;

public class ArrowComponent extends BaseComponent {
    protected final ResourceLocation texture = Accessories.of("textures/gui/all_arrow_directions.png");

    protected final int textureWidth = 32;
    protected final int textureHeight = 32;

    protected Direction direction;
    protected boolean centered = false;

    protected final AnimatableProperty<PositionedRectangle> visibleArea;
    protected boolean blend = false;

    public ArrowComponent(Direction direction) {
        this.direction = direction;
        this.visibleArea = AnimatableProperty.of(PositionedRectangle.of(0, 0, this.regionWidth(), this.regionHeight()));
    }

    protected int regionWidth() {
        return this.direction.regionWidth;
    }

    protected int regionHeight() {
        return this.direction.regionHeight;
    }

    protected int u() {
        return this.direction.u;
    }

    protected int v() {
        return this.direction.v;
    }

    protected int determineHorizontalContentSize(Sizing sizing) {
        return this.regionWidth();
    }

    protected int determineVerticalContentSize(Sizing sizing) {
        return this.regionHeight();
    }

    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        this.visibleArea.update(delta);
    }

    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        RenderSystem.enableDepthTest();
        if (this.blend) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }

        PoseStack matrices = context.pose();
        matrices.pushPose();
        matrices.translate((float) this.x, (float) this.y, 0.0F);

        if(this.centered) matrices.translate(this.direction.getXOffset(), this.direction.getYOffset(), 0.0f);

        matrices.scale((float) this.width / (float) this.regionWidth(), (float) this.height / (float) this.regionHeight(), 0.0F);
        PositionedRectangle visibleArea = (PositionedRectangle) this.visibleArea.get();
        int bottomEdge = Math.min(visibleArea.y() + visibleArea.height(), this.regionHeight());
        int rightEdge = Math.min(visibleArea.x() + visibleArea.width(), this.regionWidth());
        context.blit(this.texture, visibleArea.x(), visibleArea.y(), rightEdge - visibleArea.x(), bottomEdge - visibleArea.y(), (float) (this.u() + visibleArea.x()), (float) (this.v() + visibleArea.y()), rightEdge - visibleArea.x(), bottomEdge - visibleArea.y(), this.textureWidth, this.textureHeight);
        if (this.blend) {
            RenderSystem.disableBlend();
        }

        matrices.popPose();
    }

    public ArrowComponent changeDirection(Direction direction) {
        this.direction = direction;
        this.resetVisibleArea();

        return this;
    }

    public ArrowComponent centered(boolean value) {
        this.centered = value;

        return this;
    }

    public ArrowComponent visibleArea(PositionedRectangle visibleArea) {
        this.visibleArea.set(visibleArea);
        return this;
    }

    public ArrowComponent resetVisibleArea() {
        this.visibleArea(PositionedRectangle.of(0, 0, this.regionWidth(), this.regionHeight()));
        return this;
    }

    public AnimatableProperty<PositionedRectangle> visibleArea() {
        return this.visibleArea;
    }

    public ArrowComponent blend(boolean blend) {
        this.blend = blend;
        return this;
    }

    public boolean blend() {
        return this.blend;
    }

    public enum Direction {
        RIGHT( 0,  0, 16, 14),
        LEFT (16,  0, 16, 14),
        UP   ( 0, 16, 14, 16),
        DOWN (16, 16, 14, 16);

        public final int u;
        public final int v;
        public final int regionWidth;
        public final int regionHeight;

        Direction(int u, int v, int regionWidth, int regionHeight) {
            this.u = u;
            this.v = v;
            this.regionWidth = regionWidth;
            this.regionHeight = regionHeight;
        }

        public float getXOffset() {
            return (this == UP || this == DOWN) ? 0.5f : 0;
        }

        public float getYOffset() {
            return (this == LEFT || this == RIGHT) ? 0.5f : 0;
        }
    }
}
