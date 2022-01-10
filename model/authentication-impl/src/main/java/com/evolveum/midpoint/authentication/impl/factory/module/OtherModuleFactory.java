package com.evolveum.midpoint.authentication.impl.factory.module;

import java.util.Map;
import javax.servlet.ServletRequest;

import com.evolveum.midpoint.authentication.api.AuthModule;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModulesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherAuthenticationModuleType;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
@Experimental
public class OtherModuleFactory extends AbstractModuleFactory {

    private static final Trace LOGGER = TraceManager.getTrace(OtherModuleFactory.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public boolean match(AbstractAuthenticationModuleType module) {
        return module instanceof OtherAuthenticationModuleType;
    }

    @Override
    public AuthModule createModuleFilter(AbstractAuthenticationModuleType module, String prefixOfSequence, ServletRequest request,
            Map<Class<?>, Object> sharedObjects, AuthenticationModulesType authenticationsPolicy,
            CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel) throws Exception {

        if (!(module instanceof OtherAuthenticationModuleType)) {
            LOGGER.error("This factory support only OtherAuthenticationModuleType, but module is " + module);
            return null;
        }

        OtherAuthenticationModuleType other = (OtherAuthenticationModuleType) module;

        String factoryClass = other.getFactoryClass();

        Class<AbstractModuleFactory> factoryClazz = (Class) Class.forName(factoryClass);
        AbstractModuleFactory factory = applicationContext.getBean(factoryClazz);

        return factory.createModuleFilter(module, prefixOfSequence, request, sharedObjects,
                authenticationsPolicy, credentialPolicy, authenticationChannel);
    }
}
