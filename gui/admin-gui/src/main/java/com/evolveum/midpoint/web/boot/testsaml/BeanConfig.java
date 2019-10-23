package com.evolveum.midpoint.web.boot.testsaml;

import com.evolveum.midpoint.model.api.authentication.UserProfileService;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.provider.SamlServerConfiguration;
import org.springframework.security.saml.provider.service.authentication.GenericErrorAuthenticationFailureHandler;
import org.springframework.security.saml.provider.service.authentication.SamlAuthenticationResponseFilter;
import org.springframework.security.saml.provider.service.authentication.SimpleAuthenticationManager;
import org.springframework.security.saml.provider.service.config.SamlServiceProviderServerBeanConfiguration;
import org.springframework.security.saml.saml2.authentication.NameIdPrincipal;
import org.springframework.security.saml.spi.DefaultSamlAuthentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import javax.servlet.Filter;

@Configuration
public class BeanConfig extends SamlServiceProviderServerBeanConfiguration {

    @Autowired
    private UserProfileService userProfileService;

    private final AppConfig config;

    public BeanConfig(AppConfig config) {
        this.config = config;
    }

    @Override
    protected SamlServerConfiguration getDefaultHostSamlServerConfiguration() {
        return config;
    }



    @Override
    public Filter spAuthenticationResponseFilter() {
        MidpointSamlAuthenticationResponseFilter authenticationFilter =
                new MidpointSamlAuthenticationResponseFilter(getSamlProvisioning());
        authenticationFilter.setAuthenticationManager(new MidpointSimpleAuthenticationManager());
        authenticationFilter.setAuthenticationSuccessHandler(new SavedRequestAwareAuthenticationSuccessHandler());
        authenticationFilter.setAuthenticationFailureHandler(new GenericErrorAuthenticationFailureHandler());
        authenticationFilter.setUserProfileService(userProfileService);
        return authenticationFilter;
    }

    private class MidpointSimpleAuthenticationManager implements AuthenticationManager {
        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {

            if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof MidPointPrincipal) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            return authentication;
        }
    }
}
