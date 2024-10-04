package io.wispforest.accessories.mixin.sodium;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import io.wispforest.accessories.client.MPOATVConstructingVertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(MPOATVConstructingVertexConsumer.class)
public abstract class MPOATVConstructingVertexConsumerMixin_SodiumImpl implements VertexConsumer, VertexBufferWriter {

    @Override
    public void push(MemoryStack memoryStack, long ptr, int count, VertexFormat format) {
        long stride = format.getVertexSize();
        long positionOffset = format.getOffset(VertexFormatElement.POSITION);

        for(int vertexIndex = 0; vertexIndex < count; ++vertexIndex) {
            var positionPtr = ptr + positionOffset;

            float x = PositionAttribute.getX(positionPtr);
            float y = PositionAttribute.getY(positionPtr);
            float z = PositionAttribute.getZ(positionPtr);

            this.addVertex(x, y, z);

            ptr += stride;
        }
    }
}
