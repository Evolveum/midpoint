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
 * Portions Copyrighted 2011 Igor Farinic
 * Portions Copyrighted 2011 Peter Prochazka
 */
package com.evolveum.midpoint.init;

import org.apache.commons.configuration.Configuration;
import org.apache.cxf.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.RuntimeConfiguration;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactory;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

@Component
public class RepositoryFactory implements RuntimeConfiguration {

	private static final Trace LOGGER = TraceManager.getTrace(RepositoryFactory.class);
	
	@Autowired
	MidpointConfiguration midpointConfiguration;
	Configuration config;
	RepositoryServiceFactory repositoryServiceFactory;
	RepositoryService repositoryService;
	
	public void init() {
		loadConfiguration();
		String repositoryServiceFactoryClassName = config.getString("repositoryServiceFactoryClass");
		
		if (StringUtils.isEmpty(repositoryServiceFactoryClassName)) {
			LOGGER.error("RepositoryServiceFactory implementation class name (repositoryServiceFactoryClass) not found in configuration. Provided configuration: {}", config);
			throw new SystemException("RepositoryServiceFactory implementation class name (repositoryServiceFactoryClass) not found in configuration. Provided configuration: " + config);
		}
		
		ClassLoader classLoader = RepositoryFactory.class.getClassLoader();

	    try {
	        Class<RepositoryServiceFactory> repositoryServiceFactoryClass = (Class<RepositoryServiceFactory>) classLoader.loadClass(repositoryServiceFactoryClassName);
	        repositoryServiceFactory = repositoryServiceFactoryClass.newInstance();
	        repositoryServiceFactory.setConfiguration(config);
	        repositoryServiceFactory.init();
	    } catch (ClassNotFoundException e) {
	    	LoggingUtils.logException(LOGGER, "RepositoryServiceFactory implementation class {} defined in configuration was not found.", e, repositoryServiceFactoryClassName);
	    	throw new SystemException("RepositoryServiceFactory implementation class "+repositoryServiceFactoryClassName+" defined in configuration was not found.", e);
	    } catch (InstantiationException e) {
	    	LoggingUtils.logException(LOGGER, "RepositoryServiceFactory implementation class {} could not be instantiated.", e, repositoryServiceFactoryClassName);
	    	throw new SystemException("RepositoryServiceFactory implementation class "+repositoryServiceFactoryClassName+" could not be instantiated.", e);
		} catch (IllegalAccessException e) {
			LoggingUtils.logException(LOGGER, "RepositoryServiceFactory implementation class {} could not be instantiated.", e, repositoryServiceFactoryClassName);
	    	throw new SystemException("RepositoryServiceFactory implementation class "+repositoryServiceFactoryClassName+" could not be instantiated.", e);
		} catch (RepositoryServiceFactoryException e) {
			LoggingUtils.logException(LOGGER, "RepositoryServiceFactory implementation class {} failed to initialize.", e, repositoryServiceFactoryClassName);
	    	throw new SystemException("RepositoryServiceFactory implementation class "+repositoryServiceFactoryClassName+" failed to initialize.", e);
		}
		
	}
	
	public void destroy() {
		if (repositoryServiceFactory != null) {
			try {
				repositoryServiceFactory.destroy();
			} catch (RepositoryServiceFactoryException e) {
		    	LoggingUtils.logException(LOGGER, "Failed to destroy RepositoryServiceFactory", e);
		    	throw new SystemException("Failed to destroy RepositoryServiceFactory", e);
			}
		} else {
			LOGGER.error("RepositoryFactory is in illegal state, repositoryServiceFactory cannot be destroyed, becuase it is not set");
			throw new IllegalStateException("RepositoryFactory is in illegal state, repositoryServiceFactory cannot be destroyed, becuase it is not set");
		}
	}
	
	@Override
	public String getComponentId() {
		return "midpoint.repository";
	}

	@Override
	public Configuration getCurrentConfiguration() {
		if (repositoryServiceFactory != null) {
			config = repositoryServiceFactory.getCurrentConfiguration();
		} else {
			LOGGER.error("RepositoryFactory is in illegal state, repositoryServiceFactory is not set");
			throw new IllegalStateException("RepositoryFactory is in illegal state, repositoryServiceFactory is not set");
		}
		return config;
	}
	
	public synchronized RepositoryService getRepositoryService() {
		if (repositoryService == null) {
			try {
				repositoryService = repositoryServiceFactory.getRepositoryService();
			} catch (RepositoryServiceFactoryException e) {
				LoggingUtils.logException(LOGGER, "Failed to get repository service from factory", e);
				throw new SystemException("Failed to get repository service from factory", e);
			}
		}
		return repositoryService;
	}
	
	private void loadConfiguration() {
		config = midpointConfiguration.getConfiguration("midpoint.repository");
		
	}

	
}
