/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.crypto;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.KeyStoreBasedProtector;
import com.evolveum.midpoint.prism.crypto.KeyStoreBasedProtectorBuilder;
import com.evolveum.midpoint.prism.crypto.ProtectedData;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Class that manages encrypted and hashed values. Java Cryptography Extension is
 * needed because this class is using AES-256 for encrypting/decrypting xml
 * data.
 *
 * @author Radovan Semancik
 * @author lazyman
 */
public class KeyStoreBasedProtectorImpl extends BaseProtector implements KeyStoreBasedProtector {

    private static final String ALGORITHM_PKKDF2_NAME = "PBKDF2WithHmacSHA512";
    private static final QName ALGORITH_PBKDF2_WITH_HMAC_SHA512_QNAME = new QName(PrismConstants.NS_CRYPTO_ALGORITHM_PBKD, ALGORITHM_PKKDF2_NAME);
    private static final String ALGORITH_PBKDF2_WITH_HMAC_SHA512_URI = QNameUtil.qNameToUri(ALGORITH_PBKDF2_WITH_HMAC_SHA512_QNAME);

    private static final String KEY_DIGEST_TYPE = "SHA1";

    private static final String DEFAULT_ENCRYPTION_ALGORITHM = XMLSEC_ENCRYPTION_ALGORITHM_AES256_CBC;

    private static final char[] KEY_PASSWORD = "midpoint".toCharArray();

    private static final String DEFAULT_DIGEST_ALGORITHM = ALGORITH_PBKDF2_WITH_HMAC_SHA512_URI;
//    "http://www.w3.org/2009/xmlenc11#pbkdf2"

    private Random randomNumberGenerator;

    private static final Trace LOGGER = TraceManager.getTrace(KeyStoreBasedProtectorImpl.class);

    private String keyStorePath;
    private String keyStorePassword;
    private String encryptionKeyAlias = "default";

    private String requestedJceProviderName = null;
    private String encryptionAlgorithm;
    private String digestAlgorithm;

    private List<TrustManager> trustManagers;

    private static final KeyStore KEY_STORE;

    private static final Map<String, SecretKey> ALIAS_TO_SECRET_KEY_HASH_MAP = new HashMap<>();
    private static final Map<String, SecretKey> DIGEST_TO_SECRET_KEY_HASH_MAP = new HashMap<>();
    private static final Map<String,String> XMLSEC_TO_JCE_ALGORITHM_MAP = new HashMap<>();

    static {
        try {
            KEY_STORE = KeyStore.getInstance("jceks");
        } catch (KeyStoreException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }

        XMLSEC_TO_JCE_ALGORITHM_MAP.put(XMLSEC_ENCRYPTION_ALGORITHM_AES128_CBC, "AES/CBC/ISO10126Padding");
        XMLSEC_TO_JCE_ALGORITHM_MAP.put(XMLSEC_ENCRYPTION_ALGORITHM_AES256_CBC, "AES/CBC/ISO10126Padding");
    }

    public KeyStoreBasedProtectorImpl() {
    }

    public KeyStoreBasedProtectorImpl(KeyStoreBasedProtectorBuilder builder) {
        setKeyStorePath(builder.getKeyStorePath());
        setKeyStorePassword(builder.getKeyStorePassword());
        if (builder.getEncryptionKeyAlias() != null) {
            setEncryptionKeyAlias(builder.getEncryptionKeyAlias());
        } else {
            // using the pre-initialized default one
        }
        setRequestedJceProviderName(builder.getRequestedJceProviderName());
        setEncryptionAlgorithm(builder.getEncryptionAlgorithm());
        digestAlgorithm = builder.getDigestAlgorithm();
        trustManagers = builder.getTrustManagers();
    }

