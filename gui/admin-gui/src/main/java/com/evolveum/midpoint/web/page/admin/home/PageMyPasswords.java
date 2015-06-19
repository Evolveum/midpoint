/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.home;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.input.PasswordPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/myPasswords", action = {
        @AuthorizationAction(actionUri = PageAdminHome.AUTH_HOME_ALL_URI,
                label = PageAdminHome.AUTH_HOME_ALL_LABEL, description = PageAdminHome.AUTH_HOME_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MY_PASSWORDS_URL,
                label = "PageMyPasswords.auth.myPasswords.label", description = "PageMyPasswords.auth.myPasswords.description")})
public class PageMyPasswords extends PageAdminHome {

    private static final Trace LOGGER = TraceManager.getTrace(PageMyPasswords.class);

    private static final String DOT_CLASS = PageMyPasswords.class.getName() + ".";
    private static final String OPERATION_LOAD_USER_WITH_ACCOUNTS = DOT_CLASS + "loadUserWithAccounts";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";
    private static final String OPERATION_SAVE_PASSWORD = DOT_CLASS + "savePassword";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_PASSWORD_PANEL = "passwordPanel";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_BACK = "back";
    private static final String ID_SAVE = "save";

    private IModel<MyPasswordsDto> model;

    public PageMyPasswords() {
        model = new LoadableModel<MyPasswordsDto>(false) {

            @Override
            protected MyPasswordsDto load() {
                return loadPageModel();
            }
        };

        initLayout();
    }

    private MyPasswordsDto loadPageModel() {
        LOGGER.debug("Loading user and accounts.");

        MyPasswordsDto dto = new MyPasswordsDto();
        OperationResult result = new OperationResult(OPERATION_LOAD_USER_WITH_ACCOUNTS);
        try {
            String userOid = SecurityUtils.getPrincipalUser().getOid();
            Task task = createSimpleTask(OPERATION_LOAD_USER);
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_USER);
            PrismObject<UserType> user = getModelService().getObject(UserType.class, userOid, null, task, subResult);
            subResult.recordSuccessIfUnknown();

            dto.getAccounts().add(createDefaultPasswordAccountDto(user));

            PrismReference reference = user.findReference(UserType.F_LINK_REF);
            if (reference == null || reference.getValues() == null) {
                LOGGER.debug("No accounts found for user {}.", new Object[]{userOid});
                return dto;
            }

            final Collection<SelectorOptions<GetOperationOptions>> options =
                    SelectorOptions.createCollection(ShadowType.F_RESOURCE, GetOperationOptions.createResolve());

            List<PrismReferenceValue> values = reference.getValues();
            for (PrismReferenceValue value : values) {
                subResult = result.createSubresult(OPERATION_LOAD_ACCOUNT);
                try {
                    String accountOid = value.getOid();
                    task = createSimpleTask(OPERATION_LOAD_ACCOUNT);

                    PrismObject<ShadowType> account = getModelService().getObject(ShadowType.class,
                            accountOid, options, task, subResult);

                    dto.getAccounts().add(createPasswordAccountDto(account));
                    subResult.recordSuccessIfUnknown();
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't load account", ex);
                    subResult.recordFatalError("Couldn't load account.", ex);
                }
            }
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load accounts", ex);
            result.recordFatalError("Couldn't load accounts", ex);
        } finally {
            result.recomputeStatus();
        }

        Collections.sort(dto.getAccounts());

        if (!result.isSuccess() && !result.isHandledError()) {
            showResult(result);
        }

