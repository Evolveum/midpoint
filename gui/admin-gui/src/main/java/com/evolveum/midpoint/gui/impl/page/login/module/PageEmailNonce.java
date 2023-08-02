/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.impl.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;

import com.evolveum.midpoint.schema.SearchResultList;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.CredentialModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.login.PageAuthenticationBase;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author lskublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/emailNonce", matchUrlForSecurity = "/emailNonce")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.MAIL_NONCE)
public class PageEmailNonce extends PageAbstractAuthenticationModule<CredentialModuleAuthentication> {
    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageEmailNonce.class);

    private static final String ID_STATIC_LAYOUT = "staticLayout";
    private static final String ID_EMAIL = "email";
    private static final String ID_MAIN_FORM = "form";
    private static final String ID_SUBMIT_IDENTIFIER = "submitIdentifier";


    private static final String DOT_CLASS = PageEmailNonce.class.getName() + ".";
    protected static final String OPERATION_LOAD_DYNAMIC_FORM = DOT_CLASS + "loadDynamicForm";


    protected static final String ID_DYNAMIC_LAYOUT = "dynamicLayout";
    protected static final String ID_DYNAMIC_FORM = "dynamicForm";


    private ObjectReferenceType formRef;


    private boolean submited;
    private UserType user = null;


    public PageEmailNonce() {
        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal != null) {
            userIdentifierSubmitPerformed(null);
            submited = true;
        }
    }

    private void initFormRef() {
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(null);

        if (securityPolicy.getCredentialsReset() != null) {
            this.formRef = securityPolicy.getCredentialsReset().getFormRef();
        }

    }



    @Override
    protected void initModuleLayout(MidpointForm form) {
        form.add(new VisibleBehaviour(() -> !submited));

        initStaticLayout(form);

        initDynamicLayout(form, PageEmailNonce.this);

        initButtons(form);

    }

    protected void initDynamicLayout(final org.apache.wicket.markup.html.form.Form<?> mainForm, PageAdminLTE parentPage) {
        WebMarkupContainer dynamicLayout = new WebMarkupContainer(ID_DYNAMIC_LAYOUT);
        dynamicLayout.setOutputMarkupId(true);
        mainForm.add(dynamicLayout);

        dynamicLayout.add(new VisibleBehaviour(this::isDynamicFormVisible));

        DynamicFormPanel<FocusType> searchAttributesForm = runPrivileged(
                () -> {
                    ObjectReferenceType formRef = getFormRef();
                    if (formRef == null) {
                        return null;
                    }
                    Task task = createAnonymousTask(OPERATION_LOAD_DYNAMIC_FORM);
                    return new DynamicFormPanel<>(ID_DYNAMIC_FORM, UserType.COMPLEX_TYPE,
                            formRef.getOid(), mainForm, task, parentPage, true);
                });

        if (searchAttributesForm != null) {
            dynamicLayout.add(searchAttributesForm);
        }
    }

    public ObjectReferenceType getFormRef() {
        if (formRef == null) {
            initFormRef();
        }
        return formRef;
    }


    private void initButtons(MidpointForm form) {
        AjaxSubmitButton submitUserIdentifier = new AjaxSubmitButton(ID_SUBMIT_IDENTIFIER, createStringResource("PageBase.button.submit")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                userIdentifierSubmitPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }

        };
        submitUserIdentifier.add(new VisibleBehaviour(() -> !submited));
        form.add(submitUserIdentifier);
    }

