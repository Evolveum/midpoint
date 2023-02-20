/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public abstract class MidpointFocusVerificationFilter extends AbstractAuthenticationProcessingFilter {

    private static final String SPRING_SECURITY_FORM_ATTRIBUTE_VALUES_KEY = "attributeValues";

    protected MidpointFocusVerificationFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
        super(requiresAuthenticationRequestMatcher);
    }

    protected MidpointFocusVerificationFilter(RequestMatcher requiresAuthenticationRequestMatcher, AuthenticationManager authenticationManager) {
        super(requiresAuthenticationRequestMatcher, authenticationManager);
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
        String attrValuesString = request.getParameter(SPRING_SECURITY_FORM_ATTRIBUTE_VALUES_KEY);
        if (StringUtils.isEmpty(attrValuesString)) {
            return null;
        }

        JSONArray attributeValuesArray = new JSONArray(attrValuesString);
        Map<ItemPath, String> attributeValuesMap = new HashMap<>();
        for (int i = 0; i < attributeValuesArray.length(); i++) {
            JSONObject entry = attributeValuesArray.getJSONObject(i);

            ItemPathType itemPath = PrismContext.get().itemPathParser().asItemPathType(entry.getString(AuthConstants.ATTR_VERIFICATION_J_PATH));

            if (itemPath == null) {
                continue;
            }
            ItemPath path = itemPath.getItemPath();
            String value = entry.getString(AuthConstants.ATTR_VERIFICATION_J_VALUE);
            attributeValuesMap.put(path, value);
        }
        return attributeValuesMap;
    }
}
