/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.saml;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.*;
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml4AuthenticationRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2AuthenticationRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

import com.evolveum.midpoint.model.api.ModelAuditRecorder;

public class MidpointSaml2LoginConfigurer<B extends HttpSecurityBuilder<B>> extends AbstractAuthenticationFilterConfigurer<B, MidpointSaml2LoginConfigurer<B>, Saml2WebSsoAuthenticationFilter> {

    private static final String FILTER_PROCESSING_URL = "/saml2/authenticate/{registrationId}";

    private String loginProcessingUrl = "/login/saml2/sso/{registrationId}";
    private RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;
    private AuthenticationManager authenticationManager;
    private Saml2WebSsoAuthenticationFilter saml2WebSsoAuthenticationFilter;
    private final ModelAuditRecorder auditProvider;

    public MidpointSaml2LoginConfigurer(ModelAuditRecorder auditProvider) {
        this.auditProvider = auditProvider;
    }

    public MidpointSaml2LoginConfigurer<B> authenticationManager(AuthenticationManager authenticationManager) {
        Assert.notNull(authenticationManager, "authenticationManager cannot be null");
        this.authenticationManager = authenticationManager;
        return this;
    }

    public MidpointSaml2LoginConfigurer relyingPartyRegistrationRepository(RelyingPartyRegistrationRepository repo) {
        this.relyingPartyRegistrationRepository = repo;
        return this;
    }

    public MidpointSaml2LoginConfigurer<B> loginProcessingUrl(String loginProcessingUrl) {
        Assert.hasText(loginProcessingUrl, "loginProcessingUrl cannot be empty");
        Assert.state(loginProcessingUrl.contains("{registrationId}"), "{registrationId} path variable is required");
        this.loginProcessingUrl = loginProcessingUrl;
        return this;
    }

    protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
        return new AntPathRequestMatcher(loginProcessingUrl);
    }

    public void init(B http) throws Exception {
        Saml2AuthenticationTokenConverter authenticationConverter = new Saml2AuthenticationTokenConverter(
                (RelyingPartyRegistrationResolver) new DefaultRelyingPartyRegistrationResolver(this.relyingPartyRegistrationRepository));
        this.saml2WebSsoAuthenticationFilter = new MidpointSaml2WebSsoAuthenticationFilter(authenticationConverter, this.loginProcessingUrl, auditProvider);
        this.setAuthenticationFilter(this.saml2WebSsoAuthenticationFilter);
        super.loginProcessingUrl(this.loginProcessingUrl);
            Map<String, String> providerUrlMap = this.getIdentityProviderUrlMap(this.relyingPartyRegistrationRepository);
            boolean singleProvider = providerUrlMap.size() == 1;
            if (singleProvider) {
                this.updateAuthenticationDefaults();
                this.updateAccessDefaults(http);
                String loginUrl = (String)((Map.Entry)providerUrlMap.entrySet().iterator().next()).getKey();
                LoginUrlAuthenticationEntryPoint entryPoint = new LoginUrlAuthenticationEntryPoint(loginUrl);
                this.registerAuthenticationEntryPoint(http, entryPoint);
            } else {
                super.init(http);
            }
    }

    public void configure(B http) throws Exception {
        OpenSaml4AuthenticationRequestResolver contextResolver = new OpenSaml4AuthenticationRequestResolver(
                new DefaultRelyingPartyRegistrationResolver(
                        MidpointSaml2LoginConfigurer.this.relyingPartyRegistrationRepository));
        contextResolver.setRequestMatcher(new AntPathRequestMatcher(FILTER_PROCESSING_URL));
        http.addFilter(new MidpointSaml2WebSsoAuthenticationRequestFilter(contextResolver));
        super.configure(http);
        if (this.authenticationManager != null) {
            this.saml2WebSsoAuthenticationFilter.setAuthenticationManager(this.authenticationManager);
        }
    }

    private Map<String, String> getIdentityProviderUrlMap(RelyingPartyRegistrationRepository idpRepo) {
        Map<String, String> idps = new LinkedHashMap<>();
        if (idpRepo instanceof Iterable) {
            Iterable<RelyingPartyRegistration> repo = (Iterable<RelyingPartyRegistration>)idpRepo;
            repo.forEach((p) -> idps.put(MidpointSaml2LoginConfigurer.FILTER_PROCESSING_URL.replace(
                    "{registrationId}", p.getRegistrationId()), p.getRegistrationId()));
        }

        return idps;
    }
}
