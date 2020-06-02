/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.login;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;

/**
 * @author lskublik
 */
public abstract class AbstractPageLogin extends PageBase {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractPageLogin.class);

    private static final String ID_SEQUENCE = "sequence";
    private static final String ID_SWITCH_TO_DEFAULT_SEQUENCE = "switchToDefaultSequence";

    public AbstractPageLogin() {
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayer();
    }

    private void initLayer() {
        String sequenceName = getSequenceName();
        Label sequence = new Label(ID_SEQUENCE, createStringResource("AbstractPageLogin.authenticationSequence", sequenceName));
        sequence.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !StringUtils.isEmpty(sequenceName);
            }
        });
        add(sequence);
        AjaxButton toDefault = new AjaxButton(ID_SWITCH_TO_DEFAULT_SEQUENCE, createStringResource("AbstractPageLogin.switchToDefault")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(getMidpointApplication().getHomePage());
            }
        };
        toDefault.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !StringUtils.isEmpty(sequenceName);
            }
        });
        add(toDefault);
        initCustomLayer();
    }

    private String getSequenceName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            AuthenticationSequenceType sequence = mpAuthentication.getSequence();
            if (sequence != null && sequence.getChannel() != null
                    && !Boolean.TRUE.equals(sequence.getChannel().isDefault())
                    && SecurityPolicyUtil.DEFAULT_CHANNEL.equals(sequence.getChannel().getChannelId())) {
                return sequence.getDisplayName() != null ? sequence.getDisplayName() : sequence.getName();
            }
        }

        return null;
    }

    protected abstract void initCustomLayer();

    @Override
    protected void onConfigure() {
        super.onConfigure();

        ServletWebRequest req = (ServletWebRequest) RequestCycle.get().getRequest();
        HttpServletRequest httpReq = req.getContainerRequest();
        HttpSession httpSession = httpReq.getSession();

        Exception ex = (Exception) httpSession.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (ex == null) {
            return;
        }

        String msg = ex.getMessage();
        if (StringUtils.isEmpty(msg)) {
            msg = "web.security.provider.unavailable";
        }

        String[] msgs = msg.split(";");
        for (String message : msgs) {
            message = getLocalizationService().translate(message, null, getLocale(), message);
            error(message);
        }

        httpSession.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        clearBreadcrumbs();
    }

    @Override
    protected void createBreadcrumb() {
        //don't create breadcrumb for login page
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        if (SecurityUtils.getPrincipalUser() != null) {
            MidPointApplication app = getMidpointApplication();
            throw new RestartResponseException(app.getHomePage());
        }
    }

    @Override
    protected boolean isSideMenuVisible(boolean visibleIfLoggedIn) {
        return false;
    }
}
