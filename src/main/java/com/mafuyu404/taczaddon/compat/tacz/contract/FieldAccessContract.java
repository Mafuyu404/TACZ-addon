package com.mafuyu404.taczaddon.compat.tacz.contract;

import org.objectweb.asm.Opcodes;

public record FieldAccessContract(
        String owner,
        String name,
        String descriptor,
        int opcode,
        int minimum,
        int maximum
) {
    public static FieldAccessContract exactlyOneGetField(
            String owner,
            String name,
            String descriptor
    ) {
        return new FieldAccessContract(
                owner,
                name,
                descriptor,
                Opcodes.GETFIELD,
                1,
                1
        );
    }

    public static FieldAccessContract exactlyOnePutField(
            String owner,
            String name,
            String descriptor
    ) {
        return new FieldAccessContract(
                owner,
                name,
                descriptor,
                Opcodes.PUTFIELD,
                1,
                1
        );
    }

    public static FieldAccessContract atLeastOne(
            String owner,
            String name,
            String descriptor,
            int opcode
    ) {
        return new FieldAccessContract(
                owner,
                name,
                descriptor,
                opcode,
                1,
                Integer.MAX_VALUE
        );
    }
}
