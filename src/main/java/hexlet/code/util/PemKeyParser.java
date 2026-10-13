package hexlet.code.util;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * Parses a PKCS#8 RSA private key in PEM format and derives the matching public key.
 *
 * <p>Tolerates literal {@code \n} sequences, which some platforms (Render, Docker env vars) deliver
 * instead of real newlines in multiline variables.
 */
public final class PemKeyParser {

    private static final String BEGIN = "-----BEGIN PRIVATE KEY-----";
    private static final String END = "-----END PRIVATE KEY-----";

    private PemKeyParser() {}

    public static KeyPair parse(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("PEM must not be null or blank");
        }

        String normalized = pem.replace("\\n", "\n");

        if (!normalized.contains(BEGIN) || !normalized.contains(END)) {
            throw new IllegalArgumentException(
                    "PEM must contain '" + BEGIN + "' and '" + END + "'");
        }

        String base64 = normalized.replace(BEGIN, "").replace(END, "").replaceAll("\\s", "");

        try {
            byte[] der = Base64.getDecoder().decode(base64);
            var keyFactory = KeyFactory.getInstance("RSA");
            var privateKey =
                    (RSAPrivateCrtKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(der));
            var publicKey =
                    keyFactory.generatePublic(
                            new RSAPublicKeySpec(
                                    privateKey.getModulus(), privateKey.getPublicExponent()));
            return new KeyPair(publicKey, privateKey);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to parse PEM as RSA private key", e);
        }
    }
}
