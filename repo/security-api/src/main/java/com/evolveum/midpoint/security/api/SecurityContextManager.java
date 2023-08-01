/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.CheckedProducer;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.io.Serializable;

/**
 * Manager of security context. Used for storing authentication into
 * the security context, set up security context for task ownership, etc.
 *
 * This is a part of low-level security functions. Those are security functions that
 * deal with the basic concepts of authentication, task ownership,
 * security context and so on.
 */
public interface SecurityContextManager {

    boolean isAuthenticated();

    Authentication getAuthentication();

    /**
     * Returns principal representing the currently logged-in user.
     * Assumes that the user is logged-in. Otherwise an exception is thrown.
     */
    MidPointPrincipal getPrincipal() throws SecurityViolationException;

    /**
     * Returns OID of the current principal. After login is complete, the returned OID is the same as
     * getPrincipal().getOid(). However, during login process, this method returns the OID of the user that is
     * being authenticated/logged-in (a.k.a. temporary principal OID).
     */
    String getPrincipalOid();

    /**
     * Internal method to set temporary principal OID used during login process as a return value of getPrincipalOid() method.
     */
    void setTemporaryPrincipalOid(String value);

    /**
     * Internal method to reset temporary principal OID.
     */
    void clearTemporaryPrincipalOid();

    void setupPreAuthenticatedSecurityContext(Authentication authentication);

    void setupPreAuthenticatedSecurityContext(MidPointPrincipal principal);

    /** Calls {@link MidPointPrincipalManager} to create a principal from provided focus object and sets it up. */
    void setupPreAuthenticatedSecurityContext(PrismObject<? extends FocusType> focus, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException;

    /** Runs the provided code (within {@link ResultAwareProducer}) as a specific user and/or with elevated privileges. */
    <T> T runAs(
            @NotNull ResultAwareProducer<T> producer,
            @Nullable PrismObject<? extends FocusType> newPrincipalObject,
            boolean privileged,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException;

    /** Convenience method to deal with producers that can throw any {@link CommonException}. */
    default <T> T runAsChecked(
            ResultAwareCheckedProducer<T> producer,
            PrismObject<? extends UserType> newPrincipalObject,
            OperationResult result) throws CommonException {
        try {
            return runAs(lResult -> {
                try {
                    return producer.get(lResult);
                } catch (CommonException e) {
                    throw new TunnelException(e);
                }
            }, newPrincipalObject, false, result);
        } catch (TunnelException te) {
            MiscUtil.unwrapTunnelledException(te);
            throw new NotHereAssertionError();
        }
    }

    /** Runs the provided code (within {@link Producer}) with elevated privileges. */
    <T> T runPrivileged(@NotNull Producer<T> producer);

    /** Convenience method to deal with producers that can throw {@link CommonException}. */
    default <T> T runPrivilegedChecked(CheckedProducer<T> producer) throws CommonException {
        return MiscUtil.runChecked(this::runPrivileged, producer);
    }

    MidPointPrincipalManager getUserProfileService();

    void setUserProfileService(MidPointPrincipalManager userProfileService);

    /**
     * Store connection information for later use within current thread.
     */
    void storeConnectionInformation(@Nullable HttpConnectionInformation value);

    /**
     * Returns stored connection information.
     * Should be used for non-HTTP threads that have no access to stored Request object (see {@link SecurityUtil#getCurrentConnectionInformation()}).
     */
    @Nullable
    HttpConnectionInformation getStoredConnectionInformation();

    /** Producer of a value that is {@link Serializable} and operates under given {@link OperationResult}. */
    @FunctionalInterface
    interface ResultAwareProducer<T> extends Serializable {
        T get(@NotNull OperationResult result)
                throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
                ExpressionEvaluationException;
    }

    /** A {@link ResultAwareProducer} that can throw any {@link CommonException}. */
    @FunctionalInterface
    interface ResultAwareCheckedProducer<T> extends Serializable {
        T get(@NotNull OperationResult result)
                throws CommonException;
    }
}
