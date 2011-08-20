/**
 * 
 */
package com.evolveum.midpoint.common.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.Validate;
import org.apache.xml.security.Init;
import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.XMLCipher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
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

	private static final String ALGORITHM_KEY = "PBKDF2WithHmacSHA1";
	private static final String ALGORITHM = "AES";
	private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
	private static final String ENCODING = "utf-8";

	private static final String ALIAS_MASTER_PASSWORD = "master";
	private static final char[] KEY_PASSWORD = "changeit".toCharArray();

	private static final String KEYSTORE_PASSWORD = "changeit";
	private static final String KEYSTORE_URI = "../keystore.jceks";
	private KeyStore keyStore;

	static {
		Init.init();
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
			SecretKey secret = getSecretKey(keyStore, ALIAS_MASTER_PASSWORD, KEY_PASSWORD);
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
		Document document = DOMUtil.getDocument();
		Element plain = document.createElement("value");
		plain.setTextContent(text);

		return encrypt(plain);
	}

	public ProtectedStringType encrypt(Element plain) throws EncryptionException {
		if (plain == null) {
			return null;
		}

		ProtectedStringType protectedString = new ProtectedStringType();
		Document document;
		try {
			SecretKey secret = getSecretKey(keyStore, ALIAS_MASTER_PASSWORD, KEY_PASSWORD);
			XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.AES_256);
			xmlCipher.init(XMLCipher.ENCRYPT_MODE, secret);
			// EncryptedData data =
			// xmlCipher.encryptData(plain.getOwnerDocument(), plain);
			document = xmlCipher.doFinal(plain.getOwnerDocument(), plain);
			protectedString.setEncryptedData(document.getDocumentElement());

			// Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			// cipher.init(Cipher.ENCRYPT_MODE, secret);
			// AlgorithmParameters params = cipher.getParameters();
			// byte[] iv =
			// params.getParameterSpec(IvParameterSpec.class).getIV();
			// byte[] ciphertext = cipher.doFinal(plainText.getBytes(ENCODING));
			//
			// return protectedString;
		} catch (Exception ex) {
			throw new EncryptionException(ex.getMessage(), ex);
		}

		return protectedString;
	}

	public void initialize(KeyStore keyStore) {
		this.keyStore = keyStore;
	}

	private static SecretKey createSecretKey(char[] password, byte[] salt) throws NoSuchAlgorithmException,
			InvalidKeySpecException {
		SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM_KEY);
		KeySpec spec = new PBEKeySpec(password, salt, 1024, 256);
		SecretKey tmp = factory.generateSecret(spec);

		return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
	}

	private static SecretKey getSecretKey(KeyStore keystore, String alias, char[] password)
			throws EncryptionException {
		try {
			Key key = keystore.getKey(alias, password);
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
		KeyStore keystore = KeyStore.getInstance("jceks");
		File file = new File("./src/main/java/keystore.jceks");
		System.out.println(file.getAbsolutePath());
		keystore.load(new FileInputStream(file), "changeit".toCharArray());

		SecretKey secret = getSecretKey(keystore, "master", "changeit".toCharArray());
		// char[] password = "heslo".toCharArray();
		// byte[] salt = "sol".getBytes();
		// SecretKey secret = createSecretKey(password, salt);

		XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.AES_256);
		xmlCipher.init(XMLCipher.ENCRYPT_MODE, secret);

		Document document = DOMUtil.getDocument();
		Element passwordE = document.createElementNS(SchemaConstants.NS_C, "password");
		passwordE.setTextContent("VILKOOOOOOOOOOO");

		EncryptedData data = xmlCipher.encryptData(document, passwordE);

		String text = DOMUtil.printDom(xmlCipher.martial(data)).toString();
		System.out.println(DOMUtil.printDom(passwordE));
		System.out.println(text);
		System.out.println("*************************");
		// String text =
		// "<xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\" "
		// +
		// "Type=\"http://www.w3.org/2001/04/xmlenc#Element\">" +
		// "<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/>"
		// +
		// "<xenc:CipherData>" +
		// "<xenc:CipherValue>hv5v9lQAaUz2wF1G+06T2tkrh9ZGBUdRni/O30shk+79P9HsPEimaMfTscGkLEi8HKaEHJf8ss7x5WBcoFXdCw==</xenc:CipherValue>"
		// +
		// "</xenc:CipherData>" +
		// "</xenc:EncryptedData>";

		// secret = createSecretKey(password, salt);
		xmlCipher = XMLCipher.getInstance(XMLCipher.AES_256);
		xmlCipher.init(XMLCipher.DECRYPT_MODE, secret);
		document = DOMUtil.parseDocument(text);
		// xmlCipher.loadEncryptedData(document, document.getDocumentElement());
		Document fin = xmlCipher.doFinal(document, document.getDocumentElement());
		System.out.println(DOMUtil.printDom(document));
		// System.out.println(DOMUtil.printDom(fin));
	}
}
