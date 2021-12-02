/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.page;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.util.exception.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.*;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessages;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.gui.api.DefaultGuiConfigurationCompiler;
import com.evolveum.midpoint.gui.api.SubscriptionType;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.factory.wrapper.ItemWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.page.login.PageLogin;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerValuePanel;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.QueryConverter;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.ClusterExecutionHelper;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.CheckedProducer;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AsyncWebProcessManager;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.breadcrumbs.BreadcrumbPageClass;
import com.evolveum.midpoint.web.component.breadcrumbs.BreadcrumbPageInstance;
import com.evolveum.midpoint.web.component.dialog.MainPopupDialog;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.NewWindowNotifyingBehavior;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorRegistry;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author lazyman
 * @author semancik
 */
public abstract class PageCommon extends WebPage implements ModelServiceLocator {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageCommon.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    protected static final String OPERATION_LOAD_VIEW_COLLECTION_REF = DOT_CLASS + "loadViewCollectionRef";

    private static final String ID_TITLE = "title";
    public static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_MAIN_POPUP = "mainPopup";
//    private static final String ID_MAIN_POPUP_BODY = "popupBody";
//    private static final String ID_SUBSCRIPTION_MESSAGE = "subscriptionMessage";
//    private static final String ID_FOOTER_CONTAINER = "footerContainer";
//    private static final String ID_COPYRIGHT_MESSAGE = "copyrightMessage";

    private static final String ID_BODY = "body";

    private static final Trace LOGGER = TraceManager.getTrace(PageCommon.class);

    // Strictly speaking following fields should be transient.
    // But making them transient is causing problems on some
    // JVM version or tomcat configurations (MID-3357).
    // It seems to be somehow related to session persistence.
    // But honestly I have no idea about the real cause.
    // Anyway, setting these fields to non-transient seems to
    // fix it. And surprisingly it does not affect the session
    // size.

    @SpringBean(name = "modelController")
    private ScriptingService scriptingService;
//
//    @SpringBean(name = "modelController")
//    private ModelService modelService;
//
//    @SpringBean(name = "modelInteractionService")
//    private ModelInteractionService modelInteractionService;
//
    @SpringBean(name = "dashboardService")
    private DashboardService dashboardService;
//
//    @SpringBean(name = "modelController")
//    private TaskService taskService;
//
    @SpringBean(name = "modelDiagController")
    private ModelDiagnosticService modelDiagnosticService;
//
//    @SpringBean(name = "taskManager")
//    private TaskManager taskManager;
//
//    @SpringBean(name = "auditController")
//    private ModelAuditService modelAuditService;
//
//    @SpringBean(name = "modelController")
//    private WorkflowService workflowService;
//
//    @SpringBean(name = "workflowManager")
//    private WorkflowManager workflowManager;
//
    @SpringBean(name = "midpointConfiguration")
    private MidpointConfiguration midpointConfiguration;

    @SpringBean(name = "reportManager")
    private ReportManager reportManager;

    @SpringBean(name = "resourceValidator")
    private ResourceValidator resourceValidator;
//
//    @SpringBean(name = "modelController")
//    private AccessCertificationService certificationService;
//
//    @SpringBean(name = "accessDecisionManager")
//    private SecurityEnforcer securityEnforcer;
//
    @SpringBean(name = "clock")
    private Clock clock;
//
//    @SpringBean
//    private SecurityContextManager securityContextManager;
//
    @SpringBean
    private MidpointFormValidatorRegistry formValidatorRegistry;

    @SpringBean(name = "modelObjectResolver")
    private ObjectResolver modelObjectResolver;
//
//    @SpringBean
//    private LocalizationService localizationService;
//
    @SpringBean
    private CacheDispatcher cacheDispatcher;

    @SpringBean
    private MidpointFunctions midpointFunctions;

    @SpringBean private GuiComponentRegistry registry;
    @SpringBean private DefaultGuiConfigurationCompiler guiConfigurationRegistry;

    @SpringBean private ClusterExecutionHelper clusterExecutionHelper;

