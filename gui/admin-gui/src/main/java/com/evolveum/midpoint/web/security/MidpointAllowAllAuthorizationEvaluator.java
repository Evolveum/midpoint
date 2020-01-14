/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.StateOfModule;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.authentication.HttpModuleAuthentication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author skublik
 */

public class MidpointAllowAllAuthorizationEvaluator extends MidPointGuiAuthorizationEvaluator {

    public MidpointAllowAllAuthorizationEvaluator(SecurityEnforcer securityEnforcer, SecurityContextManager securityContextManager,
                                                  TaskManager taskManager) {
        super(securityEnforcer, securityContextManager, taskManager);
    }

    @Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes) throws AccessDeniedException, InsufficientAuthenticationException {
    }
}
