package com.evolveum.midpoint.common.crypto;

import java.util.List;

import javax.net.ssl.TrustManager;

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
	String decryptString(ProtectedStringType protectedString) throws EncryptionException;

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
	Element decrypt(ProtectedStringType protectedString) throws EncryptionException;

	/**
	 * 
	 * @param text
	 * @return {@link ProtectedStringType} with encrypted string inside it. If
	 *         input argument is null or empty, method returns null.
	 * @throws EncryptionException
	 *             this is thrown probably in case JRE/JDK doesn't have JCE
	 *             installed
	 */
	ProtectedStringType encryptString(String text) throws EncryptionException;

	/**
	 * 
	 * @param plain
	 * @return {@link ProtectedStringType} with encrypted element inside it. If
	 *         input argument is null, method returns null.
	 * @throws EncryptionException
	 *             this is thrown probably in case JRE/JDK doesn't have JCE
	 *             installed
	 */
	ProtectedStringType encrypt(Element plain) throws EncryptionException;

	/**
	 * Encrypts the ProtectedStringType "in place".
	 * @param ps
	 * @throws EncryptionException 
	 */
	void encrypt(ProtectedStringType ps) throws EncryptionException;

	/**
	 * Returns true if protected string contains encrypted data that seems valid.
	 */
	boolean isEncrypted(ProtectedStringType ps);

	/**
	 * Returns a list of trust managers that will be used to validate communicating party credentials.
	 * (e.g. used to validate remote connector connections).
	 */
	List<TrustManager> getTrustManagers();

}