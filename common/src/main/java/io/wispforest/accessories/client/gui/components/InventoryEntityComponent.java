package io.wispforest.accessories.client.gui.components;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.util.pond.OwoEntityRenderDispatcherExtension;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class InventoryEntityComponent<E extends Entity> extends EntityComponent<E> {

    public InventoryEntityComponent(Sizing sizing, E entity) {
        super(sizing, entity);
    }

    public InventoryEntityComponent(Sizing sizing, EntityType<E> type, @Nullable CompoundTag nbt) {
        super(sizing, type, nbt);
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

        //if (this.lookAtCursor) {
            float xRotation = (float) Math.toDegrees(Math.atan((mouseY - this.y - this.height / 2f) / 40f));
            //float yRotation = (float) Math.toDegrees(Math.atan((mouseX - this.x - this.width / 2f) / 40f));

            //living.yHeadRotO = -yRotation;

            //this.entity.yRotO = -yRotation;
            this.entity.xRotO = xRotation * .35f;
//
//            // We make sure the xRotation never becomes 0, as the lighting otherwise becomes very unhappy
            if (xRotation == 0) xRotation = .1f;
            matrices.mulPose(Axis.XP.rotationDegrees(xRotation * .15f));
            //matrices.mulPose(Axis.YP.rotationDegrees(yRotation * .15f));
        //} else {
            matrices.mulPose(Axis.XP.rotationDegrees(15));
            matrices.mulPose(Axis.YP.rotationDegrees(-45 + this.mouseRotation));
        //}

        var dispatcher = (OwoEntityRenderDispatcherExtension) this.dispatcher;
        dispatcher.owo$setCounterRotate(true);
        dispatcher.owo$setShowNametag(this.showNametag);

        Lighting.setupForEntityInInventory();
        //RenderSystem.setShaderLights(new Vector3f(.15f, 1, 0), new Vector3f(.15f, -1, 0));
        this.dispatcher.setRenderShadow(false);

        float h = (float) Math.atan(((x + x + this.width) / 2f - mouseX) / 40.0F);
        float i = (float) Math.atan(((y + y + this.height) / 2f - mouseY) / 40.0F);

        float prevYBodyRot = living.yBodyRot;
        float prevYRot = living.getYRot();
        float prevXRot = living.getXRot();
        float prevYHeadRot0 = living.yHeadRotO;
        float prevYHeadRot = living.yHeadRot;

//        living.yBodyRot = 180.0F + h * 30.0F;
//        living.setYRot(180.0F + h * 40.0F);
//        living.setXRot(-i * 20.0F);
        living.yHeadRot = living.yBodyRot; //living.getYRot();
        living.yHeadRotO = living.yBodyRotO; //living.getYRot();

//        Quaternionf quaternionf = new Quaternionf().rotateZ((float) Math.PI);
//        Quaternionf quaternionf2 = new Quaternionf().rotateX(i * 20.0F * (float) (Math.PI / 180.0));
//        quaternionf.mul(quaternionf2);
//
//        if (quaternionf2 != null) {
//            this.dispatcher.overrideCameraOrientation(quaternionf2.conjugate(new Quaternionf()).rotateY((float) Math.PI));
//        }

        this.dispatcher.setRenderShadow(false);
        this.dispatcher.render(this.entity, 0, 0, 0, 0, 0, matrices, this.entityBuffers, LightTexture.FULL_BRIGHT);

        this.dispatcher.setRenderShadow(true);

        living.yBodyRot = prevYBodyRot;
        living.setYRot(prevYRot);
        living.setXRot(prevXRot);
        living.yHeadRotO = prevYHeadRot0;
        living.yHeadRot = prevYHeadRot;

        this.dispatcher.setRenderShadow(true);
        this.entityBuffers.endBatch();
        Lighting.setupFor3DItems();

        matrices.popPose();

        dispatcher.owo$setCounterRotate(false);
        dispatcher.owo$setShowNametag(true);
    }
}
