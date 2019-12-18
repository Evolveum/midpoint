/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.factory;

import com.evolveum.midpoint.model.api.authentication.AuthModule;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.model.api.authentication.AuthModuleImpl;
import com.evolveum.midpoint.web.security.module.ModuleWebSecurityConfig;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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

public abstract class AbstractPasswordModuleFactory extends AbstractModuleFactory {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractPasswordModuleFactory.class);

    @Override
    public abstract boolean match(AbstractAuthenticationModuleType moduleType);

    @Override
    public AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String prefixOfSequence,
                                         ServletRequest request, Map<Class<? extends Object>, Object> sharedObjects,
                                         AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy) throws Exception {

        if (!(moduleType instanceof AbstractPasswordAuthenticationModuleType)) {
            LOGGER.error("This factory support only AbstractPasswordAuthenticationModuleType, but modelType is " + moduleType);
            return null;
        }

        ModuleWebSecurityConfiguration configuration = createConfiguration(moduleType, prefixOfSequence);

        configuration.addAuthenticationProvider(getProvider((AbstractPasswordAuthenticationModuleType)moduleType, credentialPolicy));

        ModuleWebSecurityConfig module = createModule(configuration);
        module.setObjectPostProcessor(getObjectObjectPostProcessor());
        HttpSecurity http = module.getNewHttpSecurity();
        setSharedObjects(http, sharedObjects);

        ModuleAuthentication moduleAuthentication = createEmptyModuleAuthentication(moduleType, configuration);
        SecurityFilterChain filter = http.build();
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    protected AuthenticationProvider getProvider(AbstractPasswordAuthenticationModuleType moduleType, CredentialsPolicyType credentialsPolicy){
//        Validate.notNull(credentialsPolicy);
        CredentialPolicyType usedPolicy = null;
        String credentialName = moduleType.getCredentialName();

        if (StringUtils.isNotBlank(credentialName)) {
            List<CredentialPolicyType> credentialPolicies = new ArrayList<CredentialPolicyType>();
            credentialPolicies.add(credentialsPolicy.getPassword());
            credentialPolicies.add(credentialsPolicy.getSecurityQuestions());
            credentialPolicies.addAll(credentialsPolicy.getNonce());

            for (CredentialPolicyType processedPolicy : credentialPolicies) {
                if (credentialName.equals(processedPolicy.getName())) {
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
    };

    protected abstract ModuleAuthentication createEmptyModuleAuthentication(AbstractAuthenticationModuleType moduleType, ModuleWebSecurityConfiguration configuration);

    protected abstract ModuleWebSecurityConfiguration createConfiguration (AbstractAuthenticationModuleType moduleType, String prefixOfSequence);

    protected abstract ModuleWebSecurityConfig createModule (ModuleWebSecurityConfiguration configuration);

    protected abstract AuthenticationProvider createProvider (CredentialPolicyType usedPolicy);

    protected abstract Class<? extends CredentialPolicyType> supportedClass ();
}
