/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.impl.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class PageAuthenticationBase<MA extends ModuleAuthentication> extends PageAbstractAuthenticationModule<MA> {

    @Serial private static final long serialVersionUID = 1L;
    private static final String DOT_CLASS = PageAuthenticationBase.class.getName() + ".";
    protected static final String OPERATION_LOAD_DYNAMIC_FORM = DOT_CLASS + "loadDynamicForm";

    private static final Trace LOGGER = TraceManager.getTrace(PageAuthenticationBase.class);

    protected static final String ID_DYNAMIC_LAYOUT = "dynamicLayout";
    protected static final String ID_DYNAMIC_FORM = "dynamicForm";

    private ObjectReferenceType formRef;

    public PageAuthenticationBase() {
        super();
    }

    private void initFormRef() {
        SecurityPolicyType securityPolicy = resolveSecurityPolicy();

        if (securityPolicy.getCredentialsReset() != null) {
            this.formRef = securityPolicy.getCredentialsReset().getFormRef();
        }

    }

    @NotNull
    private SecurityPolicyType resolveSecurityPolicy() {
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(null);

        if (securityPolicy == null) {
            LOGGER.error("No security policy defined.");
            getSession()
                    .error(createStringResource("PageSelfRegistrationOld.securityPolicy.notFound").getString());
            throw new RestartResponseException(PageLogin.class);
        }

        return securityPolicy;
    }

    public ObjectReferenceType getFormRef() {
        if (formRef == null) {
            initFormRef();
        }
        return formRef;
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

    protected abstract ObjectQuery createStaticFormQuery();

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

    protected abstract DynamicFormPanel<UserType> getDynamicForm();

    //TODO
    protected String getUrlProcessingLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication mpAuthentication) {
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null
                    && getModuleTypeName().equals(moduleAuthentication.getModuleTypeName())){
                String prefix = moduleAuthentication.getPrefix();
                return AuthUtil.stripSlashes(prefix) + "/spring_security_login";
            }
        }

        String key = "web.security.flexAuth.unsupported.auth.type";
        error(getString(key));
        return "/midpoint/spring_security_login";
    }

    protected  abstract String getModuleTypeName();

}
