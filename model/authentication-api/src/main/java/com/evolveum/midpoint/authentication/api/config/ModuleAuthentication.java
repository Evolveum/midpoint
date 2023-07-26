/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api.config;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.AutheticationFailedData;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import javax.xml.namespace.QName;

/**
 * Wrapper for authentication module, provide all information about actual state
 *
 * @author skublik
 */

public interface ModuleAuthentication {

    /**
     * @return identifier of the authentication module, get from configuration
     */
    String getModuleIdentifier();

    /**
     * @return type of authentication module
     */
    String getModuleTypeName();

    /**
     * @return state of module
     */
    AuthenticationModuleState getState();

    void setState(AuthenticationModuleState state);

    /**
     * @return authentication token for module
     */
    Authentication getAuthentication();

    @Experimental
    void setAuthentication(Authentication authentication);

    /**
     * @return prefix used in url
     */
    String getPrefix();

    /**
     * @return type of authenticated object, get from configuration
     */
    QName getFocusType();

    /**
     * @return necessity for this module
     */
    AuthenticationSequenceModuleNecessityType getNecessity();

    /**
     * @return order for this module
     */
    Integer getOrder();

    /**
     * Decide if the module should be evaluated during sequence evaluation.
     * Module can be skipped, e.g. when acceptEmpty is set to true and
     * credential type doesn't exist.
     *
     * Example usage: Hint might be configured for some users but not fo all.
     * We want to show hint when it is set, but not, if it's not. Therefore,
     * hint module should user acceptEmpty = true and this module will be
     * automatically skipped when no hint is set.
     */
    boolean applicable();

    /**
     * Very bad name :)
     *
     * Specify is the module on its own is considered as sufficient for the
     * authentication to succeed. For example, password authenticated is
     * sufficient, therefore user should be authenticated and allowed to
     * work with midPoint. But focusIdentification cannot be considered
     * as sufficient, as it might be of a great risk to allow user to authenticate
     * only with using primary identifier.
     *
     * Insufficient modules are: focusIdentification, attributeVerification, hint
     * If only those modules are in sequence, sequence cannot be evaluated as successuly
     * authenticated
     */
    boolean isSufficient();

    void setSufficient(boolean sufficient);

    void setFailureData(AutheticationFailedData autheticationFailedData);

    AutheticationFailedData getFailureData();

    void recordFailure(AuthenticationException exception);

    void setFocusType(QName focusType);

    ModuleAuthentication clone();
}

