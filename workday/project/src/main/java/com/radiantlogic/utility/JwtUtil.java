package com.radiantlogic.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.radiantlogic.exception.WorkdayException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;


/**
 * Utility class for handling JWT (JSON Web Token) operations.
 *
 * <p>This class provides helper methods for encoding JWT headers and payloads,
 * generating signed tokens, and supporting authentication workflows. It is designed as a stateless
 * utility and should not be instantiated.
 */
@Slf4j
public final class JwtUtil {

  /**
   * Cryptographic signing algorithm used to generate the JWT signature.
   *
   * <p>This specifies the RSA-based SHA-256 algorithm used to sign the token,
   * ensuring integrity and authenticity of the generated JWT.
   */
  private static final String SIGNING_ALGORITHM = "SHA256withRSA";

  /**
   * Base64 encoder configured for URL-safe encoding without padding.
   *
   * <p>Used for encoding JWT header and payload segments as per JWT specification,
   * ensuring the encoded output is safe for use in URLs and HTTP requests.
   */
  private static final Base64.Encoder URL_ENCODER =
      Base64.getUrlEncoder().withoutPadding();

  /**
   * Adding a private no-args constructor to prevent instantiation.
   */
  private JwtUtil() {

  }

  /**
   * Extracts the expiry time (in seconds) from the given JWT token.
   *
   * <p>This method parses the token payload and retrieves the expiration ("exp") claim,
   * which indicates when the token will become invalid.
   *
   * @param token the JWT token from which expiry information is extracted
   * @return the expiry time in seconds since epoch
   * @throws JsonProcessingException if the token payload cannot be parsed
   */
  public static Long getExpiryInstant(final String token) throws JsonProcessingException {
    String payload = token.split("\\.")[1];
    String decodedPayload = new String(Base64.getUrlDecoder().decode(payload),
        StandardCharsets.UTF_8);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(decodedPayload);
    long exp = node.path("exp").asLong(-1);
    long iat = node.path("iat").asLong(-1);

    if (exp == -1 || iat == -1) {
      throw new IllegalArgumentException("JWT missing required claims: exp or iat");
    }
    return exp - iat;
  }

  /**
   * Loads an RSA private key from the provided encoded key string.
   *
   * <p>The key is decoded and converted into a {@link PrivateKey} instance suitable for
   * cryptographic operations such as JWT signing.</p>
   *
   * @param privateKey the encoded private key string
   * @return the corresponding {@link PrivateKey} instance
   * @throws NoSuchAlgorithmException if the RSA cryptographic algorithm is not available
   * @throws InvalidKeySpecException  if the key is not valid Base64 encoded data or does not
   *                                  represent a valid PKCS8 RSA private key
   * @throws WorkdayException         if {@code privateKey} is null
   */
  public static PrivateKey loadPrivateKey(final String privateKey)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (privateKey == null) {
      throw new IllegalArgumentException("Private Key cannot be null");
    }
    try {
      String pemContent;
      pemContent = privateKey
          .replace("-----BEGIN PRIVATE KEY-----", "")
          .replace("-----END PRIVATE KEY-----", "")
          .replaceAll("\\s+", "");
      byte[] decoded = Base64.getDecoder().decode(pemContent);
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePrivate(keySpec);
    } catch (IllegalArgumentException e) {
      throw new InvalidKeySpecException("Invalid Base64 encoded private key", e);
    }

  }

  /**
   * Generates a digital signature for the given content using the provided private key.
   *
   * <p>The content is signed using the configured RSA algorithm to ensure
   * integrity and authenticity.
   *
   * @param content          the data to be signed
   * @param loadedPrivateKey the private key used for signing the content
   * @return the generated digital signature as a Base64-encoded string
   * @throws InvalidKeyException      if the provided private key is invalid
   * @throws NoSuchAlgorithmException if the signing algorithm is not available
   * @throws SignatureException       if an error occurs while generating the signature
   */
  public static String sign(final String content, final PrivateKey loadedPrivateKey)
      throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {

    Signature signer = Signature.getInstance(SIGNING_ALGORITHM);
    signer.initSign(loadedPrivateKey);
    signer.update(content.getBytes(StandardCharsets.UTF_8));
    byte[] signatureBytes = signer.sign();
    return URL_ENCODER.encodeToString(signatureBytes);
  }

  /**
   * Encodes the given string value using Base64 URL-safe encoding without padding.
   *
   * <p>This method is primarily used for encoding JWT header and payload segments
   * to ensure they are safe for transmission in URLs and HTTP requests.
   *
   * @param value the input string to be encoded
   * @return the Base64 URL-safe encoded representation of the input value
   */
  public static String getEncodedValue(final String value) {
    return URL_ENCODER.encodeToString(
        value.getBytes(StandardCharsets.UTF_8));
  }
}
