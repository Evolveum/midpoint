/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import org.apache.commons.configuration2.Configuration;

/**
 * Contract for repository factory implementation used by {@code RepositoryFactory} (system-init).
 * NOTE: This is probably not part of repo-api, it's more more repo-spi, which we don't have.
 * It really belongs more to that RepositoryFactory, but concrete implementations don't depend
 * on system-init module, so it's stuck here for now.
 */
public interface RepositoryServiceFactory {

    void init(Configuration configuration) throws RepositoryServiceFactoryException;

    void destroy() throws RepositoryServiceFactoryException;

    RepositoryService getRepositoryService() throws RepositoryServiceFactoryException;
}
