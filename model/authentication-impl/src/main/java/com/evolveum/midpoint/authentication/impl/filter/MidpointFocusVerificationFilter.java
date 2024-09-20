/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.FocusVerificationToken;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class MidpointFocusVerificationFilter extends AbstractAuthenticationProcessingFilter {

    protected static final String SPRING_SECURITY_FORM_ATTRIBUTE_VALUES_KEY = "attributeValues";

    protected MidpointFocusVerificationFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
        super(requiresAuthenticationRequestMatcher);
    }

    protected MidpointFocusVerificationFilter(RequestMatcher requiresAuthenticationRequestMatcher, AuthenticationManager authenticationManager) {
        super(requiresAuthenticationRequestMatcher, authenticationManager);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        getFailureHandler().onAuthenticationFailure(request, response, failed);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        super.doFilter(request, response, chain);


    }

    public Authentication attemptAuthentication(
            HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        validateRequest(request);

        Map<ItemPath, String> attributeValues = obtainAttributeValues(request);
        AbstractAuthenticationToken authRequest = createAuthenticationToken(attributeValues);

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    protected abstract AbstractAuthenticationToken createAuthenticationToken(Map<ItemPath, String> attributeValues);



    protected void validateRequest(HttpServletRequest request) {
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException(
                    "Authentication method not supported: " + request.getMethod());
        }
    }

    protected Map<ItemPath, String> obtainAttributeValues(HttpServletRequest request) {
        Map<ItemPath, String> attributeValuesMap = new HashMap<>();

       request.getParameterMap()
                .entrySet()
                .stream()
                .forEach(e -> {
                    if (e != null && e.getKey().startsWith(AuthConstants.ATTR_VERIFICATION_PARAMETER_START)) {
                        String itemPathKey = e.getKey().substring(AuthConstants.ATTR_VERIFICATION_PARAMETER_START.length());
                        String value = e.getValue() != null && e.getValue().length > 0 ? e.getValue()[0] : null;
                        if (value != null) {
                            attributeValuesMap.put(ItemPath.fromString(itemPathKey), e.getValue()[0]);
                        }
                    }
                });

        return attributeValuesMap;
    }
}