//    @Override
//    protected String getModuleTypeName() {
//        return AuthenticationModuleNameConstants.MAIL_NONCE;
//    }

    private void userIdentifierSubmitPerformed(AjaxRequestTarget target) {
        if (user == null) {
            user = searchUser();
            validateUserNotNullOrFail();
        }
        LOGGER.trace("Reset Password user: {}", user);

        continuePasswordReset(target);
    }

    protected UserType searchUser() {

        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal != null) {
            FocusType focus = principal.getFocus();
            return (UserType) focus;
        }

        ObjectQuery query;

        if (isDynamicForm()) {
            query = createDynamicFormQuery();
        } else {
            query = createStaticFormQuery();
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Searching for user with query:\n{}", query.debugDump(1));
        }

        return searchUserPrivileged(query);

    }


    private void continuePasswordReset(AjaxRequestTarget target) {
        validateUserNotNullOrFail();
        NonceCredentialsPolicyType noncePolicy = getMailNoncePolicy(user.asPrismObject());

        OperationResult result = saveUserNonce(user, noncePolicy);
        if (result.getStatus() == OperationResultStatus.SUCCESS) {
            submited = true;
            if (target != null) {
                target.add(PageEmailNonce.this);
            }
        } else {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("Failed to send nonce to user: {} ", result.getMessage());
            throw new RestartResponseException(PageEmailNonce.this);
        }
    }

    private void validateUserNotNullOrFail() {
        if (user == null) {
            getSession().error(getString("pageForgetPassword.message.user.not.found"));
            throw new RestartResponseException(PageEmailNonce.class);
        }
    }

    private @NotNull NonceCredentialsPolicyType getMailNoncePolicy(PrismObject<UserType> user) {
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(user);
        LOGGER.trace("Found security policy: {}", securityPolicy);

        if (securityPolicy == null) {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("No security policy, cannot process nonce credential");
            // Just log the error, but do not display it. We are still in unprivileged part of the web
            // we do not want to provide any information to the attacker.
            throw new RestartResponseException(PageEmailNonce.class);
        }
        if (securityPolicy.getCredentials() == null) {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("No credential for security policy, cannot process nonce credential");
            // Just log the error, but do not display it. We are still in unprivileged part of the web
            // we do not want to provide any information to the attacker.
            throw new RestartResponseException(PageEmailNonce.class);
        }
        if (securityPolicy.getCredentials().getNonce() == null) {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("No nonce credential for security policy, cannot process nonce credential");
            // Just log the error, but do not display it. We are still in unprivileged part of the web
            // we do not want to provide any information to the attacker.
            throw new RestartResponseException(PageEmailNonce.class);
        }

        CredentialModuleAuthentication moduleType = getAuthenticationModuleConfiguration();
        String credentialName = moduleType.getCredentialName();

        if (credentialName == null) {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("EmailNonceModuleAuthentication " + moduleType.getModuleIdentifier() + " haven't define name of credential");
            throw new RestartResponseException(PageEmailNonce.class);
        }

        NonceCredentialsPolicyType credentialByName = null;

        for (NonceCredentialsPolicyType credential : securityPolicy.getCredentials().getNonce()) {
            if (credentialName.equals(credential.getName())) {
                credentialByName = credential;
            }
        }
        if (credentialByName == null) {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("Couldn't find nonce credentials by name " + credentialName);
            throw new RestartResponseException(PageEmailNonce.class);
        }

        return credentialByName;
    }

    private void initStaticLayout(MidpointForm form) {

        WebMarkupContainer staticLayout = new WebMarkupContainer(ID_STATIC_LAYOUT);
        staticLayout.setOutputMarkupId(true);
        staticLayout.add(new VisibleBehaviour(() -> !isDynamicForm()));
        form.add(staticLayout);

        RequiredTextField<String> visibleUsername = new RequiredTextField<>(ID_EMAIL, new Model<>());
        visibleUsername.setOutputMarkupId(true);
        staticLayout.add(visibleUsername);
    }

    protected boolean isDynamicFormVisible() {
        return isDynamicForm();
    }

    protected boolean isDynamicForm() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            AuthenticationChannel channel = ((MidpointAuthentication) authentication).getAuthenticationChannel();
            if (channel != null && !SchemaConstants.CHANNEL_RESET_PASSWORD_URI.equals(channel.getChannelId())) {
                return false;
            }
        }
        return getFormRef() != null;
    }

    protected ObjectQuery createDynamicFormQuery() {
        DynamicFormPanel<UserType> userDynamicPanel = getDynamicForm();
        List<ItemPath> filledItems = userDynamicPanel.getChangedItems();
        PrismObject<UserType> user;
        try {
            user = userDynamicPanel.getObject();
        } catch (SchemaException e1) {
            getSession().error(getString("pageForgetPassword.message.usernotfound"));
            throw new RestartResponseException(getClass());
        }

        S_FilterExit filter = QueryBuilder.queryFor(UserType.class, PrismContext.get()).all();
        for (ItemPath path : filledItems) {
            PrismProperty<?> property = user.findProperty(path);
            filter = filter.and().item(path).eq(property.getAnyValue().clone());
        }
        return filter.build();
    }


    public PageBase getPageBase() {
        return (PageBase) getPage();
    }


