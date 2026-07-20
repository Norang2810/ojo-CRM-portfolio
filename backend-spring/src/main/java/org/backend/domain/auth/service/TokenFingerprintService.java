package org.backend.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class TokenFingerprintService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String MISSING_USER_AGENT = "<missing-user-agent>";

    private final byte[] secret;

    public TokenFingerprintService(@Value("${app.jwt.fingerprint-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String from(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String normalized = userAgent == null || userAgent.isBlank()
                ? MISSING_USER_AGENT
                : userAgent.trim();
        return hmac(normalized);
    }

    public boolean matches(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.US_ASCII),
                expected.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HmacSHA256 algorithm is not available", e);
        } catch (java.security.InvalidKeyException e) {
            throw new IllegalStateException("Fingerprint secret is invalid", e);
        }
    }
}
