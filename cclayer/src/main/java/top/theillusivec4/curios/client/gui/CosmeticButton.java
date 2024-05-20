package top.theillusivec4.curios.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nonnull;

public class CosmeticButton extends ImageButton {

    public static final WidgetSprites OFF =
            new WidgetSprites(new ResourceLocation(CuriosApi.MODID, "cosmetic_off"),
                    new ResourceLocation(CuriosApi.MODID, "cosmetic_off_highlighted"));

    public static final WidgetSprites ON =
            new WidgetSprites(new ResourceLocation(CuriosApi.MODID, "cosmetic_on"),
                    new ResourceLocation(CuriosApi.MODID, "cosmetic_on_highlighted"));

    private static final ResourceLocation CURIO_INVENTORY = new ResourceLocation(CuriosApi.MODID, "textures/gui/inventory_revamp.png");

    private final CuriosScreenV2 parentGui;

    CosmeticButton(CuriosScreenV2 parentGui, int xIn, int yIn, int widthIn, int heightIn) {
        super(xIn, yIn, widthIn, heightIn, OFF,
                (button) -> {
//                    parentGui.getMenu().toggleCosmetics();
//                    NetworkHandler.INSTANCE.send(PacketDistributor.SERVER.noArg(), new CPacketToggleCosmetics(parentGui.getMenu().containerId));
                });
        this.parentGui = parentGui;
        this.setTooltip(Tooltip.create(Component.translatable("gui.curios.toggle.cosmetics")));
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics guiGraphics, int x, int y, float partialTicks) {
        WidgetSprites sprites1;

        if (this.parentGui.getMenu().isViewingCosmetics) {
            sprites1 = ON;
        } else {
            sprites1 = OFF;
        }

        this.setX(this.parentGui.getGuiLeft() - 27);
        this.setY(this.parentGui.getGuiTop() - 18);
        ResourceLocation resourceLocation = sprites1.get(this.isActive(), this.isHoveredOrFocused());
        guiGraphics.blitSprite(resourceLocation, this.getX(), this.getY(), this.width, this.height);
    }
}
