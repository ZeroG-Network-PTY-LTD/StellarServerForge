package com.zerog.stellarserverforge.mods;

/** A single mod jar identified during a client-mod scan (spec §10.1). */
public record ModEntry(String modId, String fileName) {
}