    @SpringBean private AdminGuiConfigurationMergeManager adminGuiConfigurationMergeManager;

//    private List<Breadcrumb> breadcrumbs;

    // No need to store this in the session. Retrieval is cheap.
    private transient CompiledGuiProfile compiledGuiProfile;

    // No need for this to store in session. It is used only during single init and render.
    private transient Task pageTask;

    public PageCommon(PageParameters parameters) {
        super(parameters);

        LOGGER.debug("Initializing page {}", this.getClass());

        Injector.get().inject(this);
        Validate.notNull(getModelService(), "Model service was not injected.");
        Validate.notNull(getTaskManager(), "Task manager was not injected.");
        Validate.notNull(getReportManager(), "Report manager was not injected.");

        MidPointAuthWebSession.getSession().setClientCustomization();

        add(new NewWindowNotifyingBehavior());
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    public PageCommon() {
        this(null);
    }

    public MidPointApplication getMidpointApplication() {
        return (MidPointApplication) getApplication();
    }

    @Override
    public LocalizationService getLocalizationService() {
        return getMidpointApplication().getLocalizationService();
    }

    @Contract(pure = true)
    public PrismContext getPrismContext() {
        return getMidpointApplication().getPrismContext();
    }

    public SchemaService getSchemaService() {
        return getMidpointApplication().getSchemaService();
    }

    public GetOperationOptionsBuilder getOperationOptionsBuilder() {
        return getSchemaService().getOperationOptionsBuilder();
    }

    public QueryConverter getQueryConverter() {
        return getPrismContext().getQueryConverter();
    }

    public RelationRegistry getRelationRegistry() {
        return getMidpointApplication().getRelationRegistry();
    }

    public RepositoryService getRepositoryService() {
        return getMidpointApplication().getRepositoryService();
    }

    public ExpressionFactory getExpressionFactory() {
        return getMidpointApplication().getExpressionFactory();
    }

    public MatchingRuleRegistry getMatchingRuleRegistry() {
        return getMidpointApplication().getMatchingRuleRegistry();
    }

    public TaskManager getTaskManager() {
        return getMidpointApplication().getTaskManager();
    }

    public WorkflowService getWorkflowService() {
        return getMidpointApplication().getWorkflowService();
    }

    public WorkflowManager getWorkflowManager() {
        return getMidpointApplication().getWorkflowManager();
    }

    public ResourceValidator getResourceValidator() {
        return resourceValidator;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public ModelAuditService getModelAuditService() {
        return getMidpointApplication().getAuditService();
    }

    public AccessCertificationService getCertificationService() {
        return getMidpointApplication().getCertificationService();
    }

    @Override
    public ModelService getModelService() {
        return getMidpointApplication().getModel();
    }

    @Override
    public ObjectResolver getModelObjectResolver() {
        return modelObjectResolver;
    }

    public ScriptingService getScriptingService() {
        return scriptingService;
    }

    public TaskService getTaskService() {
        return getMidpointApplication().getTaskService();
    }

    @Override
    public SecurityEnforcer getSecurityEnforcer() {
        return getMidpointApplication().getSecurityEnforcer();
    }

    @Override
    public SecurityContextManager getSecurityContextManager() {
        return getMidpointApplication().getSecurityContextManager();
    }

    @Override
    public ModelInteractionService getModelInteractionService() {
        return getMidpointApplication().getModelInteractionService();
    }

    @Override
    public DashboardService getDashboardService() {
        return dashboardService;
    }

    public ModelDiagnosticService getModelDiagnosticService() {
        return modelDiagnosticService;
    }

    public GuiComponentRegistry getRegistry() {
        return registry;
    }

    public CacheDispatcher getCacheDispatcher() {
        return cacheDispatcher;
    }

    public AdminGuiConfigurationMergeManager getAdminGuiConfigurationMergeManager() {
        return adminGuiConfigurationMergeManager;
    }

    @Override
    public Task getPageTask() {
        if (pageTask == null) {
            pageTask = createSimpleTask(this.getClass().getName());
        }
        return pageTask;
    }

    public MidpointFormValidatorRegistry getFormValidatorRegistry() {
        return formValidatorRegistry;
    }

    public GuiProfiledPrincipal getPrincipal() {
        return SecurityUtils.getPrincipalUser();
    }

    public FocusType getPrincipalFocus() {
        MidPointPrincipal principal = getPrincipal();
        if (principal == null) {
            return null;
        }
        return principal.getFocus();
    }

    public static StringResourceModel createStringResourceStatic(Component component, Enum<?> e) {
        String resourceKey = createEnumResourceKey(e);
        return createStringResourceStatic(component, resourceKey);
    }

    public static String createEnumResourceKey(Enum<?> e) {
        return e.getDeclaringClass().getSimpleName() + "." + e.name();
    }

    public Task createAnonymousTask(String operation) {
        TaskManager manager = getTaskManager();
        Task task = manager.createTaskInstance(operation);

        task.setChannel(SchemaConstants.CHANNEL_USER_URI);

        return task;
    }

    public Task createSimpleTask(String operation) {
        return createSimpleTask(operation, null);
    }

    public Task createSimpleTask(String operation, String channel) {
        MidPointPrincipal user = SecurityUtils.getPrincipalUser();
        if (user == null) {
            throw new RestartResponseException(PageLogin.class);
        }
        return WebModelServiceUtils.createSimpleTask(operation, channel, user.getFocus().asPrismObject(), getTaskManager());
    }

    public MidpointConfiguration getMidpointConfiguration() {
        return midpointConfiguration;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        FeedbackMessages messages = getSession().getFeedbackMessages();
        for (FeedbackMessage message : messages) {
            getFeedbackMessages().add(message);
        }

        getSession().getFeedbackMessages().clear();
    }

    //TODO change according to new tempalte
    protected abstract IModel<String> getBodyCssClass();

    private void initLayout() {
        TransparentWebMarkupContainer body = new TransparentWebMarkupContainer(ID_BODY);
        body.add(AttributeAppender.append("class", getBodyCssClass()));
        add(body);

        Label title = new Label(ID_TITLE, createPageTitleModel());
        title.setRenderBodyOnly(true);
        add(title);

        WebMarkupContainer feedbackContainer = new WebMarkupContainer(ID_FEEDBACK_CONTAINER);
        feedbackContainer.setOutputMarkupId(true);
        feedbackContainer.setOutputMarkupPlaceholderTag(true);
        add(feedbackContainer);

        FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK);
        feedbackList.setOutputMarkupId(true);
        feedbackList.setOutputMarkupPlaceholderTag(true);
        feedbackContainer.add(feedbackList);
    }

//    public MainPopupDialog getMainPopup() {
//        return (MainPopupDialog) get(ID_MAIN_POPUP);
//    }

//    public String getMainPopupBodyId() {
//        return ModalDialog.CONTENT_ID;
//    }

    protected boolean isSideMenuVisible() {
        return SecurityUtils.getPrincipalUser() != null;
    }

//    protected void clearLessJsCache(AjaxRequestTarget target) {
//        try {
//            ArrayList<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
//            if (servers.size() > 1) {
//                LOGGER.info("Too many mbean servers, cache won't be cleared.");
//                for (MBeanServer server : servers) {
//                    LOGGER.info(server.getDefaultDomain());
//                }
//                return;
//            }
//            MBeanServer server = servers.get(0);
////            ObjectName objectName = ObjectName.getInstance(Wro4jConfig.WRO_MBEAN_NAME + ":type=WroConfiguration");
////            server.invoke(objectName, "reloadCache", new Object[] {}, new String[] {});
//            if (target != null) {
//                target.add(PageCommon.this);
//            }
//        } catch (Exception ex) {
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't clear less/js cache", ex);
//            error("Error occurred, reason: " + ex.getMessage());
//            if (target != null) {
//                target.add(getFeedbackPanel());
//            }
//        }
//    }

    public WebMarkupContainer getFeedbackPanel() {
        return (WebMarkupContainer) get(ID_FEEDBACK_CONTAINER);
    }

    public SessionStorage getSessionStorage() {
        MidPointAuthWebSession session = (MidPointAuthWebSession) getSession();
        return session.getSessionStorage();
    }

    protected abstract IModel<String> createPageTitleModel();

    public IModel<String> getPageTitleModel() {
        return (IModel<String>) get(ID_TITLE).getDefaultModel();
    }

    public String getString(String resourceKey, Object... objects) {
        return createStringResource(resourceKey, objects).getString();
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this).setModel(new Model<String>()).setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(String resourceKey, IModel model, Object... objects) {
        return new StringResourceModel(resourceKey, model).setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(PolyString polystringKey, Object... objects) {
        String resourceKey = null;
        if (polystringKey != null) {
            resourceKey = getLocalizationService().translate(polystringKey, WebComponentUtil.getCurrentLocale(), true);
        }
        return new StringResourceModel(resourceKey, this).setModel(new Model<String>()).setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(PolyStringType polystringKey, Object... objects) {
        String resourceKey = null;
        if (polystringKey != null) {
            resourceKey = getLocalizationService().translate(PolyString.toPolyString(polystringKey), WebComponentUtil.getCurrentLocale(), true);
        }
        return new StringResourceModel(resourceKey, this).setModel(new Model<String>()).setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(Enum<?> e) {
        String resourceKey = e.getDeclaringClass().getSimpleName() + "." + e.name();
        return createStringResource(resourceKey);
    }

    @NotNull
    public static StringResourceModel createStringResourceStatic(Component component, String resourceKey,
            Object... objects) {
        return new StringResourceModel(resourceKey, component).setModel(new Model<String>())
                .setDefaultValue(resourceKey).setParameters(objects);
    }

    public StringResourceModel createStringResourceDefault(String defaultKey, PolyStringType polystringKey, Object... objects) {
        if (polystringKey == null) {
            return createStringResource(defaultKey);
        } else {
            return createStringResource(polystringKey, objects);
        }
    }

    public OpResult showResult(OperationResult result, String errorMessageKey) {
        return showResult(result, errorMessageKey, true);
    }

    public OpResult showResult(OperationResult result, boolean showSuccess) {
        return showResult(result, null, showSuccess);
    }

    public OpResult showResult(OperationResult result) {
        return showResult(result, null, true);
    }

    public StringResourceModel createStringResource(Enum e, String prefix, String nullKey) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(prefix)) {
            sb.append(prefix).append('.');
        }

        if (e == null) {
            if (StringUtils.isNotEmpty(nullKey)) {
                sb.append(nullKey);
            } else {
                sb = new StringBuilder();
            }
        } else {
            sb.append(e.getDeclaringClass().getSimpleName()).append('.');
            sb.append(e.name());
        }

        return createStringResource(sb.toString());
    }

    public OpResult showResult(OperationResult result, String errorMessageKey, boolean showSuccess) {
        Validate.notNull(result, "Operation result must not be null.");
        Validate.notNull(result.getStatus(), "Operation result status must not be null.");

        OperationResult scriptResult = executeResultScriptHook(result);
        if (scriptResult == null) {
            return null;
        }

        result = scriptResult;

        OpResult opResult = OpResult.getOpResult(this, result);
        opResult.determineObjectsVisibility(this);
        switch (opResult.getStatus()) {
            case FATAL_ERROR:
            case PARTIAL_ERROR:
                getSession().error(opResult);

                break;
            case IN_PROGRESS:
            case NOT_APPLICABLE:
                getSession().info(opResult);
                break;
            case SUCCESS:
                if (!showSuccess) {
                    break;
                }
                getSession().success(opResult);

                break;
            case UNKNOWN:
            case WARNING:
            default:
                getSession().warn(opResult);

        }
        return opResult;
    }

    @NotNull
    @Override
    public CompiledGuiProfile getCompiledGuiProfile() {
        // TODO: may need to always go to ModelInteractionService to make sure the setting is up to date
        if (compiledGuiProfile == null) {
            Task task = createSimpleTask(PageCommon.DOT_CLASS + "getCompiledGuiProfile");
            try {
                compiledGuiProfile = getModelInteractionService().getCompiledGuiProfile(task, task.getResult());
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot retrieve compiled user profile", e);
                if (InternalsConfig.nonCriticalExceptionsAreFatal()) {
                    throw new SystemException("Cannot retrieve compiled user profile: " + e.getMessage(), e);
                } else {
                    // Just return empty admin GUI config, so the GUI can go on (and the problem may get fixed)
                    return new CompiledGuiProfile();
                }
            }
        }
        return compiledGuiProfile;
    }

    public <O extends ObjectType> boolean isAuthorized(ModelAuthorizationAction action, PrismObject<O> object) {
        try {
            return isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null, null)
                    || isAuthorized(action.getUrl(), null, object, null, null, null);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine authorization for {}", e, action);
            return true;            // it is only GUI thing
        }
    }

    public boolean isAuthorized(String operationUrl) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return isAuthorized(operationUrl, null, null, null, null, null);
    }

