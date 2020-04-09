/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.crypto;

import java.security.KeyStore;
import java.util.List;

import javax.net.ssl.TrustManager;

import com.evolveum.prism.xml.ns._public.types_3.EncryptedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

public interface Protector {

    String XMLSEC_ENCRYPTION_NS = "http://www.w3.org/2001/04/xmlenc";
    String XMLSEC_ENCRYPTION_ALGORITHM_AES128_CBC = XMLSEC_ENCRYPTION_NS + "#aes128-cbc";
    String XMLSEC_ENCRYPTION_ALGORITHM_AES256_CBC = XMLSEC_ENCRYPTION_NS + "#aes256-cbc";

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

    <T> void hash(ProtectedData<T> protectedData) throws EncryptionException, SchemaException;

    /**
     * Compare cleartext values protected inside the protected strings.
     *
     * This method only deals with the equality of the original values (cleartext).
     * If the two protected strings are representation of the same value then true value
     * is returned. In all other cases false value is returned.
     *
     * Please note that some cases are not decidable. For example it may not be possible
     * to compare two hashed values, e.g. in case that they are using different salt values.
     * SchemaException is thrown in that case.
     *
     * This method does not deal with any details about the protection. It just deals
     * with equality of the values. E.g. if encrypted and hashed version of the same value
     * is compared, this method returns true. This is ideal for use cases such as checking
     * equality of a password during authentication. But it is not the right algorithm for
     * data management purposes. E.g. it will provide bad results if it is used to decide
     * whether a particular value should be replaced with a new value in a data store.
     * Please see areEquivalent() method.
     *
     * @see #areEquivalent(ProtectedStringType, ProtectedStringType)
     */
    boolean compareCleartext(ProtectedStringType a, ProtectedStringType b) throws EncryptionException, SchemaException;

    /**
     * Decides equivalence of two protected data objects (for data management purposes).
     *
     * The concept of equivalence is a very tricky one when it comes
     * to protected (encrypted and hashed) data. We want to compare
     * the original values (cleartext) and not the bytes that are
     * a product of encryption (ciphertext). As many ciphers have
     * randomized initialization vectors, encrypting the same cleartext
     * will produce different ciphertext. Therefore we cannot rely on
     * equality of ciphertexts. On the other hand, we want to allow
     * re-keying. Therefore protected data types that are produced
     * from the same cleartext may still be non-equivalent if a
     * different key was used to create them. Otherwise we want
     * be able to change the key, as the value with old key will be
     * considered equivalent and it may never get replaced.
     *
     * And all of that is further complicated with hashing.
     * Hash algorithms are often salted, therefore we cannot rely on
     * comparing just the hashes. The situation is a bit easier here
     * as we do not need to deal with the key, but on the other hand
     * we do not have access to a cleartext at all. Therefore all we can
     * do is to look at hashed values and suffer re-salting all the time.
     * But it is better than the alternative, which means never be able to
     * change hashed value. If any more intelligent behavior is expected, it
     * has to be implemented in higher layers of the system where we still
     * have at least one unhashed clear value available.
     *
     * This method is designed for data management purposes. E.g. it can be used
     * to decide whether to replace certain value in a data store. This method
     * is not suitable for all purposes. E.g. it should NOT be used for password
     * management. This method may return false even if the cleartext in two
     * protected strings is the same, e.g. in case that one is encrypted and
     * the other is hashed. For that purpose see the compareCleartext method.
     *
     * @see #compareCleartext(ProtectedStringType, ProtectedStringType)
     */
    boolean areEquivalent(ProtectedStringType a, ProtectedStringType b) throws EncryptionException, SchemaException;



    boolean isEncryptedByCurrentKey(@NotNull EncryptedDataType data) throws EncryptionException;
}
