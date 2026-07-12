package com.mafuyu404.taczaddon.init;

import java.util.HashMap;
import java.util.Map;

public final class DataStorage {
    private static final Map<String, Object> STORAGE =
            new HashMap<>();

    private DataStorage() {
    }

    public static void set(String key, Object value) {
        if (key == null) {
            return;
        }

        if (value == null) {
            STORAGE.remove(key);
            return;
        }

        STORAGE.put(key, value);
    }

    public static Object get(String key) {
        return key == null ? null : STORAGE.get(key);
    }

    public static void clear() {
        STORAGE.clear();
    }
}