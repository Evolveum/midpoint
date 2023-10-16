/*
 * Copyright (c) 2022 Evolveum and contributors
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

public class FocusIdentificationAuthenticationFilter extends MidpointFocusVerificationFilter {

    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/focusIdentification", "POST");

    public FocusIdentificationAuthenticationFilter() {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER);
    }

    public FocusIdentificationAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
    }

    protected AbstractAuthenticationToken createAuthenticationToken(Map<ItemPath, String> attributeValues) {
        return new FocusVerificationToken(attributeValues);
    }

    @Override
    protected Map<ItemPath, String> obtainAttributeValues(HttpServletRequest request) {
        Map<ItemPath, String> attributeValuesMap = new HashMap<>();

        String attrValuesString = request.getParameter(SPRING_SECURITY_FORM_ATTRIBUTE_VALUES_KEY);
        if (StringUtils.isEmpty(attrValuesString)) {
            return attributeValuesMap;
        }

        JSONArray attributeValuesArray = new JSONArray(attrValuesString);
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
