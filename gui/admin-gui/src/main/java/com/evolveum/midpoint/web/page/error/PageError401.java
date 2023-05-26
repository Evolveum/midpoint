/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.error;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.security.web.WebAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/error/401")
        },
        permitAll = true, authModule = AuthenticationModuleNameConstants.HTTP_HEADER)
public class PageError401 extends PageError {

    public PageError401() {
        super(401);
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        if (exClass != null) {
            return;
        }

        ServletWebRequest req = (ServletWebRequest) RequestCycle.get().getRequest();
        HttpServletRequest httpReq = req.getContainerRequest();
        HttpSession httpSession = httpReq.getSession();

        Exception ex = (Exception) httpSession.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (ex == null) {
            return;
        }

        exClass = ex.getClass().getName();

        String msg = ex.getMessage();
        if (StringUtils.isEmpty(msg)) {
            msg = "web.security.provider.unavailable";
        }
        exMessage = Arrays.stream(msg.split(";")).map((key) -> getLocalizationService().translate(key, null, getLocale(), key)
        ).collect(Collectors.joining("; "));

        httpSession.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }
}
