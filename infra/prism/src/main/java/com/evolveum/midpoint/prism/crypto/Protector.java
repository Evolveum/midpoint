/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.prism.crypto;

import java.security.KeyStore;
import java.util.List;

import javax.net.ssl.TrustManager;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.evolveum.midpoint.util.exception.SchemaException;

public interface Protector {
	
	<T> void decrypt(ProtectedData<T> protectedData) throws EncryptionException, SchemaException;
	
	<T> void encrypt(ProtectedData<T> protectedData) throws EncryptionException;
		
	/**
	 * Returns a list of trust managers that will be used to validate communicating party credentials.
	 * (e.g. used to validate remote connector connections).
	 */
	List<TrustManager> getTrustManagers();
	
	KeyStore getKeyStore();
	
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
	String decryptString(ProtectedData<String> protectedString) throws EncryptionException;

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
	 * Returns true if protected string contains encrypted data that seems valid.
	 * DEPRECATED. Use ProtectedStringType.isEncrypted() instead
	 */
	@Deprecated
	boolean isEncrypted(ProtectedStringType ps);
	
	<T> void hash(ProtectedData<T> protectedData) throws EncryptionException, SchemaException;
	
	boolean compare(ProtectedStringType a, ProtectedStringType b) throws EncryptionException, SchemaException;

}
