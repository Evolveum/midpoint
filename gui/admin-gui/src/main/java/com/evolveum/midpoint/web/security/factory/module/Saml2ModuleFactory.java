/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.factory.module;

import com.evolveum.midpoint.model.api.authentication.AuthModule;
import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.configuration.SamlMidpointAdditionalConfiguration;
import com.evolveum.midpoint.web.security.provider.Saml2Provider;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.Saml2ModuleAuthentication;
import com.evolveum.midpoint.web.security.module.configuration.SamlModuleWebSecurityConfiguration;
import com.evolveum.midpoint.web.security.module.SamlModuleWebSecurityConfig;
import com.evolveum.midpoint.web.security.util.AuthModuleImpl;
import com.evolveum.midpoint.web.security.util.IdentityProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author skublik
 */
@Component
public class Saml2ModuleFactory extends AbstractModuleFactory {

    private static final Trace LOGGER = TraceManager.getTrace(Saml2ModuleFactory.class);

    @Autowired
    private Protector protector;

    @Autowired
    private SystemObjectCache systemObjectCache;

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType) {
        if (moduleType instanceof Saml2AuthenticationModuleType) {
            return true;
        }
        return false;
    }

    @Override
    public AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String prefixOfSequence, ServletRequest request,
                                         Map<Class<? extends Object>, Object> sharedObjects, AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel) throws Exception {
        if (!(moduleType instanceof Saml2AuthenticationModuleType)) {
            LOGGER.error("This factory support only Saml2AuthenticationModuleType, but modelType is " + moduleType);
            return null;
        }

        isSupportedChannel(authenticationChannel);

        SamlModuleWebSecurityConfiguration.setProtector(protector);
        SamlModuleWebSecurityConfiguration configuration = SamlModuleWebSecurityConfiguration.build((Saml2AuthenticationModuleType)moduleType, prefixOfSequence, getPublicUrlPrefix(request), request);
        configuration.setPrefixOfSequence(prefixOfSequence);
        configuration.addAuthenticationProvider(getObjectObjectPostProcessor().postProcess(new Saml2Provider()));

        SamlModuleWebSecurityConfig module = getObjectObjectPostProcessor().postProcess(new SamlModuleWebSecurityConfig(configuration));//, beanConfiguration));
        module.setObjectPostProcessor(getObjectObjectPostProcessor());
        HttpSecurity http = module.getNewHttpSecurity();
        setSharedObjects(http, sharedObjects);

        ModuleAuthentication moduleAuthentication = createEmptyModuleAuthentication(configuration);
        moduleAuthentication.setFocusType(moduleType.getFocusType());
        SecurityFilterChain filter = http.build();
        for (Filter f : filter.getFilters()){
            if (f instanceof Saml2WebSsoAuthenticationRequestFilter) {
                ((Saml2WebSsoAuthenticationRequestFilter) f).setRedirectMatcher(new AntPathRequestMatcher(module.getPrefix() + SamlModuleWebSecurityConfiguration.REQUEST_PROCESSING_URL_SUFFIX));
                break;
            }
        }
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    public ModuleAuthentication createEmptyModuleAuthentication(SamlModuleWebSecurityConfiguration configuration) {
        Saml2ModuleAuthentication moduleAuthentication = new Saml2ModuleAuthentication();
        List<IdentityProvider> providers = new ArrayList<>();
        ((Iterable<RelyingPartyRegistration>)configuration.getRelyingPartyRegistrationRepository()).forEach(
                p -> {
                    String authRequestPrefixUrl = "/midpoint" + configuration.getPrefix() + SamlModuleWebSecurityConfiguration.REQUEST_PROCESSING_URL_SUFFIX;
                    SamlMidpointAdditionalConfiguration config = configuration.getAdditionalConfiguration().get(p.getAssertingPartyDetails().getEntityId());
                    IdentityProvider mp = new IdentityProvider()
                                .setLinkText(config.getLinkText())
                                .setRedirectLink(authRequestPrefixUrl.replace("{registrationId}", p.getRegistrationId()));
                        providers.add(mp);
                }
        );
        moduleAuthentication.setProviders(providers);
        moduleAuthentication.setAdditionalConfiguration(configuration.getAdditionalConfiguration());
        moduleAuthentication.setNameOfModule(configuration.getNameOfModule());
        moduleAuthentication.setPrefix(configuration.getPrefix());
        return moduleAuthentication;
    }

//    private String getDiscoveryRedirect(ServiceProviderService provider,
//                                        ExternalProviderConfiguration p) throws UnsupportedEncodingException {
//        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
//                provider.getConfiguration().getBasePath()
//        );
//        builder.pathSegment(stripSlashes(provider.getConfiguration().getPrefix()) + "/discovery");
////        builder.pathSegment("saml/discovery");
//        IdentityProviderMetadata metadata = provider.getRemoteProvider(p);
//        builder.queryParam("idp", UriUtils.encode(metadata.getEntityId(), UTF_8.toString()));
//        return builder.build().toUriString();
//    }

    private String getPublicUrlPrefix(ServletRequest request) {
        try {
            PrismObject<SystemConfigurationType> systemConfig = systemObjectCache.getSystemConfiguration(new OperationResult("load system configuration"));
            return SystemConfigurationTypeUtil.getPublicHttpUrlPattern(systemConfig.asObjectable(), request.getServerName());
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load system configuration", e);
            return null;
        }
    }
}
