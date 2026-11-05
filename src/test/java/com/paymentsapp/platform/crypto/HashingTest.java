package com.paymentsapp.platform.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HashingTest {

    private static final byte[] KEY = "test-key".getBytes(StandardCharsets.UTF_8);

    @Test
    void sha256MatchesKnownVector() {
        assertThat(Hashing.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void hmacDependsOnKey() {
        String card = "2200700123473752";
        assertThat(Hashing.hmacSha256Hex(KEY, card)).hasSize(64)
                .isEqualTo(Hashing.hmacSha256Hex(KEY, card))
                .isNotEqualTo(Hashing.hmacSha256Hex("other-key".getBytes(StandardCharsets.UTF_8), card))
                .isNotEqualTo(Hashing.sha256Hex(card));
    }
}
