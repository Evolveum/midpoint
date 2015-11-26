package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.*;
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
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusProjectionDto;
import com.evolveum.midpoint.web.page.self.component.ChangePasswordPanel;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
@PageDescriptor(url = {"/self/credentials"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL,
                label = "PageSelfCredentials.auth.credentials.label",
                description = "PageSelfCredentials.auth.credentials.description")})
public class PageSelfCredentials extends PageSelf {
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TAB_PANEL = "tabPanel";
    private static final String ID_SAVE_BUTTON = "save";
    private static final String ID_CANCEL_BUTTON = "cancel";
    private static final String ID_PANEL = "panel";

    private static final Trace LOGGER = TraceManager.getTrace(PageSelfCredentials.class);
    private static final String DOT_CLASS = PageSelfCredentials.class.getName() + ".";
    private static final String OPERATION_LOAD_USER_WITH_ACCOUNTS = DOT_CLASS + "loadUserWithAccounts";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";
    private static final String OPERATION_SAVE_PASSWORD = DOT_CLASS + "savePassword";
    private static final String OPERATION_CHECK_PASSWORD = DOT_CLASS + "checkPassword";
    private static final String OPERATION_LOAD_SHADOW = DOT_CLASS + "loadShadow";
    private static final String OPERATION_GET_CREDENTIALS_POLICY = DOT_CLASS + "getCredentialsPolicy";


    private LoadableModel<MyPasswordsDto> model;
    private PrismObject<UserType> user;

    public PageSelfCredentials() {
        model = new LoadableModel<MyPasswordsDto>(false) {

            @Override
            protected MyPasswordsDto load() {
                return loadPageModel();
            }
        };

        initLayout();
    }

    public PageSelfCredentials(final MyPasswordsDto myPasswordsDto) {
        model = new LoadableModel<MyPasswordsDto>(myPasswordsDto, false) {

            @Override
            protected MyPasswordsDto load() {
                return myPasswordsDto;
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
            user = getModelService().getObject(UserType.class, userOid, null, task, subResult);
            subResult.recordSuccessIfUnknown();

            dto.getAccounts().add(createDefaultPasswordAccountDto(user));

            CredentialsPolicyType credentialsPolicyType = getPasswordCredentialsPolicy();
            if (credentialsPolicyType != null) {
                PasswordCredentialsPolicyType passwordCredentialsPolicy = credentialsPolicyType.getPassword();
                if (passwordCredentialsPolicy != null) {
                    CredentialsPropagationUserControlType propagationUserControl = passwordCredentialsPolicy.getPropagationUserControl();
                    if (propagationUserControl != null) {
                        dto.setPropagation(propagationUserControl);
                    }
                    PasswordChangeSecurityType passwordChangeSecurity = passwordCredentialsPolicy.getPasswordChangeSecurity();
                    if (passwordChangeSecurity != null) {
                        dto.setPasswordChangeSecurity(passwordChangeSecurity);
                    }

                }

            }

            if (dto.getPropagation() == null || dto.getPropagation().equals(CredentialsPropagationUserControlType.USER_CHOICE)) {
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


    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);

        List<ITab> tabs = new ArrayList<>();
        tabs.add(new AbstractTab(createStringResource("PageSelfCredentials.tabs.password")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new ChangePasswordPanel(panelId, model, model.getObject());
            }
        });

        TabbedPanel credentialsTabPanel = new TabbedPanel(ID_TAB_PANEL, tabs) {
            @Override
            protected WebMarkupContainer newLink(String linkId, final int index) {
                return new AjaxSubmitLink(linkId) {

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        super.onError(target, form);

                        target.add(getFeedbackPanel());
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onSubmit(target, form);

                        setSelectedTab(index);
                        if (target != null) {
                            target.add(findParent(TabbedPanel.class));
                        }
                    }
                };
            }
        };
        credentialsTabPanel.setOutputMarkupId(true);

        mainForm.add(credentialsTabPanel);
        initButtons(mainForm);

        add(mainForm);

    }

