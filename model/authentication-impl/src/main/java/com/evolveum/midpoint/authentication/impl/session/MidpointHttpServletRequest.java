/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;

/**
 * @author skublik
 */

public class MidpointHttpServletRequest extends HttpServletRequestWrapper {

    public MidpointHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getServletPath() {
        if (needChangePath()) {
            MidpointAuthentication mpAuth = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
            String path = AuthSequenceUtil.searchPathByChannel(mpAuth.getAuthenticationChannel().getChannelId());
            if (StringUtils.isNotEmpty(path) && path.contains("/")) {
                return "/" + path.split("/")[0];
            }
            return "/" + path;
        }
        return super.getServletPath();
    }

    @Override
    public String getPathInfo() {
        if (needChangePath()) {
            MidpointAuthentication mpAuth = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
            String path = AuthSequenceUtil.searchPathByChannel(mpAuth.getAuthenticationChannel().getChannelId());
            StringBuilder sb = new StringBuilder();
            if (StringUtils.isNotEmpty(path) && path.contains("/")) {
                String[] partOfPath = path.split("/");
                for (int i = 1; i < partOfPath.length; i++) {
                    sb.append("/").append(partOfPath[i]);
                }
                String requestPath = getRequestURI().substring(getContextPath().length());
                int startIndex = requestPath.indexOf(mpAuth.getAuthenticationChannel().getUrlSuffix() + "/") + mpAuth.getAuthenticationChannel().getUrlSuffix().length();
                String pathInfo = requestPath.substring(startIndex);
                sb.append(pathInfo);
                return sb.toString();
            }

        }
        return super.getPathInfo();
    }

    private boolean needChangePath() {
        String localePath = getRequestURI().substring(getContextPath().length());
        String[] partsOfLocalPath = AuthUtil.stripStartingSlashes(localePath).split("/");
        if (partsOfLocalPath.length > 2 && partsOfLocalPath[0].equals(ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof MidpointAuthentication) {
                MidpointAuthentication mpAuth = (MidpointAuthentication) authentication;
                return !mpAuth.getAuthenticationChannel().isDefault()
                        && partsOfLocalPath[1].equals(mpAuth.getAuthenticationChannel().getUrlSuffix());
            }
        }
        return false;
    }
}
