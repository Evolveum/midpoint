/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
