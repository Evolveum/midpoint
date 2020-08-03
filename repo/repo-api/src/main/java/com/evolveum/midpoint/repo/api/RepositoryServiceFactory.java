/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import org.apache.commons.configuration2.Configuration;

public interface RepositoryServiceFactory {

    void init(Configuration configuration) throws RepositoryServiceFactoryException;

    void destroy() throws RepositoryServiceFactoryException;

    RepositoryService getRepositoryService() throws RepositoryServiceFactoryException;
}
