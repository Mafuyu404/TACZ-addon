package com.mafuyu404.taczaddon.testutil;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Byte-level fixtures for optional mod API generations.
 *
 * <p>Compatibility tests synthesise the upstream generations instead of
 * relying on the development classpath, so a test can replay exactly one
 * generation. The member lists mirror the real upstream artifacts verified by
 * the fixture tests.
 */
public final class SyntheticOptionalApis {
    public static final String SSR =
            "com.github.exopandora.shouldersurfing.";
    public static final String SSR_NEW_PERSPECTIVE =
            SSR + "api.client.Perspective";
    public static final String SSR_NEW_SERVICE =
            SSR + "api.client.IShoulderSurfing";
    public static final String SSR_LEGACY_PERSPECTIVE =
            SSR + "api.model.Perspective";
    public static final String SSR_LEGACY_INPUT_HANDLER =
            SSR + "client.InputHandler";
    public static final String SSR_LEGACY_PLUGIN =
            SSR + "api.plugin.IShoulderSurfingPlugin";
    public static final String SSR_LEGACY_REGISTRAR =
            SSR + "api.plugin.IShoulderSurfingRegistrar";

    public static final String PERSPECTIVE_API =
            "io.github.leawind.perspectiveapi.api.";

    /** Main class package used only by the Leawind 2.x line. */
    public static final String LEAWIND_2_MAIN =
            "com.github.leawind.thirdperson.ThirdPerson";
    /** Main class package used only by the Leawind 3.x line. */
    public static final String LEAWIND_3_MAIN =
            "io.github.leawind.thirdperson.ThirdPerson";

    /** Leawind 2.x, no Perspective API at all. */
    public static Map<String, byte[]> leawindTwoOnly() {
        return Map.of(
                LEAWIND_2_MAIN,
                apiClass(LEAWIND_2_MAIN, List.of(), List.of())
        );
    }

    /**
     * Leawind 2.x with Perspective API installed independently: the API mod is
     * present, but the Leawind generation is still 2.x.
     */
    public static Map<String, byte[]> leawindTwoWithPerspectiveApi() {
        Map<String, byte[]> classes = new LinkedHashMap<>(
                perspectiveApiGeneration()
        );
        classes.putAll(leawindTwoOnly());
        return classes;
    }

    /** Leawind 3.x with a verified Perspective API generation. */
    public static Map<String, byte[]> leawindThreeWithVerifiedPerspectiveApi() {
        Map<String, byte[]> classes = new LinkedHashMap<>(
                perspectiveApiGeneration()
        );
        classes.put(
                LEAWIND_3_MAIN,
                apiClass(LEAWIND_3_MAIN, List.of(), List.of())
        );
        return classes;
    }

    /**
     * Leawind 3.x with an API whose shape is not the verified one, so the
     * integration must stay unavailable instead of loading the backend.
     */
    public static Map<String, byte[]>
    leawindThreeWithIncompatiblePerspectiveApi() {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        classes.put(
                PERSPECTIVE_API + "PerspectiveAPI",
                apiClass(
                        PERSPECTIVE_API + "PerspectiveAPI",
                        List.of(method(
                                "getCurrent",
                                "()" + descriptor(
                                        PERSPECTIVE_API + "Perspective"
                                )
                        )),
                        List.of()
                )
        );
        classes.put(
                LEAWIND_3_MAIN,
                apiClass(LEAWIND_3_MAIN, List.of(), List.of())
        );
        return classes;
    }

    private SyntheticOptionalApis() {
    }

    /** New (5.x) Shoulder Surfing API generation used by the addon backend. */
    public static Map<String, byte[]> shoulderSurfingApiV5() {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        classes.put(
                SSR_NEW_PERSPECTIVE,
                apiClass(
                        SSR_NEW_PERSPECTIVE,
                        List.of(),
                        List.of(
                                field(
                                        "FIRST_PERSON",
                                        descriptor(SSR_NEW_PERSPECTIVE)
                                ),
                                field(
                                        "SHOULDER_SURFING",
                                        descriptor(SSR_NEW_PERSPECTIVE)
                                )
                        )
                )
        );
        classes.put(
                SSR_NEW_SERVICE,
                apiClass(
                        SSR_NEW_SERVICE,
                        List.of(
                                method(
                                        "getInstance",
                                        "()" + descriptor(SSR_NEW_SERVICE)
                                ),
                                method("isShoulderSurfing", "()Z"),
                                method("isFreeLooking", "()Z"),
                                method(
                                        "changePerspective",
                                        "(" + descriptor(
                                                SSR_NEW_PERSPECTIVE
                                        ) + ")V"
                                )
                        ),
                        List.of()
                )
        );
        return classes;
    }

