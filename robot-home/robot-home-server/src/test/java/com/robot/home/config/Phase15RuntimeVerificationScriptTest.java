package com.robot.home.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Phase15 runtime verification script contract")
class Phase15RuntimeVerificationScriptTest {

    @Test
    @DisplayName("runtime table fingerprint matches the real Phase13/14 SQL chain")
    void runtimeFingerprintUsesActualSqlTableNames() throws Exception {
        Path script = Paths.get("..", "scripts", "phase15", "verify-runtime.sh");
        String content = new String(Files.readAllBytes(script), StandardCharsets.UTF_8);

        assertTrue(content.contains("company_claim company_profile_change"));
        assertTrue(content.contains("commercial_order"));
        assertFalse(content.contains("enterprise_company"));
        assertFalse(content.contains("procurement_lead"));
        assertFalse(content.contains("enterprise_order"));
    }
}
