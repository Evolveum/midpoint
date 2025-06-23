/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.page.error.PageError;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.schema.util.AuthenticationSequenceTypeUtil;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.menu.top.LocaleTextPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;

/**
 * Umbrella class for each page that should have the look and feel of login page.
 * <p>
 * It is used as a base for:
 * <p>
 * . self-management pages, such as self-registration, password reset, invitation,
 * . authentication module pages
 * <p>
 * This abstract page should not contain methods which are not used for both cases.
 * <p>
 * Basic intention is to provide common layout, such as a styles and title and
 * description of the page. Possibility to change locale and implementation for back button.
 */
public abstract class AbstractPageLogin<MA extends ModuleAuthentication>  extends PageAdminLTE {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_SEQUENCE = "sequence";
    private static final String ID_PANEL_TITLE = "panelTitle";
    private static final String ID_PANEL_DESCRIPTION = "panelDescription";
    private static final String ID_SWITCH_TO_DEFAULT_SEQUENCE = "switchToDefaultSequence";
    private static final String ID_BACK_BUTTON = "back";
    private static final String ID_ACTION_LINK = "actionLink";
    private static final String ID_ACTION_LINK_LABEL = "actionLinkLabel";
    private static final String ID_SYSTEM_NAME = "systemName";

    private final List<String> errorMessages = new ArrayList<>();

    public AbstractPageLogin(PageParameters parameters) {
        super(parameters);
        initExceptionMessage();
    }

    public AbstractPageLogin() {
        super(null);
        initExceptionMessage();
    }

    private void initExceptionMessage() {
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
            errorMessages.add(message);
        }

        httpSession.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

    @Override
    protected void addDefaultBodyStyle(TransparentWebMarkupContainer body) {
        body.add(AttributeModifier.replace("class", "login-page py-3 fp-center"));
        body.add(AttributeModifier.remove("style")); //TODO hack :) because PageBase has min-height defined.
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnDomReadyHeaderItem.forScript("$(\"input[name='username']\").focus();"));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Label systemName = new Label(ID_SYSTEM_NAME, getSystemNameModel());
        systemName.setOutputMarkupId(true);
        add(systemName);

