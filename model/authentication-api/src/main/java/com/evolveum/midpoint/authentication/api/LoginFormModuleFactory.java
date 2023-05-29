/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModulesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;

import jakarta.servlet.ServletRequest;
import java.util.Map;

/**
 * not use it, temporary needed interface for old reset password configuration
 */
@Experimental
public interface LoginFormModuleFactory {

    AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType,
            String prefixOfSequence, ServletRequest request, Map<Class<?>, Object> sharedObjects,
            AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy,
            AuthenticationChannel authenticationChannel);
}
