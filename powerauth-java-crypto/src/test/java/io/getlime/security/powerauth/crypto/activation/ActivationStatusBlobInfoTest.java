/*
 * Copyright 2018 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.getlime.security.powerauth.crypto.activation;

import io.getlime.security.powerauth.crypto.client.activation.PowerAuthClientActivation;
import io.getlime.security.powerauth.crypto.lib.config.PowerAuthConfiguration;
import io.getlime.security.powerauth.crypto.lib.generator.KeyGenerator;
import io.getlime.security.powerauth.crypto.lib.model.ActivationStatusBlobInfo;
import io.getlime.security.powerauth.crypto.lib.util.AESEncryptionUtils;
import io.getlime.security.powerauth.crypto.server.activation.PowerAuthServerActivation;
import io.getlime.security.powerauth.crypto.server.keyfactory.PowerAuthServerKeyFactory;
import io.getlime.security.powerauth.provider.CryptoProviderUtilFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.Security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test activation status blob.
 */
public class ActivationStatusBlobInfoTest {

    private final PowerAuthServerKeyFactory powerAuthServerKeyFactory = new PowerAuthServerKeyFactory();

    /**
     * Add crypto providers.
     */
    @Before
    public void setUp() {
        // Add Bouncy Castle Security Provider
        Security.addProvider(new BouncyCastleProvider());
        PowerAuthConfiguration.INSTANCE.setKeyConvertor(CryptoProviderUtilFactory.getCryptoProviderUtils());
    }

    @Test
    public void testActivationStatusBlob() throws  InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        final PowerAuthServerActivation serverActivation = new PowerAuthServerActivation();
        final PowerAuthClientActivation clientActivation = new PowerAuthClientActivation();
        // Simulate generating of device and server key pairs
        final KeyGenerator keyGenerator = new KeyGenerator();
        final KeyPair keyPairDevice = keyGenerator.generateKeyPair();
        final KeyPair keyPairServer = keyGenerator.generateKeyPair();
        // Compute shared master secret key
        final SecretKey masterSecretKey = powerAuthServerKeyFactory.generateServerMasterSecretKey(keyPairServer.getPrivate(), keyPairDevice.getPublic());
        // Derive transport key
        final SecretKey transportKey = powerAuthServerKeyFactory.generateServerTransportKey(masterSecretKey);
        // Encrypt status blob with transport key
        byte[] encryptedStatusBlob = serverActivation.encryptedStatusBlob((byte) 3,(byte) 2,(byte) 3,(byte) 1,(byte) 5, transportKey);
        // Decrypt status blob with transport key
        AESEncryptionUtils aes = new AESEncryptionUtils();
        byte[] zeroIv = new byte[16];
        byte[] statusBlob = aes.decrypt(encryptedStatusBlob, zeroIv, transportKey, "AES/CBC/NoPadding");
        // Status blob bytes 0 ... 14 are deterministic, verify them
        ByteBuffer buffer = ByteBuffer.wrap(statusBlob);
        assertEquals(ActivationStatusBlobInfo.ACTIVATION_STATUS_MAGIC_VALUE, buffer.getInt(0));
        assertEquals((byte) 3, buffer.get(4));
        assertEquals((byte) 2, buffer.get(5));
        assertEquals((byte) 3, buffer.get(6));
        assertEquals((byte) 0, buffer.get(7));
        assertEquals((byte) 0, buffer.get(8));
        assertEquals((byte) 0, buffer.get(9));
        assertEquals((byte) 0, buffer.get(10));
        assertEquals((byte) 0, buffer.get(11));
        assertEquals((byte) 0, buffer.get(12));
        assertEquals((byte) 1, buffer.get(13));
        assertEquals((byte) 5, buffer.get(14));
        // Verify decoded status blob used in client activation
        final ActivationStatusBlobInfo statusBlobDecoded = clientActivation.getStatusFromEncryptedBlob(encryptedStatusBlob, transportKey);
        assertEquals(3, statusBlobDecoded.getActivationStatus());
        assertEquals(2, statusBlobDecoded.getCurrentVersion());
        assertEquals(3, statusBlobDecoded.getUpgradeVersion());
        assertEquals(1, statusBlobDecoded.getFailedAttempts());
        assertEquals(5, statusBlobDecoded.getMaxFailedAttempts());
        assertTrue(statusBlobDecoded.isValid());
    }
}
