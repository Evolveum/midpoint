/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.init;

import org.apache.commons.configuration.Configuration;

/**
 * @author lazyman
 */
public class ProtectorConfiguration {

    private String keyStorePath;
    private String keyStorePassword;
    private String encryptionKeyAlias;
    private String xmlCipher;

    public ProtectorConfiguration(Configuration configuration) {
        this.setKeyStorePath(configuration.getString("protectorClass"));
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
