package com.mafuyu404.taczaddon.mixin;

import com.mafuyu404.taczaddon.common.RefitCompatibility;
import com.mafuyu404.taczaddon.compat.sophisticated.SophisticatedPayloadContractState;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * Binary-contract gate for mixins that target optional dependency classes.
 *
 * <p>Instead of letting a {@code require = 1} mixin turn a contract mismatch
 * into a startup crash, this plugin inspects the actual dependency class
 * bytes (ASM, no target class loading) and only applies the affected mixins
 * when the contract holds.
 *
 * <p>If a contract is missing:
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
 *     <li>BackpackContentsPayload (Sophisticated Backpacks): static
 *     {@code handlePayload(BackpackContentsPayload, IPayloadContext)V},
 *     record accessors {@code backpackUuid()Ljava/util/UUID;} and
 *     {@code backpackContents()Lnet/minecraft/nbt/CompoundTag;}</li>
 * </ul>
 *
 * <p>Dependency graph (fail closed):
 * <pre>
 * ClientMessageUnloadAttachmentMixin  requires UNLOAD_PACKET
 * ClientMessageUnloadAttachmentAccessor requires UNLOAD_PACKET
 * GunRefitScreenMixin                  requires REFIT_SCREEN AND UNLOAD_PACKET
 * BackpackContentsPayloadMixin         requires BACKPACK_CONTENTS_PAYLOAD
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
    private static final String BACKPACK_PAYLOAD_MIXIN =
            PACKAGE + "BackpackContentsPayloadMixin";

    private static final String UNLOAD_PACKET =
            "com/tacz/guns/network/message/"
                    + "ClientMessageUnloadAttachment";
    private static final String REFIT_SCREEN =
            "com/tacz/guns/client/gui/GunRefitScreen";
    private static final String BACKPACK_CONTENTS_PAYLOAD =
            "net/p3pp3rf1y/sophisticatedbackpacks/network/"
                    + "BackpackContentsPayload";

    private Boolean unloadContractValid;
    private Boolean refitContractValid;
    private Boolean backpackPayloadContractValid;

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
        if (!mixinClassName.equals(BACKPACK_PAYLOAD_MIXIN)) {
            return;
        }

        boolean installed =
                hasAppliedBackpackPayloadHook(targetClass);

        SophisticatedPayloadContractState.reportApplied(
                installed
        );

        if (!installed) {
            LOGGER.error(
                    "[taczaddon] Sophisticated Backpacks payload preflight "
                            + "passed, but the transformed BackpackContentsPayload "
                            + "does not contain the TACZAddon response hook. "
                            + "Optional immediate invalidation is unavailable; native sync is unaffected."
            );
        }
    }

    @Override
    public boolean shouldApplyMixin(
            String targetClassName,
            String mixinClassName
    ) {
        if (mixinClassName.equals(PACKAGE + "LeawindAimModeResolverMixin")) {
            // Soft target: inspect bytes only, never resolve an optional mod's classes.
            return readClassBytes(targetClassName.replace('.', '/')) != null;
        }
        if (mixinClassName.equals(BACKPACK_PAYLOAD_MIXIN)) {
            boolean valid =
                    isBackpackPayloadContractValid();

            /*
             * A positive preflight only means the target is eligible for the mixin.
             * The optional callback is usable only after postApply confirms that the
             * transformed handler invokes it. Native CLIENT_SYNC is independent.
             */
            SophisticatedPayloadContractState.reportPreflight(
                    valid
            );

            return valid;
        }

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

    /**
     * Verifies the post-transform result rather than merely trusting that
     * shouldApplyMixin accepted the raw target contract.
     *
     * <p>Mixin injection handlers are merged into the target class and may be
     * renamed (for example with a generated handler prefix), so the check searches
     * the transformed handlePayload bytecode for an INVOKESTATIC whose method name
     * contains our unique handler name.
     *
     * <p>This specifically distinguishes:
     *
     * <pre>
     * preflight passed + injection succeeded
     *     -> true
     *
     * preflight passed + require=0 injected zero callbacks
     *     -> false
     * </pre>
     */
    private static boolean hasAppliedBackpackPayloadHook(
            ClassNode targetClass
    ) {
        if (targetClass == null
                || targetClass.methods == null) {
            return false;
        }

        for (MethodNode method : targetClass.methods) {
            if (!"handlePayload".equals(method.name)
                    || !ContractVisitor.HANDLE_PAYLOAD_DESCRIPTOR.equals(
                    method.desc
            )) {
                continue;
            }

            if (method.instructions == null) {
                return false;
            }

            for (
                    var instruction =
                    method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()
            ) {
                if (!(instruction instanceof MethodInsnNode invocation)) {
                    continue;
                }

                if (invocation.getOpcode() != Opcodes.INVOKESTATIC) {
                    continue;
                }

                /*
                 * Injection handlers are merged into the target itself.
                 */
                if (!targetClass.name.equals(invocation.owner)) {
                    continue;
                }

                /*
                 * Mixin may rename the handler to something like
                 * handler$...$taczaddon$afterBackpackContentsReceived.
                 */
                if (invocation.name.contains(
                        "taczaddon$afterBackpackContentsReceived"
                )) {
                    return true;
                }
            }

            /*
             * The expected handlePayload overload exists but contains no call to
             * our injected response handler.
             */
            return false;
        }

        return false;
    }

    private boolean isBackpackPayloadContractValid() {
        if (backpackPayloadContractValid == null) {
            backpackPayloadContractValid =
                    verifyContract(
                            BACKPACK_CONTENTS_PAYLOAD,
                            "BackpackContentsPayload",
                            ContractKind.BACKPACK_CONTENTS_PAYLOAD,
                            "client Sophisticated backpack cache refresh"
                    );

            if (!backpackPayloadContractValid) {
                LOGGER.error(
                        "[taczaddon] Sophisticated Backpacks payload binary "
                                + "contract mismatch. The BackpackContents"
                                + "Payload mixin is skipped and the "
                                + "optional immediate invalidation is unavailable; native sync is unaffected; "
                                + "startup continues."
                );
            }
        }

        return backpackPayloadContractValid;
    }

    private boolean isUnloadContractValid() {
        if (unloadContractValid == null) {
            unloadContractValid =
                    verifyContract(
                            UNLOAD_PACKET,
                            "ClientMessageUnloadAttachment",
                            ContractKind.UNLOAD_PACKET,
                            "server-side virtual attachment unload "
                                    + "protection and client "
                                    + "liberateAttachment refit integration"
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
                            ContractKind.REFIT_SCREEN,
                            "client liberateAttachment refit UI"
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
            ContractKind kind,
            String disabledFeature
    ) {
        byte[] classBytes = readClassBytes(classInternalName);

        if (classBytes == null) {
            LOGGER.error(
                    "[taczaddon] Compatibility gate: could not read {} "
                            + "from the dependency classpath. Expected binary "
                            + "contract: {}. Disabled: {}.",
                    className,
                    kind.expectedDescription,
                    disabledFeature
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
                    "[taczaddon] Compatibility gate: failed to inspect {}. "
                            + "Expected binary contract: {}. Disabled: {}.",
                    className,
                    kind.expectedDescription,
                    disabledFeature,
                    exception
            );
            return false;
        }

        if (!valid) {
            LOGGER.error(
                    "[taczaddon] Compatibility gate: {} does not match the "
                            + "verified contract (expected {}). Disabled: "
                            + "{}; startup continues.",
                    className,
                    kind.expectedDescription,
                    disabledFeature
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
        ),
        BACKPACK_CONTENTS_PAYLOAD(
                "static handlePayload(BackpackContentsPayload, "
                        + "IPayloadContext)V, accessor "
                        + "backpackUuid()Ljava/util/UUID;, accessor "
                        + "backpackContents()Lnet/minecraft/nbt/CompoundTag;"
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

        private static final String HANDLE_PAYLOAD_DESCRIPTOR =
                "(Lnet/p3pp3rf1y/sophisticatedbackpacks/network/"
                        + "BackpackContentsPayload;"
                        + "Lnet/neoforged/neoforge/network/handling/"
                        + "IPayloadContext;)V";

        private static final String UUID_DESCRIPTOR =
                "Ljava/util/UUID;";

        private static final String COMPOUND_TAG_DESCRIPTOR =
                "Lnet/minecraft/nbt/CompoundTag;";

        private final ContractKind kind;

        private boolean staticHandle;
        private boolean gunSlotIndexField;
        private boolean attachmentTypeField;
        private boolean addAttachmentTypeButtons;
        private boolean addInventoryAttachmentButtons;
        private boolean currentPageField;
        private boolean staticHandlePayload;
        private boolean backpackUuidAccessor;
        private boolean backpackContentsAccessor;

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
            boolean staticMethod =
                    (access & Opcodes.ACC_STATIC) != 0;

            if ("handle".equals(name)
                    && HANDLE_DESCRIPTOR.equals(descriptor)
                    && staticMethod) {
                staticHandle = true;
            }

            if ("addAttachmentTypeButtons".equals(name)
                    && "()V".equals(descriptor)
                    && !staticMethod) {
                addAttachmentTypeButtons = true;
            }

            if ("addInventoryAttachmentButtons".equals(name)
                    && "()V".equals(descriptor)
                    && !staticMethod) {
                addInventoryAttachmentButtons = true;
            }

            if ("handlePayload".equals(name)
                    && HANDLE_PAYLOAD_DESCRIPTOR.equals(descriptor)
                    && staticMethod) {
                staticHandlePayload = true;
            }

            if ("backpackUuid".equals(name)
                    && UUID_DESCRIPTOR.equals(descriptor)
                    && !staticMethod) {
                backpackUuidAccessor = true;
            }

            if ("backpackContents".equals(name)
                    && COMPOUND_TAG_DESCRIPTOR.equals(descriptor)
                    && !staticMethod) {
                backpackContentsAccessor = true;
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
                case BACKPACK_CONTENTS_PAYLOAD ->
                        staticHandlePayload
                                && backpackUuidAccessor
                                && backpackContentsAccessor;
            };
        }
    }
}
