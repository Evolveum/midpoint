/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.crypto;

import java.io.InputStream;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
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
 * Class that manages encrypted string values.
 * 
 * @author Radovan Semancik
 * @author lazyman
 * 
 */
public class Protector {

	private static final Trace LOGGER = TraceManager.getTrace(Protector.class);
	private static final QName QNAME_KEY_NAME = new QName("http://www.w3.org/2000/09/xmldsig#", "KeyName");
	private static final QName QNAME_ENCRYPTED_DATA = new QName("http://www.w3.org/2001/04/xmlenc#",
			"EncryptedData");
	private static final String KEY_DIGEST_TYPE = "SHA1";
	// THIS SHOULD BE CONFIGURABLE LATER
	private static final String MASTER_PASSWORD_HASH = "HF6JRsNMeJt6alihT44CXKgpe0c=";
	private static final String KEYSTORE_PATH = "com/../keystore.jceks"; // "com/../../keystore.jceks"
	private static final char[] KEY_PASSWORD = "changeit".toCharArray();
	private static final char[] KEYSTORE_PASSWORD = "changeit".toCharArray();

	private static final KeyStore keyStore;

	static {
		Init.init();
		try {
			keyStore = KeyStore.getInstance("jceks");
			URL url = Protector.class.getClassLoader().getResource(KEYSTORE_PATH);
			LOGGER.debug("Keystore path: " + url);

			InputStream stream = Protector.class.getClassLoader().getResourceAsStream(KEYSTORE_PATH);
			if (stream == null) {
				throw new EncryptionException("Couldn't load keystore as resource '" + KEYSTORE_PATH + "'");
			}
			keyStore.load(stream, KEYSTORE_PASSWORD);
			stream.close();
		} catch (Exception ex) {
			throw new SystemException(ex.getMessage(), ex);
		}
	}

	public String decryptString(ProtectedStringType protectedString) throws EncryptionException {
		Element plain = decrypt(protectedString);
		if (plain == null) {
			return null;
		}

		return plain.getTextContent();
	}

	@SuppressWarnings("unchecked")
	public Element decrypt(ProtectedStringType protectedString) throws EncryptionException {
		Validate.notNull(protectedString, "Protected stringmust not be null.");
		EncryptedDataType encrypted = protectedString.getEncryptedData();
		Validate.notNull(encrypted, "Encrypted data must not be null.");

		Document document;
		try {
			String digest = MASTER_PASSWORD_HASH;
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

			SecretKey secret = getSecretKey(digest, KEY_PASSWORD);
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

	public ProtectedStringType encryptString(String text) throws EncryptionException {
		if (StringUtils.isEmpty(text)) {
			return null;
		}

		Document document = DOMUtil.getDocument();
		Element plain = document.createElement("value");
		plain.setTextContent(text);

		document.appendChild(plain);

		return encrypt(plain);
	}

	public ProtectedStringType encrypt(Element plain) throws EncryptionException {
		if (plain == null) {
			return null;
		}

		ProtectedStringType protectedString = new ProtectedStringType();
		try {
			SecretKey secret = getSecretKey(MASTER_PASSWORD_HASH, KEY_PASSWORD);
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
			EncryptedDataType data = (EncryptedDataType) JAXBUtil.unmarshal(document.getDocumentElement())
					.getValue();
			protectedString.setEncryptedData(data);
		} catch (EncryptionException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new EncryptionException(ex.getMessage(), ex);
		}

		return protectedString;
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
					LOGGER.trace("Couldn't recover key {} from keystore, reason: {}", new Object[] { alias,
							ex.getMessage() });
				}
			}
		} catch (Exception ex) {
			throw new EncryptionException(ex.getMessage(), ex);
		}

		throw new EncryptionException("Key '" + digest + "' is not in keystore.");
	}
}
