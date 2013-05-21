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
package com.evolveum.midpoint.common.crypto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.xml.security.Init;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.utils.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3._2000._09.xmldsig.KeyInfoType;
import org.w3._2001._04.xmlenc.EncryptedDataType;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.SecretKey;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Class that manages encrypted string values. Java Cryptography Extension is
 * needed because this class is using AES-256 for encrypting/decrypting xml
 * data.
 *
 * @author Radovan Semancik
 * @author lazyman
 */
public class AESProtector implements Protector {

    private static final Trace LOGGER = TraceManager.getTrace(AESProtector.class);
    private static final QName QNAME_KEY_NAME = new QName("http://www.w3.org/2000/09/xmldsig#", "KeyName");
    private static final QName QNAME_ENCRYPTED_DATA = new QName("http://www.w3.org/2001/04/xmlenc#",
            "EncryptedData");
    private static final String KEY_DIGEST_TYPE = "SHA1";
    private static final char[] KEY_PASSWORD = "midpoint".toCharArray();
    private static final String ENCRYPTED_ELEMENT_NAME = "value";

    private String keyStorePath;
    private String keyStorePassword;
    private String encryptionKeyAlias = "default";
    private String xmlCipher;

    private List<TrustManager> trustManagers;

    private static final KeyStore keyStore;

    @Autowired(required = true)
    private PrismContext prismContext;

