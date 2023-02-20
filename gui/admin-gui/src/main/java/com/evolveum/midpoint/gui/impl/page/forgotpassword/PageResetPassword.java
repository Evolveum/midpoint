/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.forgotpassword;

import com.evolveum.midpoint.gui.impl.page.login.AbstractPageLogin;
import com.evolveum.midpoint.gui.impl.page.self.credentials.ChangePasswordPanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.page.self.PageSelf;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/resetPassword", matchUrlForSecurity = "/resetPassword")
        },
        action = {
                @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                        label = PageSelf.AUTH_SELF_ALL_LABEL,
                        description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESET_PASSWORD_URL) })
public class PageResetPassword extends AbstractPageLogin {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PageResetPassword.class);

    protected static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CHANGE_PASSWORD_PANEL = "changePasswordPanel";
    private static final String CHANGE_PASSWORD_BUTTON_STYLE = "btn btn-primary login-panel-control";
    private static final String ID_BACK_BUTTON = "back";

    public PageResetPassword() {
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void initCustomLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_MAIN_FORM);
        form.setOutputMarkupId(true);
        add(form);

        ChangePasswordPanel changePasswordPanel = new ChangePasswordPanel(ID_CHANGE_PASSWORD_PANEL, new LoadableDetachableModel<FocusType>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected FocusType load() {
                return getPrincipalFocus();
            }
        }) {

            @Override
            protected boolean shouldCheckOldPassword() {
                return false;
            }

            @Override
            protected void finishChangePassword(final OperationResult result, AjaxRequestTarget target, boolean showFeedback) {

                if (result.getStatus() == OperationResultStatus.SUCCESS) {
                    result.setMessage(getString("PageResetPassword.reset.successful"));

                    PrismObject<? extends FocusType> focus = getPrincipalFocus().asPrismObject();
                    if (focus == null) {
                        AuthUtil.clearMidpointAuthentication();
                        return;
                    }

                    FocusType focusType = focus.asObjectable();

                    if (focusType.getCredentials() != null && focusType.getCredentials().getNonce() != null) {

                        try {
                            ObjectDelta<UserType> deleteNonceDelta = getPrismContext().deltaFactory().object()
                                    .createModificationDeleteContainer(UserType.class, focusType.getOid(), SchemaConstants.PATH_NONCE,
                                            focusType.getCredentials().getNonce().clone());
                            WebModelServiceUtils.save(deleteNonceDelta, result, PageResetPassword.this);
                        } catch (SchemaException e) {
                            //nothing to do, just let the nonce here. it will be invalid
                        }
                    }

                    showResult(result);
                    target.add(getFeedbackPanel());
                    AuthUtil.clearMidpointAuthentication();
                    setResponsePage(getMidpointApplication().getHomePage());
                } else if (showFeedback) {
                    showResult(result);
                }
                target.add(getFeedbackPanel());
            }

            @Override
            protected boolean isPasswordLimitationPopupVisible() {
                return true;
            }

            @Override
            protected String getChangePasswordButtonStyle() {
                return CHANGE_PASSWORD_BUTTON_STYLE;
            }
        };
        changePasswordPanel.setOutputMarkupId(true);
        form.add(changePasswordPanel);
        AjaxButton backButton = new AjaxButton(ID_BACK_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed();
            }
        };
        backButton.setOutputMarkupId(true);
        add(backButton);

    }

    @Override
    public Task createSimpleTask(String operation) {
        return createAnonymousTask(operation);
    }

    @Override
    protected void confirmAuthentication() {
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource("PageResetPassword.title");
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return createStringResource("PageResetPassword.description");
    }

}
