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
    void parsesPrivateKeyWithRealNewlines() throws Exception {
        var pair = generateRsaKeyPair();
        String pem = toPem("PRIVATE KEY", pair.getPrivate().getEncoded());

        var parsed = PemKeyParser.parsePrivateKey(pem);

        assertThat(parsed).isNotNull();
        assertThat(parsed.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void parsesPrivateKeyWithEscapedNewlines() throws Exception {
        var pair = generateRsaKeyPair();
        String pem = toPem("PRIVATE KEY", pair.getPrivate().getEncoded()).replace("\n", "\\n");

        var parsed = PemKeyParser.parsePrivateKey(pem);

        assertThat(parsed).isNotNull();
    }

    @Test
    void parsesPublicKeyWithEscapedNewlines() throws Exception {
        var pair = generateRsaKeyPair();
        String pem = toPem("PUBLIC KEY", pair.getPublic().getEncoded()).replace("\n", "\\n");

        var parsed = PemKeyParser.parsePublicKey(pem);

        assertThat(parsed).isNotNull();
        assertThat(parsed.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void parsedKeysCanSignAndVerify() throws Exception {
        var pair = generateRsaKeyPair();
        String privPem = toPem("PRIVATE KEY", pair.getPrivate().getEncoded());
        String pubPem = toPem("PUBLIC KEY", pair.getPublic().getEncoded()).replace("\n", "\\n");

        var privateKey = PemKeyParser.parsePrivateKey(privPem);
        var publicKey = PemKeyParser.parsePublicKey(pubPem);

        byte[] payload = "hello".getBytes();
        var signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(payload);
        byte[] sig = signer.sign();

        var verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(payload);

        assertThat(verifier.verify(sig)).isTrue();
    }

    @Test
    void nullPemThrows() {
        assertThatThrownBy(() -> PemKeyParser.parsePrivateKey(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PEM");
    }

    @Test
    void emptyPemThrows() {
        assertThatThrownBy(() -> PemKeyParser.parsePrivateKey("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingHeaderThrows() {
        assertThatThrownBy(() -> PemKeyParser.parsePrivateKey("just some text"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PRIVATE KEY");
    }

    @Test
    void invalidBase64Throws() {
        var pem =
                """
                -----BEGIN PRIVATE KEY-----
                %%%not-base64%%%
                -----END PRIVATE KEY-----
                """;

        assertThatThrownBy(() -> PemKeyParser.parsePrivateKey(pem))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void wrongTypeHeaderThrows() {
        var pair = generateRsaKeyPairQuiet();
        String publicPem = toPem("PUBLIC KEY", pair.getPublic().getEncoded());

        // Публичный PEM передан в parsePrivateKey — заголовок не тот
        assertThatThrownBy(() -> PemKeyParser.parsePrivateKey(publicPem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PRIVATE KEY");
    }

    // ============================ helpers ============================

    private static KeyPair generateRsaKeyPair() throws Exception {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    private static KeyPair generateRsaKeyPairQuiet() {
        try {
            return generateRsaKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String toPem(String type, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }
}
