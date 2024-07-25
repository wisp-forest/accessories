package io.wispforest.accessories.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {
    //@Invoker("blitSprite") void callBlitSprite(TextureAtlasSprite sprite, int x, int y, int blitOffset, int width, int height);
}
