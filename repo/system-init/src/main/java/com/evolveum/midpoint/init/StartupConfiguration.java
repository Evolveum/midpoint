/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2011 Peter Prochazka
 */
package com.evolveum.midpoint.init;


import java.util.Iterator;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.NotImplementedException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;

public class StartupConfiguration implements MidpointConfiguration{

	private static final Trace logger = TraceManager.getTrace(StartupConfiguration.class);

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
	 * @param configFilename alternative configuration file
	 */
	public StartupConfiguration(String configFilename) {
		this.configFilename = configFilename;
	}
	
	/**
	 * Get current configuration file name
	 * @return
	 */
	public String getConfigFilename() {
		return this.configFilename;
	}

	/**
	 * Set configuration filename
	 * @param configFilename
	 */
	public void setConfigFilename(String configFilename) {
		this.configFilename = configFilename;
	}

	
	@Override
	public Configuration getConfiguration( String componentName) {
		return config.subset(componentName);
	}
	
	/**
	 * Initialize system configuration
	 */
	public void init() {
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
		
		/* loading precednecy and configuration logic */
//		if (null != System.getProperty("midpoint.home") ) {
			
			/* Do default loading */
	//	} else {
			try {
				// system options are priority 1.
				//config.addConfiguration(new SystemConfiguration());
				// provided file is priority 2.
				config.addConfiguration(new XMLConfiguration(this.getConfigFilename()));
				/*
				String repoConfigFilename = config.getString("midpoint.repository.class").replace(".","/");
				//load repository configuration from class
				URL repoDefaultConf = ClassLoader.getSystemResource(repoConfigFilename + "/default-repository-config.xml");
				if (repoDefaultConf != null) {
					config.addConfiguration(repoDefaultConf);
				}*/
				
			} catch (ConfigurationException e) {
				logger.error("Unable to read configuration file [" + this.getConfigFilename() + "]:" + e.getMessage());
				System.out.println("Unable to read configuration file [" + this.getConfigFilename() + "]:" + e.getMessage());
			}
		//}
	}
	
	@Override
	public String toString() {
		@SuppressWarnings("unchecked")
		Iterator<String> i = config.getKeys();
		StringBuilder sb = new  StringBuilder();
		while (i.hasNext() ) {
			String key = i.next();
			sb.append(key);
			sb.append(" = ");
			sb.append(config.getString(key));
			sb.append("; ");
		}
		return sb.toString();
	}
}
