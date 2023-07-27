package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModulesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;

import jakarta.servlet.ServletRequest;

import java.util.Map;

public interface ModuleFactory<MT extends AbstractAuthenticationModuleType, MA extends ModuleAuthentication> {

    AuthModule<MA> createAuthModule(MT moduleType, String sequenceSuffix,
            ServletRequest request, Map<Class<?>, Object> sharedObjects,
            AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy,
            AuthenticationChannel authenticationChannel, AuthenticationSequenceModuleType sequenceModule) throws Exception;

    boolean match(AbstractAuthenticationModuleType module, AuthenticationChannel authenticationChannel);

    Integer getOrder();
}
