package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.RefitCompatibility;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * TaCZ binary-contract gate.
 *
 * <p>The liberateAttachment refit integration is version-bound to a verified
 * TaCZ 1.21.1 contract. Instead of letting a {@code require = 1} mixin turn a
 * contract mismatch into a startup crash, this plugin inspects the actual
 * dependency class bytes (ASM, no target class loading) and only applies the
 * affected mixins when the contract holds.
 *
 * <p>If the contract is missing:
 * <ul>
 *     <li>an ERROR is logged with the expected contract;</li>
 *     <li>the affected mixin is skipped (feature unavailable);</li>
 *     <li>the game keeps starting without a half-broken integration.</li>
 * </ul>
 *
 * <p>Verified contracts:
 * <ul>
 *     <li>ClientMessageUnloadAttachment: static
 *     {@code handle(ClientMessageUnloadAttachment, IPayloadContext)V},
 *     non-static {@code gunSlotIndex:I},
 *     non-static
 *     {@code attachmentType:Lcom/tacz/guns/api/item/attachment/AttachmentType;}</li>
 *     <li>GunRefitScreen: {@code addAttachmentTypeButtons()V},
 *     {@code addInventoryAttachmentButtons()V} (both non-static),
 *     non-static non-final {@code currentPage:I}</li>
 * </ul>
 *
 * <p>Dependency graph (fail closed):
 * <pre>
 * ClientMessageUnloadAttachmentMixin  requires UNLOAD_PACKET
 * ClientMessageUnloadAttachmentAccessor requires UNLOAD_PACKET
 * GunRefitScreenMixin                  requires REFIT_SCREEN AND UNLOAD_PACKET
 * </pre>
 *
 * <p>The server-side provenance-aware unload takeover may stand alone (it
 * protects pre-existing virtual markers even when the client GUI contract
 * changed), but the client virtual refit UI must never be enabled without the
 * server unload protection.
 */
