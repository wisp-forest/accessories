package io.wispforest.accessories.client.gui.components;

import com.mojang.math.Axis;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.renderstate.EntityElementRenderState;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class InventoryEntityComponent<E extends Entity> extends EntityComponent<E> {

    private float startingRotation = -45;

    private float lastBbWidth = 0.0f;
    private float lastBbHeight = 0.0f;

    private ScaleFitType type = ScaleFitType.NONE;

    private boolean sideBySideMode = false;
    private int additionalOffset = 0;

    public InventoryEntityComponent(Sizing horizontalSizing, Sizing verticalSizing, E entity) {
        super(Sizing.fixed(0), entity);

        this.horizontalSizing(horizontalSizing)
                .verticalSizing(verticalSizing);

        this.lastBbWidth = entity.getBbWidth();
        this.lastBbHeight = entity.getBbHeight();
    }

    public InventoryEntityComponent(Sizing horizontalSizing, Sizing verticalSizing, EntityType<E> type, @Nullable CompoundTag nbt) {
        super(Sizing.fixed(0), type, nbt);

        this.horizontalSizing(horizontalSizing)
                .verticalSizing(verticalSizing);

        this.lastBbWidth = entity.getBbWidth();
        this.lastBbHeight = entity.getBbHeight();
    }

    public static <E extends Entity> InventoryEntityComponent<E> of(Sizing horizontalSizing, Sizing verticalSizing, E entity) {
        return new InventoryEntityComponent<E>(horizontalSizing, verticalSizing, entity);
    }

    public boolean sideBySideMode() {
        return this.sideBySideMode;
    }

    public InventoryEntityComponent<E> sideBySideMode(boolean sideBySideMode) {
        this.sideBySideMode = sideBySideMode;

        return this;
    }

    public int additionalOffset() {
        return this.additionalOffset;
    }

    public InventoryEntityComponent<E> additionalOffset(int value) {
        this.additionalOffset = value;

        return this;
    }

    private float getEntityScale() {
        return (entity instanceof LivingEntity living) ? living.getScale() : 1.0f;
    }

    public float xOffset = 0.0f;
    public float yOffset = 0.0f;

    private TriConsumer<OwoUIDrawContext, Component, List<Runnable>> renderWrapping = (ctx, component, runnables) -> runnables.forEach(Runnable::run);

    public InventoryEntityComponent<E> renderWrapping(TriConsumer<OwoUIDrawContext, Component, List<Runnable>> renderWrapping) {
        this.renderWrapping = renderWrapping;

        return this;
    }

    public float getSingleInstanceWidth() {
        return (this.horizontalSizing().get().value / (sideBySideMode ? 2f : 1f)) - (sideBySideMode ? 25 : 40);
    }

    public InventoryEntityComponent<E> scaleToFit(boolean scaleToFit) {
        if(scaleToFit) {
            var componentHeight = (float) this.verticalSizing().get().value;
            var componentWidth = getSingleInstanceWidth();

            var entityHeight = entity.getBbHeight() * (Math.min(componentWidth, componentHeight) / Math.max(componentWidth, componentHeight));
            var entityWidth = entity.getBbWidth()* (Math.max(componentWidth, componentHeight) / Math.min(componentWidth, componentHeight));

            var length = Math.max(entityHeight, entityWidth);

            float baseScale = (.35f / length);

            this.scale(baseScale);

            type = ScaleFitType.BOTH;
        } else {
            this.scale(1);

            type = ScaleFitType.NONE;
        }

        return this;
    }

    public InventoryEntityComponent<E> startingRotation(float value) {
        this.startingRotation = value;

        return this;
    }

    public InventoryEntityComponent<E> scaleToFitVertically(boolean scaleToFit) {
        this.scale(scaleToFit ? (.5f / entity.getBbHeight()) : 1);

        type = scaleToFit ? ScaleFitType.VERTICAL : ScaleFitType.NONE;

        return this;
    }

    public InventoryEntityComponent<E> scaleToFitHorizontally(boolean scaleToFit) {
        this.scale(scaleToFit ? (.5f / entity.getBbWidth()) : 1);

        type = scaleToFit ? ScaleFitType.HORIZONTAL : ScaleFitType.NONE;

        return this;
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        if(!(entity instanceof LivingEntity living)) {
            super.draw(context, mouseX, mouseY, partialTicks, delta);

            return;
        }

        if (this.lastBbWidth != entity.getBbWidth() || this.lastBbHeight != entity.getBbHeight()) {
            switch (type) {
                case VERTICAL -> this.scaleToFitVertically(true);
                case HORIZONTAL -> this.scaleToFitHorizontally(true);
                case BOTH -> this.scaleToFit(true);
                case NONE -> {}
            }

            this.lastBbWidth = entity.getBbWidth();
            this.lastBbHeight = entity.getBbHeight();
        }

        var renderQueue = new ArrayList<Runnable>();

        renderQueue.add(
                () -> {
                    context.push();
                    renderLiving(context, living, mouseX, mouseY, partialTicks, true);
                    context.pop();
                }
        );

        if (sideBySideMode) {
            renderQueue.add(
                    () -> {
                        context.push();
                        renderLiving(context, living, mouseX, mouseY, partialTicks, false);
                        context.pop();
                    }
            );
        }

        this.renderWrapping.accept(
                context,
                this,
                renderQueue
        );
    }

    private void renderLiving(OwoUIDrawContext context, LivingEntity living, int mouseX, int mouseY, float partialTicks, boolean isLeftSide) {
        var matrix = new Matrix4f();

        transformMatrixStack(matrix, isLeftSide);

        this.transform.accept(matrix);

        float prevYBodyRot0 = living.yBodyRotO;
        float prevYBodyRot = living.yBodyRot;
        float prevYRot = living.getYRot();
        float prevYRot0 = living.yRotO;
        float prevXRot = living.getXRot();
        float prevXRot0 = living.xRotO;
        float prevYHeadRot0 = living.yHeadRotO;
        float prevYHeadRot = living.yHeadRot;

        rotateMatrixStack(matrix, living, mouseX, mouseY, isLeftSide);

        {
            living.yBodyRotO = 0;
            living.yBodyRot = 0;
            living.setYRot(0);
            living.yHeadRot = living.yBodyRot;
            living.yHeadRotO = living.yBodyRotO;

            var renderer = (EntityRenderer) this.manager.getRenderer(this.entity);

            var entityState = renderer.createRenderState(this.entity, partialTicks);

            entityState.x = 0;
            entityState.y = 0;
            entityState.z = 0;

            if (showNametag) {
                // TODO: FIX LATER WITH OWO UPSTREAM
                entityState.nameTag = entity.getDisplayName();
                entityState.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTicks));
            } else {
                entityState.nameTag = null;
                entityState.nameTagAttachment = null;
            }

            entityState.lightCoords = 15728880;
            entityState.hitboxesRenderState = null;
            entityState.shadowPieces.clear();
            entityState.outlineColor = 0;

            context.guiRenderState.submitPicturesInPictureState(new EntityElementRenderState(
                entityState,
                matrix,
                new ScreenRectangle(this.x, this.y, this.width, this.height),
                context.scissorStack.peek()
            ));
        }

        living.yBodyRotO = prevYBodyRot0;
        living.yBodyRot = prevYBodyRot;
        living.setYRot(prevYRot);
        living.yRotO = prevYRot0;
        living.setXRot(prevXRot);
        living.xRotO = prevXRot0;
        living.yHeadRotO = prevYHeadRot0;
        living.yHeadRot = prevYHeadRot;
    }

    private void transformMatrixStack(Matrix4f matrix, boolean isLeftSide) {
        var trueWidth = this.width / (sideBySideMode ? 2f : 1f);

        var maxLength = Math.max(trueWidth, this.height);

        var baseScaleValue = 75 * this.scale;
        var xyScaleValue = baseScaleValue * maxLength / 64f;

        var yPos = this.height / 2f;

        matrix.translate(centerOffSet(isLeftSide), -yPos, 60);

        matrix.scale(xyScaleValue, -xyScaleValue, -baseScaleValue);

        matrix.translate(0, entity.getBbHeight() / -2f, 0);

        matrix.translate(this.xOffset * (isLeftSide ? 1 : -1), this.yOffset, 0);
    }

    private float centerOffSet(boolean isLeftSide) {
        return additionalOffset * (isLeftSide ? -2.3f : 2.3f);
    }

    private void rotateMatrixStack(Matrix4f matrix, LivingEntity living, int mouseX, int mouseY, boolean isLeftSide) {
        var trueWidth = this.width / (sideBySideMode ? 2f : 1f);

        float xRotation = (float) Math.toDegrees(Math.atan((mouseY - this.y - this.height / 2f) / 40f));

        var rotationOffset = (!isLeftSide ? 180f : 0);

        if (this.lookAtCursor) {
            float yRotation = (float) Math.toDegrees(Math.atan((mouseX - this.x - (trueWidth + centerOffSet(isLeftSide))) / 40f));

            living.yHeadRotO = -yRotation;

            this.entity.yRotO = -yRotation;
            this.entity.xRotO = xRotation * .65f;

            this.entity.setYRot(this.entity.yRotO);
            this.entity.setXRot(this.entity.xRotO);

            // We make sure the xRotation never becomes 0, as the lighting otherwise becomes very unhappy
            if (xRotation == 0) xRotation = .1f;
            matrix.rotate(Axis.XP.rotationDegrees(xRotation * .35f));
            matrix.rotate(Axis.YP.rotationDegrees(yRotation * .555f + rotationOffset));
        } else {
            this.entity.xRotO = xRotation * .35f;

            this.entity.setXRot(this.entity.xRotO);

            if (xRotation == 0) xRotation = .1f;
            matrix.rotate(Axis.XP.rotationDegrees(xRotation * .15f));

            matrix.rotate(Axis.XP.rotationDegrees(15));
            matrix.rotate(Axis.YP.rotationDegrees(startingRotation + this.mouseRotation + rotationOffset));
        }
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        this.scale += (float) (amount * this.scale * 0.1f);

        return true;
    }

    @Override
    public boolean onKeyPress(KeyEvent input) {
        var keycode = input.key();

        if(keycode == GLFW.GLFW_KEY_LEFT) {
            this.xOffset -= 0.05f;
        } else if(keycode == GLFW.GLFW_KEY_RIGHT) {
            this.xOffset += 0.05f;
        }

        if(keycode == GLFW.GLFW_KEY_UP) {
            this.yOffset += 0.05f;
        } else if(keycode == GLFW.GLFW_KEY_DOWN) {
            this.yOffset -= 0.05f;
        }

        return super.onKeyPress(input);
    }

    public enum ScaleFitType {
        VERTICAL,
        HORIZONTAL,
        BOTH,
        NONE;
    }
}
