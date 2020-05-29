/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
