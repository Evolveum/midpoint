/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * 
 * Portions Copyrighted 2011 [name of copyright owner]
 *  * Portions Copyrighted 2011 Peter Prochazka
 */
package com.evolveum.midpoint.init;

import java.io.File;

import javax.annotation.PostConstruct;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.RuntimeConfiguration;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.crypto.AESProtector;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author mamut
 *
 */
@Component(value = "protector")
public class KeyStoreConfigurator extends AESProtector implements RuntimeConfiguration, Protector {

	private String keyStorePath;
	private String keyStorePassword;
	private String encryptionKeyAlias;

	@Autowired(required = true)
	private MidpointConfiguration midpointConfiguration;

	private static final String COMPONENT_NAME = "midpoint.keystore";
	
	private static final Trace LOGGER = TraceManager.getTrace(KeyStoreConfigurator.class);

	public KeyStoreConfigurator() {
		super();
	}

	@PostConstruct
	public void init() {
		Configuration c = midpointConfiguration.getConfiguration(COMPONENT_NAME);
		this.setKeyStorePath(c.getString("keyStorePath"));
		this.setKeyStorePassword(c.getString("keyStorePassword"));
		this.setEncryptionKeyAlias(c.getString("encryptionKeyAlias"));

		//Extract file if not exists
		if (c.getString("midpoint.home") != null) {
			File ks = new File(this.getKeyStorePath());
			if (!ks.exists()) {
				if (c.getString("keyStorePath").endsWith("keystore.jceks")) {
					//hack to try 2 class paths
					if (!ClassPathUtil.extractFileFromClassPath("com/../../keystore.jceks", this.getKeyStorePath())) {
						ClassPathUtil.extractFileFromClassPath("keystore.jceks", this.getKeyStorePath());
					}
				} else if (c.getString("keyStorePath").endsWith("test-keystore.jceks")) {
					if (!ClassPathUtil
							.extractFileFromClassPath("com/../../test-keystore.jceks", this.getKeyStorePath())) {
						ClassPathUtil.extractFileFromClassPath("test-keystore.jceks", this.getKeyStorePath());
					}
				} else {
					LOGGER.error("Unable to find/extract keystore file {} from classpath", c.getString("keyStorePath"));
				}
			}
		}

		super.init();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.configuration.api.RuntimeConfiguration#getComponentId()
	 */
	@Override
	public String getComponentId() {
		return COMPONENT_NAME;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.configuration.api.RuntimeConfiguration#getCurrentConfiguration()
	 */
	@Override
	public Configuration getCurrentConfiguration() {
		Configuration config = new BaseConfiguration();
		config.setProperty("keyStorePath", this.getKeyStorePath());
		config.setProperty("keyStorePassword", this.getKeyStorePassword());
		config.setProperty("defaultKeyAlias", this.getEncryptionKeyAlias());
		return config;
	}

	/**
	 * @return the keyStorePath
	 */
	public String getKeyStorePath() {
		return keyStorePath;
	}

	/**
	 * @param keyStorePath the keyStorePath to set
	 */
	@Override
	public void setKeyStorePath(String keyStorePath) {
		this.keyStorePath = keyStorePath;
		super.setKeyStorePath(keyStorePath);
	}

	/**
	 * @return the keyStorePassword
	 */
	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	/**
	 * @param keyStorePassword the keyStorePassword to set
	 */
	@Override
	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
		super.setKeyStorePassword(keyStorePassword);
	}

	/**
	 * @return the defaultKeyAlias
	 */
	public String getEncryptionKeyAlias() {
		return encryptionKeyAlias;
	}

	/**
	 * @param defaultKeyAlias the defaultKeyAlias to set
	 */
	@Override
	public void setEncryptionKeyAlias(String keyAlias) {
		this.encryptionKeyAlias = keyAlias;
		super.setEncryptionKeyAlias(keyAlias);
	}
}
