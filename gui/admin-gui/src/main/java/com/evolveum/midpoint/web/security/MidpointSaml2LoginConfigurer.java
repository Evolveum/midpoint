package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.api.ModelAuditRecorder;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.saml2.provider.service.authentication.*;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.DefaultSaml2AuthenticationRequestContextResolver;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestContextResolver;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class MidpointSaml2LoginConfigurer<B extends HttpSecurityBuilder<B>> extends AbstractAuthenticationFilterConfigurer<B, MidpointSaml2LoginConfigurer<B>, Saml2WebSsoAuthenticationFilter> {

    private static final String FILTER_PROCESSING_URL = "/saml2/authenticate/{registrationId}";

    private String loginPage;
    private String loginProcessingUrl = "/login/saml2/sso/{registrationId}";
    private RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;
    private AuthenticationConverter authenticationConverter;
    private AuthenticationManager authenticationManager;
    private Saml2WebSsoAuthenticationFilter saml2WebSsoAuthenticationFilter;
    private ModelAuditRecorder auditProvider;

    public MidpointSaml2LoginConfigurer(ModelAuditRecorder auditProvider) {
        this.auditProvider = auditProvider;
    }

    public MidpointSaml2LoginConfigurer<B> authenticationConverter(AuthenticationConverter authenticationConverter) {
        Assert.notNull(authenticationConverter, "authenticationConverter cannot be null");
        this.authenticationConverter = authenticationConverter;
        return this;
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

    public MidpointSaml2LoginConfigurer<B> loginPage(String loginPage) {
        Assert.hasText(loginPage, "loginPage cannot be empty");
        this.loginPage = loginPage;
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
        this.saml2WebSsoAuthenticationFilter = new MidpointSaml2WebSsoAuthenticationFilter(this.getAuthenticationConverter(http), this.loginProcessingUrl, auditProvider);
        this.setAuthenticationFilter(this.saml2WebSsoAuthenticationFilter);
        super.loginProcessingUrl(this.loginProcessingUrl);
        if (StringUtils.hasText(this.loginPage)) {
            super.loginPage(this.loginPage);
            super.init(http);
        } else {
            Map<String, String> providerUrlMap = this.getIdentityProviderUrlMap(FILTER_PROCESSING_URL, this.relyingPartyRegistrationRepository);
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
        this.initDefaultLoginFilter(http);
    }

    public void configure(B http) throws Exception {
        Saml2AuthenticationRequestFactory authenticationRequestResolver = new OpenSaml4AuthenticationRequestFactory();
        Saml2AuthenticationRequestContextResolver contextResolver = new DefaultSaml2AuthenticationRequestContextResolver(new DefaultRelyingPartyRegistrationResolver(MidpointSaml2LoginConfigurer.this.relyingPartyRegistrationRepository));
        http.addFilter(new MidpointSaml2WebSsoAuthenticationRequestFilter(contextResolver, authenticationRequestResolver));
        super.configure(http);
        if (this.authenticationManager != null) {
            this.saml2WebSsoAuthenticationFilter.setAuthenticationManager(this.authenticationManager);
        }
    }

    private AuthenticationConverter getAuthenticationConverter(B http) {
        return (AuthenticationConverter)(this.authenticationConverter == null ? new Saml2AuthenticationTokenConverter(new DefaultRelyingPartyRegistrationResolver(this.relyingPartyRegistrationRepository)) : this.authenticationConverter);
    }

    private void initDefaultLoginFilter(B http) {
        DefaultLoginPageGeneratingFilter loginPageGeneratingFilter = (DefaultLoginPageGeneratingFilter)http.getSharedObject(DefaultLoginPageGeneratingFilter.class);
        if (loginPageGeneratingFilter != null && !this.isCustomLoginPage()) {
            loginPageGeneratingFilter.setSaml2LoginEnabled(true);
            loginPageGeneratingFilter.setSaml2AuthenticationUrlToProviderName(this.getIdentityProviderUrlMap(FILTER_PROCESSING_URL, this.relyingPartyRegistrationRepository));
            loginPageGeneratingFilter.setLoginPageUrl(this.getLoginPage());
            loginPageGeneratingFilter.setFailureUrl(this.getFailureUrl());
        }
    }

    private Map<String, String> getIdentityProviderUrlMap(String authRequestPrefixUrl, RelyingPartyRegistrationRepository idpRepo) {
        Map<String, String> idps = new LinkedHashMap();
        if (idpRepo instanceof Iterable) {
            Iterable<RelyingPartyRegistration> repo = (Iterable)idpRepo;
            repo.forEach((p) -> {
                idps.put(authRequestPrefixUrl.replace("{registrationId}", p.getRegistrationId()), p.getRegistrationId());
            });
        }

        return idps;
    }

    private <C> C getSharedOrBean(B http, Class<C> clazz) {
        C shared = http.getSharedObject(clazz);
        return shared != null ? shared : this.getBeanOrNull(http, clazz);
    }

    private <C> C getBeanOrNull(B http, Class<C> clazz) {
        ApplicationContext context = (ApplicationContext)http.getSharedObject(ApplicationContext.class);
        if (context == null) {
            return null;
        } else {
            try {
                return context.getBean(clazz);
            } catch (NoSuchBeanDefinitionException var5) {
                return null;
            }
        }
    }
}
