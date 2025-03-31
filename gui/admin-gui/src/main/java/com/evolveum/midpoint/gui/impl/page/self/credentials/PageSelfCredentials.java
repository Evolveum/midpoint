/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.credentials;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.self.PageSelf;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.web.page.self.component.SecurityQuestionsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/self/credentials")
        },
        action = {
                @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                        label = PageSelf.AUTH_SELF_ALL_LABEL,
                        description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL,
                        label = "PageSelfCredentials.auth.credentials.label",
                        description = "PageSelfCredentials.auth.credentials.description")})
public class PageSelfCredentials extends PageSelf {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageSelfCredentials.class.getName() + ".";
    private static final String OPERATION_LOAD_PRINCIPAL = DOT_CLASS + "loadPrincipalObject";

    protected static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TAB_PANEL = "tabPanel";

    public PageSelfCredentials() {

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);

        List<ITab> tabs = new ArrayList<>(createTabs());

        TabbedPanel<ITab> credentialsTabPanel = WebComponentUtil.createTabPanel(ID_TAB_PANEL, this, tabs, null);
        credentialsTabPanel.setOutputMarkupId(true);

        mainForm.add(credentialsTabPanel);

        add(mainForm);

    }

    private Collection<? extends ITab> createTabs(){
        List<ITab> tabs = new ArrayList<>();
        tabs.add(new AbstractTab(createStringResource("PageSelfCredentials.tabs.password")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new PropagatePasswordPanel<>(panelId, new LoadableDetachableModel<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected FocusType load() {
                        MidPointPrincipal principal = getPrincipal();

                        if (principal == null) {
                            return null;
                        }

                        Task task = createSimpleTask(OPERATION_LOAD_PRINCIPAL);
                        OperationResult result = task.getResult();
                        PrismObject<FocusType> principalReloaded = WebModelServiceUtils
                                .loadObject(FocusType.class, principal.getOid(), PageSelfCredentials.this, task, result);
                        if (principalReloaded == null) {
                            return null;
                        }
                        return principalReloaded.asObjectable();
                    }
                });
            }
        });

        tabs.add(new PanelTab(createStringResource("PageSelfCredentials.tabs.securityQuestion"),
                new VisibleBehaviour(this::showQuestions)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SecurityQuestionsPanel(panelId, Model.of());
            }
        });

        return tabs;
    }

    private boolean showQuestions() {
        GuiProfiledPrincipal principal = getPrincipal();
        if (principal == null) {
            return false;
        }

        CredentialsPolicyType credentialsPolicyType = principal.getApplicableSecurityPolicy().getCredentials();
        if (credentialsPolicyType == null) {
            return false;
        }
        SecurityQuestionsCredentialsPolicyType securityQuestionsPolicy = credentialsPolicyType.getSecurityQuestions();
        if (securityQuestionsPolicy == null) {
            return false;
        }

        List<SecurityQuestionDefinitionType> secQuestAnsList = securityQuestionsPolicy.getQuestion();
        return secQuestAnsList != null && !secQuestAnsList.isEmpty();
    }

}
