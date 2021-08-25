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
    private String fixedSalt;

    public ProtectorConfiguration(Configuration configuration) {
        this.setKeyStorePath(configuration.getString("keyStorePath"));
        this.setKeyStorePassword(configuration.getString("keyStorePassword"));
        this.setEncryptionKeyAlias(configuration.getString("encryptionKeyAlias"));
        this.setXmlCipher(configuration.getString("xmlCipher"));
        this.setFixedSalt(configuration.getString("fixedSalt"));
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

    /**
     * Returns fixed salt value, used for all salt-hashed values.
     *
     * WARNING: USE OF FIXED SALT IS DANGEROUS.
     * Fixed salt makes all the salting ineffective.
     * DO NOT USE unless you really know what you are doing, and if you want to sacrifice security for convenience.
     *
     * Fixed salt was implemented to allow new scenarios when using hashed password storage.
     * E.g. with fixed salt passive-cached credentials in ShadowTypes can be compared to password history in UserType
     * as hashes for same password values are equivalent.
     * New config.xml option keystore/fixedSalt need to be set to any arbitrary value and is used globally as
     * single fixed salt for all hashed values in midPoint.
     */
    public String getFixedSalt() {
        return fixedSalt;
    }

    /**
     * Sets the fixed salt value, used for all salt-hashed values.
     *
     * WARNING: USE OF FIXED SALT IS DANGEROUS.
     * Fixed salt makes all the salting ineffective.
     * DO NOT USE unless you really know what you are doing, and if you want to sacrifice security for convenience.
     *
     * Fixed salt was implemented to allow new scenarios when using hashed password storage.
     * E.g. with fixed salt passive-cached credentials in ShadowTypes can be compared to password history in UserType
     * as hashes for same password values are equivalent.
     * New config.xml option keystore/fixedSalt need to be set to any arbitrary value and is used globally as
     * single fixed salt for all hashed values in midPoint.
     */
    public void setFixedSalt(String fixedSalt) {
        this.fixedSalt = fixedSalt;
    }
}
