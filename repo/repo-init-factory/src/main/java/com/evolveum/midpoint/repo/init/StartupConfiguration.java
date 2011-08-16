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
package com.evolveum.midpoint.repo.init;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationFactory;
import org.apache.commons.lang.NotImplementedException;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;

public class StartupConfiguration {

	private Configuration config = null;
	
	private static final Trace logger = TraceManager.getTrace(StartupConfiguration.class);
	
	public Configuration getConfiguration( String componentName) {
		loadConfiguration();
		return null;
	}
	
	public void load() {
		throw new NotImplementedException();
	}
	
	public void save() {
		throw new NotImplementedException();
	}
	
	private void loadConfiguration() {
		if (config != null) {
			config.clear();
		}
		ConfigurationFactory configFactory = new ConfigurationFactory("config.xml");
		try {
			config = configFactory.getConfiguration();
		} catch (ConfigurationException e) {
			logger.error("Error occured during loading configuration file config.xml: " + e.getMessage(), e);
			System.out.println("Error occured during loading configuration file config.xml: " + e.getMessage());
		}
	}
}
