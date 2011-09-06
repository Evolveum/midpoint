/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * 
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.UnrecoverableKeyException;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.SecretKey;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.xml.security.Init;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.utils.Base64;
import org.w3._2000._09.xmldsig.KeyInfoType;
import org.w3._2001._04.xmlenc.EncryptedDataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;

/**
 * Class that manages encrypted string values. Java Cryptography Extension is
 * needed because this class is using AES-256 for encrypting/decrypting xml
 * data.
 * 
 * @author Radovan Semancik
 * @author lazyman
 * 
 */
public class AESProtector implements Protector {

	private static final Trace LOGGER = TraceManager.getTrace(AESProtector.class);
	private static final QName QNAME_KEY_NAME = new QName("http://www.w3.org/2000/09/xmldsig#", "KeyName");
	private static final QName QNAME_ENCRYPTED_DATA = new QName("http://www.w3.org/2001/04/xmlenc#", "EncryptedData");
	private static final String KEY_DIGEST_TYPE = "SHA1";

	private String keyStorePath;
	private String keyStorePassword;
	private String encryptionKeyDigest;

	private static final KeyStore keyStore;
	private static final String ENCRYPTED_ELEMENT_NAME = "value";

	static {
		Init.init();
		try {
			keyStore = KeyStore.getInstance("jceks");
		} catch (KeyStoreException ex) {
			throw new SystemException(ex.getMessage(), ex);
		}
	}

	/**
	 * @throws SystemException
	 *             if jceks keystore is not available on {@link getKeyStorePath}
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
					throw new EncryptionException("Provided keystore file " + getKeyStorePath() + " is unreadable.");
				}
				stream = new FileInputStream(f);

			// Use class path keystore
			} else {
				LOGGER.warn("Using default keystore from classpath ({}).", getKeyStorePath());
				// Read from class path

				stream = this.getClass().getClassLoader().getResourceAsStream(getKeyStorePath());
				// ugly dirty hack to have second chance to find keystore on
				// class path
				if (stream == null) {
					stream = this.getClass().getClassLoader().getResourceAsStream("com/../../" + getKeyStorePath());
				}
			}
			// Test if we have valid stream
			if (stream == null) {
				throw new EncryptionException("Couldn't load keystore as resource '" + getKeyStorePath() + "'");
			}
			// Load keystore
			keyStore.load(stream, keyStorePassword.toCharArray());
			stream.close();

		} catch (Exception ex) {
			LOGGER.error("Unable to work with heystore " + getKeyStorePath() + ":" + ex.getMessage(), ex);
			throw new SystemException(ex.getMessage(), ex);
		}
	}

	/**
	 * 
	 * @param encryptionKeyDigest
	 *            SHA1 digest of encryption key {@link SecretKey} from keystore
	 * @throws IllegalArgumentException
	 *             if encryption key digest is null or empty string
	 */
	public void setEncryptionKeyDigest(String encryptionKeyDigest) {
		Validate.notEmpty(encryptionKeyDigest, "Encryption key digest must not be null or empty.");
		this.encryptionKeyDigest = encryptionKeyDigest;
	}

	private String getEncryptionKeyDigest() {
		if (StringUtils.isEmpty(encryptionKeyDigest)) {
			throw new IllegalStateException("Encryption key digest was not defined (is null or empty).");
		}
		return encryptionKeyDigest;
	}

	/**
	 * 
	 * @param keyStorePassword
	 * @throws IllegalArgumentException
	 *             if keystore password is null string
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
	 * 
	 * @param keyStorePath
	 * @throws IllegalArgumentException
	 *             if keystore path is null string
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
		Validate.notNull(protectedString, "Protected stringmust not be null.");
		EncryptedDataType encrypted = protectedString.getEncryptedData();
		Validate.notNull(encrypted, "Encrypted data must not be null.");

		Document document;
		try {
			String digest = getEncryptionKeyDigest();
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

			SecretKey secret = getSecretKey(digest, getKeyStorePassword().toCharArray());
			XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.AES_256);
			xmlCipher.init(XMLCipher.DECRYPT_MODE, secret);

			document = DOMUtil.getDocument();
			Element element = JAXBUtil.jaxbToDom(encrypted, QNAME_ENCRYPTED_DATA, document);
			document.appendChild(element);
			document = null;

			document = xmlCipher.doFinal(element.getOwnerDocument(), element);
		} catch (EncryptionException ex) {
			throw ex;
		} catch (Exception ex) {
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

	private ProtectedStringType encrypt(Element plain, ProtectedStringType protectedString) throws EncryptionException {
		if (plain == null) {
			return null;
		}

		try {
			SecretKey secret = getSecretKey(getEncryptionKeyDigest(), getKeyStorePassword().toCharArray());
			XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.AES_256);
			xmlCipher.init(XMLCipher.ENCRYPT_MODE, secret);

			MessageDigest sha1 = MessageDigest.getInstance(KEY_DIGEST_TYPE);
			Document document = plain.getOwnerDocument();
			if (document == null) {
				document = DOMUtil.getDocument();
				document.appendChild(plain);
			}
			KeyInfo keyInfo = new KeyInfo(document);
			keyInfo.addKeyName(Base64.encode(sha1.digest(secret.getEncoded())));
			xmlCipher.getEncryptedData().setKeyInfo(keyInfo);

			document = xmlCipher.doFinal(document, plain);
			EncryptedDataType data = (EncryptedDataType) JAXBUtil.unmarshal(document.getDocumentElement()).getValue();
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

	private static SecretKey getSecretKey(String digest, char[] password) throws EncryptionException {
		try {
			MessageDigest sha1 = MessageDigest.getInstance(KEY_DIGEST_TYPE);
			Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				if (!keyStore.isKeyEntry(alias)) {
					continue;
				}

				try {
					Key key = keyStore.getKey(alias, password);
					if (!(key instanceof SecretKey)) {
						continue;
					}

					String keyHash = Base64.encode(sha1.digest(key.getEncoded()));
					if (digest.equals(keyHash)) {
						return (SecretKey) key;
					}
				} catch (UnrecoverableKeyException ex) {
					LOGGER.trace("Couldn't recover key {} from keystore, reason: {}",
							new Object[] { alias, ex.getMessage() });
				}
			}
		} catch (Exception ex) {
			throw new EncryptionException(ex.getMessage(), ex);
		}

		throw new EncryptionException("Key '" + digest + "' is not in keystore.");
	}

}
