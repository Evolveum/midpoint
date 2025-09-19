/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager.ResultAwareProducer;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionPrivilegesSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * Takes care of optimized "run as" operations.
 *
 * By optimization we mean that if the currently logged-in user is the same as the one specified in runAs,
 * no login procedure is carried out.
 *
 * BEWARE: In order to return the original security context, the close() method must be called.
 * This object is therefore to be used in try-with-resources context ONLY.
 *
 * TODO: Rework this class. The original idea was that we can execute a set of actions, each possibly under different
 *  identity and/or "run privileged" flag, and this class would minimize the number of required logins. This worked well
 *  before the introduction "run privileged" flag. But after it was added, the switching of contexts became non-trivial.
 *  Hence, we now establish the identity for each request separately, optimizing only "runAs" operation against currently
 *  logged-in user. We'll optimize this later.
 */
@Experimental
public class RunAsRunner {

    @NotNull private final RunAsRunnerFactory beans;

    // Should be instantiated via factory only.
    RunAsRunner(@NotNull RunAsRunnerFactory factory) {
        this.beans = factory;
    }

    public void runAs(
            @NotNull ResultAwareProducer<Void> runnable,
            @Nullable ExecutionPrivilegesSpecificationType privilegesSpecification,
            OperationResult result)
            throws CommonException {
        var runAsFocus = getRequestedIdentityIfNeeded(privilegesSpecification, result);
        var runPrivileged = privilegesSpecification != null && Boolean.TRUE.equals(privilegesSpecification.isRunPrivileged());
        beans.securityContextManager.runAs(runnable, runAsFocus, runPrivileged, result);
    }

    /**
     * Returns the identity we need to switch to. Returns {@code null} if there's no identity OR we already run under
     * the requested user OID.
     */
    private @Nullable PrismObject<? extends FocusType> getRequestedIdentityIfNeeded(
            @Nullable ExecutionPrivilegesSpecificationType privilegesSpecification, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException {
        if (privilegesSpecification == null) {
            return null;
        }
        var identityRef = privilegesSpecification.getRunAsRef();
        if (identityRef == null) {
            return null;
        }
        String oid = MiscUtil.requireNonNull(identityRef.getOid(), () -> "Identity reference without OID is not supported");
        if (isLoggedIn(oid)) {
            return null; // already logged as requested OID -> no need to switch the context
        }
        QName typeName = MiscUtil.stateNonNull(identityRef.getType(), "No target type in 'runAsRef' property");
        Class<? extends FocusType> clazz = getObjectClass(typeName);
        return beans.repositoryService.getObject(clazz, oid, null, result);
    }

    private boolean isLoggedIn(@NotNull String oid) throws SecurityViolationException {
        MidPointPrincipal principal = beans.securityContextManager.getPrincipal();
        return principal != null && oid.equals(principal.getOid());
    }

    @NotNull
    private Class<? extends FocusType> getObjectClass(QName typeName) {
        Class<?> clazz = beans.prismContext.getSchemaRegistry().determineClassForTypeRequired(typeName);
        if (!FocusType.class.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Identity is not a FocusType: " + clazz + " (name: " + typeName + ")");
        }
        //noinspection unchecked
        return (Class<? extends FocusType>) clazz;
    }
}
