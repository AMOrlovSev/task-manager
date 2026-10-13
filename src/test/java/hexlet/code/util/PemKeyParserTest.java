package hexlet.code.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class PemKeyParserTest {

    @Test
    void parsesPemWithRealNewlines() throws Exception {
        var pair = generateRsaKeyPair();
        String pem = toPem(pair);

        KeyPair parsed = PemKeyParser.parse(pem);

        assertThat(parsed.getPrivate()).isNotNull();
        assertThat(parsed.getPublic()).isNotNull();
        assertThat(parsed.getPrivate().getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void parsesPemWithEscapedNewlines() throws Exception {
        var pair = generateRsaKeyPair();
        String pem = toPem(pair).replace("\n", "\\n");

        KeyPair parsed = PemKeyParser.parse(pem);

        assertThat(parsed.getPrivate()).isNotNull();
        assertThat(parsed.getPublic()).isNotNull();
    }

    @Test
    void parsesPemWithBlankLinesInside() throws Exception {
        var pair = generateRsaKeyPair();
        String pem = toPem(pair).replace("\n", "\n\n");

        KeyPair parsed = PemKeyParser.parse(pem);

        assertThat(parsed.getPrivate()).isNotNull();
    }

    @Test
    void parsedKeysCanSignAndVerify() throws Exception {
        var pair = generateRsaKeyPair();
        String pem = toPem(pair).replace("\n", "\\n");

        KeyPair parsed = PemKeyParser.parse(pem);

        byte[] payload = "hello".getBytes();
        var signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(parsed.getPrivate());
        signer.update(payload);
        byte[] sig = signer.sign();

        var verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(parsed.getPublic());
        verifier.update(payload);

        assertThat(verifier.verify(sig)).isTrue();
    }

    @Test
    void parsedPublicKeyMatchesOriginal() throws Exception {
        var pair = generateRsaKeyPair();
        String pem = toPem(pair);

        KeyPair parsed = PemKeyParser.parse(pem);

        assertThat(parsed.getPublic().getEncoded()).isEqualTo(pair.getPublic().getEncoded());
        assertThat(parsed.getPrivate().getEncoded()).isEqualTo(pair.getPrivate().getEncoded());
    }

    @Test
    void nullPemThrows() {
        assertThatThrownBy(() -> PemKeyParser.parse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PEM");
    }

    @Test
    void blankPemThrows() {
        assertThatThrownBy(() -> PemKeyParser.parse("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PEM");
    }

    @Test
    void emptyStringPemThrows() {
        assertThatThrownBy(() -> PemKeyParser.parse(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PEM");
    }

    @Test
    void missingHeaderThrows() {
        assertThatThrownBy(() -> PemKeyParser.parse("just some text"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PRIVATE KEY");
    }

    @Test
    void invalidBase64Throws() {
        String pem =
                """
                -----BEGIN PRIVATE KEY-----
                %%%not-base64%%%
                -----END PRIVATE KEY-----
                """;

        assertThatThrownBy(() -> PemKeyParser.parse(pem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to parse PEM");
    }

    @Test
    void garbageInsideHeadersThrows() {
        String pem =
                """
                -----BEGIN PRIVATE KEY-----
                aGVsbG8gd29ybGQ=
                -----END PRIVATE KEY-----
                """;

        // base64 валиден ("hello world"), но это не PKCS8 RSA
        assertThatThrownBy(() -> PemKeyParser.parse(pem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to parse PEM");
    }

    @Test
    void publicKeyPemRejectedAsPrivateKey() throws Exception {
        var pair = generateRsaKeyPair();
        String publicPem = toPublicPem(pair);

        assertThatThrownBy(() -> PemKeyParser.parse(publicPem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PRIVATE KEY");
    }

    // ============================ helpers ============================

    private static KeyPair generateRsaKeyPair() throws Exception {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    private static String toPem(KeyPair pair) {
        String base64 =
                Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(pair.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n";
    }

    private static String toPublicPem(KeyPair pair) {
        String base64 =
                Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(pair.getPublic().getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----\n";
    }
}
