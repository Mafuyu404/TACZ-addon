package com.mafuyu404.taczaddon.compat.tacz;

import com.mafuyu404.taczaddon.compat.tacz.contract.FeatureContract;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class TaczCompatibility {
    private static final Logger LOGGER =
            LogUtils.getLogger();

    private static volatile boolean initialized;
    private static volatile String implementationVersion = "unknown";
    private static volatile TaczCompatibilityProfile profile =
            TaczCompatibilityProfile.UNKNOWN;
    private static volatile boolean taczPresent;
    private static final Map<TaczFeature, FeatureState> FEATURE_STATES =
            Collections.synchronizedMap(
                    new EnumMap<>(TaczFeature.class)
            );

    private TaczCompatibility() {
    }

    public static TaczCompatibilityProfile profile() {
        ensureInitialized();
        return profile;
    }

    public static boolean isAvailable(TaczFeature feature) {
        return status(feature) == TaczFeatureStatus.SUPPORTED;
    }

    public static TaczFeatureStatus status(TaczFeature feature) {
        ensureInitialized();
        return FEATURE_STATES.get(feature).status();
    }

    public static String reason(TaczFeature feature) {
        ensureInitialized();
        return FEATURE_STATES.get(feature).reason();
    }

    public static CompatibilitySnapshot snapshot() {
        ensureInitialized();
        EnumMap<TaczFeature, TaczFeatureStatus> copy =
                new EnumMap<>(TaczFeature.class);
        for (Map.Entry<TaczFeature, FeatureState> entry
                : FEATURE_STATES.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().status());
        }
        return new CompatibilitySnapshot(
                implementationVersion,
                profile,
                Collections.unmodifiableMap(copy)
        );
    }

    public static void logSummary() {
        ensureInitialized();
        LOGGER.info(
                "[TACZ-addon] TaCZ detected: {} profile: {}",
                implementationVersion,
                profile
        );
        for (TaczFeature feature : TaczFeature.values()) {
            TaczFeatureStatus status = status(feature);
            String suffix = status == TaczFeatureStatus.SUPPORTED
                    ? ""
                    : " (" + reason(feature) + ")";
            LOGGER.info(
                    "[TACZ-addon] {}: {}{}",
                    feature,
                    status,
                    suffix
            );
        }
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (TaczCompatibility.class) {
            if (initialized) {
                return;
            }

            TaczVersionReader.VersionResolution resolution =
                    TaczVersionReader.resolve();
            implementationVersion =
                    resolution.implementationVersion();
            taczPresent = resolution.present();
            profile = resolution.profile();

            for (TaczFeature feature : TaczFeature.values()) {
                FEATURE_STATES.put(
                        feature,
                        computeState(feature)
                );
            }
            initialized = true;
        }
    }

    public static boolean isMixinBindingAvailable(
            TaczMixinBinding binding
    ) {
        ensureInitialized();
        if (!taczPresent) {
            return false;
        }

        for (TaczFeature dependency : binding.dependencies()) {
            if (!isAvailable(dependency)) {
                return false;
            }
        }

        if (binding.scope() == CompatibilityScope.PUBLIC_STABLE) {
            return binding.contract() == null
                    || TaczBinaryProbe.inspect(
                    binding.contract()
            ).passed();
        }

        if (profile != TaczCompatibilityProfile.TACZ_1_1_8_HOTFIX) {
            return false;
        }
        return binding.contract() == null
                || TaczBinaryProbe.inspect(
                binding.contract()
        ).passed();
    }

    private static FeatureState computeState(
            TaczFeature feature
    ) {
        if (!taczPresent) {
            return new FeatureState(
                    TaczFeatureStatus.NOT_PRESENT,
                    "TaCZ is not present"
            );
        }

        CompatibilityScope scope =
                TaczContractRegistry.scopeFor(feature);
        if (profile == TaczCompatibilityProfile.UNKNOWN
                && scope == CompatibilityScope.VERSION_BOUND) {
            return new FeatureState(
                    TaczFeatureStatus.UNSUPPORTED_VERSION,
                    "version-bound feature requires recognized TaCZ profile"
            );
        }

        FeatureContract contract =
                TaczContractRegistry.contractFor(feature);
        if (contract == null || contract.classes().isEmpty()) {
            return new FeatureState(
                    TaczFeatureStatus.SUPPORTED,
                    "explicit scope without binary assumptions"
            );
        }

        TaczBinaryProbe.ProbeResult result =
                TaczBinaryProbe.inspect(contract);
        return result.passed()
                ? new FeatureState(
                TaczFeatureStatus.SUPPORTED,
                "contract satisfied"
        )
                : new FeatureState(
                TaczFeatureStatus.BINARY_CONTRACT_MISMATCH,
                result.detail()
        );
    }

    public record CompatibilitySnapshot(
            String implementationVersion,
            TaczCompatibilityProfile profile,
            Map<TaczFeature, TaczFeatureStatus> features
    ) {
    }

    private record FeatureState(
            TaczFeatureStatus status,
            String reason
    ) {
    }
}
