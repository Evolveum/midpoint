/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.login;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.context.NonceAuthenticationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.forgetpassword.PageForgotPassword;
import com.evolveum.midpoint.web.page.forgetpassword.ResetPolicyDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.ArrayList;
import java.util.List;

public abstract class PageAuthenticationBase extends AbstractPageLogin {

    private static final long serialVersionUID = 1L;
    private static final String DOT_CLASS = PageAuthenticationBase.class.getName() + ".";
    private static final String OPERATION_GET_SECURITY_POLICY = DOT_CLASS + "getSecurityPolicy";

    protected static final String OPERATION_LOAD_DYNAMIC_FORM = DOT_CLASS + "loadDynamicForm";

    private static final Trace LOGGER = TraceManager.getTrace(PageAuthenticationBase.class);

    protected static final String ID_DYNAMIC_LAYOUT = "dynamicLayout";
    protected static final String ID_DYNAMIC_FORM = "dynamicForm";

    @SpringBean(name = "nonceAuthenticationEvaluator")
    private AuthenticationEvaluator<NonceAuthenticationContext> authenticationEvaluator;

    private ResetPolicyDto resetPasswordPolicy;
    private SelfRegistrationDto selfRegistrationDto;
    private SelfRegistrationDto postAuthenticationDto;

    public PageAuthenticationBase() {
//        initSelfRegistrationConfiguration();
//        initResetCredentialsConfiguration();
    }

    private void initSelfRegistrationConfiguration() {

        SecurityPolicyType securityPolicy = resolveSecurityPolicy();

        this.selfRegistrationDto = new SelfRegistrationDto();
        try {
            this.selfRegistrationDto.initSelfRegistrationDto(securityPolicy);
        } catch (SchemaException e) {
            LOGGER.error("Failed to initialize self registration configuration.", e);
            getSession().error(
                    createStringResource("PageSelfRegistration.selfRegistration.configuration.init.failed")
                            .getString());
            throw new RestartResponseException(PageLogin.class);
        }

    }

    private void initPostAuthenticationConfiguration() {

        SecurityPolicyType securityPolicy = resolveSecurityPolicy();

        this.postAuthenticationDto = new SelfRegistrationDto();
        try {
            this.postAuthenticationDto.initPostAuthenticationDto(securityPolicy);
        } catch (SchemaException e) {
            LOGGER.error("Failed to initialize self registration configuration.", e);
            getSession().error(
                    createStringResource("PageSelfRegistration.selfRegistration.configuration.init.failed")
                            .getString());
            throw new RestartResponseException(PageLogin.class);
        }

    }

    private void initResetCredentialsConfiguration() {

        // TODO: cleanup, the same as in the PageRegistrationBase
        SecurityPolicyType securityPolicy = resolveSecurityPolicy();

        this.resetPasswordPolicy = new ResetPolicyDto();
        try {
            this.resetPasswordPolicy.initResetPolicyDto(securityPolicy);
        } catch (SchemaException e) {
            LOGGER.error("Failed to initialize self registration configuration.", e);
            getSession().error(
                    createStringResource("PageSelfRegistration.selfRegistration.configuration.init.failed")
                            .getString());
            throw new RestartResponseException(PageLogin.class);
        }

    }

    private SecurityPolicyType resolveSecurityPolicy() {
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(null);

        if (securityPolicy == null) {
            LOGGER.error("No security policy defined.");
            getSession()
                    .error(createStringResource("PageSelfRegistration.securityPolicy.notFound").getString());
            throw new RestartResponseException(PageLogin.class);
        }

        return securityPolicy;
    }

    protected SecurityPolicyType resolveSecurityPolicy(PrismObject<UserType> user) {
        SecurityPolicyType securityPolicy = runPrivileged(new Producer<SecurityPolicyType>() {
            private static final long serialVersionUID = 1L;

            @Override
            public SecurityPolicyType run() {

                Task task = createAnonymousTask(OPERATION_GET_SECURITY_POLICY);
                task.setChannel(SchemaConstants.CHANNEL_GUI_SELF_REGISTRATION_URI);
                OperationResult result = new OperationResult(OPERATION_GET_SECURITY_POLICY);

                try {
                    return getModelInteractionService().getSecurityPolicy(user, task, result);
                } catch (CommonException e) {
                    LOGGER.error("Could not retrieve security policy: {}", e.getMessage(), e);
                    return null;
                }

            }

        });

        return securityPolicy;
    }