        Label panelTitle = new Label(ID_PANEL_TITLE, getLoginPanelTitleModel());
        panelTitle.setOutputMarkupId(true);
        panelTitle.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getLoginPanelTitleModel().getObject())));
        add(panelTitle);

        Label panelDescription = new Label(ID_PANEL_DESCRIPTION, getLoginPanelDescriptionModel());
        panelDescription.setOutputMarkupId(true);
        panelDescription.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getLoginPanelDescriptionModel().getObject())));
        add(panelDescription);

        AjaxButton actionLink = new AjaxButton(ID_ACTION_LINK) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                actionPerformed();
            }
        };
        actionLink.setOutputMarkupId(true);
        actionLink.add(new VisibleBehaviour(this::isActionDefined));
        add(actionLink);

        Label actionLinkLabel = new Label(ID_ACTION_LINK_LABEL, getActionLabelModel());
        actionLink.add(actionLinkLabel);

        String sequenceName = getSequenceName();
        Label sequence = new Label(ID_SEQUENCE, createStringResource("AbstractPageLogin.authenticationSequence", sequenceName));
        sequence.add(new VisibleBehaviour(() -> !StringUtils.isEmpty(sequenceName)));
        add(sequence);

        AjaxButton toDefault = new AjaxButton(ID_SWITCH_TO_DEFAULT_SEQUENCE, createStringResource("AbstractPageLogin.switchToDefault")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                AuthUtil.clearMidpointAuthentication();
                setResponsePage(getMidpointApplication().getHomePage());
            }
        };
        toDefault.add(new VisibleBehaviour(() -> !StringUtils.isEmpty(sequenceName)));
        add(toDefault);

        initCustomLayout();

        addFeedbackPanel();

        add(new LocaleTextPanel("locale"));

        AjaxButton backButton = new AjaxButton(ID_BACK_BUTTON) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed();
            }
        };
        backButton.setOutputMarkupId(true);
        backButton.add(new VisibleBehaviour(this::isBackButtonVisible));
        add(backButton);
    }

    protected abstract boolean isBackButtonVisible();

    protected abstract void initCustomLayout();

    private IModel<String> getLoginPanelTitleModel() {
        IModel<String> configuredTitleModel = getConfiguredLoginPanelTitleModel();
        if (StringUtils.isNotEmpty(configuredTitleModel.getObject())) {
            return configuredTitleModel;
        }
        return getDefaultLoginPanelTitleModel();
    }

    private IModel<String> getLoginPanelDescriptionModel() {
        IModel<String> configuredDescriptionModel = getConfiguredLoginPanelDescriptionModel();
        if (StringUtils.isNotEmpty(configuredDescriptionModel.getObject())) {
            return configuredDescriptionModel;
        }
        return getDefaultLoginPanelDescriptionModel();
    }

    private IModel<String> getConfiguredLoginPanelTitleModel() {
        ModuleAuthentication module = getAuthenticationModuleConfiguration();
        if (module == null) {
            return Model.of();
        }
        DisplayType display = module.getDisplay();
        return () -> GuiDisplayTypeUtil.getTranslatedLabel(display);
    }

    private IModel<String> getConfiguredLoginPanelDescriptionModel() {
        ModuleAuthentication module = getAuthenticationModuleConfiguration();
        if (module == null) {
            return Model.of();
        }
        DisplayType display = module.getDisplay();
        return () -> GuiDisplayTypeUtil.getHelp(display);
    }

    protected abstract IModel<String> getDefaultLoginPanelTitleModel();

    protected abstract IModel<String> getDefaultLoginPanelDescriptionModel();

    private String getSequenceName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            return null;
        }
        AuthenticationSequenceType sequence = mpAuthentication.getSequence();
        if (!AuthenticationSequenceTypeUtil.isDefaultChannel(sequence)
                && AuthenticationSequenceTypeUtil.hasChannelId(sequence, SecurityPolicyUtil.DEFAULT_CHANNEL)) {
            return AuthenticationSequenceTypeUtil.getSequenceDisplayName(sequence);

        }
        return null;
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        showExceptionMessage();
    }

    private void showExceptionMessage() {
        if (errorMessages.isEmpty()) {
            return;
        }

        errorMessages.forEach(this::error);
    }

    @Override
    protected void onAfterRender() {
        super.onAfterRender();
    }

    protected void saveException(Exception exception) {
        ServletWebRequest req = (ServletWebRequest) RequestCycle.get().getRequest();
        HttpServletRequest httpReq = req.getContainerRequest();
        HttpSession httpSession = httpReq.getSession();
        httpSession.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
    }

    protected void cancelPerformed() {
        AuthUtil.getMidpointAuthentication().restart();
        setResponsePage(getMidpointApplication().getHomePage());
    }

    protected boolean isModuleApplicable(ModuleAuthentication moduleAuthentication) {
        return moduleAuthentication != null && (AuthenticationModuleNameConstants.LOGIN_FORM.equals(moduleAuthentication.getModuleTypeName())
                || AuthenticationModuleNameConstants.LDAP.equals(moduleAuthentication.getModuleTypeName()));
    }

    protected boolean isActionDefined() {
        return false;
    }

    protected IModel<String> getActionLabelModel() {
        return () -> "";
    }

    protected void actionPerformed() {
    }

    protected MA getAuthenticationModuleConfiguration() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            getSession().error(getString("No midPoint authentication is found"));
            throw new RestartResponseException(PageError.class);
        }
        //noinspection unchecked
        return (MA) mpAuthentication.getProcessingModuleAuthentication();
    }

    protected void reloadDescriptionPanel(@NotNull AjaxRequestTarget target) {
        target.add(get(ID_PANEL_DESCRIPTION));
    }
}
