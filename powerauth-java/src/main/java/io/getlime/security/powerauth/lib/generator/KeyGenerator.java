/**
 * Copyright 2015 Lime - HighTech Solutions s.r.o.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.getlime.security.powerauth.lib.generator;

import io.getlime.security.powerauth.lib.config.PowerAuthConfiguration;
import io.getlime.security.powerauth.lib.util.AESEncryptionUtils;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of a high-level key generator class. Keys are generated
 * using elliptic curves (EC) and unless explicitly stated otherwise, they
 * have 128b length.
 *
 * @author Petr Dvorak
 *
 */
public class KeyGenerator {

    private final SecureRandom random = new SecureRandom();

    /**
     * Generate a new ECDH key pair using P256r1 curve.
     *
     * @return A new key pair instance, or null in case of an error.
     */
    public KeyPair generateKeyPair() {
        try {
            // we assume BouncyCastle provider
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDH", PowerAuthConfiguration.INSTANCE.getKeyConvertor().getProviderName());
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair kp = kpg.generateKeyPair();
            return kp;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Computes a pre-shared key for given private key and public key (ECDH).
     *
     * @param privateKey A private key.
     * @param publicKey A public key.
     * @return A new instance of the pre-shared key.
     * @throws InvalidKeyException One of the provided keys are not valid keys.
     */
    public SecretKey computeSharedKey(PrivateKey privateKey, PublicKey publicKey) throws InvalidKeyException {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", PowerAuthConfiguration.INSTANCE.getKeyConvertor().getProviderName());
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(publicKey, true);
            final byte[] sharedSecret = keyAgreement.generateSecret();
            final byte[] resultSecret = this.convert32Bto16B(sharedSecret);
            return PowerAuthConfiguration.INSTANCE.getKeyConvertor().convertBytesToSharedSecretKey(resultSecret);
        } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Convert 32B byte long array to 16B long array by applying xor between first half and second half values.
     * @param original Original byte array, 32B long.
     * @return Result byte array, 16B long.
     */
    public byte[] convert32Bto16B(byte[] original) throws IllegalArgumentException {
        if (original.length != 32) {
            throw new IllegalArgumentException("Invalid byte array size, expected: 32, provided: " + original.length);
        }
        byte[] resultSecret = new byte[16];
        for (int i = 0; i < 16; i++) {
            resultSecret[i] = (byte) (original[i] ^ original[i + 16]);
        }
        return resultSecret;
    }

    /**
     * Generate a new random byte array with given length.
     *
     * @param len Number of random bytes to be generated.
     * @return An array with len random bytes.
     */
    public byte[] generateRandomBytes(int len) {
        byte[] randomBytes = new byte[len];
        random.nextBytes(randomBytes);
        return randomBytes;
    }

    /**
     * Generate a new random symmetric key.
     *
     * @return A new instance of a symmetric key.
     */
    public SecretKey generateRandomSecretKey() {
        return PowerAuthConfiguration.INSTANCE.getKeyConvertor().convertBytesToSharedSecretKey(generateRandomBytes(16));
    }

    /**
     * Derives a new secret key KEY_SHARED from a master secret key KEY_MASTER
     * based on following KDF:
     *
     * BYTES = index, padded from left with 0x00, total 16 bytes
     * KEY_SHARED[BYTES] = AES(BYTES, KEY_MASTER)
     *
     * @param secret A master shared key
     * @param index An index of the key
     * @return A new derived key from a master key with given index.
     */
    public SecretKey deriveSecretKey(SecretKey secret, long index) {
        try {
            AESEncryptionUtils aes = new AESEncryptionUtils();
            byte[] bytes = ByteBuffer.allocate(16).putLong(0L).putLong(index).array();
            byte[] iv = new byte[16];
            byte[] encryptedBytes = aes.encrypt(bytes, iv, secret);
            return PowerAuthConfiguration.INSTANCE.getKeyConvertor().convertBytesToSharedSecretKey(Arrays.copyOf(encryptedBytes, 16));
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Derive a long AES suitable key from a password and salt. Uses PBKDF with
     * 10 000 iterations.
     *
     * @param password A password used for key derivation
     * @param salt A salt used for key derivation
     * @return A new secret key derived from the password.
     */
    public SecretKey deriveSecretKeyFromPassword(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PowerAuthConfiguration.PBKDF_ITERATIONS, 128);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1", PowerAuthConfiguration.INSTANCE.getKeyConvertor().getProviderName());
            byte[] keyBytes = skf.generateSecret(spec).getEncoded();
            SecretKey encryptionKey = new SecretKeySpec(keyBytes, "AES/ECB/NoPadding");
            return encryptionKey;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException ex) {
            Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
