package com.evolveum.midpoint.web.security.saml;

import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

public class SamlLogoutMatcher implements RequestMatcher {

    private final RequestMatcher pathMatcher;

    public SamlLogoutMatcher (String path) {
        this.pathMatcher = new AntPathRequestMatcher(path);
    }

    @Override
    public boolean matches(HttpServletRequest httpServletRequest) {
        ModuleAuthentication module = SecurityUtils.getProcessingModule(false);
        if (module != null && module.isInternalLogout()) {
            module.setInternalLogout(false);
            return true;
        }
        return pathMatcher.matches(httpServletRequest);
    }
}