        return dto;
    }

    private PasswordAccountDto createDefaultPasswordAccountDto(PrismObject<UserType> user) {
        return new PasswordAccountDto(user.getOid(), getString("PageMyPasswords.accountMidpoint"),
                getString("PageMyPasswords.resourceMidpoint"), WebMiscUtil.isActivationEnabled(user), true);
    }

    private PasswordAccountDto createPasswordAccountDto(PrismObject<ShadowType> account) {
        PrismReference resourceRef = account.findReference(ShadowType.F_RESOURCE_REF);
        String resourceName;
        if (resourceRef == null || resourceRef.getValue() == null || resourceRef.getValue().getObject() == null) {
            resourceName = getString("PageMyPasswords.couldntResolve");
        } else {
            resourceName = WebMiscUtil.getName(resourceRef.getValue().getObject());
        }

        return new PasswordAccountDto(account.getOid(), WebMiscUtil.getName(account),
                resourceName, WebMiscUtil.isActivationEnabled(account));
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        List<IColumn<PasswordAccountDto, String>> columns = initColumns();
        ListDataProvider<PasswordAccountDto> provider = new ListDataProvider<PasswordAccountDto>(this,
                new PropertyModel<List<PasswordAccountDto>>(model, MyPasswordsDto.F_ACCOUNTS));
        TablePanel accounts = new TablePanel(ID_ACCOUNTS, provider, columns);
        accounts.setItemsPerPage(30);
        accounts.setShowPaging(false);
        mainForm.add(accounts);

        PasswordPanel passwordPanel = new PasswordPanel(ID_PASSWORD_PANEL,
                new PropertyModel<String>(model, MyPasswordsDto.F_PASSWORD));
        mainForm.add(passwordPanel);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setFilter(new ComponentFeedbackMessageFilter(passwordPanel.getBaseFormComponent()));
        feedback.setOutputMarkupId(true);
        mainForm.add(feedback);

        initButtons(mainForm);
    }

    private List<IColumn<PasswordAccountDto, String>> initColumns() {
        List<IColumn<PasswordAccountDto, String>> columns = new ArrayList<IColumn<PasswordAccountDto, String>>();

        IColumn column = new CheckBoxHeaderColumn<UserType>();
        columns.add(column);

        column = new PropertyColumn(createStringResource("PageMyPasswords.name"),
                PasswordAccountDto.F_DISPLAY_NAME);
        columns.add(column);

        column = new PropertyColumn(createStringResource("PageMyPasswords.resourceName"),
                PasswordAccountDto.F_RESOURCE_NAME);
        columns.add(column);

        CheckBoxColumn enabled = new CheckBoxColumn(createStringResource("PageMyPasswords.enabled"),
                PasswordAccountDto.F_ENABLED);
        enabled.setEnabled(false);
        columns.add(enabled);

        return columns;
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE, createStringResource("PageBase.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(save);

        AjaxButton back = new AjaxButton(ID_BACK, createStringResource("PageBase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(back);
    }

    private void savePerformed(AjaxRequestTarget target) {
        List<PasswordAccountDto> accounts = WebMiscUtil.getSelectedData(
                (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_ACCOUNTS)));
        if (accounts.isEmpty()) {
            warn(getString("PageMyPasswords.noAccountSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_SAVE_PASSWORD);
        try {
            MyPasswordsDto dto = model.getObject();
            ProtectedStringType password = new ProtectedStringType();
            password.setClearValue(dto.getPassword());
            WebMiscUtil.encryptProtectedString(password, true, getMidpointApplication());

            final ItemPath valuePath = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
                    CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
            SchemaRegistry registry = getPrismContext().getSchemaRegistry();
            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
            
            
            for (PasswordAccountDto accDto : accounts) {
                PrismObjectDefinition objDef = accDto.isMidpoint() ?
                        registry.findObjectDefinitionByCompileTimeClass(UserType.class) :
                        registry.findObjectDefinitionByCompileTimeClass(ShadowType.class);

                PropertyDelta delta = PropertyDelta.createModificationReplaceProperty(valuePath, objDef, password,password);
                
                Class<? extends ObjectType> type = accDto.isMidpoint() ? UserType.class : ShadowType.class;
                
                deltas.add(ObjectDelta.createModifyDelta(accDto.getOid(), delta, type, getPrismContext()));
                
            }
            getModelService().executeChanges(deltas, null, createSimpleTask(OPERATION_SAVE_PASSWORD), result);

            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't save password changes", ex);
            result.recordFatalError("Couldn't save password changes.", ex);
        } finally {
            result.recomputeStatus();
        }

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
            showResultInSession(result);
            setResponsePage(PageDashboard.class);
        }
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        setResponsePage(PageDashboard.class);
    }
}
