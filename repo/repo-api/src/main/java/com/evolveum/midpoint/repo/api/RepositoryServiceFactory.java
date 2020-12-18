/*
 * Copyright (C) 2010-2020 Evolveum and contributors
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

    /** Method injecting configuration, called by initialization procedure. */
    void init(Configuration configuration) throws RepositoryServiceFactoryException;

    /**
     * Destroy method implements necessary shutdown steps.
     * Do not hook this to any life-cycle method (e.g. don't annotate as pre-destroy),
     * it will be called by the general repository infrastructure.
     */
    void destroy() throws RepositoryServiceFactoryException;

    /** Factory method used by initialization procedure. */
    RepositoryService createRepositoryService() throws RepositoryServiceFactoryException;
}
