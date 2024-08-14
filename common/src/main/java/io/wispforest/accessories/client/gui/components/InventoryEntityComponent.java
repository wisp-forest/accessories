package io.wispforest.accessories.client.gui.components;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.util.pond.OwoEntityRenderDispatcherExtension;
import it.unimi.dsi.fastutil.floats.FloatBinaryOperator;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.function.BinaryOperator;

public class InventoryEntityComponent<E extends Entity> extends EntityComponent<E> {

    private float startingRotation = -45;

    public InventoryEntityComponent(Sizing sizing, E entity) {
        super(sizing, entity);
    }

    public InventoryEntityComponent(Sizing sizing, EntityType<E> type, @Nullable CompoundTag nbt) {
        super(sizing, type, nbt);
    }

    public static <E extends Entity> InventoryEntityComponent<E> of(Sizing verticalSizing, Sizing horizontalSizing, E entity) {
        var component = new InventoryEntityComponent<E>(verticalSizing, entity);

        component.horizontalSizing(horizontalSizing);

        return component;
    }

    public float xOffset = 0.0f;
    public float yOffset = 0.0f;

    public EntityComponent<E> scaleToFit(boolean scaleToFit) {
        if(scaleToFit) {
            var componentHeight = (float) this.verticalSizing().get().value;
            var componentWidth = (float) this.horizontalSizing().get().value - 40;

            var entityHeight = entity.getBbHeight() * (Math.min(componentWidth, componentHeight) / Math.max(componentWidth, componentHeight));
            var entityWidth = entity.getBbWidth() * (Math.max(componentWidth, componentHeight) / Math.min(componentWidth, componentHeight));

            var length = Math.max(entityHeight, entityWidth);

            float baseScale = (.35f / length);

            this.scale(baseScale);
        } else {
            this.scale(1);
        }

        return this;
    }

    public EntityComponent<E> startingRotation(float value) {
        this.startingRotation = value;

        return this;
    }

    public EntityComponent<E> scaleToFitVertically(boolean scaleToFit) {
        this.scale(scaleToFit ? (.5f / entity.getBbHeight()) : 1);

        return this;
    }

    public EntityComponent<E> scaleToFitHorizontally(boolean scaleToFit) {
        this.scale(scaleToFit ? (.5f / entity.getBbWidth()) : 1);

        return this;
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        if(!(entity instanceof LivingEntity living)) {
            super.draw(context, mouseX, mouseY, partialTicks, delta);

            return;
        }

        var matrices = context.pose();
        matrices.pushPose();

        var maxLength = Math.max(this.width, this.height);

        matrices.translate(x + this.width / 2f, y + this.height / 2f, 100);
        matrices.scale(75 * this.scale * maxLength / 64f, -75 * this.scale * maxLength / 64f, 75 * this.scale);

        matrices.translate(0, entity.getBbHeight() / -2f, 0);

        this.transform.accept(matrices);

        float prevYBodyRot0 = living.yBodyRotO;
        float prevYBodyRot = living.yBodyRot;
        float prevYRot = living.getYRot();
        float prevYRot0 = living.yRotO;
        float prevXRot = living.getXRot();
        float prevXRot0 = living.xRotO;
        float prevYHeadRot0 = living.yHeadRotO;
        float prevYHeadRot = living.yHeadRot;

        var dispatcher = (OwoEntityRenderDispatcherExtension) this.dispatcher;

        {
            float xRotation = (float) Math.toDegrees(Math.atan((mouseY - this.y - this.height / 2f) / 40f));

            this.entity.xRotO = xRotation * .35f;

            if (xRotation == 0) xRotation = .1f;
            matrices.mulPose(Axis.XP.rotationDegrees(xRotation * .15f));

            matrices.mulPose(Axis.XP.rotationDegrees(15));
            matrices.mulPose(Axis.YP.rotationDegrees(startingRotation + this.mouseRotation));

            dispatcher.owo$setCounterRotate(true);
            dispatcher.owo$setShowNametag(this.showNametag);

            Lighting.setupForEntityInInventory();
            //RenderSystem.setShaderLights(new Vector3f(.15f, 1, 0), new Vector3f(.15f, -1, 0));
            this.dispatcher.setRenderShadow(false);

            float h = (float) Math.atan(((x + x + this.width) / 2f - mouseX) / 40.0F);
            float i = (float) Math.atan(((y + y + this.height) / 2f - mouseY) / 40.0F);

            living.yBodyRotO = 0;
            living.yBodyRot = 0;
            living.setYRot(0);
            //living.setXRot(0);
            living.yHeadRot = living.yBodyRot; //living.getYRot();
            living.yHeadRotO = living.yBodyRotO; //living.getYRot();

//        Quaternionf quaternionf = new Quaternionf().rotateZ((float) Math.PI);
//        Quaternionf quaternionf2 = new Quaternionf().rotateX(i * 20.0F * (float) (Math.PI / 180.0));
//        quaternionf.mul(quaternionf2);
//
//        if (quaternionf2 != null) {
//            this.dispatcher.overrideCameraOrientation(quaternionf2.conjugate(new Quaternionf()).rotateY((float) Math.PI));
//        }

            matrices.translate(this.xOffset, this.yOffset, 0);

            this.dispatcher.setRenderShadow(false);
            this.dispatcher.render(this.entity, 0, 0, 0, 0, 0, matrices, this.entityBuffers, LightTexture.FULL_BRIGHT);

            this.dispatcher.setRenderShadow(true);
        }

        living.yBodyRotO = prevYBodyRot0;
        living.yBodyRot = prevYBodyRot;
        living.setYRot(prevYRot);
        living.yRotO = prevYRot0;
        living.setXRot(prevXRot);
        living.xRotO = prevXRot0;
        living.yHeadRotO = prevYHeadRot0;
        living.yHeadRot = prevYHeadRot;

        this.dispatcher.setRenderShadow(true);
        this.entityBuffers.endBatch();
        Lighting.setupFor3DItems();

        matrices.popPose();

        dispatcher.owo$setCounterRotate(false);
        dispatcher.owo$setShowNametag(true);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        this.scale += (float) (amount * this.scale * 0.1f);

        return true;
    }

    @Override
    public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if(keyCode == GLFW.GLFW_KEY_LEFT) {
            this.xOffset -= 0.05f;
        } else if(keyCode == GLFW.GLFW_KEY_RIGHT) {
            this.xOffset += 0.05f;
        }

        if(keyCode == GLFW.GLFW_KEY_UP) {
            this.yOffset += 0.05f;
        } else if(keyCode == GLFW.GLFW_KEY_DOWN) {
            this.yOffset -= 0.05f;
        }

        return super.onKeyPress(keyCode, scanCode, modifiers);
    }
}
