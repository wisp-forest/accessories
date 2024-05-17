package top.theillusivec4.curios.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.network.NetworkHandler;
import top.theillusivec4.curios.common.network.client.CPacketToggleCosmetics;

import javax.annotation.Nonnull;

public class CosmeticButton extends ImageButton {

    private static final ResourceLocation CURIO_INVENTORY =
            new ResourceLocation(CuriosApi.MODID, "textures/gui/inventory_revamp.png");
    private final CuriosScreenV2 parentGui;

    CosmeticButton(CuriosScreenV2 parentGui, int xIn, int yIn, int widthIn, int heightIn) {
        super(xIn, yIn, widthIn, heightIn, 0, 0, CURIO_INVENTORY,
                (button) -> {
//                    parentGui.getMenu().toggleCosmetics();
//                    NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(), new CPacketToggleCosmetics(parentGui.getMenu().containerId));
                });
        this.parentGui = parentGui;
        this.setTooltip(Tooltip.create(Component.translatable("gui.curios.toggle.cosmetics")));
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics guiGraphics, int x, int y, float partialTicks) {
        int xTex;
        int yTex = 0;

        if (this.parentGui.getMenu().isViewingCosmetics) {
            xTex = 143;
        } else {
            xTex = 123;
        }

        if (this.isHoveredOrFocused()) {
            yTex = 17;
        }
        this.setX(this.parentGui.getGuiLeft() - 27);
        this.setY(this.parentGui.getGuiTop() - 18);
        guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), xTex, yTex, this.width,
                this.height);
    }
}
