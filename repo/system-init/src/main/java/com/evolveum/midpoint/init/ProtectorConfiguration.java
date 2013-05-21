/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