    static {
        Init.init();
        try {
            keyStore = KeyStore.getInstance("jceks");
        } catch (KeyStoreException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    /**
     * @throws SystemException if jceks keystore is not available on {@link AESProtector#getKeyStorePath}
     */
    public void init() {
        InputStream stream = null;
        try {
            // Test if use file or classpath resource
            File f = new File(getKeyStorePath());
            if (f.exists()) {
                LOGGER.info("Using file keystore at {}", getKeyStorePath());
                if (!f.canRead()) {
                    LOGGER.error("Provided keystore file {} is unreadable.", getKeyStorePath());
                    throw new EncryptionException("Provided keystore file " + getKeyStorePath()
                            + " is unreadable.");
                }
                stream = new FileInputStream(f);

                // Use class path keystore
            } else {
                LOGGER.warn("Using default keystore from classpath ({}).", getKeyStorePath());
                // Read from class path

                stream = AESProtector.class.getClassLoader().getResourceAsStream(getKeyStorePath());
                // ugly dirty hack to have second chance to find keystore on
                // class path
                if (stream == null) {
                    stream = AESProtector.class.getClassLoader().getResourceAsStream(
                            "com/../../" + getKeyStorePath());
                }
            }
            // Test if we have valid stream
            if (stream == null) {
                throw new EncryptionException("Couldn't load keystore as resource '" + getKeyStorePath()
                        + "'");
            }
            // Load keystore
            keyStore.load(stream, getKeyStorePassword().toCharArray());
            stream.close();

            // Initialze trust manager list

            TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmFactory.init(keyStore);
            trustManagers = new ArrayList<TrustManager>();
            for (TrustManager trustManager : tmFactory.getTrustManagers()) {
                trustManagers.add(trustManager);
            }


        } catch (Exception ex) {
            LOGGER.error("Unable to work with keystore {}, reason {}.",
                    new Object[]{getKeyStorePath(), ex.getMessage()}, ex);
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    public String getXmlCipher() {
        if (xmlCipher == null) {
            xmlCipher = XMLCipher.AES_128;
        }
        return xmlCipher;
    }

    public void setXmlCipher(String xmlCipher) {
        this.xmlCipher = xmlCipher;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    /**
     * @return the encryptionKeyAlias
     * @throws IllegalStateException if encryption key digest is null or empty string
     */
    private String getEncryptionKeyAlias() {
        if (StringUtils.isEmpty(encryptionKeyAlias)) {
            throw new IllegalStateException("Encryption key alias was not defined (is null or empty).");
        }
        return encryptionKeyAlias;
    }

    /**
     * @param encryptionKeyAlias Alias of the encryption key {@link SecretKey} which is used
     *                           for encryption
     * @throws IllegalArgumentException if encryption key digest is null or empty string
     */
    public void setEncryptionKeyAlias(String encryptionKeyAlias) {
        Validate.notEmpty(encryptionKeyAlias, "Encryption key alias must not be null or empty.");
        this.encryptionKeyAlias = encryptionKeyAlias;
    }

    /**
     * @param keyStorePassword
     * @throws IllegalArgumentException if keystore password is null string
     */
    public void setKeyStorePassword(String keyStorePassword) {
        Validate.notNull(keyStorePassword, "Keystore password must not be null.");
        this.keyStorePassword = keyStorePassword;
    }

    private String getKeyStorePassword() {
        if (keyStorePassword == null) {
            throw new IllegalStateException("Keystore password was not defined (null).");
        }
        return keyStorePassword;
    }

    /**
     * @param keyStorePath
     * @throws IllegalArgumentException if keystore path is null string
     */
    public void setKeyStorePath(String keyStorePath) {
        Validate.notEmpty(keyStorePath, "Key store path must not be null.");
        this.keyStorePath = keyStorePath;
    }

    private String getKeyStorePath() {
        if (StringUtils.isEmpty(keyStorePath)) {
            throw new IllegalStateException("Keystore path was not defined (is null or empty).");
        }
        return keyStorePath;
    }

    /*
      * (non-Javadoc)
      *
      * @see
      * com.evolveum.midpoint.common.crypto.Protector#decryptString(com.evolveum
      * .midpoint.xml.ns._public.common.common_1.ProtectedStringType)
      */
    @Override
    public String decryptString(ProtectedStringType protectedString) throws EncryptionException {
        Element plain = decrypt(protectedString);
        if (plain == null) {
            return null;
        }

        return plain.getTextContent();
    }

    /*
      * (non-Javadoc)
      *
      * @see
      * com.evolveum.midpoint.common.crypto.Protector#decrypt(com.evolveum.midpoint
      * .xml.ns._public.common.common_1.ProtectedStringType)
      */
    @Override
    @SuppressWarnings("unchecked")
    public Element decrypt(ProtectedStringType protectedString) throws EncryptionException {
        Validate.notNull(protectedString, "Protected string must not be null.");
        EncryptedDataType encrypted = protectedString.getEncryptedData();
        String clearValue = protectedString.getClearValue();
        if (encrypted == null && clearValue != null) {
        	// Return clear value if there is one and there is no encrypted value
        	Document document = DOMUtil.getDocument();
        	Element element = DOMUtil.createElement(document, ProtectedStringType.F_CLEAR_VALUE);
        	element.setTextContent(clearValue);
            document.appendChild(element);
            return element;
        }
        Validate.notNull(encrypted, "Encrypted data must not be null.");

        if (encrypted.getCipherData() == null
                || ((encrypted.getCipherData().getCipherValue() == null
                || encrypted.getCipherData().getCipherValue().length == 0)
                && (encrypted.getCipherData().getCipherReference() == null
                || encrypted.getCipherData().getCipherReference().getURI() == null))) {
            // The javax.crypto libraries don't handle this will, they throw NPE. Hence the check.
            throw new IllegalArgumentException("CipherData element is missing or empty");
        }

        Document document;
        try {
            String digest = getDefaultSecretKeyDigest();
            if (encrypted.getKeyInfo() != null) {
                KeyInfoType keyInfo = encrypted.getKeyInfo();
                List<Object> infos = keyInfo.getContent();
                for (Object object : infos) {
                    if (!(object instanceof JAXBElement)) {
                        continue;
                    }
                    JAXBElement<Object> info = (JAXBElement<Object>) object;
                    if (QNAME_KEY_NAME.equals(info.getName())) {
                        digest = info.getValue().toString();
                        break;
                    }
                }
            }

            SecretKey secret = getSecretKeyByDigest(digest);
            XMLCipher xmlCipher = XMLCipher.getInstance(getXmlCipher());
            xmlCipher.init(XMLCipher.DECRYPT_MODE, secret);

            document = DOMUtil.getDocument();
            Element element = getJaxbProcessor().marshalObjectToDom(encrypted, QNAME_ENCRYPTED_DATA, document);
            document.appendChild(element);
            document = null;

            document = xmlCipher.doFinal(element.getOwnerDocument(), element);
        } catch (EncryptionException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("Exception during decryption: {}", ex.getMessage(), ex);
            try {
                LOGGER.trace("The input was {}:\n{}", protectedString, getJaxbProcessor().marshalObjectToDom(protectedString, SchemaConstants.C_PROTECTED_STRING, DOMUtil.getDocument()));
            } catch (JAXBException e) {
                LOGGER.trace("Error marshalling the input {}: {}", protectedString, e.getMessage());
            }
            throw new EncryptionException(ex.getMessage(), ex);
        }

        if (document == null) {
            return null;
        }

        return document.getDocumentElement();
    }

    /*
      * (non-Javadoc)
      *
      * @see
      * com.evolveum.midpoint.common.crypto.Protector#encryptString(java.lang
      * .String)
      */
    @Override
    public ProtectedStringType encryptString(String text) throws EncryptionException {
        if (StringUtils.isEmpty(text)) {
            return null;
        }

        return encrypt(stringToElement(text));
    }

    private Element stringToElement(String text) {
        Document document = DOMUtil.getDocument();
        Element plain = document.createElement(ENCRYPTED_ELEMENT_NAME);
        plain.setTextContent(text);
        document.appendChild(plain);
        return plain;
    }

    /*
      * (non-Javadoc)
      *
      * @see
      * com.evolveum.midpoint.common.crypto.Protector#encrypt(org.w3c.dom.Element
      * )
      */
    @Override
    public ProtectedStringType encrypt(Element plain) throws EncryptionException {
        return encrypt(plain, new ProtectedStringType());
    }

    private ProtectedStringType encrypt(Element plain, ProtectedStringType protectedString)
            throws EncryptionException {
        if (plain == null) {
            return null;
        }

        try {
            SecretKey secret = getSecretKeyByAlias(getEncryptionKeyAlias());
            XMLCipher xmlCipher = XMLCipher.getInstance(getXmlCipher());
            xmlCipher.init(XMLCipher.ENCRYPT_MODE, secret);

            Document document = plain.getOwnerDocument();
            if (document == null) {
                document = DOMUtil.getDocument();
                document.appendChild(plain);
            }
            KeyInfo keyInfo = new KeyInfo(document);
            keyInfo.addKeyName(getSecretKeyDigest(secret));
            xmlCipher.getEncryptedData().setKeyInfo(keyInfo);

            document = xmlCipher.doFinal(document, plain);
            EncryptedDataType data = getJaxbProcessor().unmarshalToObject(document.getDocumentElement(), EncryptedDataType.class);
            protectedString.setEncryptedData(data);
        } catch (EncryptionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EncryptionException(ex.getMessage(), ex);
        }

        return protectedString;
    }

    /*
      * (non-Javadoc)
      *
      * @see
      * com.evolveum.midpoint.common.crypto.Protector#encrypt(com.evolveum.midpoint
      * .xml.ns._public.common.common_1.ProtectedStringType)
      */
    @Override
    public void encrypt(ProtectedStringType ps) throws EncryptionException {
        String clearValue = ps.getClearValue();
        if (clearValue == null) {
            // nothing to do
            return;
        }
        ps.setClearValue(null);
        encrypt(stringToElement(clearValue), ps);
    }

    private String getSecretKeyDigest(SecretKey key) throws EncryptionException {
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance(KEY_DIGEST_TYPE);
        } catch (NoSuchAlgorithmException ex) {
            throw new EncryptionException(ex.getMessage(), ex);
        }

        return Base64.encode(sha1.digest(key.getEncoded()));
    }

    private String getDefaultSecretKeyDigest() throws EncryptionException {
        return getSecretKeyDigest(getSecretKeyByAlias(getEncryptionKeyAlias()));
    }

    private SecretKey getSecretKeyByAlias(String alias) throws EncryptionException {
        Key key = null;
        try {
            key = keyStore.getKey(alias, KEY_PASSWORD);
        } catch (Exception ex) {
            throw new EncryptionException("Couldn't obtain key '" + alias + "' from keystore, reason: "
                    + ex.getMessage(), ex);
        }

        if (key == null || !(key instanceof SecretKey)) {
            throw new EncryptionException("Key with alias '" + alias
                    + "' is not instance of SecretKey, but '" + key + "'.");
        }

        return (SecretKey) key;
    }

    private SecretKey getSecretKeyByDigest(String digest) throws EncryptionException {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (!keyStore.isKeyEntry(alias)) {
                    continue;
                }

                try {
                    Key key = keyStore.getKey(alias, KEY_PASSWORD);
                    if (!(key instanceof SecretKey)) {
                        continue;
                    }

                    String keyHash = getSecretKeyDigest((SecretKey) key);
                    if (digest.equals(keyHash)) {
                        return (SecretKey) key;
                    }
                } catch (UnrecoverableKeyException ex) {
                    LOGGER.trace("Couldn't recover key {} from keystore, reason: {}", new Object[]{alias,
                            ex.getMessage()});
                }
            }
        } catch (Exception ex) {
            throw new EncryptionException(ex.getMessage(), ex);
        }

        throw new EncryptionException("Key '" + digest + "' is not in keystore.");
    }

    @Override
    public boolean isEncrypted(ProtectedStringType ps) {
        if (ps == null) {
            return false;
        }
        if (ps.getEncryptedData() == null) {
            return false;
        }
        // TODO
        return true;
    }

    @Override
    public List<TrustManager> getTrustManagers() {
        return trustManagers;
    }

    private PrismJaxbProcessor getJaxbProcessor() {
        return prismContext.getPrismJaxbProcessor();
    }
}
