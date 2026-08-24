package com.zerog.stellarserverforge.settings;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/** Decrypts a field's value with {@link SecretStore} at JSON-read time. */
public class EncryptedStringDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return SecretStore.decrypt(p.getValueAsString());
    }
}
