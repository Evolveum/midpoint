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

import java.security.AlgorithmParameters;
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

/**
 * 
 * @author lazyman
 * 
 */
public class AESEncryption {

	private static final String ALGORITHM_KEY = "PBKDF2WithHmacSHA1";
	private static final String ALGORITHM = "AES";
	private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
	private static final String ENCODING = "utf-8";

	public Object encrypt(String plaintext) throws EncryptionException {
		if (StringUtils.isEmpty(plaintext)) {
			return null;
		}

		char[] password = "heslo".toCharArray();
		byte[] salt = "sol".getBytes();
		String plainText = "Hello, World!";

		try {
			SecretKey secret = createSecretKey(password, salt);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, secret);
			AlgorithmParameters params = cipher.getParameters();
			byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
			byte[] ciphertext = cipher.doFinal(plainText.getBytes(ENCODING));

			return null;
		} catch (Exception ex) {
			throw new EncryptionException(ex.getMessage(), ex);
		}
	}

	public String decrypt(Object object) throws EncryptionException {
		Validate.notNull(object, "AAAAAAAAAAAA must not be null.");

		char[] password = "heslo".toCharArray();
		byte[] salt = "sol".getBytes();
		byte[] ciphertext = "ciphertext".getBytes();

		try {
			SecretKey secret = createSecretKey(password, salt);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			AlgorithmParameters params = cipher.getParameters();
			byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
			cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));

			return new String(cipher.doFinal(ciphertext), ENCODING);
		} catch (Exception ex) {
			throw new EncryptionException(ex.getMessage(), ex);
		}
	}

	private SecretKey createSecretKey(char[] password, byte[] salt) throws NoSuchAlgorithmException,
			InvalidKeySpecException {
		SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM_KEY);
		KeySpec spec = new PBEKeySpec(password, salt, 1024, 256);
		SecretKey tmp = factory.generateSecret(spec);

		return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
	}

	private String asHex(byte buf[]) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < buf.length; i++) {
			if (((int) buf[i] & 0xff) < 0x10)
				builder.append("0");

			builder.append(Long.toString((int) buf[i] & 0xff, 16));
		}

		return builder.toString();
	}
}