    /**
     * @throws SystemException if jceks keystore is not available on {@link KeyStoreBasedProtectorImpl#getKeyStorePath}
     */
    public void init() {
        validateConfiguration();
        InputStream stream;
        try {
            // Test if use file or classpath resource
            File f = new File(getKeyStorePath());
            if (f.exists()) {
                LOGGER.info("Using file keystore at {}", getKeyStorePath());
                if (!f.canRead()) {
                    LOGGER.error("Provided keystore file {} is unreadable.", getKeyStorePath());
                    throw new EncryptionException("Provided keystore file " + getKeyStorePath()
                        + " is unreadable.");
                }
                stream = new FileInputStream(f);

                // Use class path keystore
            } else {
                LOGGER.warn("Using default keystore from classpath ({}).", getKeyStorePath());
                // Read from class path

                stream = KeyStoreBasedProtectorImpl.class.getClassLoader().getResourceAsStream(getKeyStorePath());
                // ugly dirty hack to have second chance to find keystore on
                // class path
                if (stream == null) {
                    stream = KeyStoreBasedProtectorImpl.class.getClassLoader().getResourceAsStream(
                        "com/../../" + getKeyStorePath());
                }
            }
            // Test if we have valid stream
            if (stream == null) {
                throw new EncryptionException("Couldn't load keystore as resource '" + getKeyStorePath()
                    + "'");
            }
            // Load keystore
            KEY_STORE.load(stream, getKeyStorePassword().toCharArray());
            Enumeration<String> aliases = KEY_STORE.aliases();
            Set<String> keyEntryAliasesInKeyStore = new HashSet<>();

            MessageDigest sha1;
            try {
                sha1 = MessageDigest.getInstance(KEY_DIGEST_TYPE);
            } catch (NoSuchAlgorithmException ex) {
                throw new EncryptionException(ex.getMessage(), ex);
            }

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                try {
                    if (!KEY_STORE.isKeyEntry(alias)) {
                        LOGGER.trace("Alias {} is not a key entry and shall be skipped", alias);
                        continue;
                    }
                    keyEntryAliasesInKeyStore.add(alias);
                    Key key = KEY_STORE.getKey(alias, KEY_PASSWORD);
                    if (!(key instanceof SecretKey)) {
                        continue;
                    }
                    final SecretKey secretKey = (SecretKey) key;
                    LOGGER.trace("Found secret key for alias {}", alias);
                    ALIAS_TO_SECRET_KEY_HASH_MAP.put(alias, secretKey);

                    final String digest = Base64.encodeBase64String(sha1.digest(key.getEncoded()));
                    LOGGER.trace("Calculated digest {} for key alias {}", digest, key);
                    DIGEST_TO_SECRET_KEY_HASH_MAP.put(digest, secretKey);

                } catch (UnrecoverableKeyException ex) {
                    LOGGER.trace("Couldn't recover key {} from keystore, reason: {}", new Object[]{alias, ex.getMessage()});
                }
            }
            LOGGER.trace("Found {} aliases in keystore identified as secret keys", ALIAS_TO_SECRET_KEY_HASH_MAP.size());
            stream.close();

            // Initialize trust manager list

            TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmFactory.init(KEY_STORE);
            trustManagers = new ArrayList<>();
            for (TrustManager trustManager : tmFactory.getTrustManagers()) {
                trustManagers.add(trustManager);
            }

        } catch (Exception ex) {
            LOGGER.error("Unable to work with keystore {}, reason {}.",
                new Object[]{getKeyStorePath(), ex.getMessage()}, ex);
            throw new SystemException(ex.getMessage(), ex);
        }

