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
import java.security.Key;
import java.security.KeyStore;

import javax.crypto.SecretKey;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.xml.security.Init;
import org.apache.xml.security.encryption.EncryptionProperties;
import org.apache.xml.security.encryption.EncryptionProperty;
import org.apache.xml.security.encryption.XMLCipher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;

/**
 * Class that manages encrypted string values.
 * 
 * @author Radovan Semancik
 * @author lazyman
 * 
 */
public class Protector {

	private static final String ALIAS_MASTER_PASSWORD = "master";
	// this constants may be configurable later
	private static final char[] KEY_PASSWORD = "changeit".toCharArray();
	private static final char[] KEYSTORE_PASSWORD = "changeit".toCharArray();
	private static final KeyStore keyStore;

	static {
		Init.init();
		try {
			keyStore = KeyStore.getInstance("jceks");
			// "com/../../keystore.jceks"
			String path = "com/../keystore.jceks";
			InputStream stream = Protector.class.getClassLoader().getResourceAsStream(path);

			if (stream == null) {
				throw new EncryptionException("Couldn't load keystore as resource '" + path + "'");
			}
			keyStore.load(stream, KEYSTORE_PASSWORD);
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

	public Element decrypt(ProtectedStringType protectedString) throws EncryptionException {
		Validate.notNull(protectedString, "Protected stringmust not be null.");
		Element encrypted = protectedString.getEncryptedData();
		Validate.notNull(encrypted, "Encrypted data must not be null.");

		Document document;
		try {
			String alias = StringUtils.isEmpty(protectedString.getAlias()) ? ALIAS_MASTER_PASSWORD
					: protectedString.getAlias();
			SecretKey secret = getSecretKey(alias, KEY_PASSWORD);
			XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.AES_256);
			xmlCipher.init(XMLCipher.DECRYPT_MODE, secret);
			document = xmlCipher.doFinal(encrypted.getOwnerDocument(), encrypted);
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
		Document document;
		try {
			String alias = StringUtils.isEmpty(protectedString.getAlias()) ? ALIAS_MASTER_PASSWORD
					: protectedString.getAlias();
			SecretKey secret = getSecretKey(alias, KEY_PASSWORD);
			XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.AES_256);
			xmlCipher.init(XMLCipher.ENCRYPT_MODE, secret);
//			EncryptionProperties properties = xmlCipher.createEncryptionProperties();
//			EncryptionProperty property = xmlCipher.createEncryptionProperty();
//			property.setId("alias");
//			Element element = DOMUtil.getDocument().createElementNS("aaa", "alias");
//			element.setTextContent(alias);
//			property.addEncryptionInformation(element);
//			properties.addEncryptionProperty(property);
//			xmlCipher.getEncryptedData().setEncryptionProperties(properties);
			document = xmlCipher.doFinal(plain.getOwnerDocument(), plain);
			protectedString.setEncryptedData(document.getDocumentElement());
		} catch (Exception ex) {
			throw new EncryptionException(ex.getMessage(), ex);
		}

		return protectedString;
	}

	private static SecretKey getSecretKey(String alias, char[] password) throws EncryptionException {
		try {
			Key key = keyStore.getKey(alias, password);
			if (!(key instanceof SecretKey)) {
				throw new IllegalStateException("Key '" + alias + "' is not instance of SecretKey");
			}

			return (SecretKey) key;
		} catch (Exception ex) {
			throw new EncryptionException(ex.getMessage(), ex);
		}
	}

	@Deprecated
	public static void main(String... args) throws Exception {
		String plain = "welcome to protector";
		Protector protector = new Protector();
		ProtectedStringType encrypted = protector.encryptString(plain);
		System.out.println(JAXBUtil.marshalWrap(encrypted));
		System.out.println("***");
		System.out.println(protector.decryptString(encrypted));
		System.out.println("=======");
		Document document = DOMUtil.getDocument();
		Element element = document.createElement("element");
		element.setTextContent(plain);
		document.appendChild(element);
		encrypted = protector.encrypt(element);
		System.out.println(JAXBUtil.marshalWrap(encrypted));
		System.out.println("***");
		System.out.println(DOMUtil.printDom(protector.decrypt(encrypted)));
	}
}
