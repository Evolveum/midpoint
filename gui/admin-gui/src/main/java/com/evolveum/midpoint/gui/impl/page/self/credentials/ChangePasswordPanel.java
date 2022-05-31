/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.credentials;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordLimitationsPanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.PasswordPolicyValidationPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;
import com.evolveum.midpoint.web.page.self.PageSelfCredentials;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.visit.IVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangePasswordPanel<F extends FocusType> extends BasePanel<F> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ChangePasswordPanel.class);

    private static final String ID_PASSWORD_PANEL = "passwordPanel";
    private static final String ID_OLD_PASSWORD_CONTAINER = "oldPasswordContainer";
    private static final String ID_OLD_PASSWORD_FIELD = "oldPassword";
    private static final String ID_PASSWORD_LABEL = "passwordLabel";
    public static final String ID_ACCOUNTS_TABLE = "accounts";
    public static final String ID_ACCOUNTS_CONTAINER = "accountsContainer";

    private static final String DOT_CLASS = PageSelfCredentials.class.getName() + ".";
    private static final String OPERATION_VALIDATE_PASSWORD = DOT_CLASS + "validatePassword";
    private static final String OPERATION_LOAD_USER_WITH_ACCOUNTS = DOT_CLASS + "loadUserWithAccounts";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";
    private static final String OPERATION_GET_CREDENTIALS_POLICY = DOT_CLASS + "getCredentialsPolicy";

   private Map<String, List<StringLimitationResult>> limitationsByPolicyOid = new HashMap<>();
   private String currentPasswordValue = null;

    public ChangePasswordPanel(String id, IModel<F> objectModel) {
        super(id, objectModel);
    }

    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer oldPasswordContainer = new WebMarkupContainer(ID_OLD_PASSWORD_CONTAINER);
        oldPasswordContainer.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return true;//canEditPassword();
            }

            @Override
            public boolean isVisible() {
                return true;//isCheckOldPassword();
            }
        });
//        add(oldPasswordContainer);

        PasswordTextField oldPasswordField =
                new PasswordTextField(ID_OLD_PASSWORD_FIELD, Model.of(currentPasswordValue));
        oldPasswordField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        oldPasswordField.setRequired(false);
        oldPasswordField.setResetPassword(false);
        oldPasswordField.setOutputMarkupId(true);
 //       oldPasswordContainer.
                add(oldPasswordField);

        Label passwordLabel = new Label(ID_PASSWORD_LABEL, createStringResource("PageSelfCredentials.passwordLabel1"));
        add(passwordLabel);

        IModel<ProtectedStringType> passwordValueModel =  new PropertyModel<>(getModel(),
                FocusType.F_CREDENTIALS + "." + CredentialsType.F_PASSWORD + "." + PasswordType.F_VALUE);
        PasswordPanel passwordPanel = new PasswordPanel(ID_PASSWORD_PANEL, passwordValueModel, getModelObject().asPrismObject()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected <F extends FocusType> ValuePolicyType getValuePolicy(PrismObject<F> object) {
                return null;//getModelObject().getFocusPolicy();
            }

            @Override
            protected void updatePasswordValidation(AjaxRequestTarget target) {
                super.updatePasswordValidation(target);
                limitationsByPolicyOid.clear();
//                getTable().visitChildren(PasswordPolicyValidationPanel.class,
//                        (IVisitor<PasswordPolicyValidationPanel, PasswordPolicyValidationPanel>) (panel, iVisit) -> {
//                            panel.refreshValidationPopup(target);
//                        });
            }

            @Override
            protected boolean canEditPassword() {
                return true;//com.evolveum.midpoint.web.page.self.component.ChangePasswordPanel.this.canEditPassword();
            }

            @Override
            protected boolean isRemovePasswordVisible() {
                return false;
            }
        };
        passwordPanel.getBaseFormComponent().add(new AttributeModifier("autofocus", ""));
        add(passwordPanel);

        LoadableDetachableModel<List<StringLimitationResult>> limitationsModel = new LoadableDetachableModel<>() {
            @Override
            protected List<StringLimitationResult> load() {
                return getLimitationsForActualPassword(passwordValueModel.getObject());
            }
        };

        PasswordLimitationsPanel passwordLimitationsPanel = new PasswordLimitationsPanel("passwordValidationPanel", limitationsModel);
        passwordLimitationsPanel.setOutputMarkupId(true);
        add(passwordLimitationsPanel);
    }

    private List<StringLimitationResult> getLimitationsForActualPassword(ProtectedStringType passwordValue) {
        ValuePolicyType valuePolicy = getValuePolicy();
        if (valuePolicy != null) {
            Task task = getPageBase().createAnonymousTask(OPERATION_VALIDATE_PASSWORD);
            try {
                return getPageBase().getModelInteractionService().validateValue(passwordValue == null ? new ProtectedStringType() : passwordValue,
                        valuePolicy, getModelObject().asPrismObject(), task, task.getResult());
            } catch (Exception e) {
                LOGGER.error("Couldn't validate password security policy", e);
            }
        }
        return new ArrayList<>();
    }

    protected <F extends FocusType> ValuePolicyType getValuePolicy() {
        ValuePolicyType valuePolicyType = null;
        try {
            MidPointPrincipal user = AuthUtil.getPrincipalUser();
            if (getPageBase() != null) {
                if (user != null) {
                    Task task = getPageBase().createSimpleTask("load value policy");
                    valuePolicyType = searchValuePolicy(task);
                } else {
                    valuePolicyType = getPageBase().getSecurityContextManager().runPrivileged((Producer<ValuePolicyType>) () -> {
                        Task task = getPageBase().createAnonymousTask("load value policy");
                        return searchValuePolicy(task);
                    });
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't load security policy for focus " + getModelObject().asPrismObject(), e);
        }
        return valuePolicyType;
    }

    private ValuePolicyType searchValuePolicy(Task task) {
        CredentialsPolicyType credentials = WebComponentUtil.getPasswordCredentialsPolicy(getModelObject().asPrismObject(), getPageBase(), task);
        if (credentials != null && credentials.getPassword() != null
                && credentials.getPassword().getValuePolicyRef() != null) {
            PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.resolveReferenceNoFetch(
                    credentials.getPassword().getValuePolicyRef(), getPageBase(), task, task.getResult());
            if (valuePolicy != null) {
                return valuePolicy.asObjectable();
            }
        }
        return null;
    }

}