    public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl, AuthorizationPhaseType phase,
            PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        Task task = getPageTask();
        AuthorizationParameters<O, T> params = new AuthorizationParameters.Builder<O, T>()
                .oldObject(object)
                .delta(delta)
                .target(target)
                .build();
        boolean isAuthorized = getSecurityEnforcer().isAuthorized(operationUrl, phase, params, ownerResolver, task, task.getResult());
        if (!isAuthorized && (ModelAuthorizationAction.GET.getUrl().equals(operationUrl) || ModelAuthorizationAction.SEARCH.getUrl().equals(operationUrl))) {
            isAuthorized = getSecurityEnforcer().isAuthorized(ModelAuthorizationAction.READ.getUrl(), phase, params, ownerResolver, task, task.getResult());
        }
        return isAuthorized;
    }

    public <O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, AuthorizationPhaseType phase,
            PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        AuthorizationParameters<O, T> params = new AuthorizationParameters.Builder<O, T>()
                .oldObject(object)
                .delta(delta)
                .target(target)
                .build();
        getSecurityEnforcer().authorize(operationUrl, phase, params, ownerResolver, getPageTask(), result);
    }


    private OperationResult executeResultScriptHook(OperationResult result) {
        CompiledGuiProfile adminGuiConfiguration = getCompiledGuiProfile();
        if (adminGuiConfiguration.getFeedbackMessagesHook() == null) {
            return result;
        }

        FeedbackMessagesHookType hook = adminGuiConfiguration.getFeedbackMessagesHook();
        ExpressionType expressionType = hook.getOperationResultHook();
        if (expressionType == null) {
            return result;
        }

        String contextDesc = "operation result (" + result.getOperation() + ") script hook";

        Task task = getPageTask();
        OperationResult topResult = task.getResult();
        try {
            ExpressionFactory factory = getExpressionFactory();
            PrismPropertyDefinition<OperationResultType> outputDefinition = getPrismContext().definitionFactory().createPropertyDefinition(
                    ExpressionConstants.OUTPUT_ELEMENT_NAME, OperationResultType.COMPLEX_TYPE);
            Expression<PrismPropertyValue<OperationResultType>, PrismPropertyDefinition<OperationResultType>> expression = factory.makeExpression(expressionType, outputDefinition, MiscSchemaUtil.getExpressionProfile(), contextDesc, task, topResult);

            VariablesMap variables = new VariablesMap();

            OperationResultType resultType = result.createOperationResultType();

            variables.put(ExpressionConstants.VAR_INPUT, resultType, OperationResultType.class);

            ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, contextDesc, task);
            PrismValueDeltaSetTriple<PrismPropertyValue<OperationResultType>> outputTriple = expression.evaluate(context, topResult);
            if (outputTriple == null) {
                return null;
            }

            Collection<PrismPropertyValue<OperationResultType>> values = outputTriple.getNonNegativeValues();
            if (values.isEmpty()) {
                return null;
            }

            if (values.size() > 1) {
                throw new SchemaException("Expression " + contextDesc + " produced more than one value");
            }

            OperationResultType newResultType = values.iterator().next().getRealValue();
            if (newResultType == null) {
                return null;
            }

            return OperationResult.createOperationResult(newResultType);
        } catch (Throwable e) {
            topResult.recordFatalError(e);
            if (StringUtils.isEmpty(result.getMessage())) {
                topResult.setMessage("Couldn't process operation result script hook.");
            }
            topResult.addSubresult(result);
            LoggingUtils.logUnexpectedException(LOGGER, contextDesc, e);
            if (InternalsConfig.nonCriticalExceptionsAreFatal()) {
                throw new SystemException(e.getMessage(), e);
            } else {
                return topResult;
            }
        }
    }

    public String createComponentPath(String... components) {
        return StringUtils.join(components, ":");
    }

    public String createPropertyModelExpression(String... components) {
        return StringUtils.join(components, ".");
    }

    /**
     * It's here only because of some IDEs - it's not properly filtering
     * resources during maven build. "describe" variable is not replaced.
     *
     * @return "unknown" instead of "git describe" for current build.
     */
    @Deprecated
    public String getDescribe() {
        return getString("pageBase.unknownBuildNumber");
    }

    // returns to previous page via restart response exception
    public RestartResponseException getRestartResponseException(Class<? extends Page> defaultBackPageClass) {
        return new RestartResponseException(defaultBackPageClass);
    }

    public long getItemsPerPage(UserProfileStorage.TableId tableId) {
        return getItemsPerPage(tableId.name());
    }

    public long getItemsPerPage(String tableIdName) {
        UserProfileStorage userProfile = getSessionStorage().getUserProfile();
        return userProfile.getPagingSize(tableIdName);
    }

    public void navigateToNext(Class<? extends WebPage> page) {
        navigateToNext(page, null);
    }

    public void navigateToNext(Class<? extends WebPage> pageType, PageParameters params) {
        WebPage page = createWebPage(pageType, params);
        navigateToNext(page);
    }

    public void navigateToNext(WebPage page) {
        setResponsePage(page);
    }

    public WebPage createWebPage(Class<? extends WebPage> pageType, PageParameters params) {
        IPageFactory pFactory = Session.get().getPageFactory();
        WebPage page;
        if (params == null) {
            page = pFactory.newPage(pageType);
        } else {
            page = pFactory.newPage(pageType, params);
        }
        return page;
    }

    protected void setTimeZone() {
        String timeZone = null;
        GuiProfiledPrincipal principal = SecurityUtils.getPrincipalUser();
        if (principal != null && principal.getCompiledGuiProfile() != null) {
            timeZone = principal.getCompiledGuiProfile().getDefaultTimezone();
        }
        if (timeZone != null) {
            WebSession.get().getClientInfo().getProperties().
                    setTimeZone(TimeZone.getTimeZone(timeZone));
        }
    }

    public <T> T runPrivileged(Producer<T> producer) {
        return getSecurityContextManager().runPrivileged(producer);
    }

    public <T> T runAsChecked(CheckedProducer<T> producer, PrismObject<UserType> user) throws CommonException {
        return getSecurityContextManager().runAsChecked(producer, user);
    }

    @NotNull
    public PrismObject<UserType> getAdministratorPrivileged(OperationResult parentResult) throws CommonException {
        OperationResult result = parentResult.createSubresult(OPERATION_LOAD_USER);
        try {
            return getSecurityContextManager().runPrivilegedChecked(() -> {
                Task task = createAnonymousTask(OPERATION_LOAD_USER);
                return getModelService()
                        .getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), null, task, result);
            });
        } catch (Throwable t) {
            result.recordFatalError(createStringResource("PageBase.message.getAdministratorPrivileged.fatalError", t.getMessage()).getString(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public AsyncWebProcessManager getAsyncWebProcessManager() {
        return MidPointApplication.get().getAsyncWebProcessManager();
    }

    @Override
    public Locale getLocale() {
        return getSession().getLocale();
    }

    //REGISTRY

    @Override
    public <O extends ObjectType> PrismObjectWrapperFactory<O> findObjectWrapperFactory(PrismObjectDefinition<O> objectDef) {
        return registry.getObjectWrapperFactory(objectDef);
    }

    public <ID extends ItemDefinition, C extends Containerable> ItemWrapperFactory<?, ?, ?> findWrapperFactory(ID def, PrismContainerValue<C> parentValue) {
        return registry.findWrapperFactory(def, parentValue);
    }

    public <C extends Containerable> PrismContainerWrapperFactory<C> findContainerWrapperFactory(PrismContainerDefinition<C> def) {
        return registry.findContainerWrapperFactory(def);
    }

    public <IW extends ItemWrapper, VW extends PrismValueWrapper, PV extends PrismValue> VW createValueWrapper(IW parentWrapper, PV newValue, ValueStatus status, WrapperContext context) throws SchemaException {

        PrismContainerValue<?> parentValue = null;
        if (parentWrapper != null) {
            PrismContainerValueWrapper<?> parentValueWrapper = parentWrapper.getParent();
            if (parentValueWrapper != null) {
                parentValue = parentValueWrapper.getNewValue();
            }
        }
        ItemWrapperFactory<IW, VW, PV> factory = registry.findWrapperFactory(parentWrapper, parentValue);

        return factory.createValueWrapper(parentWrapper, newValue, status, context);
    }

    public <ID extends ItemDefinition, IW extends ItemWrapper> IW createItemWrapper(ID def, PrismContainerValueWrapper<?> parent, WrapperContext ctx) throws SchemaException {

        ItemWrapperFactory<IW, ?, ?> factory = registry.findWrapperFactory(def, parent.getNewValue());
        ctx.setShowEmpty(true);
        ctx.setCreateIfEmpty(true);
        return factory.createWrapper(parent, def, ctx);
    }

    public <I extends Item, IW extends ItemWrapper> IW createItemWrapper(I item, ItemStatus status, WrapperContext ctx) throws SchemaException {

        ItemWrapperFactory<IW, ?, ?> factory = registry.findWrapperFactory(item.getDefinition(), null);

        ctx.setCreateIfEmpty(true);
        return factory.createWrapper(null, item, status, ctx);
    }

    private Class<?> getWrapperPanel(QName typeName) {
        return registry.getPanelClass(typeName);
    }

    public <IW extends ItemWrapper> Panel initItemPanel(String panelId, QName typeName, IModel<IW> wrapperModel, ItemPanelSettings itemPanelSettings) throws SchemaException {
        Class<?> panelClass = getWrapperPanel(typeName);
        if (panelClass == null) {
            ErrorPanel errorPanel = new ErrorPanel(panelId, () -> "Cannot create panel for " + typeName);
            errorPanel.add(new VisibleBehaviour(() -> getApplication().usesDevelopmentConfig()));
            return errorPanel;
        }

        Constructor<?> constructor;
        try {
            constructor = panelClass.getConstructor(String.class, IModel.class, ItemPanelSettings.class);
            return (Panel) constructor.newInstance(panelId, wrapperModel, itemPanelSettings);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new SystemException("Cannot instantiate " + panelClass, e);
        }
    }

    public <C extends Containerable> Panel initContainerValuePanel(String id, IModel<PrismContainerValueWrapper<C>> model,
            ItemPanelSettings settings) {
        //TODO find from registry first
        return new PrismContainerValuePanel<>(id, model, settings) {
            @Override
            protected boolean isRemoveButtonVisible() {
                return false;
            }
        };
    }

    public Clock getClock() {
        return clock;
    }

    public ModelExecuteOptions executeOptions() {
        return ModelExecuteOptions.create(getPrismContext());
    }

    public Class<? extends Panel> findObjectPanel(String identifier) {
        return guiConfigurationRegistry.findPanel(identifier);
    }

    public SimpleCounter getCounterProvider(String identifier) {
        return guiConfigurationRegistry.findCounter(identifier);
    }

    public boolean isNativeRepo() {
        return getRepositoryService().isNative();
    }
}
