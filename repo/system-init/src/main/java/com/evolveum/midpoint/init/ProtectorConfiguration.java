/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import org.apache.commons.configuration2.Configuration;

/**
 * @author lazyman
 */
public class ProtectorConfiguration {

    private String keyStorePath;
    private String keyStorePassword;
    private String encryptionKeyAlias;
    private String xmlCipher;

    public ProtectorConfiguration(Configuration configuration) {
        this.setKeyStorePath(configuration.getString("keyStorePath"));
        this.setKeyStorePassword(configuration.getString("keyStorePassword"));
        this.setEncryptionKeyAlias(configuration.getString("encryptionKeyAlias"));
        this.setXmlCipher(configuration.getString("xmlCipher"));
    }

    public String getEncryptionKeyAlias() {
        return encryptionKeyAlias;
    }

    public void setEncryptionKeyAlias(String encryptionKeyAlias) {
        this.encryptionKeyAlias = encryptionKeyAlias;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public String getXmlCipher() {
        return xmlCipher;
    }

    public void setXmlCipher(String xmlCipher) {
        this.xmlCipher = xmlCipher;
    }
}
