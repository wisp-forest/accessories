package io.wispforest.accessories.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.util.TriState;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.wispforest.accessories.client.AccessoriesRenderLayer.shaderColor;

public class AccessoriesPipelines {

    public static final RenderType.CompositeRenderType HSV_GUI = RenderType.create(
            "accessories:hsv_gui",
            786432,
            OwoUIPipelines.GUI_HSV,
            RenderType.CompositeState.builder().createCompositeState(false)
    );
//            RenderType.create(
//            "accessories:hsv_gui",
//            DefaultVertexFormat.POSITION_COLOR,
//            VertexFormat.Mode.QUADS,
//            786432,
//            RenderType.CompositeState.builder()
//                    .setShaderState(OwoClient.HSV_PROGRAM.renderPhaseProgram())
//                    .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
//                    .setDepthTestState(RenderType.LEQUAL_DEPTH_TEST)
//                    .createCompositeState(false)
//    );

    public static final RenderPipeline.Snippet SPECTRUM_SNIPPET = RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_SNIPPET)
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
                            .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.FALSE, false))
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
                                .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.FALSE, false))
                                .setTexturingState(new RenderStateShard.TexturingStateShard("setting_shader_color", () -> {
                                    RenderSystem.setShaderColor(color.red(), color.green(), color.blue(), 1f);
                                }, () -> {
                                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                                }))
                                .createCompositeState(false));
            }
    );

    public static void registerPipelines(Consumer<RenderPipeline> pipelineRegister) {
        pipelineRegister.accept(SPECTRUM);
        pipelineRegister.accept(COLORED_GUI_TEXTURED_PIPE);
    }

    public static TextureTarget BUFFER;
    public static boolean OVERRIDE_RENDER_TARGET = false;
    public static Color SHADER_COLOR = null;
    public static final RenderType RENDER_TYPE = RenderType.create(
        "dawg",
        786432,
        RenderPipelines.GUI_TEXTURED_OVERLAY,
        RenderType.CompositeState.builder().setTextureState(new RenderStateShard.EmptyTextureStateShard(
            () -> {
                RenderSystem.setShaderTexture(0, BUFFER.getColorTexture());
                if (SHADER_COLOR != null) RenderSystem.setShaderColor(SHADER_COLOR.red(), SHADER_COLOR.green(), SHADER_COLOR.blue(), SHADER_COLOR.alpha());
            },
            () -> {
                if (SHADER_COLOR != null) {
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    SHADER_COLOR = null;
                }
            }
        )).createCompositeState(false)
    );

    @ApiStatus.Internal
    public static void initialize(Minecraft client) {
        var window = client.getWindow();
        BUFFER = new TextureTarget("accessories_buffer_thingy", window.getWidth(), window.getHeight(), true);
        WindowResizeCallback.EVENT.register((innerClient, innerWindow) -> {
            if (BUFFER == null) return;
            BUFFER.resize(innerWindow.getWidth(), innerWindow.getHeight());
        });
    }
}