public final class TaczAddonMixinPlugin
        implements IMixinConfigPlugin {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TaczAddonMixinPlugin.class);

    private static final String PACKAGE =
            "com.mafuyu404.taczaddon.mixin.";

    private static final String UNLOAD_MIXIN =
            PACKAGE + "ClientMessageUnloadAttachmentMixin";
    private static final String UNLOAD_ACCESSOR =
            PACKAGE + "ClientMessageUnloadAttachmentAccessor";
    private static final String REFIT_MIXIN =
            PACKAGE + "GunRefitScreenMixin";

    private static final String UNLOAD_PACKET =
            "com/tacz/guns/network/message/"
                    + "ClientMessageUnloadAttachment";
    private static final String REFIT_SCREEN =
            "com/tacz/guns/client/gui/GunRefitScreen";

    private Boolean unloadContractValid;
    private Boolean refitContractValid;

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(
            Set<String> myTargets,
            Set<String> otherTargets
    ) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
    }

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
    }

    @Override
    public boolean shouldApplyMixin(
            String targetClassName,
            String mixinClassName
    ) {
        if (mixinClassName.equals(UNLOAD_MIXIN)
                || mixinClassName.equals(UNLOAD_ACCESSOR)) {
            return RefitCompatibility.shouldEnableUnload(
                    isUnloadContractValid()
            );
        }

        if (mixinClassName.equals(REFIT_MIXIN)) {
            return RefitCompatibility.shouldEnableRefit(
                    isUnloadContractValid(),
                    isRefitContractValid()
            );
        }

        return true;
    }

    private boolean isUnloadContractValid() {
        if (unloadContractValid == null) {
            unloadContractValid =
                    verifyContract(
                            UNLOAD_PACKET,
                            "ClientMessageUnloadAttachment",
                            ContractKind.UNLOAD_PACKET
                    );

            if (!unloadContractValid) {
                LOGGER.error(
                        "[taczaddon] TaCZ unload binary contract mismatch. "
                                + "Server-side virtual attachment unload "
                                + "protection disabled. Client "
                                + "liberateAttachment refit integration will "
                                + "also be disabled."
                );
            }
        }

        return unloadContractValid;
    }

    private boolean isRefitContractValid() {
        if (refitContractValid == null) {
            if (isServerSide()) {
                /*
                 * GunRefitScreen is a client-side target and its mixin lives
                 * in the "client" section. On a dedicated server the class is
                 * not applicable (and may not even be present in stripped
                 * distributions); this is the normal state, not an
                 * incompatibility.
                 */
                refitContractValid = false;
                return false;
            }

            refitContractValid =
                    verifyContract(
                            REFIT_SCREEN,
                            "GunRefitScreen",
                            ContractKind.REFIT_SCREEN
                    );

            if (!refitContractValid) {
                LOGGER.error(
                        "[taczaddon] TaCZ GunRefitScreen binary contract "
                                + "mismatch. Client liberateAttachment refit "
                                + "UI disabled. Server-side "
                                + "provenance-aware unload protection remains "
                                + "enabled if its own contract is valid."
                );
            }
        }

        return refitContractValid;
    }

    private static boolean isServerSide() {
        return MixinEnvironment.getCurrentEnvironment().getSide()
                == MixinEnvironment.Side.SERVER;
    }

    private static boolean verifyContract(
            String classInternalName,
            String className,
            ContractKind kind
    ) {
        byte[] classBytes = readClassBytes(classInternalName);

        if (classBytes == null) {
            LOGGER.error(
                    "[taczaddon] TaCZ compatibility gate: could not read {} "
                            + "from the dependency classpath. Expected binary "
                            + "contract: {}. liberateAttachment refit "
                            + "integration disabled.",
                    className,
                    kind.expectedDescription
            );
            return false;
        }

        boolean valid;

        try {
            ContractVisitor visitor = new ContractVisitor(kind);
            new ClassReader(classBytes).accept(
                    visitor,
                    ClassReader.SKIP_CODE
                            | ClassReader.SKIP_DEBUG
                            | ClassReader.SKIP_FRAMES
            );
            valid = visitor.matches();
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "[taczaddon] TaCZ compatibility gate: failed to inspect "
                            + "{}. Expected binary contract: {}. "
                            + "liberateAttachment refit integration disabled.",
                    className,
                    kind.expectedDescription,
                    exception
            );
            return false;
        }

        if (!valid) {
            LOGGER.error(
                    "[taczaddon] TaCZ compatibility gate: {} does not match "
                            + "the verified contract (expected {}). "
                            + "liberateAttachment refit integration disabled; "
                            + "startup continues.",
                    className,
                    kind.expectedDescription
            );
        }

        return valid;
    }

    private static byte[] readClassBytes(String classInternalName) {
        String resourcePath = classInternalName + ".class";
        ClassLoader loader =
                TaczAddonMixinPlugin.class.getClassLoader();

        try (InputStream stream =
                     loader == null
                             ? ClassLoader
                             .getSystemResourceAsStream(resourcePath)
                             : loader.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }
            return stream.readAllBytes();
        } catch (IOException exception) {
            return null;
        }
    }

    private enum ContractKind {
        UNLOAD_PACKET(
                "static handle(ClientMessageUnloadAttachment, "
                        + "IPayloadContext)V, non-static gunSlotIndex:I, "
                        + "non-static attachmentType:AttachmentType"
        ),
        REFIT_SCREEN(
                "non-static addAttachmentTypeButtons()V, "
                        + "non-static addInventoryAttachmentButtons()V, "
                        + "non-static non-final currentPage:I"
        );

        private final String expectedDescription;

        ContractKind(String expectedDescription) {
            this.expectedDescription = expectedDescription;
        }
    }

    private static final class ContractVisitor
            extends ClassVisitor {

        private static final String HANDLE_DESCRIPTOR =
                "(Lcom/tacz/guns/network/message/"
                        + "ClientMessageUnloadAttachment;"
                        + "Lnet/neoforged/neoforge/network/handling/"
                        + "IPayloadContext;)V";

        private static final String ATTACHMENT_TYPE_DESCRIPTOR =
                "Lcom/tacz/guns/api/item/attachment/"
                        + "AttachmentType;";

        private final ContractKind kind;

        private boolean staticHandle;
        private boolean gunSlotIndexField;
        private boolean attachmentTypeField;
        private boolean addAttachmentTypeButtons;
        private boolean addInventoryAttachmentButtons;
        private boolean currentPageField;

        ContractVisitor(ContractKind kind) {
            super(Opcodes.ASM9);
            this.kind = kind;
        }

        @Override
        public FieldVisitor visitField(
                int access,
                String name,
                String descriptor,
                String signature,
                Object value
        ) {
            boolean nonStatic =
                    (access & Opcodes.ACC_STATIC) == 0;
            boolean nonFinal =
                    (access & Opcodes.ACC_FINAL) == 0;

            if ("gunSlotIndex".equals(name)
                    && "I".equals(descriptor)
                    && nonStatic) {
                gunSlotIndexField = true;
            }

            if ("attachmentType".equals(name)
                    && ATTACHMENT_TYPE_DESCRIPTOR.equals(
                    descriptor
            ) && nonStatic) {
                attachmentTypeField = true;
            }

            if ("currentPage".equals(name)
                    && "I".equals(descriptor)
                    && nonStatic
                    && nonFinal) {
                currentPageField = true;
            }

            return null;
        }

        @Override
        public MethodVisitor visitMethod(
                int access,
                String name,
                String descriptor,
                String signature,
                String[] exceptions
        ) {
            if ("handle".equals(name)
                    && HANDLE_DESCRIPTOR.equals(descriptor)
                    && (access & Opcodes.ACC_STATIC) != 0) {
                staticHandle = true;
            }

            if ("addAttachmentTypeButtons".equals(name)
                    && "()V".equals(descriptor)
                    && (access & Opcodes.ACC_STATIC) == 0) {
                addAttachmentTypeButtons = true;
            }

            if ("addInventoryAttachmentButtons".equals(name)
                    && "()V".equals(descriptor)
                    && (access & Opcodes.ACC_STATIC) == 0) {
                addInventoryAttachmentButtons = true;
            }

            return null;
        }

        boolean matches() {
            return switch (kind) {
                case UNLOAD_PACKET ->
                        staticHandle
                                && gunSlotIndexField
                                && attachmentTypeField;
                case REFIT_SCREEN ->
                        addAttachmentTypeButtons
                                && addInventoryAttachmentButtons
                                && currentPageField;
            };
        }
    }
}
