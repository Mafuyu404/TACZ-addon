package com.mafuyu404.taczaddon.init;

import net.minecraftforge.eventbus.api.Event;

public class SyncEvent extends Event {
    private final String key;
    private final Object value;

    public SyncEvent(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}

