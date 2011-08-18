/**
 * 
 */
package com.evolveum.midpoint.common.crypto;

import java.security.KeyStore;

import org.apache.commons.lang.NotImplementedException;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;

/**
 * Class that manages encrypted string values.
 * 
 * 
 * 
 * @author Radovan Semancik
 */
public class Protector {
	
	private KeyStore keyStore;
	
	public String decrypt(ProtectedStringType protectedString) {
		// TODO
		throw new NotImplementedException();
	}

	public ProtectedStringType encrypt(String string) {
		// TODO
		throw new NotImplementedException();
	}
	
	public void initialize(KeyStore keystore) {
		this.keyStore = keyStore;
	}
}
