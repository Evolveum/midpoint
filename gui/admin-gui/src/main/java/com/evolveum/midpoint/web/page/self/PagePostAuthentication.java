/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.self;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
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
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.login.PageAbstractFlow;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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

    public PagePostAuthentication(PageParameters pageParameters) {
        super(pageParameters);
    }

    @Override
    public void initalizeModel() {
        userModel = new LoadableModel<UserType>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected UserType load() {
                //TODO: fix this... part of this is executed in object wrapper facotry..
                // but the prism object in object wrapper was overriden with this loading..
                MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
                Task task = createSimpleTask("load self");
                PrismObject<UserType> user = WebModelServiceUtils.loadObject(UserType.class, principal.getOid(), PagePostAuthentication.this, task, task.getResult());
                try {
                    PrismObjectDefinition<UserType> userDef = getModelInteractionService().getEditObjectDefinition(user, null, task, task.getResult());
                    if (userDef != null) {
                        user.setDefinition(userDef);
                    }
                } catch (SchemaException | ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException
                        | CommunicationException | SecurityViolationException e) {
                    //TODO: nothing critical even by the error. for now just log it
                    LoggingUtils.logException(LOGGER, "Cannot apply edited obejct definition", e);
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
            getSession().error("Unexected error occured. Please contact system administrator.");
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
        Form<?> form = getMainForm();
        return createDynamicPanel(form, task);
    }

    @Override
    protected boolean isSideMenuVisible(boolean visibleIfLoggedIn) {
        return false;
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
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Error during saving user.", e);
            result.recordFatalError(getString("PagePostAuthentication.message.submitRegistration.fatalError"), e);
        }

        result.computeStatus();

        if (result.isAcceptable()) {
            runPrivileged(() -> {
                ObjectDelta<UserType> lifecycleDelta = getPrismContext().deltaFactory().object().createModificationDeleteProperty(UserType.class,
                        userModel.getObject().getOid(), UserType.F_LIFECYCLE_STATE,
                        getPostAuthenticationConfiguration().getRequiredLifecycleState());

//                try {
//                    if (getUserDelta().findItemDelta(SchemaConstants.PATH_PASSWORD_VALUE) != null) {
//                        PrismProperty<Boolean> forceChangeProperty = userModel.getObject().asPrismObject().findProperty(SchemaConstants.PATH_PASSWORD_FORCE_CHANGE);
//                        if (forceChangeProperty != null && !forceChangeProperty.isEmpty()) {
//                            lifecycleDelta.addModificationDeleteProperty(SchemaConstants.PATH_PASSWORD_FORCE_CHANGE, forceChangeProperty.getRealValue());
//                        }
//                    }
//                } catch (SchemaException e) {
//                    LoggingUtils.logException(LOGGER, "Cannot create delete delta for property: force change", e);
//                }
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
            MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
            try {
                getModelInteractionService().refreshPrincipal(principal.getOid(), principal.getFocus().getClass());
                navigateToNext(getMidpointApplication().getHomePage());
            } catch (CommonException e) {
                LOGGER.error("Error while refreshing user: ", e);
                target.add(PagePostAuthentication.this);
            }

        }

        target.add(getFeedbackPanel());

    }

    private ObjectDelta<UserType> getUserDelta() throws SchemaException {
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
    protected boolean isLogoLinkEnabled() {
        return false;
    }
}
