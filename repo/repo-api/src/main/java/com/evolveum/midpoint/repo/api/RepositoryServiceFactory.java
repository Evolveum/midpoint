package com.evolveum.midpoint.repo.api;

import org.apache.commons.configuration.Configuration;

public interface RepositoryServiceFactory {

	void init(Configuration configuration) throws RepositoryServiceFactoryException;

	void destroy() throws RepositoryServiceFactoryException;

	RepositoryService getRepositoryService() throws RepositoryServiceFactoryException;
}