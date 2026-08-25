package com.zerog.stellarserverforge.javamanaged;

import com.zerog.stellarserverforge.model.McVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaVersionRulesTest {

    @Test
    void oldVersionsRequireJava8Only() {
        var options = JavaVersionRules.resolve(McVersion.parse("1.7.10"));
        assertEquals(8, options.defaultVersion());
        assertTrue(options.onlyOneChoice());
    }

    @Test
    void mc1_16_5OffersJava8Or11() {
        var options = JavaVersionRules.resolve(McVersion.parse("1.16.5"));
        assertEquals(8, options.defaultVersion());
        assertEquals(java.util.List.of(8, 11), options.options());
    }

    @Test
    void mc1_16_4Java8Only() {
        var options = JavaVersionRules.resolve(McVersion.parse("1.16.4"));
        assertTrue(options.onlyOneChoice());
        assertEquals(8, options.defaultVersion());
    }

    @Test
    void mc1_17RequiresJava16Only() {
        var options = JavaVersionRules.resolve(McVersion.parse("1.17"));
        assertTrue(options.onlyOneChoice());
        assertEquals(16, options.defaultVersion());
    }

    @Test
    void mc1_18To19OffersSeventeenTwentyOneTwentyFive() {
        var options = JavaVersionRules.resolve(McVersion.parse("1.18.2"));
        assertEquals(17, options.defaultVersion());
        assertEquals(java.util.List.of(17, 21, 25), options.options());
    }

    @Test
    void mc1_20_4DefaultsToSeventeen() {
        var options = JavaVersionRules.resolve(McVersion.parse("1.20.4"));
        assertEquals(17, options.defaultVersion());
        assertEquals(java.util.List.of(17, 21, 25), options.options());
    }

    @Test
    void mc1_20_5DefaultsToTwentyOne() {
        // 1.20.5 bumped Minecraft's own minimum Java requirement to 21 ahead of the usual
        // major-version cadence (confirmed via Mojang's 1.20.5 release notes / Minecraft Wiki).
        var options = JavaVersionRules.resolve(McVersion.parse("1.20.5"));
        assertEquals(21, options.defaultVersion());
        assertEquals(java.util.List.of(21, 25), options.options());
    }

    @Test
    void mc1_20_6DefaultsToTwentyOne() {
        var options = JavaVersionRules.resolve(McVersion.parse("1.20.6"));
        assertEquals(21, options.defaultVersion());
        assertEquals(java.util.List.of(21, 25), options.options());
    }

    @Test
    void mc1_21DefaultsToTwentyOne() {
        var options = JavaVersionRules.resolve(McVersion.parse("1.21"));
        assertEquals(21, options.defaultVersion());
    }

    @Test
    void aboveMc1_21RequiresJava25Only() {
        var options = JavaVersionRules.resolve(McVersion.parse("1.22"));
        assertTrue(options.onlyOneChoice());
        assertEquals(25, options.defaultVersion());
    }
}
