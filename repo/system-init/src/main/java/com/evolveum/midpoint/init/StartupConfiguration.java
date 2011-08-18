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

import java.net.URL;
import java.util.Iterator;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.NotImplementedException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class StartupConfiguration {

	private static final Trace logger = TraceManager.getTrace(StartupConfiguration.class);

	private CompositeConfiguration config = null;
	private String configFilename = null;

	private String getConfigFilename() {
		if (this.configFilename == null) {
			configFilename = "config.xml";
		}
		return configFilename;
	}

	public void setConfigFilename(String configFilename) {
		this.configFilename = configFilename;
	}


	public StartupConfiguration() {
		loadConfiguration();
	}

	public StartupConfiguration(String configFilename) {
		this.configFilename = configFilename;
		loadConfiguration();
	}

	
	/**
	 * Read subset of configuration for component
	 * 
	 * @param componentName
	 * @return
	 */
	public Configuration getConfiguration( String componentName) {
		return config.subset(componentName);
	}
	
	public void load() {
		loadConfiguration();
	}
	
	public void save() {
		throw new NotImplementedException();
	}
	
	
	
	private void loadConfiguration() {
		if (config != null) {
			config.clear();
		} else {
			config = new CompositeConfiguration();
		}
		
		/* loading precednecy and configuration logic */
		if (null != System.getProperty("midpoint.home") ) {
			
			/* Do default loading */
		} else {
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
		}
		
		// debug :-)
		
		
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
	
	public String dumpConfig(Configuration c) {
		@SuppressWarnings("unchecked")
		Iterator<String> i = c.getKeys();
		StringBuilder sb = new  StringBuilder();
		while (i.hasNext() ) {
			String key = i.next();
			sb.append(key);
			sb.append(" = ");
			sb.append(c.getString(key));
			sb.append("; ");
		}
		return sb.toString();
	}
}