    private void initButtons(Form mainForm) {
        AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE_BUTTON, createStringResource("PageBase.button.save")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onSavePerformed(target);
            }
        };
        mainForm.setDefaultButton(save);
        mainForm.add(save);

        AjaxSubmitButton cancel = new AjaxSubmitButton(ID_CANCEL_BUTTON, createStringResource("PageBase.button.cancel")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onCancelPerformed(target);
            }
        };
        mainForm.add(cancel);
    }

    private PasswordAccountDto createDefaultPasswordAccountDto(PrismObject<UserType> user) {
        return new PasswordAccountDto(user.getOid(), user.getName().getNorm(),
                getString("PageSelfCredentials.resourceMidpoint"), WebMiscUtil.isActivationEnabled(user), true);
    }

    private PasswordAccountDto createPasswordAccountDto(PrismObject<ShadowType> account) {
        PrismReference resourceRef = account.findReference(ShadowType.F_RESOURCE_REF);
        String resourceName;
        if (resourceRef == null || resourceRef.getValue() == null || resourceRef.getValue().getObject() == null) {
            resourceName = getString("PageSelfCredentials.couldntResolve");
        } else {
            resourceName = WebMiscUtil.getName(resourceRef.getValue().getObject());
        }

        PasswordAccountDto passwordAccountDto = new PasswordAccountDto(account.getOid(), WebMiscUtil.getName(account),
                resourceName, WebMiscUtil.isActivationEnabled(account));
        passwordAccountDto.setPasswordOutbound(getPasswordOutbound(account));
        return passwordAccountDto;
    }

    private void onSavePerformed(AjaxRequestTarget target) {
        List<PasswordAccountDto> selectedAccounts = getSelectedAccountsList();

        if ((model.getObject().getPasswordChangeSecurity() == null) || (model.getObject().getPasswordChangeSecurity() != null &&
                model.getObject().getPasswordChangeSecurity().equals(PasswordChangeSecurityType.OLD_PASSWORD))) {
            LOGGER.debug("Check old password");
            if (model.getObject().getOldPassword() == null
                    || model.getObject().getOldPassword().trim().equals("")){
                warn(getString("PageSelfCredentials.specifyOldPasswordMessage"));
                target.add(getFeedbackPanel());
                return;
            } else {
                OperationResult checkPasswordResult = new OperationResult(OPERATION_CHECK_PASSWORD);
                Task checkPasswordTask = createSimpleTask(OPERATION_CHECK_PASSWORD);
                try {
                    ProtectedStringType oldPassword = new ProtectedStringType();
                    oldPassword.setClearValue(model.getObject().getOldPassword());
                    boolean isCorrectPassword = getModelInteractionService().checkPassword(user.getOid(), oldPassword,
                            checkPasswordTask, checkPasswordResult);
                    if (!isCorrectPassword) {
                        warn(getString("PageSelfCredentials.incorrectOldPassword"));
                        target.add(getFeedbackPanel());
                        return;
                    }
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't check password", ex);
                    checkPasswordResult.recordFatalError("Couldn't check password." + ex.getMessage(), ex);
                    target.add(getFeedbackPanel());
                    return;
                } finally {
                    checkPasswordResult.computeStatus();
                }
            }
        }
        if (selectedAccounts.isEmpty()) {
            warn(getString("PageSelfCredentials.noAccountSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_SAVE_PASSWORD);
        try {
            MyPasswordsDto dto = model.getObject();
            ProtectedStringType password = dto.getPassword();
            WebMiscUtil.encryptProtectedString(password, true, getMidpointApplication());

            final ItemPath valuePath = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
                    CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
            SchemaRegistry registry = getPrismContext().getSchemaRegistry();
            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();


            for (PasswordAccountDto accDto : selectedAccounts) {
                    PrismObjectDefinition objDef = accDto.isMidpoint() ?
                            registry.findObjectDefinitionByCompileTimeClass(UserType.class) :
                            registry.findObjectDefinitionByCompileTimeClass(ShadowType.class);

                    PropertyDelta delta = PropertyDelta.createModificationReplaceProperty(valuePath, objDef, password, password);

                    Class<? extends ObjectType> type = accDto.isMidpoint() ? UserType.class : ShadowType.class;

                    deltas.add(ObjectDelta.createModifyDelta(accDto.getOid(), delta, type, getPrismContext()));
            }
            getModelService().executeChanges(deltas, null, createSimpleTask(OPERATION_SAVE_PASSWORD), result);

            result.recordSuccess();
        } catch (Exception ex) {
            MyPasswordsDto dto = model.getObject();
            ProtectedStringType password = dto.getPassword();
            if (password != null){
                password.setEncryptedData(null);
            }
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
            if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                    AuthorizationConstants.AUTZ_UI_HOME_ALL_URL)) {
                setResponsePage(PageDashboard.class);
            } else {
                setResponsePage(PageSelfDashboard.class);
            }
        }
    }

    private List<PasswordAccountDto> getSelectedAccountsList(){
        List<PasswordAccountDto> passwordAccountDtos = model.getObject().getAccounts();
        List<PasswordAccountDto> selectedAccountList = new ArrayList<>();
        if (model.getObject().getPropagation() != null
                && model.getObject().getPropagation().equals(CredentialsPropagationUserControlType.MAPPING)){
            selectedAccountList.addAll(passwordAccountDtos);
        } else {
            for (PasswordAccountDto passwordAccountDto : passwordAccountDtos) {
                if (passwordAccountDto.getCssClass().equals(ChangePasswordPanel.SELECTED_ACCOUNT_ICON_CSS)) {
                    selectedAccountList.add(passwordAccountDto);
                }
            }
        }
        return selectedAccountList;
    }
    private void onCancelPerformed(AjaxRequestTarget target) {
        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                AuthorizationConstants.AUTZ_UI_HOME_ALL_URL)) {
            setResponsePage(PageDashboard.class);
        } else {
            setResponsePage(PageSelfDashboard.class);
        }
    }

    private List<ShadowType> loadShadowTypeList() {
        List<ObjectReferenceType> references = user.asObjectable().getLinkRef();
        Task task = createSimpleTask(OPERATION_LOAD_SHADOW);
        List<ShadowType> shadowTypeList = new ArrayList<>();

        for (ObjectReferenceType reference : references) {
            OperationResult subResult = new OperationResult(OPERATION_LOAD_SHADOW);
            try {
                Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(ShadowType.F_RESOURCE,
                        GetOperationOptions.createResolve());

                if (reference.getOid() == null) {
                    continue;
                }
                PrismObject<ShadowType> shadow = WebModelUtils.loadObject(ShadowType.class, reference.getOid(), options, this, task, subResult);
                shadowTypeList.add(shadow.asObjectable());
            } catch (Exception ex) {
                LoggingUtils.logException(LOGGER, "Couldn't load account", ex);
                subResult.recordFatalError("Couldn't load account." + ex.getMessage(), ex);
            } finally {
                subResult.computeStatus();
            }
        }
        return shadowTypeList;

    }

    private boolean getPasswordOutbound(PrismObject<ShadowType> shadow) {
        try {
            RefinedObjectClassDefinition rOCDef = getModelInteractionService().getEditObjectClassDefinition(shadow,
                    shadow.asObjectable().getResource().asPrismObject(),
                    AuthorizationPhaseType.REQUEST);
            if (rOCDef != null && rOCDef.getPasswordOutbound() != null ){
                return true;
            }
        } catch (SchemaException ex) {

        }
        return false;
    }

    public PrismObject<UserType> getUser() {
        return user;
    }

    private CredentialsPolicyType getPasswordCredentialsPolicy (){
        LOGGER.debug("Getting credentials policy");
        Task task = createSimpleTask(OPERATION_GET_CREDENTIALS_POLICY);
        OperationResult result = new OperationResult(OPERATION_GET_CREDENTIALS_POLICY);
        CredentialsPolicyType credentialsPolicyType = null;
        try {
            credentialsPolicyType = getModelInteractionService().getCredentialsPolicy(user, task, result);
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load credentials policy", ex);
            result.recordFatalError("Couldn't load credentials policy." + ex.getMessage(), ex);
        } finally {
            result.computeStatus();
        }
        return credentialsPolicyType;
    }
}
