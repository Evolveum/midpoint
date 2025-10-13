/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.authorization.evaluator;

import java.util.Collection;

import org.springframework.context.ApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.TaskManager;

/**
 * @author skublik
 */
public class MidpointAllowAllAuthorizationEvaluator extends MidPointGuiAuthorizationEvaluator {

    public MidpointAllowAllAuthorizationEvaluator(
            SecurityEnforcer securityEnforcer, SecurityContextManager securityContextManager, TaskManager taskManager, ApplicationContext applicationContext) {
        super(securityEnforcer, securityContextManager, taskManager, applicationContext);
    }

    @Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes)
            throws AccessDeniedException, InsufficientAuthenticationException {
    }
}