    public SelfRegistrationDto getSelfRegistrationConfiguration() {

        if (selfRegistrationDto == null) {
            initSelfRegistrationConfiguration();
        }

        return selfRegistrationDto;

    }

    public ResetPolicyDto getResetPasswordPolicy() {
        if (resetPasswordPolicy == null) {
            initResetCredentialsConfiguration();
        }
        return resetPasswordPolicy;
    }

    public SelfRegistrationDto getPostAuthenticationConfiguration() {

        if (postAuthenticationDto == null) {
            initPostAuthenticationConfiguration();
        }

        return postAuthenticationDto;

    }

    public AuthenticationEvaluator<NonceAuthenticationContext> getAuthenticationEvaluator() {
        return authenticationEvaluator;
    }

    protected void initDynamicLayout(final org.apache.wicket.markup.html.form.Form<?> mainForm, PageBase parentPage) {
        WebMarkupContainer dynamicLayout = new WebMarkupContainer(ID_DYNAMIC_LAYOUT);
        dynamicLayout.setOutputMarkupId(true);
        mainForm.add(dynamicLayout);

        dynamicLayout.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isDynamicForm();
            }
        });

        DynamicFormPanel<UserType> searchAttributesForm = runPrivileged(
                () -> {
                    ObjectReferenceType formRef = getResetPasswordPolicy().getFormRef();
                    if (formRef == null) {
                        return null;
                    }
                    Task task = createAnonymousTask(OPERATION_LOAD_DYNAMIC_FORM);
                    return new DynamicFormPanel<UserType>(ID_DYNAMIC_FORM, UserType.COMPLEX_TYPE,
                            formRef.getOid(), mainForm, task, parentPage, true);
                });

        if (searchAttributesForm != null) {
            dynamicLayout.add(searchAttributesForm);
        }
    }

    protected boolean isDynamicForm() {
        return getResetPasswordPolicy().getFormRef() != null;
    }

    protected void cancelPerformed() {
        setResponsePage(getMidpointApplication().getHomePage());
    }

    protected AjaxButton createBackButton(String id){
        AjaxButton back = new AjaxButton(id) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed();
            }
        };
        return back;
    }

    protected UserType searchUser() {
        ObjectQuery query = null;

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

    protected abstract ObjectQuery createStaticFormQuery();

    protected UserType searchUserPrivileged(ObjectQuery query) {
        UserType userType = runPrivileged(new Producer<UserType>() {

            @Override
            public UserType run() {

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
                    LOGGER.trace("Empty user list in ForgetPassword");
                    return null;
                }

                if (users.size() > 1) {
                    LOGGER.trace("Problem while seeking for user");
                    return null;
                }

                UserType user = users.iterator().next().asObjectable();
                LOGGER.trace("User found for ForgetPassword: {}", user);

                return user;
            }

        });
        return userType;
    }

    protected ObjectQuery createDynamicFormQuery() {
        DynamicFormPanel<UserType> userDynamicPanel = getDynamicForm();
        List<ItemPath> filledItems = userDynamicPanel.getChangedItems();
        PrismObject<UserType> user;
        try {
            user = userDynamicPanel.getObject();
        } catch (SchemaException e1) {
            getSession().error(getString("pageForgetPassword.message.usernotfound"));
            throw new RestartResponseException(PageForgotPassword.class);
        }

        List<EqualFilter> filters = new ArrayList<>();
        QueryFactory queryFactory = getPrismContext().queryFactory();
        for (ItemPath path : filledItems) {
            PrismProperty<?> property = user.findProperty(path);
            EqualFilter filter = queryFactory.createEqual(path, property.getDefinition(), null);
            filter.setValue(property.getAnyValue().clone());
            filters.add(filter);
        }
        return queryFactory.createQuery(queryFactory.createAnd((List) filters));
    }

    protected abstract DynamicFormPanel<UserType> getDynamicForm();
}
