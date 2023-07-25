/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModulesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.HintAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lskublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/hint", matchUrlForSecurity = "/hint")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.HINT)
public class PagePasswordHint extends PageAuthenticationBase<HintAuthenticationModuleType> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PagePasswordHint.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_HINT_PANEL = "hintPanel";
    private static final String ID_HINT_LABEL = "hintLabel";
    private static final String ID_BACK_BUTTON = "back";
    private static final String ID_BACK_BUTTON_LABEL = "backButtonLabel";
    private static final String ID_PASSWORD_RESET_SUBMITED = "resetPasswordInfo";

    private String hint = null;

    public PagePasswordHint() {
    }

    protected void initCustomLayout() {
        MidpointForm form = new MidpointForm(ID_MAIN_FORM);
        form.add(AttributeModifier.replace("action", (IModel<String>) this::getUrlProcessingLogin));
        add(form);

        initHintPanel(form);

        initButtons(form);
    }

    private void initHintPanel(MidpointForm<?> form) {
        initHintValue();

        WebMarkupContainer hintPanel = new WebMarkupContainer(ID_HINT_PANEL);
        hintPanel.setOutputMarkupId(true);
        hintPanel.add(new VisibleBehaviour(this::isHintPresent));
        form.add(hintPanel);

        Label hintLabel = new Label(ID_HINT_LABEL, new LoadableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return createStringResource("PageEmailNonce.passwordHintLabel", hint).getString();
            }
        });
        hintLabel.setOutputMarkupId(true);
        hintPanel.add(hintLabel);
    }

    private void initHintValue() {
        UserType user = searchUser();
        hint = getUserPasswordHint(user);
    }

    private boolean isHintPresent() {
        return StringUtils.isNotEmpty(getHintValue());
    }

    private String getHintValue() {
        return hint != null ? hint : null;
    }

    private void initButtons(MidpointForm form) {
        form.add(createBackButton(ID_BACK_BUTTON));
    }

    @Override
    protected AjaxButton createBackButton(String id){
        AjaxButton backButton = new AjaxButton(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed();
            }
        };
        backButton.setOutputMarkupId(true);

        Label backButtonLabel = new Label(ID_BACK_BUTTON_LABEL, new LoadableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return createStringResource(isHintPresent() ? "PageEmailNonce.backButtonAfterHintLabel" : "PageEmailNonce.backButtonLabel").getString();
            }
        });
        backButton.add(backButtonLabel);
        return backButton;
    }

    @Override
    protected ObjectQuery createStaticFormQuery() {
        return null;
    }


    private String getUserPasswordHint(@NotNull UserType user) {
        return user.getCredentials() != null && user.getCredentials().getPassword() != null ?
                user.getCredentials().getPassword().getHint() : null;
    }

    @Override
    protected boolean isDynamicFormVisible() {
        return super.isDynamicFormVisible() && !isHintPresent();
    }

    protected DynamicFormPanel getDynamicForm(){
        return (DynamicFormPanel) getMainForm().get(createComponentPath(ID_DYNAMIC_LAYOUT, ID_DYNAMIC_FORM));
    }

    private MidpointForm getMainForm() {
        return (MidpointForm) get(ID_MAIN_FORM);
    }

    @Override
    protected String getModuleTypeName() {
        return AuthenticationModuleNameConstants.HINT;
    }

    @Override
    protected List<HintAuthenticationModuleType> getAuthetcationModules(AuthenticationModulesType modules) {
        return modules.getHint();
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource("PagePasswordHint.panelTitle");
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return createStringResource("PagePasswordHint.description");
    }

}

