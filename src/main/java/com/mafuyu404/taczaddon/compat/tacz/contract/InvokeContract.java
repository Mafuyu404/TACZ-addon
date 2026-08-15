package com.mafuyu404.taczaddon.compat.tacz.contract;

import java.util.List;

public record InvokeContract(
        String owner,
        String name,
        String descriptor,
        int minimum,
        int maximum,
        List<String> nameAliases
) {
    public InvokeContract(
            String owner,
            String name,
            String descriptor,
            int minimum,
            int maximum
    ) {
        this(owner, name, descriptor, minimum, maximum, List.of());
    }

    public static InvokeContract exactlyOne(
            String owner,
            String name,
            String descriptor
    ) {
        return new InvokeContract(owner, name, descriptor, 1, 1);
    }

    public static InvokeContract atLeastOne(
            String owner,
            String name,
            String descriptor
    ) {
        return new InvokeContract(owner, name, descriptor, 1, Integer.MAX_VALUE);
    }

    public boolean matchesName(String actualName) {
        if (name == null) {
            return true;
        }
        if (name.equals(actualName)) {
            return true;
        }
        return nameAliases.contains(actualName);
    }
}
