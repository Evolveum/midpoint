/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.factory.module;

import com.evolveum.midpoint.model.api.authentication.AuthModule;
import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.provider.MidpointSaml2Provider;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.Saml2ModuleAuthentication;
import com.evolveum.midpoint.web.security.module.configuration.SamlModuleWebSecurityConfiguration;
import com.evolveum.midpoint.web.security.module.SamlModuleWebSecurityConfig;
import com.evolveum.midpoint.web.security.util.AuthModuleImpl;
import com.evolveum.midpoint.web.security.util.IdentityProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModuleSaml2Type;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModulesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml.provider.config.ExternalProviderConfiguration;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.config.LocalServiceProviderConfiguration;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import javax.servlet.ServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.security.saml.util.StringUtils.stripSlashes;

/**
 * @author skublik
 */
@Component
public class Saml2ModuleFactory extends AbstractModuleFactory {

    private static final Trace LOGGER = TraceManager.getTrace(Saml2ModuleFactory.class);

    @Autowired
    private Protector protector;

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType) {
        if (moduleType instanceof AuthenticationModuleSaml2Type) {
            return true;
        }
        return false;
    }

    @Override
    public AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String prefixOfSequence, ServletRequest request,
                                         Map<Class<? extends Object>, Object> sharedObjects, AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel) throws Exception {
        if (!(moduleType instanceof AuthenticationModuleSaml2Type)) {
            LOGGER.error("This factory support only AuthenticationModuleSaml2Type, but modelType is " + moduleType);
            return null;
        }

        isSupportedChannel(authenticationChannel);

        SamlModuleWebSecurityConfiguration.setProtector(protector);
        SamlModuleWebSecurityConfiguration configuration = SamlModuleWebSecurityConfiguration.build((AuthenticationModuleSaml2Type)moduleType, prefixOfSequence, request);
        configuration.setPrefixOfSequence(prefixOfSequence);
        configuration.addAuthenticationProvider(getObjectObjectPostProcessor().postProcess(new MidpointSaml2Provider()));
//        MidpointSamlProviderServerBeanConfiguration beanConfiguration =getObjectObjectPostProcessor().postProcess(new MidpointSamlProviderServerBeanConfiguration(configuration));

        SamlModuleWebSecurityConfig module = getObjectObjectPostProcessor().postProcess(new SamlModuleWebSecurityConfig(configuration));//, beanConfiguration));
        module.setObjectPostProcessor(getObjectObjectPostProcessor());
        HttpSecurity http = module.getNewHttpSecurity();
        setSharedObjects(http, sharedObjects);

        ModuleAuthentication moduleAuthentication = createEmptyModuleAuthentication(module.getBeanConfiguration(), configuration);
        moduleAuthentication.setFocusType(moduleType.getFocusType());
        SecurityFilterChain filter = http.build();
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    public ModuleAuthentication createEmptyModuleAuthentication(SamlServiceProviderServerBeanConfiguration beanConfiguration,
                                                                SamlModuleWebSecurityConfiguration configuration) {
        Saml2ModuleAuthentication moduleAuthentication = new Saml2ModuleAuthentication();

        ServiceProviderService provider = beanConfiguration.getSamlProvisioning().getHostedProvider();
        LocalServiceProviderConfiguration samlConfiguration = provider.getConfiguration();
        List<IdentityProvider> providers = new ArrayList<>();
        samlConfiguration.getProviders().stream().forEach(
                p -> {
                    try {
                        IdentityProvider mp = new IdentityProvider()
                                .setLinkText(p.getLinktext())
                                .setRedirectLink(getDiscoveryRedirect(provider, p));
                        providers.add(mp);
                    } catch (Exception x) {
                        LOGGER.debug("Unable to retrieve metadata for provider:" + p.getMetadata() + " with message:" + x.getMessage());
                    }
                }
        );

        moduleAuthentication.setProviders(providers);
        moduleAuthentication.setNamesOfUsernameAttributes(configuration.getNamesOfUsernameAttributes());
        moduleAuthentication.setNameOfModule(configuration.getNameOfModule());
        moduleAuthentication.setPrefix(configuration.getPrefix());
        return moduleAuthentication;
    }

    private String getDiscoveryRedirect(ServiceProviderService provider,
                                        ExternalProviderConfiguration p) throws UnsupportedEncodingException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                provider.getConfiguration().getBasePath()
        );
        builder.pathSegment(stripSlashes(provider.getConfiguration().getPrefix()) + "/discovery");
//        builder.pathSegment("saml/discovery");
        IdentityProviderMetadata metadata = provider.getRemoteProvider(p);
        builder.queryParam("idp", UriUtils.encode(metadata.getEntityId(), UTF_8.toString()));
        return builder.build().toUriString();
    }
}
