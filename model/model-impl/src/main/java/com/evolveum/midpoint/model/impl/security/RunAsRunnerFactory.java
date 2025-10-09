/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.security.api.SecurityContextManager;

/**
 * Factory for RunAsRunner instances. Its sole purpose is to provide necessary autowired prism beans.
 */
@Component
public class RunAsRunnerFactory {

    @Autowired PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") RepositoryService repositoryService;
    @Autowired SecurityContextManager securityContextManager;

    /**
     * Do not forget to close returned runner! Use try-with-resources construct.
     */
    public RunAsRunner runner() {
        return new RunAsRunner(this);
    }
}
