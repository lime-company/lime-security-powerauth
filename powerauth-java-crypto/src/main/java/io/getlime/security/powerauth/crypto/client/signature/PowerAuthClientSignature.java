/*
 * Copyright 2016 Wultra s.r.o.
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
package io.getlime.security.powerauth.crypto.client.signature;

import io.getlime.security.powerauth.crypto.lib.util.SignatureUtils;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Class implementing client-side signature related processes.
 *
 * @author Petr Dvorak
 *
 */
public class PowerAuthClientSignature {

    private final SignatureUtils signatureUtils = new SignatureUtils();

    /**
     * Compute a PowerAuth 2.0 signature for given data, signature keys and
     * counter. Signature keys are symmetric keys deduced using
     * private device key KEY_DEVICE_PRIVATE and server public key
     * KEY_SERVER_PUBLIC, and then using KDF function with proper index. See
     * PowerAuth protocol specification for details.
     *
     * PowerAuth protocol version: 2.0
     *
     * @deprecated Use {@link #signatureForData(byte[], List, byte[])}.
     *
     * @param data Data to be signed.
     * @param signatureKeys A signature keys.
     * @param ctr Numeric counter / index of the derived key KEY_DERIVED.
     * @return PowerAuth 2.0 signature for given data.
     */
    @Deprecated
    public String signatureForData(byte[] data, List<SecretKey> signatureKeys, long ctr) {
        return signatureUtils.computePowerAuthSignature(data, signatureKeys, ctr);
    }

    /**
     * Compute a PowerAuth 3.0 signature for given data, signature keys and
     * counter. Signature keys are symmetric keys deduced using
     * private device key KEY_DEVICE_PRIVATE and server public key
     * KEY_SERVER_PUBLIC, and then using KDF function with proper index. See
     * PowerAuth protocol specification for details.
     *
     * PowerAuth protocol version: 3.0
     *
     * @param data Data to be signed.
     * @param signatureKeys A signature keys.
     * @param ctrData Hash based counter / index of the derived key KEY_DERIVED.
     * @return PowerAuth 3.0 signature for given data.
     */
    public String signatureForData(byte[] data, List<SecretKey> signatureKeys, byte[] ctrData) {
        return signatureUtils.computePowerAuthSignature(data, signatureKeys, ctrData);
    }

}
