/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.api.config.RemoteModuleAuthentication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */

public class RemoteModuleAuthenticationImpl extends ModuleAuthenticationImpl implements RemoteModuleAuthentication, Serializable {

    public static final String AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX = "/authenticate";
    public static final String AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX_WITH_REG_ID =
            AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX + "/{registrationId}";
    public static final String AUTHORIZATION_REQUEST_PROCESSING_URL_SUFFIX = "/authorization";
    public static final String AUTHORIZATION_REQUEST_PROCESSING_URL_SUFFIX_WITH_REG_ID =
            AUTHORIZATION_REQUEST_PROCESSING_URL_SUFFIX + "/{registrationId}";

    private List<IdentityProvider> providers = new ArrayList<>();

    public RemoteModuleAuthenticationImpl(String nameOfType, AuthenticationSequenceModuleType sequenceModule) {
        super(nameOfType, sequenceModule);
    }

    public void setProviders(List<IdentityProvider> providers) {
        this.providers = providers;
    }

    public List<IdentityProvider> getProviders() {
        return providers;
    }
}
