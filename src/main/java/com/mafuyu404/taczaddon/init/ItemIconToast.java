package com.mafuyu404.taczaddon.init;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ItemIconToast implements Toast {
    private final Component title;
    private final Component description;
    private final ItemStack icon;  // 瑕佹樉绀虹殑鐗╁搧鍥炬爣
    private long visibleTime;

    // 鏋勯€犲嚱鏁?
    public ItemIconToast(Component title, Component description, ItemStack icon) {
        this.title = title;
        this.description = description;
        this.icon = icon;
    }

    @Override
    public @NotNull Toast.Visibility render(@NotNull GuiGraphics gui, @NotNull ToastComponent toastComponent, long timer) {
        gui.fill(0, 0, this.width(), this.height(), 0xF0101010);

        gui.renderFakeItem(icon, 8, 8);

        gui.drawString(toastComponent.getMinecraft().font, title.getString(), 30, 7, 0xFFD700);
        gui.drawString(toastComponent.getMinecraft().font, description.getString(), 30, 18, 0xFFFFFF);

        return timer >= 2000L ? Visibility.HIDE : Visibility.SHOW;
    }

    // 蹇€熸樉绀烘柟娉?
    public static void show(Component title, Component description, ItemStack icon) {
        Minecraft.getInstance().getToasts().addToast(new ItemIconToast(title, description, icon));
    }

    // 绀轰緥鐢ㄦ硶锛氭樉绀轰竴涓捇鐭冲浘鏍囩殑 Toast
    public static void create(String title, String desc, ItemStack itemStack) {
        show(
                Component.translatable(title),
                Component.translatable(desc),
                itemStack // 鏇挎崲涓轰换鎰忕墿鍝?
        );
    }
}
