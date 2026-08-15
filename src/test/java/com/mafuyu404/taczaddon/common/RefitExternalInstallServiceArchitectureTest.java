package com.mafuyu404.taczaddon.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RefitExternalInstallServiceArchitectureTest {
    @Test
    void externalInstallIsAuthoritativeAndTransactional()
            throws IOException {
        String source = Files.readString(
                Path.of(
                        "src/main/java/com/mafuyu404/taczaddon/common/"
                                + "RefitExternalInstallService.java"
                ),
                StandardCharsets.UTF_8
        );
        String normalized = source.replaceAll("\\s+", "");

        assertTrue(normalized.contains(
                "RefitSourceResolver.resolveExternalSources(player)"
        ));
        assertTrue(normalized.contains(
                "RefitSourceResolver.findSource("
        ));
        assertTrue(normalized.contains("request.locator()"));
        assertTrue(normalized.contains("source.isValid(player)"));
        assertTrue(normalized.contains(
                "source.extractItem(locator.slot(),1,true)"
        ));
        assertTrue(normalized.contains(
                "source.extractItem(locator.slot(),1,false)"
        ));
        assertTrue(normalized.contains(
                "matchesExtracted(extracted,current)"
        ));
        assertTrue(normalized.contains(
                "gun.installAttachment(gunStack,extracted)"
        ));
        assertTrue(normalized.contains("rollbackExtraction("));
        assertTrue(normalized.contains("player.drop(remainder,false)"));
        assertTrue(normalized.contains(
                "LiberateAttachmentService.isEnabled(player)"
        ));
    }
}