    /**
     * Legacy (4.x) Shoulder Surfing API generation TaCZ 1.1.8-hotfix links
     * against.
     */
    public static Map<String, byte[]> shoulderSurfingLegacy() {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        classes.put(
                SSR_LEGACY_PERSPECTIVE,
                apiClass(
                        SSR_LEGACY_PERSPECTIVE,
                        List.of(method(
                                "current",
                                "()" + descriptor(SSR_LEGACY_PERSPECTIVE)
                        )),
                        List.of(field(
                                "SHOULDER_SURFING",
                                descriptor(SSR_LEGACY_PERSPECTIVE)
                        ))
                )
        );
        classes.put(
                SSR_LEGACY_INPUT_HANDLER,
                apiClass(
                        SSR_LEGACY_INPUT_HANDLER,
                        List.of(),
                        List.of(field(
                                "FREE_LOOK",
                                "Lnet/minecraft/client/KeyMapping;"
                        ))
                )
        );
        classes.put(
                SSR_LEGACY_REGISTRAR,
                apiClass(
                        SSR_LEGACY_REGISTRAR,
                        List.of(method(
                                "registerAdaptiveItemCallback",
                                "(Ljava/util/function/Predicate;)"
                                        + descriptor(SSR_LEGACY_REGISTRAR)
                        )),
                        List.of()
                )
        );
        classes.put(
                SSR_LEGACY_PLUGIN,
                apiClass(
                        SSR_LEGACY_PLUGIN,
                        List.of(method(
                                "register",
                                "(" + descriptor(SSR_LEGACY_REGISTRAR)
                                        + ")V"
                        )),
                        List.of()
                )
        );
        return classes;
    }

    /** Verified Perspective API 1.5.0-beta contract shape. */
    public static Map<String, byte[]> perspectiveApiGeneration() {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        classes.put(
                PERSPECTIVE_API + "PerspectiveAPI",
                apiClass(
                        PERSPECTIVE_API + "PerspectiveAPI",
                        List.of(
                                method(
                                        "getCurrent",
                                        "()" + descriptor(
                                                PERSPECTIVE_API + "Perspective"
                                        )
                                ),
                                method(
                                        "isCurrent",
                                        "(Ljava/lang/String;)Z"
                                ),
                                method(
                                        "getOverrideChain",
                                        "()" + descriptor(
                                                PERSPECTIVE_API
                                                        + "PerspectiveOverrideChain"
                                        )
                                )
                        ),
                        List.of()
                )
        );
        classes.put(
                PERSPECTIVE_API + "Perspective",
                apiClass(
                        PERSPECTIVE_API + "Perspective",
                        List.of(method(
                                "info",
                                "()" + descriptor(
                                        PERSPECTIVE_API + "PerspectiveInfo"
                                )
                        )),
                        List.of()
                )
        );
        classes.put(
                PERSPECTIVE_API + "PerspectiveInfo",
                apiClass(
                        PERSPECTIVE_API + "PerspectiveInfo",
                        List.of(method("id", "()Ljava/lang/String;")),
                        List.of()
                )
        );
        classes.put(
                PERSPECTIVE_API + "PerspectiveOverrideChain",
                apiClass(
                        PERSPECTIVE_API + "PerspectiveOverrideChain",
                        List.of(method(
                                "register",
                                "(ILjava/util/function/Supplier;)"
                                        + descriptor(
                                                PERSPECTIVE_API
                                                        + "PerspectiveOverrideRegistration"
                                        )
                        )),
                        List.of()
                )
        );
        classes.put(
                PERSPECTIVE_API + "PerspectiveOverrideRegistration",
                apiClass(
                        PERSPECTIVE_API
                                + "PerspectiveOverrideRegistration",
                        List.of(method("unregister", "()Z")),
                        List.of()
                )
        );
        return classes;
    }

    public static String descriptor(String binaryName) {
        return "L" + binaryName.replace('.', '/') + ";";
    }

    public static Member method(String name, String descriptor) {
        return new Member(true, name, descriptor);
    }

    public static Member field(String name, String descriptor) {
        return new Member(false, name, descriptor);
    }

    public record Member(
            boolean method,
            String name,
            String descriptor
    ) {
    }

    /** Generates a public interface with exactly the requested shape. */
    public static byte[] apiClass(
            String binaryName,
            List<Member> methods,
            List<Member> fields
    ) {
        ClassWriter writer = new ClassWriter(
                ClassWriter.COMPUTE_MAXS
        );
        writer.visit(
                Opcodes.V17,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT
                        | Opcodes.ACC_INTERFACE,
                binaryName.replace('.', '/'),
                null,
                "java/lang/Object",
                null
        );
        for (Member member : fields) {
            writer.visitField(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC
                            | Opcodes.ACC_FINAL,
                    member.name(),
                    member.descriptor(),
                    null,
                    null
            ).visitEnd();
        }
        for (Member member : methods) {
            MethodVisitor visitor = writer.visitMethod(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                    member.name(),
                    member.descriptor(),
                    null,
                    null
            );
            visitor.visitEnd();
        }
        writer.visitEnd();
        return writer.toByteArray();
    }
}
