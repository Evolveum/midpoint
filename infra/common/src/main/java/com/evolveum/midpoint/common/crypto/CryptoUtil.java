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
package com.evolveum.midpoint.common.crypto;

import java.io.ByteArrayOutputStream;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailServerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmsConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmsGatewayConfigurationType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 *
 */
public class CryptoUtil {

	private static final Trace LOGGER = TraceManager.getTrace(CryptoUtil.class);

	/**
	 * Encrypts all encryptable values in the object.
	 */
	public static <T extends ObjectType> void encryptValues(final Protector protector, final PrismObject<T> object) throws EncryptionException{
	    Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable){
				if (!(visitable instanceof PrismPropertyValue)) {
					return;
				}
				PrismPropertyValue<?> pval = (PrismPropertyValue<?>)visitable;
				try {
					encryptValue(protector, pval);
				} catch (EncryptionException e) {
					throw new TunnelException(e);
				}
			}
		};
		try {
			object.accept(visitor);
		} catch (TunnelException e) {
			EncryptionException origEx = (EncryptionException)e.getCause();
			throw origEx;
		}
	}

	/**
	 * Encrypts all encryptable values in delta.
	 */
	public static <T extends ObjectType> void encryptValues(final Protector protector, final ObjectDelta<T> delta) throws EncryptionException{
	    Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable){
 				if (!(visitable instanceof PrismPropertyValue)) {
					return;
				}
				PrismPropertyValue<?> pval = (PrismPropertyValue<?>)visitable;
				try {
					encryptValue(protector, pval);
				} catch (EncryptionException e) {
					throw new TunnelException(e);
				}
			}
		};
		try {
			delta.accept(visitor);
		} catch (TunnelException e) {
			EncryptionException origEx = (EncryptionException)e.getCause();
			throw origEx;
		}
	}

	private static <T extends ObjectType> void encryptValue(Protector protector, PrismPropertyValue<?> pval) throws EncryptionException{
    	Itemable item = pval.getParent();
    	if (item == null) {
    		return;
    	}
    	ItemDefinition itemDef = item.getDefinition();
    	if (itemDef == null || itemDef.getTypeName() == null) {
    		return;
    	}

    	if (itemDef.getTypeName().equals(ProtectedStringType.COMPLEX_TYPE)) {
            QName propName = item.getElementName();
            PrismPropertyValue<ProtectedStringType> psPval = (PrismPropertyValue<ProtectedStringType>)pval;
            ProtectedStringType ps = psPval.getValue();
            encryptProtectedStringType(protector, ps, propName.getLocalPart());
            if (pval.getParent() == null){
                pval.setParent(item);
            }
    	} else if (itemDef.getTypeName().equals(NotificationConfigurationType.COMPLEX_TYPE)) {
            // this is really ugly hack needed because currently it is not possible to break NotificationConfigurationType into prism item [pm]
            NotificationConfigurationType ncfg = ((PrismPropertyValue<NotificationConfigurationType>) pval).getValue();
            if (ncfg.getMail() != null) {
                for (MailServerConfigurationType mscfg : ncfg.getMail().getServer()) {
                    encryptProtectedStringType(protector, mscfg.getPassword(), "mail server password");
                }
            }
            if (ncfg.getSms() != null) {
                for (SmsConfigurationType smscfg : ncfg.getSms()) {
                    for (SmsGatewayConfigurationType gwcfg : smscfg.getGateway()) {
                        encryptProtectedStringType(protector, gwcfg.getPassword(), "sms gateway password");
                    }
                }
            }
        }
    }

    private static void encryptProtectedStringType(Protector protector, ProtectedStringType ps, String propName) throws EncryptionException {
    	if (ps.isHashed()) {
    		throw new EncryptionException("Attempt to encrypt hashed value for "+propName);
    	}
    	if (ps != null && ps.getClearValue() != null) {
            try {
                protector.encrypt(ps);
            } catch (EncryptionException e) {
                throw new EncryptionException("Failed to encrypt value for field " + propName + ": " + e.getMessage(), e);
            }
        }
    }

	// Checks that everything is encrypted
	public static <T extends ObjectType> void checkEncrypted(final PrismObject<T> object) {
	    Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable){
				if (!(visitable instanceof PrismPropertyValue)) {
					return;
				}
				PrismPropertyValue<?> pval = (PrismPropertyValue<?>)visitable;
				checkEncrypted(pval);
			}
		};
		try {
			object.accept(visitor);
		} catch (IllegalStateException e) {
			throw new IllegalStateException(e.getMessage() + " in " + object, e);
		}

	}

	// Checks that everything is encrypted
	public static <T extends ObjectType> void checkEncrypted(final ObjectDelta<T> delta) {
	    Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable){
				if (!(visitable instanceof PrismPropertyValue)) {
					return;
				}
				PrismPropertyValue<?> pval = (PrismPropertyValue<?>)visitable;
				checkEncrypted(pval);
			}
		};
		try {
			delta.accept(visitor);
		} catch (IllegalStateException e) {
			throw new IllegalStateException(e.getMessage() + " in delta " + delta, e);
		}
	}

	private static <T extends ObjectType> void checkEncrypted(PrismPropertyValue<?> pval) {
    	Itemable item = pval.getParent();
    	if (item == null) {
    		return;
    	}
    	ItemDefinition itemDef = item.getDefinition();
    	if (itemDef == null || itemDef.getTypeName() == null) {
    		return;
    	}
    	if (itemDef.getTypeName().equals(ProtectedStringType.COMPLEX_TYPE)) {
            QName propName = item.getElementName();
            PrismPropertyValue<ProtectedStringType> psPval = (PrismPropertyValue<ProtectedStringType>)pval;
            ProtectedStringType ps = psPval.getValue();
            if (ps.getClearValue() != null) {
                throw new IllegalStateException("Unencrypted value in field " + propName);
            }
        } else if (itemDef.getTypeName().equals(NotificationConfigurationType.COMPLEX_TYPE)) {
            // this is really ugly hack needed because currently it is not possible to break NotificationConfigurationType into prism item [pm]
            NotificationConfigurationType ncfg = ((PrismPropertyValue<NotificationConfigurationType>) pval).getValue();
            if (ncfg.getMail() != null) {
                for (MailServerConfigurationType mscfg : ncfg.getMail().getServer()) {
                    if (mscfg.getPassword() != null && mscfg.getPassword().getClearValue() != null) {
                        throw new IllegalStateException("Unencrypted value in mail server config password entry");
                    }
                }
            }
            if (ncfg.getSms() != null) {
                for (SmsConfigurationType smscfg : ncfg.getSms()) {
                    for (SmsGatewayConfigurationType gwcfg : smscfg.getGateway()) {
                        if (gwcfg.getPassword() != null && gwcfg.getPassword().getClearValue() != null) {
                            throw new IllegalStateException("Unencrypted value in SMS gateway config password entry");
                        }
                    }
                }
            }
        }
    }

	public static void checkEncrypted(Collection<? extends ItemDelta> modifications) {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable){
				if (!(visitable instanceof PrismPropertyValue)) {
					return;
				}
				PrismPropertyValue<?> pval = (PrismPropertyValue<?>)visitable;
				checkEncrypted(pval);
			}
		};
		for (ItemDelta<?,?> delta: modifications) {
			try {
				delta.accept(visitor);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage() + " in modification " + delta, e);
			}

		}
	}

	private final static byte [] DEFAULT_IV_BYTES = {
        (byte) 0x51,(byte) 0x65,(byte) 0x22,(byte) 0x23,
        (byte) 0x64,(byte) 0x05,(byte) 0x6A,(byte) 0xBE,
        (byte) 0x51,(byte) 0x65,(byte) 0x22,(byte) 0x23,
        (byte) 0x64,(byte) 0x05,(byte) 0x6A,(byte) 0xBE,
    };

	public static void securitySelfTest(OperationResult parentTestResult) {
		OperationResult result = parentTestResult.createSubresult(CryptoUtil.class.getName()+".securitySelfTest");

		// Providers
		for (Provider provider: Security.getProviders()) {
			String providerName = provider.getName();
			OperationResult providerResult = result.createSubresult(CryptoUtil.class.getName()+".securitySelfTest.provider."+providerName);
			try {
				providerResult.addContext("info", provider.getInfo());
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				provider.storeToXML(os, "Crypto provider "+providerName);
				String propXml = os.toString();
				providerResult.addContext("properties", propXml);
				providerResult.recordSuccess();
			} catch (Throwable e) {
				LOGGER.error("Security self test (provider properties) failed: {}", e.getMessage() ,e);
				providerResult.recordFatalError(e);
			}
		}

		securitySelfTestAlgorithm("AES", "AES/CBC/PKCS5Padding", null, false, result);
		OperationResult cryptoResult = result.getLastSubresult();
		if (cryptoResult.isError()) {
			// Do a test encryption. It happens sometimes that the key generator
            // generates a key that is not supported by the cipher.
			// Fall back to known key size supported by all JCE implementations
			securitySelfTestAlgorithm("AES", "AES/CBC/PKCS5Padding", 128, true, result);
			OperationResult cryptoResult2 = result.getLastSubresult();
			if (cryptoResult2.isSuccess()) {
				cryptoResult.setStatus(OperationResultStatus.HANDLED_ERROR);
			}
		}

		result.computeStatus();
	}

	private static void securitySelfTestAlgorithm(String algorithmName, String transformationName,
			Integer keySize, boolean critical, OperationResult parentResult) {
		OperationResult subresult = parentResult.createSubresult(CryptoUtil.class.getName()+".securitySelfTest.algorithm."+algorithmName);
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithmName);
			if (keySize != null) {
				keyGenerator.init(keySize);
			}
			subresult.addReturn("keyGeneratorProvider", keyGenerator.getProvider().getName());
			subresult.addReturn("keyGeneratorAlgorithm", keyGenerator.getAlgorithm());
			subresult.addReturn("keyGeneratorKeySize", keySize != null ? keySize : -1);

			SecretKey key = keyGenerator.generateKey();
			subresult.addReturn("keyAlgorithm", key.getAlgorithm());
			subresult.addReturn("keyLength", key.getEncoded().length*8);
			subresult.addReturn("keyFormat", key.getFormat());
			subresult.recordSuccess();

			IvParameterSpec iv = new IvParameterSpec(DEFAULT_IV_BYTES);

			String plainString = "Scurvy seadog";

			Cipher cipher = Cipher.getInstance(transformationName);
			subresult.addReturn("cipherAlgorithmName", algorithmName);
			subresult.addReturn("cipherTansfromationName", transformationName);
			subresult.addReturn("cipherAlgorithm", cipher.getAlgorithm());
			subresult.addReturn("cipherBlockSize", cipher.getBlockSize());
			subresult.addReturn("cipherProvider", cipher.getProvider().getName());
			subresult.addReturn("cipherMaxAllowedKeyLength", cipher.getMaxAllowedKeyLength(transformationName));
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] encryptedBytes = cipher.doFinal(plainString.getBytes());

            cipher = Cipher.getInstance(transformationName);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String decryptedString = new String(decryptedBytes);

            if (!plainString.equals(decryptedString)) {
            	subresult.recordFatalError("Encryptor roundtrip failed; encrypted="+plainString+", decrypted="+decryptedString);
			} else {
				subresult.recordSuccess();
			}
            LOGGER.debug("Security self test (algorithmName={}, transformationName={}, keySize={}) success",
					new Object[] {algorithmName, transformationName, keySize});
		} catch (Throwable e) {
			if (critical) {
				LOGGER.error("Security self test (algorithmName={}, transformationName={}, keySize={}) failed: {}-{}",
						new Object[] {algorithmName, transformationName, keySize, e.getMessage() ,e});
				subresult.recordFatalError(e);
			} else {
				LOGGER.warn("Security self test (algorithmName={}, transformationName={}, keySize={}) failed: {}-{} (failure is expected in some cases)",
						new Object[] {algorithmName, transformationName, keySize, e.getMessage() ,e});
				subresult.recordWarning(e);
			}
		}
	}

}