//    @Override
    protected ObjectQuery createStaticFormQuery() {
        RequiredTextField<String> emailTextFiled = getEmail();
        String email = emailTextFiled != null ? emailTextFiled.getModelObject() : null;
        LOGGER.debug("Reset Password user info form submitted. email={}", email);

        return getPrismContext().queryFor(UserType.class).item(UserType.F_EMAIL_ADDRESS)
                .eq(email).matchingCaseIgnore().build();

    }

    private MidpointForm getMainForm() {
        return (MidpointForm) get(ID_MAIN_FORM);
    }

    protected DynamicFormPanel<UserType> getDynamicForm(){
        return (DynamicFormPanel) getMainForm().get(createComponentPath(ID_DYNAMIC_LAYOUT, ID_DYNAMIC_FORM));
    }

    private RequiredTextField<String> getEmail(){
        //noinspection unchecked
        return (RequiredTextField<String>) getMainForm().get(createComponentPath(ID_STATIC_LAYOUT, ID_EMAIL));
    }

    private OperationResult saveUserNonce(final UserType user, final NonceCredentialsPolicyType noncePolicy) {
        return runPrivileged(new Producer<>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public OperationResult run() {
                Task task = createAnonymousTask("generateUserNonce");
                task.setChannel(SchemaConstants.CHANNEL_RESET_PASSWORD_URI);
                task.setOwner(user.asPrismObject());
                OperationResult result = new OperationResult("generateUserNonce");
                ProtectedStringType nonceCredentials = new ProtectedStringType();
                try {
                    nonceCredentials
                            .setClearValue(generateNonce(noncePolicy, task, user.asPrismObject(), result));

                    ObjectDelta<UserType> nonceDelta = getPrismContext().deltaFactory().object()
                            .createModificationReplaceProperty(UserType.class, user.getOid(),
                                    SchemaConstants.PATH_NONCE_VALUE, nonceCredentials);

                    WebModelServiceUtils.save(nonceDelta, result, task, PageEmailNonce.this);
                } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException |
                         ConfigurationException | SecurityViolationException e) {
                    result.recordFatalError(getString("PageForgotPassword.message.saveUserNonce.fatalError"));
                    LoggingUtils.logException(LOGGER, "Failed to generate nonce for user: " + e.getMessage(),
                            e);
                }

                result.computeStatusIfUnknown();
                return result;
            }

        });
    }

    private <O extends ObjectType> String generateNonce(NonceCredentialsPolicyType noncePolicy, Task task,
                                                        PrismObject<O> user, OperationResult result)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        ValuePolicyType policy = null;

        if (noncePolicy != null && noncePolicy.getValuePolicyRef() != null) {
            PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.loadObject(ValuePolicyType.class,
                    noncePolicy.getValuePolicyRef().getOid(), PageEmailNonce.this, task, result);
            policy = valuePolicy.asObjectable();
        }

        return getModelInteractionService().generateValue(policy, 24, false, user, "nonce generation", task, result);
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return createStringResource(submited ? "PageEmailNonce.checkYourMail" : "PageEmailNonce.identification").getString();
            }
        };
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return createStringResource(submited ? "PageForgotPassword.form.submited.message"
                        : "PageEmailNonce.specifyMailDescription").getString();
            }
        };
    }

    protected UserType searchUserPrivileged(ObjectQuery query) {
        return runPrivileged((Producer<UserType>) () -> {

            Task task = createAnonymousTask("load user");
            OperationResult result = new OperationResult("search user");

            SearchResultList<PrismObject<UserType>> users;
            try {
                users = getModelService().searchObjects(UserType.class, query, null, task, result);
            } catch (SchemaException | ObjectNotFoundException | SecurityViolationException
                    | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                LoggingUtils.logException(LOGGER, "failed to search user", e);
                return null;
            }

            if ((users == null) || (users.isEmpty())) {
                LOGGER.trace("Empty user list while user authentication");
                return null;
            }

            if (users.size() > 1) {
                LOGGER.trace("Problem while seeking for user");
                return null;
            }

            UserType user = users.iterator().next().asObjectable();
            LOGGER.trace("User found for authentication: {}", user);

            return user;
        });
    }


}

