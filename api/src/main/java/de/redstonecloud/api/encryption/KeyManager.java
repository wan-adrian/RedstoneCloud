package de.redstonecloud.api.encryption;

import de.redstonecloud.api.exception.EncryptionException;

import javax.crypto.Cipher;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;

import lombok.Getter;

public final class KeyManager {
    @Getter private static volatile PublicKey publicKey;
    private static volatile PrivateKey privateKey;

    public static PublicKey init() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(4096);

            KeyPair pair = generator.generateKeyPair();
            publicKey = pair.getPublic();
            privateKey = pair.getPrivate();

            return publicKey;
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    public static byte[] encrypt(byte[] message, PublicKey key) {
        Objects.requireNonNull(key, "Encryption keys not initialized");
        return doFinal(message, Cipher.ENCRYPT_MODE, key);
    }

    public static byte[] decrypt(byte[] message) {
        Objects.requireNonNull(privateKey, "Encryption keys not initialized");
        return doFinal(message, Cipher.DECRYPT_MODE, privateKey);
    }

    private static byte[] doFinal(byte[] message, int mode, Key key) {
        try {
            Objects.requireNonNull(message, "message");
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(mode, key);
            return cipher.doFinal(message);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }
}
