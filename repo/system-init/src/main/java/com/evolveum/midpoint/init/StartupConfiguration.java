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
 * Portions Copyrighted 2011 [name of copyright owner] Portions Copyrighted 2011
 * Peter Prochazka
 */
package com.evolveum.midpoint.init;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.NotImplementedException;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class StartupConfiguration implements MidpointConfiguration {

	private static final Trace LOGGER = TraceManager.getTrace(StartupConfiguration.class);
	private static final String MIDPOINT_HOME = "midpoint.home";

	private CompositeConfiguration config = null;
	private String configFilename = null;

	/**
	 * Default constructor
	 */
	public StartupConfiguration() {
		this.configFilename = "config.xml";
	}

	/**
	 * Constructor
	 * 
	 * @param configFilename
	 *            alternative configuration file
	 */
	public StartupConfiguration(String configFilename) {
		this.configFilename = configFilename;
	}

	/**
	 * Get current configuration file name
	 * 
	 * @return
	 */
	public String getConfigFilename() {
		return this.configFilename;
	}

	/**
	 * Set configuration filename
	 * 
	 * @param configFilename
	 */
	public void setConfigFilename(String configFilename) {
		this.configFilename = configFilename;
	}

	@Override
	public Configuration getConfiguration(String componentName) {
		if (null == componentName) {
			throw new IllegalArgumentException("NULL argument");
		}
		Configuration sub = config.subset(componentName);
		// Insert replacement for relative path to midpoint.home else clean
		// replace
		if (System.getProperty(MIDPOINT_HOME) != null) {
			sub.addProperty(MIDPOINT_HOME, System.getProperty(MIDPOINT_HOME));
		} else {
			@SuppressWarnings("unchecked")
			Iterator<String> i = sub.getKeys();
			while (i.hasNext()) {
				String key = i.next();
				sub.setProperty(key, sub.getString(key).replace("${" + MIDPOINT_HOME + "}/", ""));
				sub.setProperty(key, sub.getString(key).replace("${" + MIDPOINT_HOME + "}", ""));
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Configuration for {} :", componentName);
			@SuppressWarnings("unchecked")
			Iterator<String> i = sub.getKeys();
			while (i.hasNext()) {
				String key = i.next();
				LOGGER.debug("    {} = {}", key, sub.getString(key));
			}
		}
		return sub;
	}

	/**
	 * Initialize system configuration
	 */
	public void init() {
		if (System.getProperty(MIDPOINT_HOME) == null || System.getProperty(MIDPOINT_HOME).isEmpty()) {
			LOGGER.warn("*****************************************************************************************");
			LOGGER.warn(MIDPOINT_HOME
					+ " is not set ! Using default configuration, for more information see http://wiki.evolveum.com/display/midPoint/");
			LOGGER.warn("*****************************************************************************************");

			System.out.println("*******************************************************************************");
			System.out.println(MIDPOINT_HOME + " is not set ! Using default configuration, for more information");
			System.out.println("                 see http://wiki.evolveum.com/display/midPoint/");
			System.out.println("*******************************************************************************");
			return;
		}

		loadConfiguration();
	}

	/**
	 * Load system configuration
	 */
	public void load() {
		loadConfiguration();
	}

	/**
	 * Save system configuration
	 * 
	 * @TODO not implement yet
	 */
	public void save() {
		throw new NotImplementedException();
	}

	/**
	 * Loading logic
	 */
	private void loadConfiguration() {
		if (config != null) {
			config.clear();
		} else {
			config = new CompositeConfiguration();
		}

		/* configuration logic */
		// load from midpoint.home
		if (null != System.getProperty(MIDPOINT_HOME)) {
			try {
				//Fix missing last slash in path
				if (!System.getProperty(MIDPOINT_HOME).endsWith("/")) {
					System.setProperty(MIDPOINT_HOME, System.getProperty(MIDPOINT_HOME) + "/");
				}

				//Load configuration
				String path = System.getProperty(MIDPOINT_HOME) + this.getConfigFilename();
				LOGGER.info("Loading midPoint configuration from file {}", path);
				File f = new File(path);
				if (!f.exists()) {
					LOGGER.warn("Configuration file {} does not exists. Need to do extraction ...", path);

					ApplicationHomeSetup ah = new ApplicationHomeSetup();
					ah.init(MIDPOINT_HOME);
					ClassPathUtil.extractFileFromClassPath("config.xml", path);

				}
				this.setConfigFilename(path);
				//Load and parse properties
				config.addProperty(MIDPOINT_HOME, System.getProperty(MIDPOINT_HOME));
				config.addConfiguration(new XMLConfiguration(this.getConfigFilename()));

			} catch (ConfigurationException e) {
				LOGGER.error("Unable to read configuration file [" + this.getConfigFilename() + "]:" + e.getMessage());
				System.out.println("Unable to read configuration file [" + this.getConfigFilename() + "]:"
						+ e.getMessage());
			}

		} else {
			// Load from class path
			try {
				config.addConfiguration(new XMLConfiguration(this.getConfigFilename()));
			} catch (ConfigurationException e) {
				LOGGER.error("Unable to read configuration file [" + this.getConfigFilename() + "]:" + e.getMessage());
				System.out.println("Unable to read configuration file [" + this.getConfigFilename() + "]:"
						+ e.getMessage());
			}
		}
	}

	@Override
	public String toString() {
		@SuppressWarnings("unchecked")
		Iterator<String> i = config.getKeys();
		StringBuilder sb = new StringBuilder();
		while (i.hasNext()) {
			String key = i.next();
			sb.append(key);
			sb.append(" = ");
			sb.append(config.getString(key));
			sb.append("; ");
		}
		return sb.toString();
	}
}
