/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.factory.module;

import com.evolveum.midpoint.model.api.authentication.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.ModuleWebSecurityConfig;
import com.evolveum.midpoint.web.security.util.AuthModuleImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import javax.servlet.ServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author skublik
 */

public abstract class AbstractCredentialModuleFactory<C extends ModuleWebSecurityConfiguration> extends AbstractModuleFactory {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractCredentialModuleFactory.class);

    @Override
    public abstract boolean match(AbstractAuthenticationModuleType moduleType);

    @Override
    public AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String prefixOfSequence,
                                         ServletRequest request, Map<Class<? extends Object>, Object> sharedObjects,
                                         AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel) throws Exception {

        if (!(moduleType instanceof AbstractCredentialAuthenticationModuleType)) {
            LOGGER.error("This factory support only AbstractPasswordAuthenticationModuleType, but modelType is " + moduleType);
            return null;
        }

        isSupportedChannel(authenticationChannel);

        C configuration = createConfiguration(moduleType, prefixOfSequence, authenticationChannel);

        configuration.addAuthenticationProvider(getProvider((AbstractCredentialAuthenticationModuleType)moduleType, credentialPolicy));

        ModuleWebSecurityConfig module = createModule(configuration);
        module.setObjectPostProcessor(getObjectObjectPostProcessor());
        HttpSecurity http = module.getNewHttpSecurity();
        setSharedObjects(http, sharedObjects);

        ModuleAuthentication moduleAuthentication = createEmptyModuleAuthentication(moduleType, configuration);
        moduleAuthentication.setFocusType(moduleType.getFocusType());
        SecurityFilterChain filter = http.build();
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    protected AuthenticationProvider getProvider(AbstractCredentialAuthenticationModuleType moduleType, CredentialsPolicyType credentialsPolicy){
//        Validate.notNull(credentialsPolicy);
        CredentialPolicyType usedPolicy = null;
        String credentialName = moduleType.getCredentialName();


            List<CredentialPolicyType> credentialPolicies = new ArrayList<CredentialPolicyType>();
            if (credentialsPolicy != null) {
                credentialPolicies.add(credentialsPolicy.getPassword());
                credentialPolicies.add(credentialsPolicy.getSecurityQuestions());
                credentialPolicies.addAll(credentialsPolicy.getNonce());
            }

            for (CredentialPolicyType processedPolicy : credentialPolicies) {
                if (processedPolicy != null) {
                    if (StringUtils.isNotBlank(credentialName)) {
                        if (credentialName.equals(processedPolicy.getName())) {
                            usedPolicy = processedPolicy;
                        }
                    } else if (processedPolicy.getClass().isAssignableFrom(supportedClass())) {
                        usedPolicy = processedPolicy;
                    }
                }
            }
        if (usedPolicy == null && PasswordCredentialsPolicyType.class.equals(supportedClass())) {
            return getObjectObjectPostProcessor().postProcess(createProvider(null));
        }
        if (usedPolicy == null) {
            String message = StringUtils.isBlank(credentialName) ? ("Couldn't find credentialfor module " + moduleType) : ("Couldn't find credential with name " + credentialName);
            IllegalArgumentException e = new IllegalArgumentException(message);
            LOGGER.error(message);
            throw e;
        }

        if (!usedPolicy.getClass().equals(supportedClass())) {
            String message = "Module " + moduleType.getName() + "support only " + supportedClass() + " type of credential" ;
            IllegalArgumentException e = new IllegalArgumentException(message);
            LOGGER.error(message);
            throw e;
        }

        return getObjectObjectPostProcessor().postProcess(createProvider(usedPolicy));
    }

    protected abstract ModuleAuthentication createEmptyModuleAuthentication(AbstractAuthenticationModuleType moduleType, C configuration);

    protected abstract C createConfiguration(AbstractAuthenticationModuleType moduleType, String prefixOfSequence, AuthenticationChannel authenticationChannel);

    protected abstract ModuleWebSecurityConfig createModule (C configuration);

    protected abstract AuthenticationProvider createProvider (CredentialPolicyType usedPolicy);

    protected abstract Class<? extends CredentialPolicyType> supportedClass ();
}
