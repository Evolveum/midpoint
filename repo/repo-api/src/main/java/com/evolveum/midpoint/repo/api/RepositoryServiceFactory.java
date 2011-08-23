package com.evolveum.midpoint.repo.api;

import org.apache.commons.configuration.Configuration;

import com.evolveum.midpoint.common.configuration.api.RuntimeConfiguration;

public interface RepositoryServiceFactory extends RuntimeConfiguration {

	public abstract void init() throws RepositoryServiceFactoryException;

	public abstract void destroy() throws RepositoryServiceFactoryException;

	public abstract RepositoryService getRepositoryService() throws RepositoryServiceFactoryException;
	
	public abstract void setConfiguration(Configuration config);

}