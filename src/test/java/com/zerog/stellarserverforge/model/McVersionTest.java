package com.zerog.stellarserverforge.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McVersionTest {

    @Test
    void parsesLegacyTwoPartVersion() {
        McVersion v = McVersion.parse("1.20");
        assertEquals(20, v.major());
        assertEquals(0, v.minor());
        assertEquals(0, v.hotfix());
    }

    @Test
    void parsesLegacyThreePartVersion() {
        McVersion v = McVersion.parse("1.20.1");
        assertEquals(20, v.major());
        assertEquals(1, v.minor());
        assertEquals(0, v.hotfix());
    }

    @Test
    void parsesLegacyOldVersion() {
        McVersion v = McVersion.parse("1.7.10");
        assertEquals(7, v.major());
        assertEquals(10, v.minor());
    }

    @Test
    void parsesFutureYearBasedTwoPartVersion() {
        McVersion v = McVersion.parse("26.1");
        assertEquals(26, v.major());
        assertEquals(1, v.minor());
        assertEquals(0, v.hotfix());
    }

    @Test
    void parsesFutureYearBasedThreePartVersion() {
        McVersion v = McVersion.parse("26.1.2");
        assertEquals(26, v.major());
        assertEquals(1, v.minor());
        assertEquals(2, v.hotfix());
    }

    @Test
    void rawPreservesOriginalString() {
        assertEquals("1.20.1", McVersion.parse("1.20.1").raw());
    }
}
