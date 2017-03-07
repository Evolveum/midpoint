package com.evolveum.midpoint.tools.ninja;

import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.UnrecoverableEntryException;
import java.util.Enumeration;

import javax.crypto.SecretKey;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.evolveum.midpoint.prism.crypto.ProtectorImpl;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;

public class KeyStoreDumper extends BaseNinjaAction{
	
	public void execute(){
		
		try{
		ApplicationContext context = new ClassPathXmlApplicationContext(CONTEXTS);
		
		Protector protector = context.getBean("protector", Protector.class);
		KeyStore keyStore = protector.getKeyStore();
		
		System.out.println("###################################################");
		System.out.println("Printing keys from key store");
		
		if (protector instanceof ProtectorImpl){
			ProtectorImpl aesProtector = (ProtectorImpl) protector;
			System.out.println("Using key store from location: " + aesProtector.getKeyStorePath());
//			System.out.println("Cipher: " + aesProtector.getXmlCipher());
			
		}
		
		Enumeration<String> aliases = keyStore.aliases();
		
		while (aliases.hasMoreElements()){
			String alias = aliases.nextElement();
			
			System.out.println("===== ALIAS: "+ alias +"=====");
			System.out.println("Creation date: " + keyStore.getCreationDate(alias));
			System.out.println("Type: " + keyStore.getType());
			if (keyStore.getCertificate(alias) != null){
				System.out.println("Certificate: " + keyStore.getCertificate(alias));
			}
			if (keyStore.getCertificateChain(alias) != null){
				System.out.println("Certificate chain: " + keyStore.getCertificateChain(alias));
			}
			
			ProtectionParameter protParam = new KeyStore.PasswordProtection("midpoint".toCharArray());
			Entry entry = keyStore.getEntry(alias, protParam);
			
			
			if (entry instanceof SecretKeyEntry){
				System.out.println("Secret key entry: ");
				SecretKeyEntry skEntry = (SecretKeyEntry) entry;
				SecretKey key = skEntry.getSecretKey();
				System.out.println("	Algorithm: " + key.getAlgorithm());
				System.out.println("	Format: " + key.getFormat());
				System.out.println("	Key length: " + key.getEncoded().length * 8);
				if (protector instanceof ProtectorImpl) {
					System.out.println("	Key name: " + ((ProtectorImpl) protector).getSecretKeyDigest(key));
				}
//				Cipher cipher = Cipher.getInstance(key.getAlgorithm());
//				System.out.println("	Cipher algorithm" + cipher.getAlgorithm());
			}
			
			
			
			//TODO: add dump also for other types of keys
			Provider provider = keyStore.getProvider();
			System.out.println("Provder name: " + provider.getName() +"\n");
			
		}
		
		System.out.println("###################################################");
		
		} catch (KeyStoreException ex){
			System.out.println("Failed to print information about keyStore. Reason: " + ex.getMessage());
			return;
		} catch (UnrecoverableEntryException ex){
			System.out.println("Failed to print information about keyStore. Reason: " + ex.getMessage());
			return;
		} catch (NoSuchAlgorithmException ex){
			System.out.println("Failed to print information about keyStore. Reason: " + ex.getMessage());
			return;
		} catch (EncryptionException ex){
			System.out.println("Failed to print information about keyStore. Reason: " + ex.getMessage());
			return;
		}
		
		
		
		
		
	}

}
