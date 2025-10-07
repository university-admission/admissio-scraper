package org.admissio.scraper.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class VstupDataDecryptor {

    private static final String IV_SOURCE = "2025";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private VstupDataDecryptor() {}

    public static String decrypt(String base64EncryptedData, String dynamicKeyPart) throws Exception {
        if (base64EncryptedData == null || base64EncryptedData.isEmpty()) {
            return base64EncryptedData;
        }

        String keyHex = sha256ToHex(dynamicKeyPart).substring(0, 32);
        SecretKeySpec secretKey = new SecretKeySpec(keyHex.getBytes(StandardCharsets.UTF_8), ALGORITHM);

        String ivHex = sha256ToHex(IV_SOURCE).substring(0, 16);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivHex.getBytes(StandardCharsets.UTF_8));

        byte[] initialDecodedBytes = Base64.getDecoder().decode(base64EncryptedData);
        String malformedString = new String(initialDecodedBytes, StandardCharsets.UTF_8);
        byte[] finalCiphertextBytes = Base64.getDecoder().decode(malformedString);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

        byte[] decryptedBytes = cipher.doFinal(finalCiphertextBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private static String sha256ToHex(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}