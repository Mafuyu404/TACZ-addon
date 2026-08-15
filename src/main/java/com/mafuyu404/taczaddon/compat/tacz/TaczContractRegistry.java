package com.mafuyu404.taczaddon.compat.tacz;

import com.mafuyu404.taczaddon.compat.tacz.contract.ClassContract;
import com.mafuyu404.taczaddon.compat.tacz.contract.FeatureContract;
import com.mafuyu404.taczaddon.compat.tacz.contract.FieldAccessContract;
import com.mafuyu404.taczaddon.compat.tacz.contract.FieldContract;
import com.mafuyu404.taczaddon.compat.tacz.contract.InvokeContract;
import com.mafuyu404.taczaddon.compat.tacz.contract.MethodContract;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TaczContractRegistry {
    public static final String PROFILE_ID = "1.1.8-hotfix";

    private static final String MIXIN_TACZ =
            "com.mafuyu404.taczaddon.mixin.tacz.";
    private static final String MIXIN_V1 =
            "com.mafuyu404.taczaddon.mixin.tacz.v1_1_8.";

    private static final Map<TaczFeature, FeatureContract> FEATURE_CONTRACTS;
    private static final Map<TaczFeature, CompatibilityScope> FEATURE_SCOPES;
    private static final Map<String, TaczMixinBinding> MIXIN_BINDINGS;

    static {
        Map<TaczFeature, FeatureContract> featureContracts =
                new EnumMap<>(TaczFeature.class);
        Map<TaczFeature, CompatibilityScope> featureScopes =
                new EnumMap<>(TaczFeature.class);
        Map<String, TaczMixinBinding> mixinBindings = new HashMap<>();

        FeatureContract aimCamera = publicStable(
                TaczFeature.AIM_CAMERA,
                new ClassContract(
                        "com.tacz.guns.client.input.AimKey"
                ).withMethods(new MethodContract(
                        "onAimPress",
                        "(Lnet/minecraftforge/client/event/"
                                + "InputEvent$MouseButton$Post;)V"
                )).withFields(FieldContract.of(
                        "AIM_KEY",
                        "Lnet/minecraft/client/KeyMapping;"
                ))
        );
        featureContracts.put(
                TaczFeature.AIM_CAMERA,
                aimCamera
        );
        featureScopes.put(
                TaczFeature.AIM_CAMERA,
                CompatibilityScope.PUBLIC_STABLE
        );
        mixinBindings.put(
                MIXIN_TACZ + "AimKeyMixin",
                new TaczMixinBinding(
                        MIXIN_TACZ + "AimKeyMixin",
                        TaczFeature.AIM_CAMERA,
                        aimCamera,
                        CompatibilityScope.PUBLIC_STABLE
                )
        );

        FeatureContract crawlDisable = publicStable(
                TaczFeature.CRAWL_DISABLE,
                new ClassContract(
                        "com.tacz.guns.entity.shooter.LivingEntityCrawl"
                ).withMethods(new MethodContract(
                        "tickCrawling",
                        "()V"
                ))
        );
        featureContracts.put(
                TaczFeature.CRAWL_DISABLE,
                crawlDisable
        );
        featureScopes.put(
                TaczFeature.CRAWL_DISABLE,
                CompatibilityScope.PUBLIC_STABLE
        );
        mixinBindings.put(
                MIXIN_TACZ + "NoCrawlMixin",
                new TaczMixinBinding(
                        MIXIN_TACZ + "NoCrawlMixin",
                        TaczFeature.CRAWL_DISABLE,
                        crawlDisable,
                        CompatibilityScope.PUBLIC_STABLE
                )
        );

        FeatureContract taczSsr5Crosshair = versionBound(
                TaczFeature.TACZ_SSR5_CROSSHAIR,
                new ClassContract(
                        "com.tacz.guns.compat.shouldersurfing."
                                + "ShoulderSurfingCompat"
                ).withMethods(new MethodContract(
                        "showCrosshair",
                        "()Z"
                ))
        );
        featureContracts.put(
                TaczFeature.TACZ_SSR5_CROSSHAIR,
                taczSsr5Crosshair
        );
        featureScopes.put(
                TaczFeature.TACZ_SSR5_CROSSHAIR,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_TACZ + "ShoulderSurfingCompatMixin",
                binding(
                        MIXIN_TACZ + "ShoulderSurfingCompatMixin",
                        TaczFeature.TACZ_SSR5_CROSSHAIR,
                        taczSsr5Crosshair
                )
        );

        FeatureContract backpackQuery = versionBound(
                TaczFeature.BACKPACK_AMMO_QUERY,
                abstractGunCanReloadContract(),
                kineticHasAmmoContract(),
                gunAnimationHasAmmoContract()
        );
        featureContracts.put(
                TaczFeature.BACKPACK_AMMO_QUERY,
                backpackQuery
        );
        featureScopes.put(
                TaczFeature.BACKPACK_AMMO_QUERY,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "AbstractGunItemMixin",
                binding(
                        MIXIN_V1 + "AbstractGunItemMixin",
                        TaczFeature.BACKPACK_AMMO_QUERY,
                        versionBound(
                                TaczFeature.BACKPACK_AMMO_QUERY,
                                abstractGunCanReloadContract()
                        )
                )
        );
        mixinBindings.put(
                MIXIN_V1 + "ModernKineticGunScriptAPIHasAmmoMixin",
                binding(
                        MIXIN_V1 + "ModernKineticGunScriptAPIHasAmmoMixin",
                        TaczFeature.BACKPACK_AMMO_QUERY,
                        versionBound(
                                TaczFeature.BACKPACK_AMMO_QUERY,
                                kineticHasAmmoContract()
                        )
                )
        );
        mixinBindings.put(
                MIXIN_V1 + "GunAnimationStateContextMixin",
                binding(
                        MIXIN_V1 + "GunAnimationStateContextMixin",
                        TaczFeature.BACKPACK_AMMO_QUERY,
                        versionBound(
                                TaczFeature.BACKPACK_AMMO_QUERY,
                                gunAnimationHasAmmoContract()
                        )
                )
        );

        FeatureContract backpackConsume = versionBound(
                TaczFeature.BACKPACK_AMMO_CONSUME,
                kineticConsumeContract()
        );
        featureContracts.put(
                TaczFeature.BACKPACK_AMMO_CONSUME,
                backpackConsume
        );
        featureScopes.put(
                TaczFeature.BACKPACK_AMMO_CONSUME,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "ModernKineticGunScriptAPIMixin",
                binding(
                        MIXIN_V1 + "ModernKineticGunScriptAPIMixin",
                        TaczFeature.BACKPACK_AMMO_CONSUME,
                        backpackConsume
                )
        );

        FeatureContract refit = versionBound(
                TaczFeature.LIBERATED_REFIT,
                refitPacketFields(),
                refitPacketHandle(),
                unloadPacketFields(),
                unloadPacketHandle(),
                refitScreenContract(),
                refitSlotContract()
        );
        featureContracts.put(
                TaczFeature.LIBERATED_REFIT,
                refit
        );
        featureScopes.put(
                TaczFeature.LIBERATED_REFIT,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "ClientMessageRefitGunAccess",
                binding(
                        MIXIN_V1 + "ClientMessageRefitGunAccess",
                        TaczFeature.LIBERATED_REFIT,
                        versionBound(
                                TaczFeature.LIBERATED_REFIT,
                                refitPacketFields()
                        )
                )
        );
        mixinBindings.put(
                MIXIN_V1 + "ClientMessageRefitGunMixin",
                binding(
                        MIXIN_V1 + "ClientMessageRefitGunMixin",
                        TaczFeature.LIBERATED_REFIT,
                        versionBound(
                                TaczFeature.LIBERATED_REFIT,
                                refitPacketFields(),
                                refitPacketHandle()
                        )
                )
        );
        mixinBindings.put(
                MIXIN_V1 + "ClientMessageUnloadAttachmentAccess",
                binding(
                        MIXIN_V1 + "ClientMessageUnloadAttachmentAccess",
                        TaczFeature.LIBERATED_REFIT,
                        versionBound(
                                TaczFeature.LIBERATED_REFIT,
                                unloadPacketFields()
                        )
                )
        );
        mixinBindings.put(
                MIXIN_V1 + "ClientMessageUnloadAttachmentMixin",
                binding(
                        MIXIN_V1 + "ClientMessageUnloadAttachmentMixin",
                        TaczFeature.LIBERATED_REFIT,
                        versionBound(
                                TaczFeature.LIBERATED_REFIT,
                                unloadPacketFields(),
                                unloadPacketHandle()
                        )
                )
        );
        mixinBindings.put(
                MIXIN_V1 + "GunRefitScreenMixin",
                binding(
                        MIXIN_V1 + "GunRefitScreenMixin",
                        TaczFeature.LIBERATED_REFIT,
                        versionBound(
                                TaczFeature.LIBERATED_REFIT,
                                refitScreenContract()
                        )
                )
        );
        mixinBindings.put(
                MIXIN_V1 + "InventoryAttachmentSlotAccess",
                binding(
                        MIXIN_V1 + "InventoryAttachmentSlotAccess",
                        TaczFeature.LIBERATED_REFIT,
                        versionBound(
                                TaczFeature.LIBERATED_REFIT,
                                refitSlotContract()
                        )
                )
        );

        FeatureContract screenAccess = versionBound(
                TaczFeature.GUNSMITH_SCREEN_ACCESS,
                gunsmithScreenFields()
        );
        featureContracts.put(
                TaczFeature.GUNSMITH_SCREEN_ACCESS,
                screenAccess
        );
        featureScopes.put(
                TaczFeature.GUNSMITH_SCREEN_ACCESS,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "GunSmithTableScreenAccessMixin",
                binding(
                        MIXIN_V1 + "GunSmithTableScreenAccessMixin",
                        TaczFeature.GUNSMITH_SCREEN_ACCESS,
                        screenAccess
                )
        );

        FeatureContract browse = versionBound(
                TaczFeature.GUNSMITH_BROWSE_MEMORY,
                browseContract()
        );
        featureContracts.put(
                TaczFeature.GUNSMITH_BROWSE_MEMORY,
                browse
        );
        featureScopes.put(
                TaczFeature.GUNSMITH_BROWSE_MEMORY,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "GunSmithTableBrowseMemoryMixin",
                binding(
                        MIXIN_V1 + "GunSmithTableBrowseMemoryMixin",
                        TaczFeature.GUNSMITH_BROWSE_MEMORY,
                        browse,
                        TaczFeature.GUNSMITH_SCREEN_ACCESS
                )
        );

        FeatureContract sourceView = versionBound(
                TaczFeature.GUNSMITH_EXTERNAL_SOURCE_VIEW,
                sourceViewContract()
        );
        featureContracts.put(
                TaczFeature.GUNSMITH_EXTERNAL_SOURCE_VIEW,
                sourceView
        );
        featureScopes.put(
                TaczFeature.GUNSMITH_EXTERNAL_SOURCE_VIEW,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "GunSmithTableSourceViewMixin",
                binding(
                        MIXIN_V1 + "GunSmithTableSourceViewMixin",
                        TaczFeature.GUNSMITH_EXTERNAL_SOURCE_VIEW,
                        sourceView,
                        TaczFeature.GUNSMITH_SCREEN_ACCESS
                )
        );

        FeatureContract craftBridge = versionBound(
                TaczFeature.GUNSMITH_CRAFT_BRIDGE,
                craftBridgeContract()
        );
        featureContracts.put(
                TaczFeature.GUNSMITH_CRAFT_BRIDGE,
                craftBridge
        );
        featureScopes.put(
                TaczFeature.GUNSMITH_CRAFT_BRIDGE,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "GunSmithTableCraftBridgeMixin",
                binding(
                        MIXIN_V1 + "GunSmithTableCraftBridgeMixin",
                        TaczFeature.GUNSMITH_CRAFT_BRIDGE,
                        craftBridge,
                        TaczFeature.GUNSMITH_SCREEN_ACCESS
                )
        );

        FeatureContract session = versionBound(
                TaczFeature.GUNSMITH_SESSION,
                new ClassContract(
                        "com.tacz.guns.inventory.GunSmithTableMenu"
                ).withMethods(new MethodContract(
                        "getRecipe",
                        "(Lnet/minecraft/resources/ResourceLocation;"
                                + "Lnet/minecraft/world/item/crafting/"
                                + "RecipeManager;)"
                                + "Lcom/tacz/guns/crafting/"
                                + "GunSmithTableRecipe;"
                ))
        );
        featureContracts.put(
                TaczFeature.GUNSMITH_SESSION,
                session
        );
        featureScopes.put(
                TaczFeature.GUNSMITH_SESSION,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "GunSmithTableMenuAccess",
                binding(
                        MIXIN_V1 + "GunSmithTableMenuAccess",
                        TaczFeature.GUNSMITH_SESSION,
                        session
                )
        );

        FeatureContract fastSwap = versionBound(
                TaczFeature.FAST_SWAP,
                localPlayerDrawContract(),
                livingEntityDrawGunContract()
        );
        featureContracts.put(
                TaczFeature.FAST_SWAP,
                fastSwap
        );
        featureScopes.put(
                TaczFeature.FAST_SWAP,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "LocalPlayerDrawMixin",
                binding(
                        MIXIN_V1 + "LocalPlayerDrawMixin",
                        TaczFeature.FAST_SWAP,
                        versionBound(
                                TaczFeature.FAST_SWAP,
                                localPlayerDrawContract()
                        )
                )
        );
        mixinBindings.put(
                MIXIN_V1 + "LivingEntityDrawGunMixin",
                binding(
                        MIXIN_V1 + "LivingEntityDrawGunMixin",
                        TaczFeature.FAST_SWAP,
                        versionBound(
                                TaczFeature.FAST_SWAP,
                                livingEntityDrawGunContract()
                        )
                )
        );

        FeatureContract shootWhileReload = versionBound(
                TaczFeature.SHOOT_WHILE_RELOADING,
                serverShootContract(),
                clientShootReloadContract()
        );
        featureContracts.put(
                TaczFeature.SHOOT_WHILE_RELOADING,
                shootWhileReload
        );
        featureScopes.put(
                TaczFeature.SHOOT_WHILE_RELOADING,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "LivingEntityShootReloadInterruptMixin",
                binding(
                        MIXIN_V1 + "LivingEntityShootReloadInterruptMixin",
                        TaczFeature.SHOOT_WHILE_RELOADING,
                        versionBound(
                                TaczFeature.SHOOT_WHILE_RELOADING,
                                serverShootContract()
                        )
                )
        );
        mixinBindings.put(
                MIXIN_V1 + "LocalPlayerShootReloadInterruptMixin",
                binding(
                        MIXIN_V1 + "LocalPlayerShootReloadInterruptMixin",
                        TaczFeature.SHOOT_WHILE_RELOADING,
                        versionBound(
                                TaczFeature.SHOOT_WHILE_RELOADING,
                                clientShootReloadContract()
                        )
                )
        );

        FeatureContract slide = versionBound(
                TaczFeature.SLIDE_SHOOT,
                livingEntitySlideContract(),
                localPlayerShootContract()
        );
        featureContracts.put(
                TaczFeature.SLIDE_SHOOT,
                slide
        );
        featureScopes.put(
                TaczFeature.SLIDE_SHOOT,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "LivingEntityShootMixin",
                binding(
                        MIXIN_V1 + "LivingEntityShootMixin",
                        TaczFeature.SLIDE_SHOOT,
                        versionBound(
                                TaczFeature.SLIDE_SHOOT,
                                livingEntitySlideContract()
                        )
                )
        );
        mixinBindings.put(
                MIXIN_V1 + "LocalPlayerSlideShootMixin",
                binding(
                        MIXIN_V1 + "LocalPlayerSlideShootMixin",
                        TaczFeature.SLIDE_SHOOT,
                        versionBound(
                                TaczFeature.SLIDE_SHOOT,
                                localPlayerShootContract()
                        )
                )
        );

        FeatureContract betterMelee = versionBound(
                TaczFeature.BETTER_MELEE,
                localPlayerShootContract()
        );
        featureContracts.put(
                TaczFeature.BETTER_MELEE,
                betterMelee
        );
        featureScopes.put(
                TaczFeature.BETTER_MELEE,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "LocalPlayerBetterMeleeMixin",
                binding(
                        MIXIN_V1 + "LocalPlayerBetterMeleeMixin",
                        TaczFeature.BETTER_MELEE,
                        betterMelee
                )
        );

        FeatureContract hud = versionBound(
                TaczFeature.HUD_AMMO,
                hudContract()
        );
        featureContracts.put(
                TaczFeature.HUD_AMMO,
                hud
        );
        featureScopes.put(
                TaczFeature.HUD_AMMO,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "GunHudOverlayMixin",
                binding(
                        MIXIN_V1 + "GunHudOverlayMixin",
                        TaczFeature.HUD_AMMO,
                        hud
                )
        );

        FeatureContract tooltip = versionBound(
                TaczFeature.TOOLTIP_EXTENSION,
                tooltipContract()
        );
        featureContracts.put(
                TaczFeature.TOOLTIP_EXTENSION,
                tooltip
        );
        featureScopes.put(
                TaczFeature.TOOLTIP_EXTENSION,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "ClientAttachmentItemTooltipMixin",
                binding(
                        MIXIN_V1 + "ClientAttachmentItemTooltipMixin",
                        TaczFeature.TOOLTIP_EXTENSION,
                        tooltip
                )
        );

        FeatureContract animation = versionBound(
                TaczFeature.CLIENT_ANIMATION,
                animationContract()
        );
        featureContracts.put(
                TaczFeature.CLIENT_ANIMATION,
                animation
        );
        featureScopes.put(
                TaczFeature.CLIENT_ANIMATION,
                CompatibilityScope.VERSION_BOUND
        );
        mixinBindings.put(
                MIXIN_V1 + "AnimateGeoItemRendererMixin",
                binding(
                        MIXIN_V1 + "AnimateGeoItemRendererMixin",
                        TaczFeature.CLIENT_ANIMATION,
                        animation
                )
        );

        FEATURE_CONTRACTS =
                Collections.unmodifiableMap(featureContracts);
        FEATURE_SCOPES =
                Collections.unmodifiableMap(featureScopes);
        MIXIN_BINDINGS =
                Collections.unmodifiableMap(mixinBindings);
    }

    private TaczContractRegistry() {
    }

    public static FeatureContract contractFor(TaczFeature feature) {
        return FEATURE_CONTRACTS.get(feature);
    }

    public static CompatibilityScope scopeFor(TaczFeature feature) {
        return FEATURE_SCOPES.get(feature);
    }

    public static TaczMixinBinding bindingForMixin(String mixinClassName) {
        return MIXIN_BINDINGS.get(mixinClassName);
    }

    public static TaczFeature featureForMixin(String mixinClassName) {
        TaczMixinBinding binding = bindingForMixin(mixinClassName);
        return binding == null ? null : binding.feature();
    }

    public static boolean hasContract(TaczFeature feature) {
        return FEATURE_CONTRACTS.containsKey(feature);
    }

    private static FeatureContract publicStable(
            TaczFeature feature,
            ClassContract... classes
    ) {
        return new FeatureContract(feature, PROFILE_ID, classes);
    }

    private static FeatureContract versionBound(
            TaczFeature feature,
            ClassContract... classes
    ) {
        return new FeatureContract(feature, PROFILE_ID, classes);
    }

    private static TaczMixinBinding binding(
            String mixinClassName,
            TaczFeature feature,
            FeatureContract contract,
            TaczFeature... dependencies
    ) {
        return new TaczMixinBinding(
                mixinClassName,
                feature,
                contract,
                CompatibilityScope.VERSION_BOUND,
                List.of(dependencies)
        );
    }

    private static ClassContract abstractGunCanReloadContract() {
        return new ClassContract(
                "com.tacz.guns.api.item.gun.AbstractGunItem"
        ).withMethods(new MethodContract(
                "canReload",
                "(Lnet/minecraft/world/entity/LivingEntity;"
                        + "Lnet/minecraft/world/item/ItemStack;)Z",
                List.of(livingEntityGetCapability())
        ));
    }

    private static ClassContract kineticHasAmmoContract() {
        return new ClassContract(
                "com.tacz.guns.item.ModernKineticGunScriptAPI"
        ).withMethods(new MethodContract(
                "hasAmmoToConsume",
                "()Z",
                List.of(livingEntityGetCapability())
        ));
    }

    private static ClassContract gunAnimationHasAmmoContract() {
        return new ClassContract(
                "com.tacz.guns.client.animation.statemachine."
                        + "GunAnimationStateContext"
        ).withMethods(new MethodContract(
                "hasAmmoToConsume",
                "()Z",
                List.of(InvokeContract.exactlyOne(
                        "com/tacz/guns/client/animation/statemachine/"
                                + "GunAnimationStateContext",
                        "processCameraEntity",
                        "(Ljava/util/function/Function;)"
                                + "Ljava/util/Optional;"
                ))
        ));
    }

    private static InvokeContract livingEntityGetCapability() {
        return new InvokeContract(
                "net/minecraft/world/entity/LivingEntity",
                null,
                "(Lnet/minecraftforge/common/capabilities/Capability;"
                        + "Lnet/minecraft/core/Direction;)"
                        + "Lnet/minecraftforge/common/util/LazyOptional;",
                1,
                1
        );
    }

    private static ClassContract kineticConsumeContract() {
        return new ClassContract(
                "com.tacz.guns.item.ModernKineticGunScriptAPI"
        ).withMethods(new MethodContract(
                "consumeAmmoFromPlayer",
                "(I)I"
        )).withFields(
                FieldContract.of(
                        "shooter",
                        "Lnet/minecraft/world/entity/LivingEntity;"
                ),
                FieldContract.of(
                        "itemStack",
                        "Lnet/minecraft/world/item/ItemStack;"
                ),
                FieldContract.of(
                        "abstractGunItem",
                        "Lcom/tacz/guns/api/item/gun/AbstractGunItem;"
                )
        );
    }

    private static ClassContract refitPacketFields() {
        return new ClassContract(
                "com.tacz.guns.network.message.ClientMessageRefitGun"
        ).withFields(
                FieldContract.of("attachmentSlotIndex", "I"),
                FieldContract.of("gunSlotIndex", "I"),
                FieldContract.of(
                        "attachmentType",
                        "Lcom/tacz/guns/api/item/attachment/"
                                + "AttachmentType;"
                )
        );
    }

    private static ClassContract refitPacketHandle() {
        return new ClassContract(
                "com.tacz.guns.network.message.ClientMessageRefitGun"
        ).withMethods(new MethodContract(
                "handle",
                "(Lcom/tacz/guns/network/message/"
                        + "ClientMessageRefitGun;"
                        + "Ljava/util/function/Supplier;)V",
                List.of(enqueueWork())
        ));
    }

    private static ClassContract unloadPacketFields() {
        return new ClassContract(
                "com.tacz.guns.network.message."
                        + "ClientMessageUnloadAttachment"
        ).withFields(
                FieldContract.of("gunSlotIndex", "I"),
                FieldContract.of(
                        "attachmentType",
                        "Lcom/tacz/guns/api/item/attachment/"
                                + "AttachmentType;"
                )
        );
    }

    private static ClassContract unloadPacketHandle() {
        return new ClassContract(
                "com.tacz.guns.network.message."
                        + "ClientMessageUnloadAttachment"
        ).withMethods(new MethodContract(
                "handle",
                "(Lcom/tacz/guns/network/message/"
                        + "ClientMessageUnloadAttachment;"
                        + "Ljava/util/function/Supplier;)V",
                List.of(enqueueWork())
        ));
    }

    private static InvokeContract enqueueWork() {
        return InvokeContract.exactlyOne(
                "net/minecraftforge/network/NetworkEvent$Context",
                "enqueueWork",
                "(Ljava/lang/Runnable;)"
                        + "Ljava/util/concurrent/CompletableFuture;"
        );
    }

    private static ClassContract refitScreenContract() {
        return new ClassContract(
                "com.tacz.guns.client.gui.GunRefitScreen"
        ).withMethods(
                new MethodContract(
                        "addInventoryAttachmentButtons",
                        "()V",
                        List.of(
                                localPlayerGetInventory(),
                                InvokeContract.exactlyOne(
                                        "com/tacz/guns/client/gui/"
                                                + "components/refit/"
                                                + "InventoryAttachmentSlot",
                                        "<init>",
                                        "(IIILnet/minecraft/world/entity/"
                                                + "player/Inventory;"
                                                + "Lnet/minecraft/client/gui/"
                                                + "components/Button$OnPress;)V"
                                )
                        )
                ),
                new MethodContract(
                        "addAttachmentTypeButtons",
                        "()V",
                        List.of(localPlayerGetInventory())
                )
        ).withFields(FieldContract.of("currentPage", "I"));
    }

    private static ClassContract refitSlotContract() {
        return new ClassContract(
                "com.tacz.guns.client.gui.components.refit."
                        + "InventoryAttachmentSlot"
        ).withMethods(new MethodContract(
                "<init>",
                "(IIILnet/minecraft/world/entity/player/Inventory;"
                        + "Lnet/minecraft/client/gui/components/"
                        + "Button$OnPress;)V"
        )).withFields(FieldContract.of(
                "inventory",
                "Lnet/minecraft/world/entity/player/Inventory;"
        ));
    }

    private static InvokeContract localPlayerGetInventory() {
        return new InvokeContract(
                "net/minecraft/client/player/LocalPlayer",
                "getInventory",
                "()Lnet/minecraft/world/entity/player/Inventory;",
                1,
                1,
                List.of("m_150109_")
        );
    }

    private static ClassContract gunsmithScreenFields() {
        return new ClassContract(
                "com.tacz.guns.client.gui.GunSmithTableScreen"
        ).withFields(
                FieldContract.of(
                        "selectedRecipe",
                        "Lcom/tacz/guns/crafting/GunSmithTableRecipe;"
                ),
                FieldContract.of(
                        "selectedRecipeList",
                        "Ljava/util/List;"
                ),
                FieldContract.of(
                        "selectedType",
                        "Lnet/minecraft/resources/ResourceLocation;"
                ),
                FieldContract.of("indexPage", "I"),
                FieldContract.of("typePage", "I"),
                FieldContract.of(
                        "playerIngredientCount",
                        "Lit/unimi/dsi/fastutil/ints/Int2IntArrayMap;"
                )
        );
    }

    private static ClassContract browseContract() {
        MethodContract init = new MethodContract(
                "init",
                "()V",
                List.of(InvokeContract.exactlyOne(
                        "com/tacz/guns/client/gui/GunSmithTableScreen",
                        "updateSelectedRecipeAfterFiltering",
                        "()V"
                ))
        ).withAliases("m_7856_");

        return new ClassContract(
                "com.tacz.guns.client.gui.GunSmithTableScreen"
        ).withMethods(
                init,
                new MethodContract(
                        "getSelectedRecipe",
                        "(Lnet/minecraft/resources/ResourceLocation;)"
                                + "Lcom/tacz/guns/crafting/"
                                + "GunSmithTableRecipe;"
                ),
                new MethodContract(
                        "getPlayerIngredientCount",
                        "(Lcom/tacz/guns/crafting/GunSmithTableRecipe;)V"
                )
        ).withFields(
                FieldContract.of(
                        "selectedRecipe",
                        "Lcom/tacz/guns/crafting/GunSmithTableRecipe;"
                ),
                FieldContract.of(
                        "selectedRecipeList",
                        "Ljava/util/List;"
                ),
                FieldContract.of("indexPage", "I"),
                FieldContract.of(
                        "selectedType",
                        "Lnet/minecraft/resources/ResourceLocation;"
                ),
                FieldContract.of(
                        "recipes",
                        "Ljava/util/Map;"
                ),
                FieldContract.of(
                        "recipeKeys",
                        "Ljava/util/LinkedHashMap;"
                ),
                FieldContract.of("typePage", "I")
        );
    }

    private static ClassContract sourceViewContract() {
        return new ClassContract(
                "com.tacz.guns.client.gui.GunSmithTableScreen"
        ).withMethods(
                new MethodContract(
                        "updateIngredientCount",
                        "()V"
                ),
                new MethodContract(
                        "getPlayerIngredientCount",
                        "(Lcom/tacz/guns/crafting/"
                                + "GunSmithTableRecipe;)V"
                )
        ).withFields(FieldContract.of(
                "playerIngredientCount",
                "Lit/unimi/dsi/fastutil/ints/Int2IntArrayMap;"
        ));
    }

    private static ClassContract craftBridgeContract() {
        return new ClassContract(
                "com.tacz.guns.client.gui.GunSmithTableScreen"
        ).withMethods(new MethodContract(
                "addCraftButton",
                "()V",
                List.of(InvokeContract.exactlyOne(
                        "net/minecraft/client/gui/components/ImageButton",
                        "<init>",
                        "(IIIIIIILnet/minecraft/resources/ResourceLocation;"
                                + "Lnet/minecraft/client/gui/components/"
                                + "Button$OnPress;)V"
                ))
        )).withFields(FieldContract.of(
                "selectedRecipe",
                "Lcom/tacz/guns/crafting/GunSmithTableRecipe;"
        ));
    }

    private static ClassContract localPlayerDrawContract() {
        return new ClassContract(
                "com.tacz.guns.client.gameplay.LocalPlayerDraw"
        ).withMethods(
                new MethodContract(
                        "draw",
                        "(Lnet/minecraft/world/item/ItemStack;)V",
                        List.of(InvokeContract.exactlyOne(
                                "com/tacz/guns/client/gameplay/"
                                        + "LocalPlayerDraw",
                                "resetData",
                                "()V"
                        ))
                ),
                new MethodContract("resetData", "()V")
        ).withFields(FieldContract.of(
                "data",
                "Lcom/tacz/guns/client/gameplay/LocalPlayerDataHolder;"
        ));
    }

    private static ClassContract livingEntityDrawGunContract() {
        return new ClassContract(
                "com.tacz.guns.entity.shooter.LivingEntityDrawGun"
        ).withMethods(new MethodContract(
                "draw",
                "(Ljava/util/function/Supplier;)V",
                List.of(InvokeContract.exactlyOne(
                        "com/tacz/guns/entity/shooter/ShooterDataHolder",
                        "initialData",
                        "()V"
                ))
        )).withFields(FieldContract.of(
                "data",
                "Lcom/tacz/guns/entity/shooter/ShooterDataHolder;"
        ));
    }

    private static ClassContract serverShootContract() {
        return new ClassContract(
                "com.tacz.guns.entity.shooter.LivingEntityShoot"
        ).withMethods(new MethodContract(
                "shoot",
                "(Ljava/util/function/Supplier;"
                        + "Ljava/util/function/Supplier;JFZ)"
                        + "Lcom/tacz/guns/api/entity/ShootResult;",
                List.of(
                        InvokeContract.exactlyOne(
                                "com/tacz/guns/api/entity/"
                                        + "ReloadState$StateType",
                                "isReloading",
                                "()Z"
                        ),
                        InvokeContract.exactlyOne(
                                "com/tacz/guns/network/NetworkHandler",
                                "sendToTrackingEntity",
                                "(Ljava/lang/Object;"
                                        + "Lnet/minecraft/world/entity/"
                                        + "Entity;)V"
                        )
                )
        )).withFields(FieldContract.of(
                "shooter",
                "Lnet/minecraft/world/entity/LivingEntity;"
        ));
    }

    private static ClassContract clientShootReloadContract() {
        return new ClassContract(
                "com.tacz.guns.client.gameplay.LocalPlayerShoot"
        ).withMethods(
                new MethodContract(
                        "shoot",
                        "()Lcom/tacz/guns/api/entity/ShootResult;",
                        List.of(
                                InvokeContract.exactlyOne(
                                        "com/tacz/guns/client/gameplay/"
                                                + "LocalPlayerDataHolder",
                                        "lockState",
                                        "(Ljava/util/function/Predicate;)V"
                                )
                        )
                ).withFieldAccesses(
                        FieldAccessContract.exactlyOneGetField(
                                "com/tacz/guns/client/gameplay/"
                                        + "LocalPlayerDataHolder",
                                "clientStateLock",
                                "Z"
                        )
                ),
                new MethodContract(
                        "preCheck",
                        "(Lcom/tacz/guns/api/item/IGun;"
                                + "Lcom/tacz/guns/api/entity/IGunOperator;"
                                + "Lcom/tacz/guns/client/resource/index/"
                                + "ClientGunIndex;"
                                + "Lnet/minecraft/world/item/ItemStack;"
                                + "Lcom/tacz/guns/client/resource/"
                                + "GunDisplayInstance;"
                                + "Lcom/tacz/guns/resource/pojo/data/gun/"
                                + "GunData;Z)"
                                + "Lcom/tacz/guns/api/entity/ShootResult;",
                        List.of(InvokeContract.exactlyOne(
                                "com/tacz/guns/api/entity/"
                                        + "ReloadState$StateType",
                                "isReloading",
                                "()Z"
                        ))
                )
        ).withFields(FieldContract.of(
                "player",
                "Lnet/minecraft/client/player/LocalPlayer;"
        ));
    }

    private static ClassContract livingEntitySlideContract() {
        return new ClassContract(
                "com.tacz.guns.entity.shooter.LivingEntityShoot"
        ).withMethods(new MethodContract(
                "shoot",
                "(Ljava/util/function/Supplier;"
                        + "Ljava/util/function/Supplier;J)"
                        + "Lcom/tacz/guns/api/entity/ShootResult;"
        )).withFields(FieldContract.of(
                "shooter",
                "Lnet/minecraft/world/entity/LivingEntity;"
        ));
    }

    private static ClassContract localPlayerShootContract() {
        return new ClassContract(
                "com.tacz.guns.client.gameplay.LocalPlayerShoot"
        ).withMethods(new MethodContract(
                "shoot",
                "()Lcom/tacz/guns/api/entity/ShootResult;"
        )).withFields(FieldContract.of(
                "player",
                "Lnet/minecraft/client/player/LocalPlayer;"
        ));
    }

    private static ClassContract hudContract() {
        return new ClassContract(
                "com.tacz.guns.client.gui.overlay.GunHudOverlay"
        ).withMethods(
                new MethodContract(
                        "handleCacheCount",
                        "(Lnet/minecraft/client/player/LocalPlayer;"
                                + "Lnet/minecraft/world/item/ItemStack;"
                                + "Lcom/tacz/guns/resource/pojo/data/gun/"
                                + "GunData;"
                                + "Lcom/tacz/guns/api/item/IGun;Z)V",
                        List.of(InvokeContract.exactlyOne(
                                "com/tacz/guns/client/gui/overlay/"
                                        + "GunHudOverlay",
                                "handleInventoryAmmo",
                                "(Lnet/minecraft/world/item/ItemStack;"
                                        + "Lnet/minecraft/world/entity/"
                                        + "player/Inventory;)V"
                        ))
                ),
                new MethodContract(
                        "handleInventoryAmmo",
                        "(Lnet/minecraft/world/item/ItemStack;"
                                + "Lnet/minecraft/world/entity/player/"
                                + "Inventory;)V"
                )
        );
    }

    private static ClassContract tooltipContract() {
        return new ClassContract(
                "com.tacz.guns.client.tooltip."
                        + "ClientAttachmentItemTooltip"
        ).withMethods(
                new MethodContract(
                        "<init>",
                        "(Lcom/tacz/guns/inventory/tooltip/"
                                + "AttachmentItemTooltip;)V"
                ),
                new MethodContract(
                        "getAllAllowGuns",
                        "(Ljava/util/List;"
                                + "Lnet/minecraft/resources/ResourceLocation;)"
                                + "Ljava/util/List;"
                )
        ).withFields(
                FieldContract.of(
                        "attachment",
                        "Lnet/minecraft/world/item/ItemStack;"
                ),
                FieldContract.of(
                        "components",
                        "Ljava/util/List;"
                )
        );
    }

    private static ClassContract animationContract() {
        return new ClassContract(
                "com.tacz.guns.client.renderer.item."
                        + "AnimateGeoItemRenderer"
        ).withMethods(
                new MethodContract(
                        "getStateMachine",
                        "(Lnet/minecraft/world/item/ItemStack;)"
                                + "Lcom/tacz/guns/api/client/animation/"
                                + "statemachine/LuaAnimationStateMachine;"
                ),
                new MethodContract(
                        "initContext",
                        "(Lnet/minecraft/world/item/ItemStack;"
                                + "Lnet/minecraft/world/entity/player/Player;"
                                + "F)Lcom/tacz/guns/client/animation/"
                                + "statemachine/ItemAnimationStateContext;"
                ),
                new MethodContract(
                        "getPutAwayDuration",
                        "(Lnet/minecraft/world/item/ItemStack;)J"
                ),
                new MethodContract(
                        "getPutAwayTime",
                        "(Lnet/minecraft/world/item/ItemStack;)J"
                ),
                new MethodContract(
                        "tryExit",
                        "(Lnet/minecraft/world/item/ItemStack;J)V"
                )
        );
    }
}
