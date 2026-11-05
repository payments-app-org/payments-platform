package com.paymentsapp.platform.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// Хэши и подписи. SHA-256 — для несекретных отпечатков (хэш запроса, получателя в котировке);
// для данных, которые можно подобрать перебором (номер карты), — только HMAC с секретным ключом.
public final class Hashing {

    private static final String SHA_256 = "SHA-256";
    private static final String HMAC_SHA_256 = "HmacSHA256";

    private Hashing() {
    }

    public static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(SHA_256).digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(key, HMAC_SHA_256));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String hmacSha256Hex(byte[] key, String value) {
        return HexFormat.of().formatHex(hmacSha256(key, value.getBytes(StandardCharsets.UTF_8)));
    }
}