        randomNumberGenerator = new SecureRandom();
    }

    public String getRequestedJceProviderName() {
        return requestedJceProviderName;
    }

    public void setRequestedJceProviderName(String requestedJceProviderName) {
        this.requestedJceProviderName = requestedJceProviderName;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    private String getCipherAlgorithm() {
        if (encryptionAlgorithm != null) {
            return encryptionAlgorithm;
        } else {
            return DEFAULT_ENCRYPTION_ALGORITHM;
        }
    }

    private String getDigestAlgorithm() {
        if (digestAlgorithm != null) {
            return digestAlgorithm;
        }
        return DEFAULT_DIGEST_ALGORITHM;
    }

    // TODO: make it configurable

    private int getPbkdKeyLength() {
        return 256;
    }

    private int getPbkdIterations() {
        return 10000;
    }

    private int getPbkdSaltLength() {
        return 32;
    }

    private void validateConfiguration() {
        Validate.notEmpty(encryptionKeyAlias, "Encryption key alias must not be null or empty.");
        Validate.notNull(keyStorePassword, "Keystore password must not be null.");
        Validate.notEmpty(keyStorePath, "Key store path must not be null.");
    }

    private String getEncryptionKeyAlias() {
        return encryptionKeyAlias;
    }

    @Override
    protected <T> byte[] decryptBytes(ProtectedData<T> protectedData) throws SchemaException, EncryptionException {
        EncryptedDataType encryptedDataType = protectedData.getEncryptedDataType();

        EncryptionMethodType encryptionMethodType = encryptedDataType.getEncryptionMethod();
        if (encryptionMethodType == null) {
            throw new SchemaException("No encryptionMethod element in protected data");
        }
        String algorithmUri = encryptionMethodType.getAlgorithm();
        if (StringUtils.isBlank(algorithmUri)) {
            throw new SchemaException("No algorithm URI in encryptionMethod element in protected data");
        }

        KeyInfoType keyInfo = encryptedDataType.getKeyInfo();
        if (keyInfo == null) {
            throw new SchemaException("No keyInfo element in protected data");
        }
        String keyName = keyInfo.getKeyName();
        if (StringUtils.isBlank(keyName)) {
            throw new SchemaException("No keyName defined in keyInfo element in protected data");
        }
        SecretKey key = getSecretKeyByDigest(keyName);

        CipherDataType cipherData = encryptedDataType.getCipherData();
        if (cipherData == null) {
            throw new SchemaException("No cipherData element in protected data");
        }
        byte[] encryptedBytes = cipherData.getCipherValue();
        if (encryptedBytes == null || encryptedBytes.length == 0) {
            throw new SchemaException("No cipherValue in cipherData element in protected data");
        }

        byte[] decryptedData;
        try {
            decryptedData = decryptBytes(encryptedBytes, algorithmUri, key);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
            | NoSuchProviderException | IllegalBlockSizeException | BadPaddingException
            | InvalidAlgorithmParameterException e) {
            throw new EncryptionException(e.getMessage(), e);
        }
        return decryptedData;
    }

    @Override
    public <T> void encrypt(ProtectedData<T> protectedData) throws EncryptionException {
        if (protectedData.isEncrypted()) {
            throw new IllegalArgumentException("Attempt to encrypt protected data that are already encrypted");
        }
        SecretKey key = getSecretKeyByAlias(getEncryptionKeyAlias());
        String algorithm = getCipherAlgorithm();

        byte[] clearBytes = protectedData.getClearBytes();

        byte[] encryptedBytes;
        try {
            encryptedBytes = encryptBytes(clearBytes, algorithm, key);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
            | NoSuchProviderException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
            throw new EncryptionException(e.getMessage(), e);
        }

        // Construct encryption types
        EncryptedDataType encryptedDataType = new EncryptedDataType();

        EncryptionMethodType encryptionMethodType = new EncryptionMethodType();
        encryptionMethodType.setAlgorithm(algorithm);
        encryptedDataType.setEncryptionMethod(encryptionMethodType);

        KeyInfoType keyInfoType = new KeyInfoType();
        keyInfoType.setKeyName(getSecretKeyDigest(key));
        encryptedDataType.setKeyInfo(keyInfoType);

        CipherDataType cipherDataType = new CipherDataType();
        cipherDataType.setCipherValue(encryptedBytes);
        encryptedDataType.setCipherData(cipherDataType);

        protectedData.setEncryptedData(encryptedDataType);
        protectedData.destroyCleartext();
    }

    private byte[] encryptBytes(byte[] clearData, String algorithmUri, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, algorithmUri);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedData = cipher.doFinal(clearData);

        // Place IV at the beginning of the encrypted bytes so it can be reused on decryption
        byte[] iv = cipher.getIV();
        byte[] encryptedBytes = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, encryptedBytes, 0, iv.length);
        System.arraycopy(encryptedData, 0, encryptedBytes, iv.length, encryptedData.length);

        return encryptedBytes;
    }

    private byte[] decryptBytes(byte[] encryptedBytes, String algorithmUri, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, algorithmUri);

        // Extract IV from the beginning of the encrypted bytes
        int ivLen = cipher.getBlockSize();
        byte[] ivBytes = new byte[ivLen];
        System.arraycopy(encryptedBytes, 0, ivBytes, 0, ivLen);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        byte[] decryptedData = cipher.doFinal(encryptedBytes, ivLen, encryptedBytes.length - ivLen);

        return decryptedData;
    }

    private Cipher getCipher(int cipherMode, String algorithmUri) throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, InvalidKeyException, InvalidAlgorithmParameterException {
        String jceAlgorithm = XMLSEC_TO_JCE_ALGORITHM_MAP.get(algorithmUri);
        Cipher cipher;
        if (requestedJceProviderName == null) {
            cipher = Cipher.getInstance(jceAlgorithm);
        } else {
            cipher = Cipher.getInstance(jceAlgorithm, requestedJceProviderName);
        }
        if (LOGGER.isTraceEnabled()) {
            String desc;
            if (cipherMode == Cipher.ENCRYPT_MODE) {
                desc = "Encrypting";
            } else if (cipherMode == Cipher.DECRYPT_MODE) {
                desc = "Decrypting";
            } else {
                desc = "Ciphering (mode " + cipherMode + ")";
            }
            LOGGER.trace("{} data by JCE algorithm {} (URI {}), cipher {}, provider {}", desc, jceAlgorithm, algorithmUri, cipher.getAlgorithm(),
                    cipher.getProvider().getName());
        }
        return cipher;
    }

    /**
     * TODO remove, used only in midpoint ninja cmd tool, not part of API
     */
    @Deprecated
    @Override
    public String getSecretKeyDigest(SecretKey key) throws EncryptionException {
        for (Map.Entry<String, SecretKey> entry : DIGEST_TO_SECRET_KEY_HASH_MAP.entrySet()) {
            if (entry.getValue().equals(key)) {
                return entry.getKey();
            }
        }

        throw new EncryptionException("Could not find hash for secret key algorithm " + key.getAlgorithm()
            + ". Hash values for keys must be recomputed during initialization");
    }

    @Override
    public List<TrustManager> getTrustManagers() {
        return trustManagers;
    }

    @Override
    public KeyStore getKeyStore() {
        return KEY_STORE;
    }

    /**
     * @param encryptionKeyAlias Alias of the encryption key {@link SecretKey} which is used
     *                           for encryption
     * @throws IllegalArgumentException if encryption key digest is null or empty string
     */
    public void setEncryptionKeyAlias(String encryptionKeyAlias) {
        this.encryptionKeyAlias = encryptionKeyAlias;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    private String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    @Override
    public String getKeyStorePath() {
        return keyStorePath;
    }

    private SecretKey getSecretKeyByAlias(String alias) throws EncryptionException {
        if (alias == null || alias.isEmpty()) {
            throw new EncryptionException("Key alias must be specified and cannot be blank.");
        }

        if (ALIAS_TO_SECRET_KEY_HASH_MAP.containsKey(alias)) {
            return ALIAS_TO_SECRET_KEY_HASH_MAP.get(alias);
        }
        throw new EncryptionException("No key mapped to alias " + alias
            + " could be found in the keystore. Keys by aliases must be recomputed during initialization");
    }

    private SecretKey getSecretKeyByDigest(String digest) throws EncryptionException {
        if (digest == null || digest.isEmpty()) {
            throw new EncryptionException("Key digest must be specified and cannot be blank.");
        }

        if (DIGEST_TO_SECRET_KEY_HASH_MAP.containsKey(digest)) {
            return DIGEST_TO_SECRET_KEY_HASH_MAP.get(digest);
        }
        throw new EncryptionException("No key mapped to key digest " + digest
            + " could be found in the keystore. Keys digests must be recomputed during initialization");
    }

    @Override
    public <T> void hash(ProtectedData<T> protectedData) throws EncryptionException, SchemaException {
        if (protectedData.isHashed()) {
            throw new IllegalArgumentException("Attempt to hash protected data that are already hashed");
        }
        String algorithmUri = getDigestAlgorithm();
        QName algorithmQName = QNameUtil.uriToQName(algorithmUri);
        String algorithmNamespace = algorithmQName.getNamespaceURI();
        if (algorithmNamespace == null) {
            throw new SchemaException("No algorithm namespace");
        }

        HashedDataType hashedDataType;
        switch (algorithmNamespace) {
            case PrismConstants.NS_CRYPTO_ALGORITHM_PBKD:
                if (!protectedData.canSupportType(String.class)) {
                    throw new SchemaException("Non-string protected data");
                }
                hashedDataType = hashPbkd((ProtectedData<String>) protectedData, algorithmUri, algorithmQName.getLocalPart());
                break;
            default:
                throw new SchemaException("Unknown namespace " + algorithmNamespace);
        }

        protectedData.setHashedData(hashedDataType);
        protectedData.destroyCleartext();
        protectedData.setEncryptedData(null);
    }

    private HashedDataType hashPbkd(ProtectedData<String> protectedData, String algorithmUri, String algorithmName) throws EncryptionException {

        char[] clearChars = getClearChars(protectedData);
        byte[] salt = generatePbkdSalt();
        int iterations = getPbkdIterations();

        SecretKeyFactory secretKeyFactory;
        try {
            secretKeyFactory = SecretKeyFactory.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(e.getMessage(), e);
        }
        PBEKeySpec keySpec = new PBEKeySpec(clearChars, salt, iterations, getPbkdKeyLength());
        SecretKey key;
        try {
            key = secretKeyFactory.generateSecret(keySpec);
        } catch (InvalidKeySpecException e) {
            throw new EncryptionException(e.getMessage(), e);
        }
        byte[] hashBytes = key.getEncoded();

        HashedDataType hashedDataType = new HashedDataType();

        DigestMethodType digestMethod = new DigestMethodType();
        digestMethod.setAlgorithm(algorithmUri);
        digestMethod.setSalt(salt);
        digestMethod.setWorkFactor(iterations);
        hashedDataType.setDigestMethod(digestMethod);

        hashedDataType.setDigestValue(hashBytes);

        return hashedDataType;
    }

    private char[] getClearChars(ProtectedData<String> protectedData) throws EncryptionException {
        if (protectedData.isEncrypted()) {
            return decryptString(protectedData).toCharArray();
        } else {
            return protectedData.getClearValue().toCharArray();
        }
    }

    private byte[] generatePbkdSalt() {
        byte[] salt = new byte[getPbkdSaltLength() / 8];
        randomNumberGenerator.nextBytes(salt);
        return salt;
    }

    @Override
    public boolean compareCleartext(ProtectedStringType a, ProtectedStringType b) throws EncryptionException, SchemaException {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.isHashed() && b.isHashed()) {
            throw new SchemaException("Cannot compare two hashed protected strings");
        }

        if (a.isHashed() || b.isHashed()) {
            String clear;
            ProtectedStringType hashedPs;
            if (a.isHashed()) {
                hashedPs = a;
                clear = decryptString(b);
            } else {
                hashedPs = b;
                clear = decryptString(a);
            }
            if (clear == null) {
                return false;
            }
            return compareHashed(hashedPs, clear.toCharArray());

        } else {
            return compareEncryptedCleartext(a, b);
        }
    }

    private boolean compareEncryptedCleartext(ProtectedStringType a, ProtectedStringType b) throws EncryptionException {
        String aClear = decryptString(a);
        String bClear = decryptString(b);
        if (aClear == null && bClear == null) {
            return true;
        }
        if (aClear == null || bClear == null) {
            return false;
        }
        return aClear.equals(bClear);
    }

    private boolean compareHashed(ProtectedStringType hashedPs, char[] clearChars) throws SchemaException, EncryptionException {
        HashedDataType hashedDataType = hashedPs.getHashedDataType();
        DigestMethodType digestMethodType = hashedDataType.getDigestMethod();
        if (digestMethodType == null) {
            throw new SchemaException("No digest type");
        }
        String algorithmUri = digestMethodType.getAlgorithm();
        QName algorithmQName = QNameUtil.uriToQName(algorithmUri);
        String algorithmNamespace = algorithmQName.getNamespaceURI();
        if (algorithmNamespace == null) {
            throw new SchemaException("No algorithm namespace");
        }

        switch (algorithmNamespace) {
            case PrismConstants.NS_CRYPTO_ALGORITHM_PBKD:
                return compareHashedPbkd(hashedDataType, algorithmQName.getLocalPart(), clearChars);
            default:
                throw new SchemaException("Unkown namespace " + algorithmNamespace);
        }
    }

    private boolean compareHashedPbkd(HashedDataType hashedDataType, String algorithmName, char[] clearChars) throws EncryptionException {
        DigestMethodType digestMethodType = hashedDataType.getDigestMethod();
        byte[] salt = digestMethodType.getSalt();
        Integer workFactor = digestMethodType.getWorkFactor();
        byte[] digestValue = hashedDataType.getDigestValue();
        int keyLen = digestValue.length * 8;

        SecretKeyFactory secretKeyFactory;
        try {
            secretKeyFactory = SecretKeyFactory.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException(e.getMessage(), e);
        }
        PBEKeySpec keySpec = new PBEKeySpec(clearChars, salt, workFactor, keyLen);
        SecretKey key;
        try {
            key = secretKeyFactory.generateSecret(keySpec);
        } catch (InvalidKeySpecException e) {
            throw new EncryptionException(e.getMessage(), e);
        }
        byte[] hashBytes = key.getEncoded();

        return Arrays.equals(digestValue, hashBytes);
    }

    @Override
    public boolean areEquivalent(ProtectedStringType a, ProtectedStringType b) throws EncryptionException, SchemaException {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.isHashed()) {
            if (b.isHashed()) {
                return areEquivalentHashed(a, b);
            } else {
                return false;
            }
        }
        if (a.isEncrypted()) {
            if (b.isEncrypted()) {
                return areEquivalentEncrypted(a, b);
            } else {
                return false;
            }
        }
        return Objects.equals(a.getClearValue(), b.getClearValue());
    }

    private boolean areEquivalentHashed(ProtectedStringType a, ProtectedStringType b) {
        // We cannot compare two hashes in any other way.
        return Objects.equals(a.getHashedDataType(), b.getHashedDataType());
    }

    private boolean areEquivalentEncrypted(ProtectedStringType a, ProtectedStringType b) throws EncryptionException {
        EncryptedDataType ae = a.getEncryptedDataType();
        EncryptedDataType be = b.getEncryptedDataType();
        if (!Objects.equals(ae.getEncryptionMethod(), be.getEncryptionMethod())) {
            return false;
        }
        if (!Objects.equals(ae.getKeyInfo(), be.getKeyInfo())) {
            return false;
        }

        if (Objects.equals(ae.getCipherData(), be.getCipherData())) {
            return true;
        }

        try {

            return compareEncryptedCleartext(a, b);

        } catch (EncryptionException e) {
            // We cannot decrypt one of the values. Therefore we do not really know whether they are
            // the same or different. Re-throwing the exception here would stop all action. And,
            // strictly speaking, that would be the right thing to do. But as this method is used
            // in a low-level prism code, re-throwing this exception may stop all operations that
            // could lead to fixing the error. Therefore just log the error, but otherwise pretend
            // that the values are not equivalent. That is still OK with the interface contract.
            LOGGER.warn("Cannot decrypt a value for comparison: "+e.getMessage(), e);
            return false;
        }
    }


    @Override
    public boolean isEncryptedByCurrentKey(@NotNull EncryptedDataType data) throws EncryptionException {
        String encryptedUsingKeyName = data.getKeyInfo().getKeyName();
        if (encryptedUsingKeyName == null) {
            throw new IllegalStateException("No key name in encrypted data: " + data);
        }
        SecretKey encryptedUsingKey = getSecretKeyByDigest(encryptedUsingKeyName);
        SecretKey currentKey = getSecretKeyByAlias(getEncryptionKeyAlias());
        return currentKey.equals(encryptedUsingKey);
    }

}
