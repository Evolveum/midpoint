/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.self;

import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.impl.page.login.PageAbstractFlow;
import com.evolveum.midpoint.gui.impl.page.login.module.PageLogin;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

@PageDescriptor(urls = {@Url(mountUrl = "/self/postAuthentication", matchUrlForSecurity="/self/postAuthentication")},
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_POST_AUTHENTICATION_URL,
                        label = "PagePostAuthentication.auth.postAuthentication.label",
                        description = "PagePostAuthentication.auth.postAuthentication.description"),
        }
        )
public class PagePostAuthentication extends PageAbstractFlow {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PagePostAuthentication.class);

    private static final String DOT_CLASS = PagePostAuthentication.class.getName() + ".";

    private static final String OPERATION_LOAD_WRAPPER = DOT_CLASS + "loadWrapper";
    private static final String ID_WRAPPER_CONTENT = "wrapperContent";
    private static final String ID_MAIN_PANEL = "main";
    private static final String ID_PASSWORD_PANEL = "password";


    private IModel<UserType> userModel;
    private PrismObjectWrapper<UserType> objectWrapper;

    public PagePostAuthentication() {
        super(null);
    }

    public PagePostAuthentication(PageParameters pageParameters) {
        super(pageParameters);
    }

    @Override
    public void initializeModel() {
        userModel = new LoadableModel<>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected UserType load() {
                //TODO: fix this... part of this is executed in object wrapper facotry..
                // but the prism object in object wrapper was overridden with this loading..
                MidPointPrincipal principal = AuthUtil.getPrincipalUser();
                Task task = createSimpleTask("load self");
                PrismObject<UserType> user = WebModelServiceUtils.loadObject(UserType.class, principal.getOid(), PagePostAuthentication.this, task, task.getResult());
                try {
                    PrismObjectDefinition<UserType> userDef = getModelInteractionService().getEditObjectDefinition(user, null, task, task.getResult());
                    user.applyDefinition(userDef, true);
                } catch (SchemaException | ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException
                        | CommunicationException | SecurityViolationException e) {
                    //TODO: nothing critical even by the error. for now just log it
                    LoggingUtils.logException(LOGGER, "Cannot apply edited object definition", e);
                }

                return user.asObjectable();
            }
        };

    }

    @Override
    public IModel<UserType> getUserModel() {
        return userModel;
    }

    @Override
    public boolean isCustomFormDefined() {
        return getPostAuthenticationConfiguration().getFormRef() != null;
    }

    @Override
    protected WebMarkupContainer initStaticLayout() {
        Task task = createSimpleTask(OPERATION_LOAD_WRAPPER);
        OperationResult result = new OperationResult(OPERATION_LOAD_WRAPPER);
        PrismObjectWrapperFactory<UserType> factory = findObjectWrapperFactory(userModel.getObject().asPrismObject().getDefinition());

        WrapperContext context = new WrapperContext(task, result);
        try {
            objectWrapper = factory.createObjectWrapper(userModel.getObject().asPrismObject(), ItemStatus.NOT_CHANGED, context);
        } catch (SchemaException e) {
            result.recordFatalError(getString("PagePostAuthentication.message.couldntPerformPostAuth.fatalError"));
            showResult(result);
            throw new RestartResponseException(PageLogin.class);
        }

        WebMarkupContainer wrappers = new WebMarkupContainer(ID_WRAPPER_CONTENT);

        try {
            Panel main = initItemPanel(ID_MAIN_PANEL, UserType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(Model.of(objectWrapper), ItemPath.EMPTY_PATH), null);
            wrappers.add(main);

            Panel password = initItemPanel(ID_PASSWORD_PANEL, PasswordType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(Model.of(objectWrapper), ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD)), null);
            wrappers.add(password);

        } catch (SchemaException e) {
            LOGGER.error("Cannot create panel, {}", e.getMessage(), e);
            getSession().error("Unexpected error occurred. Please contact system administrator.");
            throw new RestartResponseException(PageLogin.class);
        }

        return wrappers;
    }

    private List<ItemPath> getVisibleContainers() {
        return Arrays.asList(ItemPath.EMPTY_PATH, SchemaConstants.PATH_PASSWORD);

    }

    @Override
    protected WebMarkupContainer initDynamicLayout() {
        Task task = createSimpleTask(OPERATION_LOAD_DYNAMIC_FORM);
        MidpointForm<?> form = getMainForm();
        return createDynamicPanel(form, task);
    }

    @Override
    protected void submitRegistration(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_SAVE_USER);
        ObjectDelta<UserType> userDelta = null;
        try {
            userDelta = getUserDelta();
            getPrismContext().adopt(userDelta);
            WebModelServiceUtils.save(userDelta, result, this);
            result.recordSuccessIfUnknown();
        } catch (CommonException e) {
            LoggingUtils.logException(LOGGER, "Error during saving user.", e);
            result.recordFatalError(getString("PagePostAuthentication.message.submitRegistration.fatalError"), e);
        }

        result.computeStatus();

        if (result.isAcceptable()) {
            runPrivileged(() -> {
                ObjectDelta<UserType> lifecycleDelta = getPrismContext().deltaFactory().object().createModificationDeleteProperty(UserType.class,
                        userModel.getObject().getOid(), UserType.F_LIFECYCLE_STATE,
                        getPostAuthenticationConfiguration().getRequiredLifecycleState());
                OperationResult opResult = new OperationResult(OPERATION_SAVE_USER);
                Task task = createAnonymousTask(OPERATION_SAVE_USER);
                WebModelServiceUtils.save(lifecycleDelta, opResult, task, PagePostAuthentication.this);
                opResult.recordSuccessIfUnknown();
                return opResult;
            });
        }

        result.computeStatus();
        showResult(result, true);
        if (!result.isAcceptable()) {
            target.add(PagePostAuthentication.this);
        } else {
            isSubmitted = true;
            MidPointPrincipal principal = AuthUtil.getPrincipalUser();
            try {
                getModelInteractionService().refreshPrincipal(principal.getOid(), principal.getFocus().getClass());
                setResponsePage(getMidpointApplication().getHomePage());
            } catch (CommonException e) {
                LOGGER.error("Error while refreshing user: ", e);
                target.add(PagePostAuthentication.this);
            }

        }

        target.add(getFeedbackPanel());

    }

    private ObjectDelta<UserType> getUserDelta() throws CommonException {
        if (!isCustomFormDefined()) {
            return objectWrapper.getObjectDelta();
        }

        return getDynamicFormPanel().getObjectDelta();

    }

    @Override
    protected boolean isBackButtonVisible() {
        return false;
    }

    @Override
    protected ObjectReferenceType getCustomFormRef() {
        return getPostAuthenticationConfiguration().getFormRef();
    }

    @Override
    protected SecurityPolicyType resolveSecurityPolicy(Task task, OperationResult result) throws CommonException {
        FocusType principal = getPrincipalFocus();
        return getModelInteractionService().getSecurityPolicy(
                principal == null ? null : principal.asPrismObject(), task, result);
    }

    @Override
    protected String getSubmitLabelKey() {
        return "PageBase.button.submit";
    }
}
