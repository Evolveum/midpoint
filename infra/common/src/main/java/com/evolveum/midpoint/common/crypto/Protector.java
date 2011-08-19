/**
 * 
 */
package com.evolveum.midpoint.common.crypto;

import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.xml.security.encryption.XMLCipher;
import org.w3._2001._04.xmlenc.CipherDataType;
import org.w3._2001._04.xmlenc.EncryptedDataType;

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

	public String decrypt(ProtectedStringType protectedString) throws EncryptionException {
		Validate.notNull(protectedString, "Protected stringmust not be null.");

		char[] password = "heslo".toCharArray();
		byte[] salt = "sol".getBytes();

		try {
			SecretKey secret = getSecretKey(keyStore, ALIAS_MASTER_PASSWORD, KEY_PASSWORD);
			// createSecretKey(password, salt);
			XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.AES_256);
			xmlCipher.init(XMLCipher.DECRYPT_MODE, secret);

			// Document whatIsThis = xmlCipher.doFinal(context, source);
		} catch (EncryptionException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new EncryptionException(ex.getMessage(), ex);
		}
		
		return null;

		// EncryptedDataType encryptedData = protectedString.getEncryptedData();
		// if (encryptedData == null) {
		// return null;
		// }
		// CipherDataType cipherData = encryptedData.getCipherData();
		// if (cipherData == null) {
		// return null;
		// }
		//
		// String encoding = StringUtils.isEmpty(encryptedData.getEncoding()) ?
		// ENCODING : encryptedData
		// .getEncoding();
		// try {
		// SecretKey secret = createSecretKey(password, salt);
		//
		// Cipher cipher = Cipher.getInstance(TRANSFORMATION);
		// AlgorithmParameters params = cipher.getParameters();
		// byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
		// cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
		//
		// return new String(cipher.doFinal(cipherData.getCipherValue()),
		// encoding);
		// } catch (Exception ex) {
		// throw new EncryptionException(ex.getMessage(), ex);
		// }
	}

	public ProtectedStringType encrypt(String string) throws EncryptionException {
		if (StringUtils.isEmpty(string)) {
			return null;
		}

		char[] password = "heslo".toCharArray();
		byte[] salt = "sol".getBytes();
		String plainText = "Hello, World!";

		ProtectedStringType protectedString = new ProtectedStringType();
		EncryptedDataType encryptedData = new EncryptedDataType();
		protectedString.setEncryptedData(encryptedData);
		try {
			SecretKey secret = createSecretKey(password, salt);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, secret);
			AlgorithmParameters params = cipher.getParameters();
			byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
			byte[] ciphertext = cipher.doFinal(plainText.getBytes(ENCODING));

			return protectedString;
		} catch (Exception ex) {
			throw new EncryptionException(ex.getMessage(), ex);
		}
	}

	public void initialize(KeyStore keyStore) {
		this.keyStore = keyStore;
	}

	private SecretKey createSecretKey(char[] password, byte[] salt) throws NoSuchAlgorithmException,
			InvalidKeySpecException {
		SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM_KEY);
		KeySpec spec = new PBEKeySpec(password, salt, 1024, 256);
		SecretKey tmp = factory.generateSecret(spec);

		return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
	}

	public SecretKey getSecretKey(KeyStore keystore, String alias, char[] password)
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
}
