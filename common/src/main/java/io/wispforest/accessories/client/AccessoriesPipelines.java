package io.wispforest.accessories.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.wispforest.accessories.Accessories;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIPipelines;
import io.wispforest.owo.ui.event.WindowResizeCallback;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class AccessoriesPipelines {

    // Inline equivalent of private RenderPipelines.MATRICES_PROJECTION_SNIPPET
    private static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .buildSnippet();

    public static final RenderPipeline.Snippet SPECTRUM_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withFragmentShader(Accessories.of("core/spectrum_position_tex"))
            .withVertexShader(Accessories.of("core/spectrum_position_tex"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .withSampler("InputSampler")
            .buildSnippet();

    public static final RenderPipeline SPECTRUM = RenderPipeline.builder(SPECTRUM_SNIPPET)
            .withLocation(Accessories.of("pipeline/spectrum"))
            .build();

    // Inline equivalent of private RenderPipelines.GUI_TEXTURED_SNIPPET
    private static final RenderPipeline.Snippet GUI_TEXTURED_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
            .withVertexShader("core/gui")
            .withFragmentShader("core/gui")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .buildSnippet();

    public static final RenderPipeline COLORED_GUI_TEXTURED_PIPE = RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
            .withLocation(Accessories.of("pipeline/colored_gui_textured"))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .withColorTargetState(new ColorTargetState(new BlendFunction(SourceFactor.ONE, DestFactor.ONE)))
            .build();

    // Using OwoUIPipelines.GUI_HSV directly instead of creating RenderType
    public static RenderPipeline getHsvGuiPipeline() {
        return OwoUIPipelines.GUI_HSV;
    }

    // Using SPECTRUM pipeline directly
    public static RenderPipeline getSpectrumPipeline() {
        return SPECTRUM;
    }

    // Using COLORED_GUI_TEXTURED_PIPE directly
    public static RenderPipeline getColoredGuiTexturedPipeline() {
        return COLORED_GUI_TEXTURED_PIPE;
    }

    public static void registerPipelines(Consumer<RenderPipeline> pipelineRegister) {
        pipelineRegister.accept(SPECTRUM);
        pipelineRegister.accept(COLORED_GUI_TEXTURED_PIPE);
    }

    private static TextureTarget BUFFER = null;

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
