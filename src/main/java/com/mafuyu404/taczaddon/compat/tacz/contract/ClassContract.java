package com.mafuyu404.taczaddon.compat.tacz.contract;

import java.util.List;

public record ClassContract(
        String className,
        List<MethodContract> methods,
        List<FieldContract> fields
) {
    public ClassContract(String className) {
        this(className, List.of(), List.of());
    }

    public ClassContract withMethods(MethodContract... methods) {
        return new ClassContract(
                className,
                List.of(methods),
                fields
        );
    }

    public ClassContract withFields(FieldContract... fields) {
        return new ClassContract(
                className,
                methods,
                List.of(fields)
        );
    }
}
