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
import java.util.List;
import java.util.Random;

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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.xml.security.Init;
import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.Base64;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.CipherDataType;
import com.evolveum.prism.xml.ns._public.types_3.DigestMethodType;
import com.evolveum.prism.xml.ns._public.types_3.EncryptedDataType;
import com.evolveum.prism.xml.ns._public.types_3.EncryptionMethodType;
import com.evolveum.prism.xml.ns._public.types_3.HashedDataType;
import com.evolveum.prism.xml.ns._public.types_3.KeyInfoType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Class that manages encrypted and hashed values. Java Cryptography Extension is
 * needed because this class is using AES-256 for encrypting/decrypting xml
 * data.
 *
 * @author Radovan Semancik
 * @author lazyman
 */
public class ProtectorImpl extends BaseProtector {
	
	private static final String ALGORITHM_PKKDF2_NAME = "PBKDF2WithHmacSHA512";
	private static final QName ALGORITH_PBKDF2_WITH_HMAC_SHA512_QNAME = new QName(PrismConstants.NS_CRYPTO_ALGORITHM_PBKD, ALGORITHM_PKKDF2_NAME);
	private static final String ALGORITH_PBKDF2_WITH_HMAC_SHA512_URI = QNameUtil.qNameToUri(ALGORITH_PBKDF2_WITH_HMAC_SHA512_QNAME);
	
	private static final String KEY_DIGEST_TYPE = "SHA1";
	private static final String DEFAULT_ENCRYPTION_ALGORITHM = XMLCipher.AES_128;
    private static final char[] KEY_PASSWORD = "midpoint".toCharArray();
	
    private static final String DEFAULT_DIGEST_ALGORITHM = ALGORITH_PBKDF2_WITH_HMAC_SHA512_URI;
//    "http://www.w3.org/2009/xmlenc11#pbkdf2"
    
    private Random randomNumberGenerator;
    
	private static final Trace LOGGER = TraceManager.getTrace(ProtectorImpl.class);

	private String keyStorePath;
    private String keyStorePassword;
    private String encryptionKeyAlias = "default";
	
	private String requestedJceProviderName = null;
	private String encryptionAlgorithm;
	private String digestAlgorithm;
	
	private List<TrustManager> trustManagers;
    private static final KeyStore keyStore;
    
    static {
        try {
            keyStore = KeyStore.getInstance("jceks");
        } catch (KeyStoreException ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }
    
    /**
     * @throws SystemException if jceks keystore is not available on {@link ProtectorImpl#getKeyStorePath}
     */
    public void init() {
        InputStream stream = null;
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

                stream = ProtectorImpl.class.getClassLoader().getResourceAsStream(getKeyStorePath());
                // ugly dirty hack to have second chance to find keystore on
                // class path
                if (stream == null) {
                    stream = ProtectorImpl.class.getClassLoader().getResourceAsStream(
                            "com/../../" + getKeyStorePath());
                }
            }
            // Test if we have valid stream
            if (stream == null) {
                throw new EncryptionException("Couldn't load keystore as resource '" + getKeyStorePath()
                        + "'");
            }
            // Load keystore
            keyStore.load(stream, getKeyStorePassword().toCharArray());
            stream.close();

            // Initialze trust manager list

            TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmFactory.init(keyStore);
            trustManagers = new ArrayList<TrustManager>();
            for (TrustManager trustManager : tmFactory.getTrustManagers()) {
                trustManagers.add(trustManager);
            }
            
            //init apache crypto library
            Init.init();
            
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
		} else {
			return DEFAULT_DIGEST_ALGORITHM;
		}
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
	
