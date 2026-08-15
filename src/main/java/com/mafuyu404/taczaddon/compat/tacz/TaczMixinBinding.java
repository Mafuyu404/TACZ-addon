package com.mafuyu404.taczaddon.compat.tacz;

import com.mafuyu404.taczaddon.compat.tacz.contract.FeatureContract;

import java.util.List;

public record TaczMixinBinding(
        String mixinClassName,
        TaczFeature feature,
        FeatureContract contract,
        CompatibilityScope scope,
        List<TaczFeature> dependencies
) {
    public TaczMixinBinding(
            String mixinClassName,
            TaczFeature feature,
            FeatureContract contract,
            CompatibilityScope scope
    ) {
        this(
                mixinClassName,
                feature,
                contract,
                scope,
                List.of()
        );
    }
}
