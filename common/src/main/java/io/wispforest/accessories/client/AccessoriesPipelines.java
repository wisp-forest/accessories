package io.wispforest.accessories.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.wispforest.accessories.Accessories;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIPipelines;
import io.wispforest.owo.ui.event.WindowResizeCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class AccessoriesPipelines {

    public static final RenderType.CompositeRenderType HSV_GUI = RenderType.create(
            "accessories:hsv_gui",
            786432,
            OwoUIPipelines.GUI_HSV,
            RenderType.CompositeState.builder().createCompositeState(false)
    );

    public static final RenderPipeline.Snippet SPECTRUM_SNIPPET = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withFragmentShader(Accessories.of("core/spectrum_position_tex"))
            .withVertexShader(Accessories.of("core/spectrum_position_tex"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withSampler("InputSampler")
            .buildSnippet();

    public static final RenderPipeline SPECTRUM = RenderPipeline.builder(SPECTRUM_SNIPPET)
            .withLocation(Accessories.of("pipeline/spectrum"))
            .build();

    public static final Function<ResourceLocation, RenderType> SPECTRUM_GUI = Util.memoize(
            resourceLocation -> RenderType.create(
                    "accessories:spectrum_gui",
                    786432,
                    SPECTRUM,
                    RenderType.CompositeState.builder()
                            .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false))
                            .createCompositeState(false)
            )
    );

    public static final RenderPipeline COLORED_GUI_TEXTURED_PIPE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Accessories.of("pipeline/colored_gui_textured"))
            .withColorWrite(true)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE))
            .build();

    public static final BiFunction<Color, ResourceLocation, RenderType> COLORED_GUI_TEXTURED = Util.memoize(
            (color, resourceLocation) -> {
                return RenderType.create(
                        "accessories:colored_gui_textured",
                        786432,
                        COLORED_GUI_TEXTURED_PIPE,
                        RenderType.CompositeState.builder()
                                .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false))
                                /*.setTexturingState(new RenderStateShard.TexturingStateShard("setting_shader_color", () -> {
                                    RenderSystem.setShaderColor(color.red(), color.green(), color.blue(), 1f);
                                }, () -> {
                                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                                }))*/
                                .createCompositeState(false));
            }
    );

    public static void registerPipelines(Consumer<RenderPipeline> pipelineRegister) {
        pipelineRegister.accept(SPECTRUM);
        pipelineRegister.accept(COLORED_GUI_TEXTURED_PIPE);
    }

    private static TextureTarget BUFFER = null;

    private static Color SHADER_COLOR = null;
    // TODO: FIX THIS WHEN WE CARE ENOUGH TO RESOLVE ISSUES
//    private static final RenderType HOVER_EFFECT = RenderType.create(
//        "accessories_hover_effect",
//        786432,
//        RenderPipelines.GUI_TEXTURED_OVERLAY,
//        RenderType.CompositeState.builder().setTextureState(new RenderStateShard.EmptyTextureStateShard(
//            () -> {
//                RenderSystem.setShaderTexture(0, getOrCreateBuffer().getColorTexture());
//                if (SHADER_COLOR != null) {
//                    RenderSystem.setShaderColor(SHADER_COLOR.red(), SHADER_COLOR.green(), SHADER_COLOR.blue(), SHADER_COLOR.alpha());
//                }
//            },
//            () -> {
//                if (SHADER_COLOR != null) {
//                    RenderSystem.setShaderColor(1, 1, 1, 1);
//                    SHADER_COLOR = null;
//                }
//            }
//        )).createCompositeState(false)
//    );

//    public static RenderType setupHoverEffect(Color color) {
//        SHADER_COLOR = color;
//
//        return HOVER_EFFECT;
//    }

    public static TextureTarget getOrCreateBuffer() {
        try {
            if (BUFFER == null) {
                var window = Minecraft.getInstance().getWindow();

                BUFFER = new TextureTarget("accessories_buffer_thingy", window.getWidth(), window.getHeight(), true);

                WindowResizeCallback.EVENT.register((innerClient, innerWindow) -> {
                    if (BUFFER == null) return;
                    BUFFER.resize(innerWindow.getWidth(), innerWindow.getHeight());
                });
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create the buffer for Accessories Hover Rendering due to an error!", e);
        }

        return BUFFER;
    }
}
