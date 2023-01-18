/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.AttributeVerificationToken;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class AttributeVerificationAuthenticationFilter extends MidpointUsernamePasswordAuthenticationFilter {

    private static final String SPRING_SECURITY_FORM_ATTRIBUTE_VALUES_KEY = "attributeValues";

    public Authentication attemptAuthentication(
            HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (isPostOnly() && !request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException(
                    "Authentication method not supported: " + request.getMethod());
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return authentication;
        }
        Map<ItemPath, String> attributeValues = obtainAttributeValues(request);
        UsernamePasswordAuthenticationToken authRequest =
                new AttributeVerificationToken(authentication.getPrincipal(), attributeValues);

        return this.getAuthenticationManager().authenticate(authRequest);
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

            ItemPath path = ItemPath.create(entry.get(AuthConstants.ATTR_VERIFICATION_J_PATH));
            String value = entry.getString(AuthConstants.ATTR_VERIFICATION_J_VALUE);
            attributeValuesMap.put(path, value);
        }
        return attributeValuesMap;
    }

}
