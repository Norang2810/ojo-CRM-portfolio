package org.backend.domain.auth.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class TokenHashService {

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    public boolean matches(String rawToken, String expectedHash) {
        if (rawToken == null || expectedHash == null) {
            return false;
        }

        byte[] actual = hash(rawToken).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = expectedHash.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expected);
    }
}
