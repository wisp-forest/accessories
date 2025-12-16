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
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class AccessoriesPipelines {

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

    public static final RenderPipeline COLORED_GUI_TEXTURED_PIPE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Accessories.of("pipeline/colored_gui_textured"))
            .withColorWrite(true)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE))
            .build();

    public static RenderPipeline getHsvGuiPipeline() {
        return OwoUIPipelines.GUI_HSV;
    }

    public static RenderPipeline getSpectrumPipeline() {
        return SPECTRUM;
    }

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
