package com.mafuyu404.taczaddon.client;

public interface GunSmithPropertyFilterAccess {
    default boolean taczaddon$handlePropertyClick(double x, double y, int button) { return false; }
    int taczaddon$getAttachmentPropertyIndex();
    void taczaddon$setAttachmentPropertyIndex(int index);
    int taczaddon$getAttachmentPropertyCount();
}
