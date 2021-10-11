/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.crypto;

import javax.crypto.SecretKey;

/**
 *  TODO add other relevant methods here
 */
public interface KeyStoreBasedProtector extends Protector {

    String getKeyStorePath();

    // TODO consider what to do with this method
    String getSecretKeyDigest(SecretKey key) throws EncryptionException;
}
