/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.api;

import com.evolveum.midpoint.util.CheckedProducer;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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

    void setupPreAuthenticatedSecurityContext(PrismObject<? extends FocusType> focus) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    <T> T runAs(Producer<T> producer, PrismObject<UserType> user) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    /**
     * Convenience method to deal with producers that can throw CommonException.
     */
    default <T> T runAsChecked(CheckedProducer<T> producer, PrismObject<UserType> user) throws CommonException {
        return MiscUtil.runChecked((p) -> runAs(p, user), producer);
    }

    <T> T runPrivileged(Producer<T> producer);

    /**
     * Convenience method to deal with producers that can throw CommonException.
     */
    default <T> T runPrivilegedChecked(CheckedProducer<T> producer) throws CommonException {
        return MiscUtil.runChecked(this::runPrivileged, producer);
    }

    // runPrivileged method is in SecurityEnforcer. It needs to be there because it works with authorizations.

    MidPointPrincipalManager getGuiProfiledPrincipalManager();

    void setGuiProfiledPrincipalManager(MidPointPrincipalManager userProfileService);

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
}
