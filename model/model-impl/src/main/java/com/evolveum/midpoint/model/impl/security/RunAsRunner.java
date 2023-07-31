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
import com.evolveum.midpoint.util.CheckedCommonRunnable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;

import javax.xml.namespace.QName;
import java.util.Objects;

/**
 * Takes care of optimized "run as" operations.
 *
 * By optimization we mean that if the currently logged-in user is the same as the one specified in runAs,
 * no login procedure is carried out.
 *
 * BEWARE: In order to return the original security context, the close() method must be called.
 * This object is therefore to be used in try-with-resources context ONLY.
 */
@Experimental
public class RunAsRunner implements AutoCloseable {

    @NotNull private final RunAsRunnerFactory beans;

    private final Authentication originalAuthentication;

    // Should be instantiated via factory only.
    RunAsRunner(@NotNull RunAsRunnerFactory factory) {
        this.beans = factory;
        this.originalAuthentication = beans.securityContextManager.getAuthentication();
    }

    @Override
    public void close() {
        beans.securityContextManager.setupPreAuthenticatedSecurityContext(originalAuthentication);
    }

    public void runAs(CheckedCommonRunnable runnable, ObjectReferenceType identityRef, OperationResult parentResult)
            throws CommonException {
        establishRequiredIdentity(identityRef, parentResult);
        runnable.run();
    }

    private void establishRequiredIdentity(ObjectReferenceType identityRef, OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        if (identityRef != null) {
            String oid = identityRef.getOid();
            if (oid == null) {
                throw new UnsupportedOperationException("Identity reference without OID is not supported");
            }
            if (!isLoggedIn(oid)) {
                QName typeName = Objects.requireNonNull(identityRef.getType(), "target type");
                logIn(typeName, oid, parentResult);
            }
        } else {
            // no requirements here
        }
    }

    private boolean isLoggedIn(@NotNull String oid) throws SecurityViolationException {
        MidPointPrincipal principal = beans.securityContextManager.getPrincipal();
        return principal != null && oid.equals(principal.getOid());
    }

    private void logIn(QName typeName, String oid, OperationResult parentResult) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        Class<? extends FocusType> clazz = getObjectClass(typeName);
        PrismObject<? extends FocusType> focus = beans.repositoryService.getObject(clazz, oid, null, parentResult);
        beans.securityContextManager.setupPreAuthenticatedSecurityContext(focus, parentResult);
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
