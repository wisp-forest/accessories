package io.wispforest.accessories.client.gui;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.wispforest.owo.client.OwoClient;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public class ExtraDrawingUtils {

    private static final RenderType TASTE_THE_RAINBOW = RenderType.create(
            "taste_the_rainbow",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            786432,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(){
                        @Override
                        public void setupRenderState() {
                            OwoClient.HSV_PROGRAM.use();
                        }
                    })
                    //.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    //.setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
    );

    public static void drawRectOutline(OwoUIDrawContext context, int x, int y, int width, int height) {
        drawRectOutline(context, x, y, width, height, Insets.of(4));
    }

    public static void drawRectOutline(OwoUIDrawContext context, int x, int y, int width, int height, Insets insets) {
        Runnable runnable = () -> {
            var color = Color.WHITE.argb();

            context.fill(TASTE_THE_RAINBOW,x, y, x + width, y + height, 0, color);

            if(false) {
                context.fill(TASTE_THE_RAINBOW, x - insets.left(), y - insets.top(), x + width + insets.right(), y, 0, color);
                context.fill(TASTE_THE_RAINBOW, x - insets.left(), y + height, x + width + insets.right(), y + height - insets.bottom(), 0, color);

                context.fill(TASTE_THE_RAINBOW, x - insets.left(), y, x, y + height, 0, color);
                context.fill(TASTE_THE_RAINBOW, x + width, y, x + width + insets.right(), y + height, 0, color);
            }
        };

        runnable.run();

        //context.drawManaged(runnable);
    }
}
