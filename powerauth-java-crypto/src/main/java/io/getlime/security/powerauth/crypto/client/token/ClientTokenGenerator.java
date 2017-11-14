/*
 * Copyright 2016 Lime - HighTech Solutions s.r.o.
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
package io.getlime.security.powerauth.crypto.client.token;

import io.getlime.security.powerauth.crypto.lib.util.TokenUtils;


/**
 * Class that simplifies working with tokens on the client side.
 *
 * @author Petr Dvorak, petr@lime-company.eu
 */
public class ClientTokenGenerator {

    private TokenUtils tokenUtils = new TokenUtils();

    /**
     * Generate random token nonce, 16 random bytes.
     * @return Random token nonce.
     */
    public byte[] generateTokenNonce() {
        return tokenUtils.generateTokenNonce();
    }

    /**
     * Helper method to get current timestamp for the purpose of token timestamping, encoded as 8 bytes.
     * @return Current timestamp in milliseconds.
     */
    public byte[] generateTokenTimestamp() {
        return tokenUtils.generateTokenTimestamp();
    }

    /**
     * Compute the digest of provided token information using given token secret.
     *
     * @param nonce Token nonce, 16 random bytes.
     * @param timestamp Token timestamp, Unix timestamp format encoded as 8 bytes.
     * @param tokenSecret Token secret, 16 random bytes.
     * @return Token digest computed using provided data bytes with given token secret.
     */
    public byte[] computeTokenDigest(byte[] nonce, byte[] timestamp, byte[] tokenSecret) {
        return tokenUtils.computeTokenDigest(nonce, timestamp, tokenSecret);
    }

}
