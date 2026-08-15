package com.mafuyu404.taczaddon.compat.tacz.contract;

import java.util.List;

public record MethodContract(
        String name,
        String descriptor,
        List<String> nameAliases,
        List<InvokeContract> invocations,
        List<FieldAccessContract> fieldAccesses
) {
    public MethodContract(String name, String descriptor) {
        this(name, descriptor, List.of(), List.of(), List.of());
    }

    public MethodContract(
            String name,
            String descriptor,
            List<InvokeContract> invocations
    ) {
        this(name, descriptor, List.of(), invocations, List.of());
    }

    public MethodContract withInvocations(
            InvokeContract... extraInvocations
    ) {
        return new MethodContract(
                name,
                descriptor,
                nameAliases,
                List.of(extraInvocations),
                fieldAccesses
        );
    }

    public MethodContract withAliases(String... aliases) {
        return new MethodContract(
                name,
                descriptor,
                List.of(aliases),
                invocations,
                fieldAccesses
        );
    }

    public MethodContract withFieldAccesses(
            FieldAccessContract... accesses
    ) {
        return new MethodContract(
                name,
                descriptor,
                nameAliases,
                invocations,
                List.of(accesses)
        );
    }
}
