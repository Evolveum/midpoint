package com.evolveum.midpoint.authentication.impl.factory.module;

import java.util.Map;

import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;

import com.evolveum.midpoint.authentication.impl.module.configurer.ModuleWebSecurityConfigurer;

import jakarta.servlet.ServletRequest;

import com.evolveum.midpoint.authentication.api.AuthModule;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
@Experimental
public class OtherModuleFactory<MT extends AbstractAuthenticationModuleType,
        MA extends ModuleAuthentication> implements ModuleFactory<MT, MA> {

    private static final Trace LOGGER = TraceManager.getTrace(OtherModuleFactory.class);

    @Autowired
    private ApplicationContext applicationContext;

//    @Override
    public boolean match(AbstractAuthenticationModuleType module, AuthenticationChannel authenticationChannel) {
        return module instanceof OtherAuthenticationModuleType;
    }

    @Override
    public Integer getOrder() {
        return 0;
    }

    //    @Override
    public AuthModule<MA> createAuthModule(MT module, String sequenceSuffix, ServletRequest request,
            Map<Class<?>, Object> sharedObjects, AuthenticationModulesType authenticationsPolicy,
            CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel, AuthenticationSequenceModuleType sequenceModule) throws Exception {

        if (!(module instanceof OtherAuthenticationModuleType other)) {
            LOGGER.error("This factory support only OtherAuthenticationModuleType, but module is " + module);
            return null;
        }

        String factoryClass = other.getFactoryClass();

        Class<ModuleFactory<MT, MA>> factoryClazz = (Class) Class.forName(factoryClass);
        ModuleFactory<MT, MA> factory = applicationContext.getBean(factoryClazz);

        return factory.createAuthModule(module, sequenceSuffix, request, sharedObjects,
                authenticationsPolicy, credentialPolicy, authenticationChannel, sequenceModule);
    }
}
