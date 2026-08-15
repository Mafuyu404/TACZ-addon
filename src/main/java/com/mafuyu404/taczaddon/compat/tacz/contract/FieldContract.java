package com.mafuyu404.taczaddon.compat.tacz.contract;

public record FieldContract(String name, String descriptor) {
    public static FieldContract of(String name, String descriptor) {
        return new FieldContract(name, descriptor);
    }
}
