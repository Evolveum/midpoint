/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.util;

import org.springframework.security.saml.key.KeyType;
import org.springframework.security.saml.key.SimpleKey;

/**
 * @author skublik
 */

public class KeyStoreKey extends SimpleKey {

    private String keyAlias;
    private String keyPassword;
    private String keyStorePath;
    private String keyStorePassword;
    private KeyType type;

    public KeyStoreKey() {

    }

    public KeyStoreKey(String keyAlias, String keyPassword, String keyStorePath, String keyStorePassword, KeyType type) {
        this.keyAlias = keyAlias;
        this.keyPassword = keyPassword;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.type = type;

    }

    @Override
    public String getName() {
        return getKeyAlias();
    }

    @Override
    public SimpleKey setName(String name) {
        return setKeyAlias(name);
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public KeyStoreKey setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
        return this;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public KeyStoreKey setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
        return this;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public KeyStoreKey setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
        return this;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public KeyStoreKey setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
        return this;
    }

    public KeyType getType() {
        return type;
    }

    public KeyStoreKey setType(KeyType type) {
        this.type = type;
        return this;
    }

    public SimpleKey clone(String alias, KeyType type) {
        return new KeyStoreKey(alias, getKeyPassword(), getKeyStorePath(), getKeyStorePassword(), type);
    }
}
