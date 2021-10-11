/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.web.security.util.KeyStoreKey;
import org.opensaml.security.credential.impl.KeyStoreCredentialResolver;
import org.springframework.security.saml.key.SimpleKey;
import org.springframework.security.saml.spi.opensaml.OpenSamlImplementation;

import java.security.KeyStore;
import java.time.Clock;
import java.util.Collections;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author skublik
 */

public class MidpointOpenSamlImplementation extends OpenSamlImplementation {

    public MidpointOpenSamlImplementation(Clock time) {
        super(time);
    }

    public KeyStoreCredentialResolver getCredentialsResolver(SimpleKey key) {
        KeyStore ks = getSamlKeyStoreProvider().getKeyStore(key);
        Map<String, String> passwords;
        if (key instanceof KeyStoreKey) {
            passwords = Collections.singletonMap(((KeyStoreKey)key).getKeyAlias(), ((KeyStoreKey)key).getKeyPassword());
        } else {
            passwords = hasText(key.getPrivateKey()) ?
                    Collections.singletonMap(key.getName(), key.getPassphrase()) :
                    Collections.emptyMap();
        }
        KeyStoreCredentialResolver resolver = new KeyStoreCredentialResolver(
                ks,
                passwords
        );
        return resolver;
    }
}
