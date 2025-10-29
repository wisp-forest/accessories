package io.wispforest.accessories.fabric.mixin.sodium;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.wispforest.accessories.client.MPOATVConstructingVertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(MPOATVConstructingVertexConsumer.class)
public abstract class MPOATVConstructingVertexConsumerMixin_SodiumImpl implements VertexConsumer, VertexBufferWriter {
    @Override
    public void push(MemoryStack memoryStack, long ptr, int count, VertexFormatDescription description) {
        long stride = description.stride();
        long positionOffset = description.getElementOffset(CommonVertexAttribute.POSITION);

        for(int vertexIndex = 0; vertexIndex < count; ++vertexIndex) {
            var positionPtr = ptr + positionOffset;

            float x = PositionAttribute.getX(positionPtr);
            float y = PositionAttribute.getY(positionPtr);
            float z = PositionAttribute.getZ(positionPtr);

            this.vertex(x, y, z);

            ptr += stride;
        }
    }
}
