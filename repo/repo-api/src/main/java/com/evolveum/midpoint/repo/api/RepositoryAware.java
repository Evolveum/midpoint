/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

/**
 * Interface for beans that are repository-ware, i.e. those that hold
 * the instance of repository service.
 *
 * @author Radovan Semancik
 */
public interface RepositoryAware {

    RepositoryService getRepositoryService();

    void setRepositoryService(RepositoryService repositoryService);

}
