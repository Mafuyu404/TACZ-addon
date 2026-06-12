package com.mafuyu404.taczaddon.init;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ClientSessionState {
    private static BlockPos lastGunSmithInteractPos;
    private static ItemStack gunSmithJeiStack = ItemStack.EMPTY;
    private static List<String> gunSwitchList = List.of();
    private static Map<String, String> attachmentData = Map.of();
    private static boolean liberateAttachment;
    private static boolean showAttachmentDetail;

    private ClientSessionState() {
    }

    public static void clear() {
        lastGunSmithInteractPos = null;
        gunSmithJeiStack = ItemStack.EMPTY;
        gunSwitchList = List.of();
        attachmentData = Map.of();
        liberateAttachment = false;
        showAttachmentDetail = false;
    }

    public static Optional<BlockPos> getLastGunSmithInteractPos() {
        return Optional.ofNullable(lastGunSmithInteractPos);
    }

    public static void setLastGunSmithInteractPos(BlockPos blockPos) {
        lastGunSmithInteractPos = blockPos;
    }

    public static ItemStack getGunSmithJeiStack() {
        return gunSmithJeiStack.copy();
    }

    public static void setGunSmithJeiStack(ItemStack itemStack) {
        gunSmithJeiStack = itemStack.copy();
    }

    public static List<String> getGunSwitchList() {
        return gunSwitchList;
    }

    public static void setGunSwitchList(List<String> gunIds) {
        gunSwitchList = List.copyOf(gunIds);
    }

    public static Map<String, String> getAttachmentData() {
        return attachmentData;
    }

    public static void setAttachmentData(Map<String, String> data) {
        attachmentData = Map.copyOf(data);
    }

    public static boolean isLiberateAttachment() {
        return liberateAttachment;
    }

    public static boolean isShowAttachmentDetail() {
        return showAttachmentDetail;
    }

    public static void setRuleState(boolean liberateAttachment, boolean showAttachmentDetail) {
        ClientSessionState.liberateAttachment = liberateAttachment;
        ClientSessionState.showAttachmentDetail = showAttachmentDetail;
    }
}
