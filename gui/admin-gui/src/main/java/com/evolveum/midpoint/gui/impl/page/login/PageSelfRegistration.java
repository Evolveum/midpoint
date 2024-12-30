/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.component.password.PasswordPropertyPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@PageDescriptor(urls = { @Url(mountUrl = "/registration", matchUrlForSecurity = "/registration") },
        permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.MAIL_NONCE)
public class PageSelfRegistration extends PageAbstractFlow {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSelfRegistration.class);

    private static final String DOT_CLASS = PageSelfRegistration.class.getName() + ".";

    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_FIRST_NAME = "firstName";
    private static final String ID_LAST_NAME = "lastName";
    private static final String ID_EMAIL = "email";
    private static final String ID_PASSWORD = "password";
    private static final String ID_TOOLTIP = "tooltip";
    private static final String ID_COMPONENT_FEEDBACK = "componentFeedback";
    private static final String ID_STATIC_FORM = "staticForm";

    protected IModel<UserType> userModel;
    private UserType user;

    public PageSelfRegistration() {
        super(null);
    }

    public PageSelfRegistration(UserType userType) {
        super(null);
        this.user = userType;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        getBackButton().add(AttributeAppender.append(
                "aria-describedby", () -> isSubmitted ? getDescription().getMarkupId() : null));
    }

    @Override
    public void initializeModel() {
        userModel = new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected UserType load() {
                if (user != null) {
                    return user;
                }
                return instantiateUser();
            }
        };
    }

    protected UserType instantiateUser() {
        PrismObjectDefinition<UserType> userDef = getUserDefinition();
        PrismObject<UserType> user;
        try {
            user = userDef.instantiate();
        } catch (SchemaException e) {
            UserType userType = new UserType();
            user = userType.asPrismObject();

        }
        return user.asObjectable();
    }

    private PrismObjectDefinition<UserType> getUserDefinition() {
        return getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
    }

    @Override
    protected WebMarkupContainer initStaticLayout() {
        WebMarkupContainer staticRegistrationForm = new WebMarkupContainer(ID_STATIC_FORM);
        staticRegistrationForm.setOutputMarkupId(true);
        staticRegistrationForm.add(new VisibleBehaviour(() -> !isSubmitted));
        add(staticRegistrationForm);

        FeedbackPanel feedback = new FeedbackPanel(ID_COMPONENT_FEEDBACK,
                new ContainerFeedbackMessageFilter(PageSelfRegistration.this));
        feedback.setOutputMarkupId(true);
        staticRegistrationForm.add(feedback);

        TextPanel<String> firstName = new TextPanel<>(ID_FIRST_NAME,
                new PropertyModel<>(getUserModel(), UserType.F_GIVEN_NAME.getLocalPart() + ".orig") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void setObject(String object) {
                        getUserModel().getObject().setGivenName(new PolyStringType(object));
                    }
                });
        initInputProperties(feedback, firstName);
        staticRegistrationForm.add(firstName);
        firstName.getBaseFormComponent().add(
                AttributeAppender.append("aria-label", createStringResource("UserType.givenName")));

        TextPanel<String> lastName = new TextPanel<>(ID_LAST_NAME,
                new PropertyModel<>(getUserModel(), UserType.F_FAMILY_NAME.getLocalPart() + ".orig") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void setObject(String object) {
                        getUserModel().getObject().setFamilyName(new PolyStringType(object));
                    }

                });
        initInputProperties(feedback, lastName);
        staticRegistrationForm.add(lastName);
        lastName.getBaseFormComponent().add(
                AttributeAppender.append("aria-label", createStringResource("UserType.familyName")));

        TextPanel<String> email = new TextPanel<>(ID_EMAIL,
                new PropertyModel<>(getUserModel(), UserType.F_EMAIL_ADDRESS.getLocalPart()));
        initInputProperties(feedback, email);
        staticRegistrationForm.add(email);
        email.getBaseFormComponent().add(
                AttributeAppender.append("aria-label", createStringResource("UserType.emailAddress")));

        createPasswordPanel(staticRegistrationForm);
        return staticRegistrationForm;
    }

    private void initInputProperties(FeedbackPanel feedback, TextPanel<String> input) {
        input.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        input.getBaseFormComponent().setRequired(true);
        feedback.setFilter(new ContainerFeedbackMessageFilter(input.getBaseFormComponent()));
        input.setRenderBodyOnly(true);
    }

    private void createPasswordPanel(WebMarkupContainer staticRegistrationForm) {
        PasswordPanel password = new PasswordPropertyPanel(ID_PASSWORD,
                new PropertyModel<>(getUserModel(), "credentials.password.value"), false, true, null);
        password.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        password.getBaseFormComponent().add(
                AttributeAppender.append("aria-label", createStringResource("CredentialsType.password")));
        password.getBaseFormComponent().setRequired(true);
        staticRegistrationForm.add(password);

//        Label help = new Label(ID_TOOLTIP);
//        final StringResourceModel tooltipText = createStringResource("PageSelfRegistration.password.policy");
//        help.add(AttributeModifier.replace("title", tooltipText));
//        help.add(new InfoTooltipBehavior());
//        help.add(new VisibleEnableBehaviour() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isVisible() {
//
//                return StringUtils.isNotEmpty(tooltipText.getObject());
//            }
//        });
//        staticRegistrationForm.add(help);
    }

    @Override
    protected WebMarkupContainer initDynamicLayout() {
        DynamicFormPanel<UserType> dynamicForm = runPrivileged(
                () -> {
                    Task task = createAnonymousTask(OPERATION_LOAD_DYNAMIC_FORM);
                    return createDynamicPanel(getMainForm(), task);
                });

        return dynamicForm;
    }

    @Override
    public IModel<UserType> getUserModel() {
        return userModel;
    }

    @Override
    public boolean isCustomFormDefined() {
        return getSelfRegistrationConfiguration().getFormRef() != null;
    }

    @Override
    protected void submitRegistration(AjaxRequestTarget target) {

        OperationResult result = new OperationResult(OPERATION_SAVE_USER);
        saveUser(result);
        result.computeStatus();

        if (result.getStatus() == OperationResultStatus.SUCCESS || result.getStatus() == OperationResultStatus.HANDLED_ERROR) {
            getSession()
                    .success(createStringResource("PageSelfRegistration.registration.success").getString());
            afterUserRegistration(target);
        } else if (result.getStatus() == OperationResultStatus.IN_PROGRESS) {
            getSession()
                    .info(createStringResource("PageSelfRegistration.registration.inprogress").getString());
            afterUserRegistration(target);
        } else if (result.getStatus() == OperationResultStatus.WARNING) {
            getSession()
                    .warn(createStringResource("PageSelfRegistration.registration.success").getString());
            afterUserRegistration(target);
        } else {
            String message;
            if (result.getUserFriendlyMessage() != null) {
                message = WebModelServiceUtils.translateMessage(result, this);
            } else {
                message = result.getMessage();
            }
            getSession().error(
                    createStringResource("PageSelfRegistration.registration.error", message)
                            .getString());
            target.add(getFeedbackPanel());
            LOGGER.error("Failed to register user {}. Reason {}", getUserModel().getObject(), result.getMessage());
            return;
        }
        target.add(getFeedbackPanel());
        target.add(PageSelfRegistration.this);
    }

    private void afterUserRegistration(AjaxRequestTarget target) {
        String sequenceIdentifier = getSelfRegistrationConfiguration().getAdditionalAuthentication();
        if (SecurityUtils.sequenceExists(getSelfRegistrationConfiguration().getAuthenticationPolicy(), sequenceIdentifier)) {
            target.add(PageSelfRegistration.this);
        }
        LOGGER.trace("Registration for user {} was successfull.", getUserModel().getObject());
        isSubmitted = true;
    }

    private void saveUser(OperationResult result) {
        try {
            PrismObject<UserType> administrator = getAdministratorPrivileged(result);

            runAsChecked(
                    (lResult) -> {
                        ObjectDelta<UserType> userDelta;
                        Task task = createSimpleTask(OPERATION_SAVE_USER, null);
                        task.setChannel(SchemaConstants.CHANNEL_SELF_REGISTRATION_URI);
                        try {
                            userDelta = prepareUserDelta(task, lResult);
                            userDelta.setPrismContext(getPrismContext());
                        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
                            lResult.recordFatalError(getString("PageSelfRegistration.message.createDelta.fatalError", e.getMessage()), e);
                            return null;
                        }
                        WebModelServiceUtils.save(userDelta, executeOptions().overwrite(), lResult, task, PageSelfRegistration.this);
                        return null;
                    },
                    administrator, result);
        } catch (CommonException | RuntimeException e) {
            result.recordFatalError(getString("PageSelfRegistration.message.saveUser.fatalError"), e);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    protected ObjectDelta<UserType> prepareUserDelta(Task task, OperationResult result) throws CommonException {
        LOGGER.trace("Preparing user ADD delta (new user registration)");
        UserType userType = prepareUserToSave(task, result);
        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(userType.asPrismObject());
        LOGGER.trace("Going to register user {}", userDelta);
        return userDelta;
    }

    private UserType prepareUserToSave(Task task, OperationResult result) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        SelfRegistrationDto selfRegistrationConfiguration = getSelfRegistrationConfiguration();
        UserType userType = getUserModel().getObject();
        UserType userToSave = userType.clone();

        if (!isCustomFormDefined()) {
            applyPassword(userToSave);
            if (selfRegistrationConfiguration.getRequiredLifecycleState() != null) {
                String userLifecycle = userToSave.getLifecycleState();
                if (!selfRegistrationConfiguration.getRequiredLifecycleState().equals(userLifecycle)) {
                    LOGGER.error(
                            "Registration not allowed for a user {} -> Unsatisfied Configuration for required lifecycle, expected {} but was {}",
                            userToSave.getEmailAddress() != null
                                    ? userToSave.getEmailAddress()
                                    : userToSave,
                            selfRegistrationConfiguration.getRequiredLifecycleState(),
                            userLifecycle);
                    getSession().error(createStringResource(
                            "PageSelfRegistration.registration.failed.unsatisfied.registration.configuration")
                            .getString());
                    throw new RestartResponseException(this);
                }

            }
        } else {

            try {
                userToSave = getDynamicFormPanel().getObject().asObjectable().clone();
            } catch (CommonException e) {
                LoggingUtils.logException(LOGGER, "Failed to construct delta " + e.getMessage(), e);
                throw new RestartResponseException(this);
            }
        }

        // CredentialsType credentials =
        applyNonce(userToSave, selfRegistrationConfiguration.getNoncePolicy(), task, result);
        // userToSave.setCredentials(credentials);
        if (selfRegistrationConfiguration.getInitialLifecycleState() != null) {
            LOGGER.trace("Setting initial lifecycle state of registered user to {}",
                    selfRegistrationConfiguration.getInitialLifecycleState());
            userToSave.setLifecycleState(selfRegistrationConfiguration.getInitialLifecycleState());
        }

        try {
            getPrismContext().adopt(userToSave);
        } catch (SchemaException e) {
            // nothing to do, try without it
        }

        return userToSave;

    }

    protected NonceType createNonce(NonceCredentialsPolicyType noncePolicy, Task task, OperationResult result) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        ProtectedStringType nonceCredentials = new ProtectedStringType();
        nonceCredentials.setClearValue(generateNonce(noncePolicy, task, result));

        NonceType nonceType = new NonceType();
        nonceType.setValue(nonceCredentials);

        return nonceType;
    }

    private void applyPassword(UserType user) {
        getCredentials(user).setPassword(createPassword());
    }

    private void applyNonce(UserType user, NonceCredentialsPolicyType noncePolicy, Task task, OperationResult result) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        getCredentials(user).setNonce(createNonce(noncePolicy, task, result));
    }

    private CredentialsType getCredentials(UserType user) {
        CredentialsType credentials = user.getCredentials();
        if (user.getCredentials() == null) {
            credentials = new CredentialsType();
            user.setCredentials(credentials);
        }

        return credentials;
    }

    protected PasswordType createPassword() {
        PasswordType password = new PasswordType();
        ProtectedStringType protectedString = new ProtectedStringType();
        protectedString.setClearValue(getPassword());
        password.setValue(protectedString);
        return password;
    }

    private <O extends ObjectType> String generateNonce(NonceCredentialsPolicyType noncePolicy,
            Task task, OperationResult result)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        ValuePolicyType policy = null;

        if (noncePolicy != null && noncePolicy.getValuePolicyRef() != null) {
            PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.loadObject(ValuePolicyType.class,
                    noncePolicy.getValuePolicyRef().getOid(), PageSelfRegistration.this, task, result);
            if (valuePolicy == null) {
                LOGGER.error("Nonce cannot be generated, as value policy {} cannot be fetched", noncePolicy.getValuePolicyRef().getOid());
                throw new ObjectNotFoundException("Nonce cannot be generated"); // no more information (security); TODO implement more correctly
            }
            policy = valuePolicy.asObjectable();
        }

        return getModelInteractionService().generateValue(policy,
                24, false, (PrismObject<O>) null, "nonce generation (registration)", task, result);
    }

    private String getPassword() {
        PasswordPanel password = (PasswordPanel)
                get(createComponentPath(ID_MAIN_FORM, ID_CONTENT_AREA, ID_STATIC_FORM, ID_PASSWORD));
        return (String) password.getBaseFormComponent().getModel().getObject();
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    @Override
    protected ObjectReferenceType getCustomFormRef() {
        return getSelfRegistrationConfiguration().getFormRef();
    }

    @Override
    public Task createSimpleTask(String operation) {
        return createAnonymousTask(operation);
    }

    @Override
    protected IModel<String> getTitleModel() {
        return createStringResource("PageSelfRegistration.welcome.message");
    }

    protected IModel<String> getDescriptionModel() {
        return new LoadableModel<String>() {
            @Override
            protected String load() {
                return isSubmitted ? createStringResource("PageSelfRegistration.registration.confirm.message").getString() :
                        createStringResource("PageSelfRegistration.additional.message",
                                WebComponentUtil.getMidpointCustomSystemName(PageSelfRegistration.this, "MidPoint")).getString();

            }
        };
    }
}