	/**
     * @return the encryptionKeyAlias
     * @throws IllegalStateException if encryption key digest is null or empty string
     */
    private String getEncryptionKeyAlias() {
        if (StringUtils.isEmpty(encryptionKeyAlias)) {
            throw new IllegalStateException("Encryption key alias was not defined (is null or empty).");
        }
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
		String jceAlgorithm = JCEMapper.translateURItoJCEID(algorithmUri);//JCEMapper.getJCEKeyAlgorithmFromURI(algorithmUri);
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
            LOGGER.trace("{} data by JCE algorithm {} (URI {}), cipher {}, provider {}", new Object[]{
				desc, jceAlgorithm, algorithmUri, cipher.getAlgorithm(), cipher.getProvider().getName()});
		}
		return cipher;
	}
	
    public String getSecretKeyDigest(SecretKey key) throws EncryptionException {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance(KEY_DIGEST_TYPE);
        } catch (NoSuchAlgorithmException ex) {
            throw new EncryptionException(ex.getMessage(), ex);
        }

        return Base64.encode(sha1.digest(key.getEncoded()));
    }
	
	@Override
	public List<TrustManager> getTrustManagers() {
		return trustManagers;
	}

	@Override
	public KeyStore getKeyStore() {
		return keyStore;
	}

    /**
     * @param encryptionKeyAlias Alias of the encryption key {@link SecretKey} which is used
     *                           for encryption
     * @throws IllegalArgumentException if encryption key digest is null or empty string
     */
    public void setEncryptionKeyAlias(String encryptionKeyAlias) {
        Validate.notEmpty(encryptionKeyAlias, "Encryption key alias must not be null or empty.");
        this.encryptionKeyAlias = encryptionKeyAlias;
    }

    /**
     * @param keyStorePassword
     * @throws IllegalArgumentException if keystore password is null string
     */
    public void setKeyStorePassword(String keyStorePassword) {
        Validate.notNull(keyStorePassword, "Keystore password must not be null.");
        this.keyStorePassword = keyStorePassword;
    }

    private String getKeyStorePassword() {
        if (keyStorePassword == null) {
            throw new IllegalStateException("Keystore password was not defined (null).");
        }
        return keyStorePassword;
    }

    /**
     * @param keyStorePath
     * @throws IllegalArgumentException if keystore path is null string
     */
    public void setKeyStorePath(String keyStorePath) {
        Validate.notEmpty(keyStorePath, "Key store path must not be null.");
        this.keyStorePath = keyStorePath;
    }

    public String getKeyStorePath() {
        if (StringUtils.isEmpty(keyStorePath)) {
            throw new IllegalStateException("Keystore path was not defined (is null or empty).");
        }
        return keyStorePath;
    }
    
    private SecretKey getSecretKeyByAlias(String alias) throws EncryptionException {
        Key key;
        try {
            key = keyStore.getKey(alias, KEY_PASSWORD);
        } catch (Exception ex) {
            throw new EncryptionException("Couldn't obtain key '" + alias + "' from keystore, reason: "
                    + ex.getMessage(), ex);
        }

        if (key == null || !(key instanceof SecretKey)) {
            throw new EncryptionException("Key with alias '" + alias
                    + "' is not instance of SecretKey, but '" + key + "'.");
        }

        return (SecretKey) key;
    }

    private SecretKey getSecretKeyByDigest(String digest) throws EncryptionException {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (!keyStore.isKeyEntry(alias)) {
                    continue;
                }

                try {
                    Key key = keyStore.getKey(alias, KEY_PASSWORD);
                    if (!(key instanceof SecretKey)) {
                        continue;
                    }

                    String keyHash = getSecretKeyDigest((SecretKey) key);
                    if (digest.equals(keyHash)) {
                        return (SecretKey) key;
                    }
                } catch (UnrecoverableKeyException ex) {
                    LOGGER.trace("Couldn't recover key {} from keystore, reason: {}", new Object[]{alias,
                            ex.getMessage()});
                }
            }
        } catch (Exception ex) {
            throw new EncryptionException(ex.getMessage(), ex);
        }

        throw new EncryptionException("Key '" + digest + "' is not in keystore.");
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
					throw new SchemaException("Non-string proteted data");
				}
				hashedDataType = hashPbkd((ProtectedData<String>) protectedData, algorithmUri, algorithmQName.getLocalPart());
				break;
			default:
				throw new SchemaException("Unkown namespace "+algorithmNamespace);
		}
		
		protectedData.setHashedData(hashedDataType);
		protectedData.destroyCleartext();
    }
    
    private HashedDataType hashPbkd(ProtectedData<String> protectedData, String algorithmUri, String algorithmName) throws EncryptionException {	
		
    	char[] clearChars = protectedData.getClearValue().toCharArray();
    	byte[] salt = generatePbkdSalt();
    	int iterations = getPbkdIterations();
		
		SecretKeyFactory secretKeyFactory;
		try {
			secretKeyFactory = SecretKeyFactory.getInstance( algorithmName );
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e.getMessage(), e);
		}
		PBEKeySpec keySpec = new PBEKeySpec( clearChars, salt, iterations, getPbkdKeyLength() );
        SecretKey key;
		try {
			key = secretKeyFactory.generateSecret( keySpec );
		} catch (InvalidKeySpecException e) {
			throw new EncryptionException(e.getMessage(), e);
		}
        byte[] hashBytes = key.getEncoded( );

        HashedDataType hashedDataType = new HashedDataType();
        
        DigestMethodType digestMethod = new DigestMethodType();
        digestMethod.setAlgorithm(algorithmUri);
        digestMethod.setSalt(salt);
        digestMethod.setWorkFactor(iterations);
		hashedDataType.setDigestMethod(digestMethod);
		
		hashedDataType.setDigestValue(hashBytes);
		
		return hashedDataType;
	}

	private byte[] generatePbkdSalt() {
		byte[] salt = new byte[getPbkdSaltLength()/8];
		randomNumberGenerator.nextBytes(salt);
		return salt;
	}

	@Override
	public boolean compare(ProtectedStringType a, ProtectedStringType b) throws EncryptionException, SchemaException {
		if (a == b) {
			return true;
		}
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a.isHashed() && b.isHashed()) {
			throw new SchemaException("Cannot compare two hased protected strings");
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
			return compareHashed(hashedPs, clear.toCharArray());
			
		} else {
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
				throw new SchemaException("Unkown namespace "+algorithmNamespace);
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
			secretKeyFactory = SecretKeyFactory.getInstance( algorithmName );
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e.getMessage(), e);
		}
		PBEKeySpec keySpec = new PBEKeySpec( clearChars, salt, workFactor, keyLen );
        SecretKey key;
		try {
			key = secretKeyFactory.generateSecret( keySpec );
		} catch (InvalidKeySpecException e) {
			throw new EncryptionException(e.getMessage(), e);
		}
        byte[] hashBytes = key.getEncoded( );
        
        return Arrays.equals(digestValue, hashBytes);
	}

	
    	
}
