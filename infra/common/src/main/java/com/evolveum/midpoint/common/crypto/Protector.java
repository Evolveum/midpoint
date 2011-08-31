package com.evolveum.midpoint.common.crypto;

import org.w3c.dom.Element;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;

public interface Protector {

	/**
	 * 
	 * @param protectedString
	 * @return decrypted String from protectedString object
	 * @throws EncryptionException
	 *             this is thrown probably in case JRE/JDK doesn't have JCE
	 *             installed
	 * @throws IllegalArgumentException
	 *             if protectedString argument is null or EncryptedData in
	 *             protectedString argument is null
	 */
	public abstract String decryptString(ProtectedStringType protectedString) throws EncryptionException;

	/**
	 * 
	 * @param protectedString
	 * @return decrypted DOM {@link Element}
	 * @throws EncryptionException
	 *             this is thrown probably in case JRE/JDK doesn't have JCE
	 *             installed
	 * @throws IllegalArgumentException
	 *             if protectedString argument is null or EncryptedData in
	 *             protectedString argument is null
	 */
	@SuppressWarnings("unchecked")
	public abstract Element decrypt(ProtectedStringType protectedString) throws EncryptionException;

	/**
	 * 
	 * @param text
	 * @return {@link ProtectedStringType} with encrypted string inside it. If
	 *         input argument is null or empty, method returns null.
	 * @throws EncryptionException
	 *             this is thrown probably in case JRE/JDK doesn't have JCE
	 *             installed
	 */
	public abstract ProtectedStringType encryptString(String text) throws EncryptionException;

	/**
	 * 
	 * @param plain
	 * @return {@link ProtectedStringType} with encrypted element inside it. If
	 *         input argument is null, method returns null.
	 * @throws EncryptionException
	 *             this is thrown probably in case JRE/JDK doesn't have JCE
	 *             installed
	 */
	public abstract ProtectedStringType encrypt(Element plain) throws EncryptionException;

}