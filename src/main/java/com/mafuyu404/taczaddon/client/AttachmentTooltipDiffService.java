package com.mafuyu404.taczaddon.client;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Calculates attribute differences between two gun-attachment states.
 *
 * Uses structured {@code DiagramsData.modifier()} values rather than
 * parsing display strings.
 */
@OnlyIn(Dist.CLIENT)
public final class AttachmentTooltipDiffService {
    private static final double EPSILON = 1.0E-6D;

    private static final Map<String, String> PROPERTY_ALIASES =
            Map.of("inaccuracy", "hipfire_inaccuracy");

    private AttachmentTooltipDiffService() {
    }

    public record PropertyDifference(
            String diagramTitleKey,
            double absoluteDelta,
            OptionalDouble relativePercent
    ) {
    }

    /**
     * Calculates property differences between a baseline gun state
     * (same-type attachment removed if present) and a candidate gun
     * state (hovered attachment installed).
     *
     * @param heldGun              the current main-hand gun (copied, not
     *                             mutated)
     * @param candidateAttachment  the hovered attachment stack
     * @return ordered map of normalized property key to difference
     */
    public static Map<String, PropertyDifference> calculate(
            ItemStack heldGun,
            ItemStack candidateAttachment
    ) {
        Map<String, PropertyDifference> result =
                new LinkedHashMap<>();

        IGun gun = IGun.getIGunOrNull(heldGun);
        if (gun == null) {
            return result;
        }

        IAttachment attachmentApi =
                IAttachment.getIAttachmentOrNull(candidateAttachment);

        if (attachmentApi == null) {
            return result;
        }

        AttachmentType type =
                attachmentApi.getType(candidateAttachment);

        ResourceLocation gunId =
                gun.getGunId(heldGun);

        GunData gunData = TimelessAPI
                .getCommonGunIndex(gunId)
                .map(i -> i.getGunData())
                .orElse(null);

        if (gunData == null) {
            return result;
        }

        /* ---- baseline gun ---- */
        ItemStack baselineGun = heldGun.copy();
        IGun baselineIGun =
                IGun.getIGunOrNull(baselineGun);

        if (baselineIGun == null) {
            return result;
        }

        ItemStack oldSameType =
                baselineIGun.getAttachment(baselineGun, type);

        if (!oldSameType.isEmpty()) {
            baselineIGun.unloadAttachment(baselineGun, type);
        }

        Map<String, Double> baselineValues =
                evaluateGun(baselineGun, gunData);

        /* ---- candidate gun ---- */
        if (!baselineIGun.allowAttachment(
                baselineGun,
                candidateAttachment
        )) {
            return result;
        }

        ItemStack candidateGun = baselineGun.copy();
        IGun candidateIGun =
                IGun.getIGunOrNull(candidateGun);

        if (candidateIGun == null) {
            return result;
        }

        candidateIGun.installAttachment(
                candidateGun,
                candidateAttachment.copy()
        );

        Map<String, Double> candidateValues =
                evaluateGun(candidateGun, gunData);

        /* ---- compute differences ---- */
        for (Map.Entry<String, Double> entry :
                candidateValues.entrySet()) {
            String key = entry.getKey();
            double candidateValue = entry.getValue();
            Double baselineValue = baselineValues.get(key);

            if (baselineValue == null) {
                continue;
            }

            double delta = candidateValue - baselineValue;

            if (Double.isNaN(delta)
                    || Double.isInfinite(delta)
                    || Math.abs(delta) < EPSILON) {
                continue;
            }

            String propertyKey = PROPERTY_ALIASES.getOrDefault(
                    key,
                    key
            );

            String diagramKey =
                    "gui.tacz.gun_refit.property_diagrams."
                            + propertyKey;

            result.put(
                    propertyKey,
                    new PropertyDifference(
                            diagramKey,
                            delta,
                            OptionalDouble.empty()
                    )
            );
        }

        return result;
    }

    /**
     * Evaluates all attachment modifiers for the given gun stack and
     * returns a map of normalized property keys to structured modifier
     * values.
     */
    private static Map<String, Double> evaluateGun(
            ItemStack gunStack,
            GunData gunData
    ) {
        Map<String, Double> values = new LinkedHashMap<>();

        AttachmentCacheProperty cacheProperty =
                new AttachmentCacheProperty();

        cacheProperty.eval(gunStack, gunData);

        AttachmentPropertyManager.getModifiers()
                .forEach((modifierKey, modifier) -> {
                    try {
                        modifier.getPropertyDiagramsData(
                                        gunStack,
                                        gunData,
                                        cacheProperty
                                )
                                .forEach(diagram -> {
                                    String normalizedKey =
                                            normalizeDiagramKey(
                                                    diagram.titleKey()
                                            );

                                    if (normalizedKey == null) {
                                        return;
                                    }

                                    Number modifierValue =
                                            diagram.modifier();

                                    if (modifierValue == null) {
                                        return;
                                    }

                                    double value =
                                            modifierValue
                                                    .doubleValue();

                                    if (Double.isNaN(value)
                                            || Double.isInfinite(
                                            value
                                    )) {
                                        return;
                                    }

                                    values.put(
                                            normalizedKey,
                                            value
                                    );
                                });
                    } catch (RuntimeException ignored) {
                        // Skip one failing modifier
                    }
                });

        return values;
    }

    /**
     * Extracts a normalized property key from a diagram title key.
     *
     * Expected prefix: {@code gui.tacz.gun_refit.property_diagrams.}
     *
     * @return the normalized key, or null if the prefix doesn't match
     */
    static String normalizeDiagramKey(String titleKey) {
        if (titleKey == null || titleKey.isBlank()) {
            return null;
        }

        String prefix =
                "gui.tacz.gun_refit.property_diagrams.";

        if (!titleKey.startsWith(prefix)) {
            return null;
        }

        return titleKey.substring(prefix.length());
    }

    /**
     * Extracts a normalized property key from a tooltip translation key.
     *
     * Expected prefix: {@code tooltip.tacz.attachment.}
     *
     * @return the normalized key, or null if the prefix doesn't match
     */
    public static String normalizeTooltipKey(String translationKey) {
        if (translationKey == null || translationKey.isBlank()) {
            return null;
        }

        String prefix = "tooltip.tacz.attachment.";

        if (!translationKey.startsWith(prefix)) {
            return null;
        }

        String remainder = translationKey
                .substring(prefix.length());

        // The first dot-separated segment after the prefix is the
        // property name.
        int dot = remainder.indexOf('.');
        if (dot > 0) {
            return remainder.substring(0, dot);
        }

        return null;
    }
}
