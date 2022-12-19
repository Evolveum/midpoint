/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.page;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessages;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.ExternalImage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.LegacyValidator;
import com.evolveum.midpoint.gui.api.DefaultGuiConfigurationCompiler;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.SubscriptionType;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.factory.wrapper.ItemWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.registry.DataProviderRegistry;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.menu.LeftMenuPanel;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerValuePanel;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
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
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.ClusterExecutionHelper;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.CheckedProducer;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AsyncWebProcessManager;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.web.boot.Wro4jConfig;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.breadcrumbs.BreadcrumbPageClass;
import com.evolveum.midpoint.web.component.breadcrumbs.BreadcrumbPageInstance;
import com.evolveum.midpoint.web.component.dialog.MainPopupDialog;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.menu.BaseMenuItem;
import com.evolveum.midpoint.web.component.menu.SideBarMenuItem;
import com.evolveum.midpoint.web.component.menu.UserMenuPanel;
import com.evolveum.midpoint.web.component.menu.top.LocalePanel;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.page.self.PageAssignmentsList;
import com.evolveum.midpoint.web.page.self.PageSelf;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
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
public abstract class PageBase extends WebPage implements ModelServiceLocator {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageBase.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";

    private static final String ID_TITLE = "title";
    private static final String ID_MAIN_HEADER = "mainHeader";
    private static final String ID_PAGE_TITLE_CONTAINER = "pageTitleContainer";
    private static final String ID_PAGE_TITLE_REAL = "pageTitleReal";
    private static final String ID_PAGE_TITLE = "pageTitle";
    private static final String ID_DEBUG_PANEL = "debugPanel";
    private static final String ID_VERSION = "version";
    public static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_DEBUG_BAR = "debugBar";
    private static final String ID_CLEAR_CACHE = "clearCssCache";
    private static final String ID_DUMP_PAGE_TREE = "dumpPageTree";
    private static final String ID_CART_BUTTON = "cartButton";
    private static final String ID_CART_ITEMS_COUNT = "itemsCount";
    private static final String ID_SIDEBAR_MENU = "sidebarMenu";
    private static final String ID_RIGHT_MENU = "rightMenu";
    private static final String ID_LOCALE = "locale";
    private static final String ID_MENU_TOGGLE = "menuToggle";
    private static final String ID_BREADCRUMB = "breadcrumb";
    private static final String ID_BC_LINK = "bcLink";
    private static final String ID_BC_ICON = "bcIcon";
    private static final String ID_BC_NAME = "bcName";
    private static final String ID_MAIN_POPUP = "mainPopup";
    private static final String ID_MAIN_POPUP_BODY = "popupBody";
    private static final String ID_SUBSCRIPTION_MESSAGE = "subscriptionMessage";
    private static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_COPYRIGHT_MESSAGE = "copyrightMessage";
    private static final String ID_LOGO = "logo";
    private static final String ID_CUSTOM_LOGO = "customLogo";
    private static final String ID_CUSTOM_LOGO_IMG_SRC = "customLogoImgSrc";
    private static final String ID_CUSTOM_LOGO_IMG_CSS = "customLogoImgCss";
    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_DEPLOYMENT_NAME = "deploymentName";
    private static final String ID_BODY = "body";

    private static final int DEFAULT_BREADCRUMB_STEP = 2;
    public static final String PARAMETER_OBJECT_COLLECTION_TYPE_OID = "collectionOid";
    public static final String PARAMETER_OBJECT_COLLECTION_NAME = "collectionName";
    public static final String PARAMETER_DASHBOARD_TYPE_OID = "dashboardOid";
    public static final String PARAMETER_DASHBOARD_WIDGET_NAME = "dashboardWidgetName";
    public static final String PARAMETER_SEARCH_BY_NAME = "name";

    private static final String CLASS_DEFAULT_SKIN = "skin-blue-light";

    private static final String OPERATION_GET_SYSTEM_CONFIG = DOT_CLASS + "getSystemConfiguration";
    private static final String OPERATION_GET_DEPLOYMENT_INFORMATION = DOT_CLASS + "getDeploymentInformation";

    private static final Trace LOGGER = TraceManager.getTrace(PageBase.class);

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

    @SpringBean(name = "modelController")
    private ModelService modelService;

    // Temporary
    @SpringBean(name = "lightweightIdentifierGeneratorImpl")
    private LightweightIdentifierGenerator lightweightIdentifierGenerator;

    @SpringBean(name = "modelInteractionService")
    private ModelInteractionService modelInteractionService;

    @SpringBean(name = "dashboardService")
    private DashboardService dashboardService;

    @SpringBean(name = "modelController")
    private TaskService taskService;

    @SpringBean(name = "modelDiagController")
    private ModelDiagnosticService modelDiagnosticService;

    @SpringBean(name = "taskManager")
    private TaskManager taskManager;

    @SpringBean(name = "auditController")
    private ModelAuditService modelAuditService;

    @SpringBean(name = "modelController")
    private WorkflowService workflowService;

    @SpringBean(name = "workflowManager")
    private WorkflowManager workflowManager;

    @SpringBean(name = "midpointConfiguration")
    private MidpointConfiguration midpointConfiguration;

    @SpringBean(name = "reportManager")
    private ReportManager reportManager;

    @SpringBean(name = "resourceValidator")
    private ResourceValidator resourceValidator;

    @SpringBean(name = "modelController")
    private AccessCertificationService certificationService;

    @SpringBean(name = "accessDecisionManager")
    private SecurityEnforcer securityEnforcer;

    @SpringBean(name = "clock")
    private Clock clock;

    @SpringBean
    private SecurityContextManager securityContextManager;

    @SpringBean
    private MidpointFormValidatorRegistry formValidatorRegistry;

    @SpringBean(name = "modelObjectResolver")
    private ObjectResolver modelObjectResolver;

    @SpringBean
    private LocalizationService localizationService;

    @SpringBean
    private CacheDispatcher cacheDispatcher;

    @SpringBean
    private MidpointFunctions midpointFunctions;

    @SpringBean private GuiComponentRegistry registry;
    @SpringBean private DataProviderRegistry dataProviderRegistry;

    @SpringBean private DefaultGuiConfigurationCompiler guiConfigurationRegistry;

    @SpringBean private ClusterExecutionHelper clusterExecutionHelper;

    @SpringBean private AdminGuiConfigurationMergeManager adminGuiConfigurationMergeManager;

    private List<Breadcrumb> breadcrumbs;

    private boolean initialized = false;

    // No need to store this in the session. Retrieval is cheap.
    private transient CompiledGuiProfile compiledGuiProfile;

    // No need for this to store in session. It is used only during single init and render.
    private transient Task pageTask;

    public PageBase(PageParameters parameters) {
        super(parameters);

        LOGGER.debug("Initializing page {}", this.getClass());

        Injector.get().inject(this);
        Validate.notNull(modelService, "Model service was not injected.");
        Validate.notNull(taskManager, "Task manager was not injected.");
        Validate.notNull(reportManager, "Report manager was not injected.");

        MidPointAuthWebSession.getSession().setClientCustomization();

        add(new NewWindowNotifyingBehavior());
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        if (initialized) {
            return;
        }
        initialized = true;

        createBreadcrumb();
    }

    protected void createBreadcrumb() {
        PageParameters pageParameters = getPageParameters();
        clearPageParametersForBreadcrumbIfNeeded(pageParameters);
        BreadcrumbPageClass bc = new BreadcrumbPageClass(new IModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getPageTitleModel().getObject();
            }
        }, this.getClass(), getPageParameters());

        addBreadcrumb(bc);
    }

    private void clearPageParametersForBreadcrumbIfNeeded(PageParameters parameters) {
        pageParametersToBeRemovedFromBreadcrumb().forEach(parameters::remove);
    }

    protected List<String> pageParametersToBeRemovedFromBreadcrumb() {
        return new ArrayList<>();
    }

    protected void createInstanceBreadcrumb() {
        BreadcrumbPageInstance bc = new BreadcrumbPageInstance(new IModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getPageTitleModel().getObject();
            }
        }, this);

        addBreadcrumb(bc);
    }

    public void updateBreadcrumbParameters(String key, Object value) {
        List<Breadcrumb> list = getBreadcrumbs();
        if (list.isEmpty()) {
            return;
        }

        Breadcrumb bc = list.get(list.size() - 1);
        PageParameters params = bc.getParameters();
        if (params == null) {
            return;
        }

        params.set(key, value);
    }

    public PageBase() {
        this(null);
    }

    public MidPointApplication getMidpointApplication() {
        return (MidPointApplication) getApplication();
    }

    public WebApplicationConfiguration getWebApplicationConfiguration() {
        MidPointApplication application = getMidpointApplication();
        return application.getWebApplicationConfiguration();
    }

    @Override
    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    @Override
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

    @Override
    public ExpressionFactory getExpressionFactory() {
        return getMidpointApplication().getExpressionFactory();
    }

    public MatchingRuleRegistry getMatchingRuleRegistry() {
        return getMidpointApplication().getMatchingRuleRegistry();
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public WorkflowService getWorkflowService() {
        return workflowService;
    }

    public WorkflowManager getWorkflowManager() {
        return workflowManager;
    }

    public ResourceValidator getResourceValidator() {
        return resourceValidator;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public ModelAuditService getModelAuditService() {
        return modelAuditService;
    }

    public AccessCertificationService getCertificationService() {
        return certificationService;
    }

    @Override
    public ModelService getModelService() {
        return modelService;
    }

    public LightweightIdentifierGenerator getLightweightIdentifierGenerator() {
        return lightweightIdentifierGenerator;
    }

    @Override
    public ObjectResolver getModelObjectResolver() {
        return modelObjectResolver;
    }

    public ScriptingService getScriptingService() {
        return scriptingService;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    @Override
    public SecurityEnforcer getSecurityEnforcer() {
        return securityEnforcer;
    }

    @Override
    public SecurityContextManager getSecurityContextManager() {
        return securityContextManager;
    }

    @Override
    public ModelInteractionService getModelInteractionService() {
        return modelInteractionService;
    }

    @Override
    public DashboardService getDashboardService() {
        return dashboardService;
    }

    public ModelDiagnosticService getModelDiagnosticService() {
        return modelDiagnosticService;
    }

    @Override
    public GuiComponentRegistry getRegistry() {
        return registry;
    }

    public DataProviderRegistry getDataProviderRegistry() {
        return dataProviderRegistry;
    }

    public CacheDispatcher getCacheDispatcher() {
        return cacheDispatcher;
    }

    @Override
    public AdminGuiConfigurationMergeManager getAdminGuiConfigurationMergeManager() {
        return adminGuiConfigurationMergeManager;
    }

    @NotNull
    @Override
    public CompiledGuiProfile getCompiledGuiProfile() {
        // TODO: may need to always go to ModelInteractionService to make sure the setting is up to date
        if (compiledGuiProfile == null) {
            Task task = createSimpleTask(PageBase.DOT_CLASS + "getCompiledGuiProfile");
            try {
                compiledGuiProfile = modelInteractionService.getCompiledGuiProfile(task, task.getResult());
            } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
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

    @Override
    public Task getPageTask() {
        if (pageTask == null) {
            pageTask = createSimpleTask(this.getClass().getName());
        }
        return pageTask;
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

    @Override
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

    public boolean hasSubjectRoleRelation(String oid, List<QName> subjectRelations) {
        FocusType focusType = getPrincipalFocus();
        if (focusType == null) {
            return false;
        }

        if (oid == null) {
            return false;
        }

        for (ObjectReferenceType roleMembershipRef : focusType.getRoleMembershipRef()) {
            if (oid.equals(roleMembershipRef.getOid()) &&
                    getPrismContext().relationMatches(subjectRelations, roleMembershipRef.getRelation())) {
                return true;
            }
        }
        return false;
    }

    public static StringResourceModel createStringResourceStatic(Component component, Enum<?> e) {
        String resourceKey = createEnumResourceKey(e);
        return createStringResourceStatic(resourceKey);
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

    @Override
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

    private void initHeaderLayout(WebMarkupContainer container) {
        WebMarkupContainer menuToggle = new WebMarkupContainer(ID_MENU_TOGGLE);
        menuToggle.add(createUserStatusBehaviour());
        container.add(menuToggle);

        UserMenuPanel rightMenu = new UserMenuPanel(ID_RIGHT_MENU);
        rightMenu.add(createUserStatusBehaviour());
        container.add(rightMenu);

        LocalePanel locale = new LocalePanel(ID_LOCALE);
        container.add(locale);
    }

    private void initTitleLayout(WebMarkupContainer mainHeader) {
        WebMarkupContainer pageTitleContainer = new WebMarkupContainer(ID_PAGE_TITLE_CONTAINER);
        pageTitleContainer.add(createUserStatusBehaviour());
        pageTitleContainer.setOutputMarkupId(true);
        mainHeader.add(pageTitleContainer);

        WebMarkupContainer pageTitle = new WebMarkupContainer(ID_PAGE_TITLE);
        pageTitleContainer.add(pageTitle);

        IModel<String> deploymentNameModel = new IModel<>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
                if (info == null) {
                    return "";
                }

                return StringUtils.isEmpty(info.getName()) ? "" : info.getName() + ": ";
            }
        };

        Label deploymentName = new Label(ID_DEPLOYMENT_NAME, deploymentNameModel);
        deploymentName.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(deploymentNameModel.getObject())));
        deploymentName.setRenderBodyOnly(true);
        pageTitle.add(deploymentName);

        Label pageTitleReal = new Label(ID_PAGE_TITLE_REAL, createPageTitleModel());
        pageTitleReal.setRenderBodyOnly(true);
        pageTitle.add(pageTitleReal);

        IModel<List<Breadcrumb>> breadcrumbsModel = () -> getBreadcrumbs();

        ListView<Breadcrumb> breadcrumbs = new ListView<>(ID_BREADCRUMB, breadcrumbsModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Breadcrumb> item) {
//                final Breadcrumb dto = item.getModelObject();

                AjaxLink<String> bcLink = new AjaxLink<>(ID_BC_LINK) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        redirectBackToBreadcrumb(item.getModelObject());
                    }
                };
                item.add(bcLink);
                bcLink.add(new VisibleEnableBehaviour() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isEnabled() {
                        return item.getModelObject().isUseLink();
                    }
                });

                WebMarkupContainer bcIcon = new WebMarkupContainer(ID_BC_ICON);
                bcIcon.add(new VisibleEnableBehaviour() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        return item.getModelObject().getIcon() != null && item.getModelObject().getIcon().getObject() != null;
                    }
                });
                bcIcon.add(AttributeModifier.replace("class", item.getModelObject().getIcon()));
                bcLink.add(bcIcon);

                Label bcName = new Label(ID_BC_NAME, item.getModelObject().getLabel());
                bcLink.add(bcName);

                item.add(new VisibleEnableBehaviour() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        return item.getModelObject().isVisible();
                    }
                });
            }
        };
        breadcrumbs.add(new VisibleBehaviour(() -> !isErrorPage()));
        mainHeader.add(breadcrumbs);

        initCartButton(mainHeader);
    }

    private void initCartButton(WebMarkupContainer mainHeader) {
        AjaxButton cartButton = new AjaxButton(ID_CART_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                navigateToNext(new PageAssignmentsList<>(true));
            }
        };
        cartButton.setOutputMarkupId(true);
        cartButton.add(getShoppingCartVisibleBehavior());
        mainHeader.add(cartButton);

        Label cartItemsCount = new Label(ID_CART_ITEMS_COUNT, new LoadableModel<String>(true) {
            private static final long serialVersionUID = 1L;

            @Override
            public String load() {
                return Integer.toString(getSessionStorage().getRoleCatalog().getAssignmentShoppingCart().size());
            }
        });
        cartItemsCount.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !(getSessionStorage().getRoleCatalog().getAssignmentShoppingCart().size() == 0);
            }
        });
        cartItemsCount.setOutputMarkupId(true);
        cartButton.add(cartItemsCount);
    }

    private void initLayout() {
        TransparentWebMarkupContainer body = new TransparentWebMarkupContainer(ID_BODY);
//        body.add(new AttributeAppender("class", "hold-transition ", " "));
//        body.add(new AttributeAppender("class", "custom-hold-transition ", " "));

        body.add(AttributeAppender.append("class", new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
                if (info == null || StringUtils.isEmpty(info.getSkin())) {
                    return CLASS_DEFAULT_SKIN;
                }

                return info.getSkin();
            }
        }));
        add(body);

        WebMarkupContainer mainHeader = new WebMarkupContainer(ID_MAIN_HEADER);
        mainHeader.setOutputMarkupId(true);
        add(mainHeader);

        AjaxLink<String> logo = new AjaxLink<>(ID_LOGO) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Class<? extends Page> page = MidPointApplication.get().getHomePage();
                setResponsePage(page);
            }
        };
        logo.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !isCustomLogoVisible();
            }

            @Override
            public boolean isEnabled() {
                return isLogoLinkEnabled();
            }
        });
        mainHeader.add(logo);

        AjaxLink<String> customLogo = new AjaxLink<>(ID_CUSTOM_LOGO) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                //TODO may be this should lead to customerUrl ?
                Class<? extends Page> page = MidPointApplication.get().getHomePage();
                setResponsePage(page);
            }
        };
        customLogo.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isCustomLogoVisible();
            }
        });
        mainHeader.add(customLogo);

        WebMarkupContainer navigation = new WebMarkupContainer(ID_NAVIGATION);
        navigation.setOutputMarkupId(true);
        mainHeader.add(navigation);

        IModel<IconType> logoModel = new IModel<>() {

            private static final long serialVersionUID = 1L;

            @Override
            public IconType getObject() {
                DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
                return info != null ? info.getLogo() : null;
            }
        };

        ExternalImage customLogoImgSrc = new ExternalImage(ID_CUSTOM_LOGO_IMG_SRC) {

            @Override
            protected void buildSrcAttribute(ComponentTag tag, IModel<?> srcModel) {
                tag.put("src", WebComponentUtil.getIconUrlModel(logoModel.getObject()).getObject());
            }
        };
        customLogoImgSrc.add(new VisibleBehaviour(() -> logoModel.getObject() != null && StringUtils.isEmpty(logoModel.getObject().getCssClass())));

        WebMarkupContainer customLogoImgCss = new WebMarkupContainer(ID_CUSTOM_LOGO_IMG_CSS);
        customLogoImgCss.add(new VisibleBehaviour(() -> logoModel.getObject() != null && StringUtils.isNotEmpty(logoModel.getObject().getCssClass())));
        customLogoImgCss.add(new AttributeAppender("class", new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return logoModel.getObject() != null ? logoModel.getObject().getCssClass() : null;
            }
        }));

        mainHeader.add(new AttributeAppender("style", new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return logoModel.getObject() != null ? "background-color: " + GuiStyleConstants.DEFAULT_BG_COLOR + " !important;" : null;
            }
        }));

        customLogo.add(customLogoImgSrc);
        customLogo.add(customLogoImgCss);

        Label title = new Label(ID_TITLE, createPageTitleModel());
        title.setRenderBodyOnly(true);
        add(title);

        initHeaderLayout(navigation);
        initTitleLayout(navigation);

        logo.add(createHeaderColorStyleModel(false));
        customLogo.add(createHeaderColorStyleModel(false));
        mainHeader.add(createHeaderColorStyleModel(false));

        navigation.add(createHeaderColorStyleModel(true));

        initDebugBarLayout();

        LeftMenuPanel sidebarMenu = new LeftMenuPanel(ID_SIDEBAR_MENU);
        sidebarMenu.add(createUserStatusBehaviour());
        add(sidebarMenu);

        WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
        footerContainer.add(new VisibleBehaviour(() -> !isErrorPage() && isFooterVisible()));
        add(footerContainer);

        WebMarkupContainer version = new WebMarkupContainer(ID_VERSION) {

            private static final long serialVersionUID = 1L;

            @Deprecated
            public String getDescribe() {
                return PageBase.this.getDescribe();
            }
        };
        version.add(new VisibleBehaviour(() ->
                isFooterVisible() && RuntimeConfigurationType.DEVELOPMENT.equals(getApplication().getConfigurationType())));
        footerContainer.add(version);

        WebMarkupContainer copyrightMessage = new WebMarkupContainer(ID_COPYRIGHT_MESSAGE);
        copyrightMessage.add(getFooterVisibleBehaviour());
        footerContainer.add(copyrightMessage);

        Label subscriptionMessage = new Label(ID_SUBSCRIPTION_MESSAGE,
                new IModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        String subscriptionId = getSubscriptionId();
                        if (!WebComponentUtil.isSubscriptionIdCorrect(subscriptionId)) {
                            return " " + createStringResource("PageBase.nonActiveSubscriptionMessage").getString();
                        }
                        if (SubscriptionType.DEMO_SUBSRIPTION.getSubscriptionType().equals(subscriptionId.substring(0, 2))) {
                            return " " + createStringResource("PageBase.demoSubscriptionMessage").getString();
                        }
                        return "";
                    }
                });
        subscriptionMessage.setOutputMarkupId(true);
        subscriptionMessage.add(getFooterVisibleBehaviour());
        footerContainer.add(subscriptionMessage);

        WebMarkupContainer feedbackContainer = new WebMarkupContainer(ID_FEEDBACK_CONTAINER);
        feedbackContainer.setOutputMarkupId(true);
        feedbackContainer.setOutputMarkupPlaceholderTag(true);
        add(feedbackContainer);

        FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK);
        feedbackList.setOutputMarkupId(true);
        feedbackList.setOutputMarkupPlaceholderTag(true);
        feedbackContainer.add(feedbackList);

        MainPopupDialog mainPopup = new MainPopupDialog(ID_MAIN_POPUP);
//        mainPopup.showUnloadConfirmation(false);
//        mainPopup.setResizable(false);
        add(mainPopup);
    }

    private AttributeAppender createHeaderColorStyleModel(boolean checkSkinUsage) {
        return new AttributeAppender("style", new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
                if (info == null || StringUtils.isEmpty(info.getHeaderColor())) {
                    return null;
                }

//                TODO fix for MID-4897
//                if (checkSkinUsage && StringUtils.isEmpty(info.getSkin())) {
//                    return null;
//                }

                return "background-color: " + info.getHeaderColor() + " !important;";
            }
        });
    }

    public MainPopupDialog getMainPopup() {
        return (MainPopupDialog) get(ID_MAIN_POPUP);
    }

    public String getMainPopupBodyId() {
        return getMainPopup().CONTENT_ID;
    }

    public void showMainPopup(Popupable popupable, AjaxRequestTarget target) {
        MainPopupDialog dialog = getMainPopup();
        dialog.getDialogComponent().add(AttributeModifier.replace("style",
                dialog.generateWidthHeightParameter("" + (popupable.getWidth() > 0 ? popupable.getWidth() : ""),
                        popupable.getWidthUnit(),
                        "" + (popupable.getHeight() > 0 ? popupable.getHeight() : ""), popupable.getHeightUnit())));
        dialog.setContent(popupable.getComponent());
        dialog.setTitle(popupable.getTitle());
        dialog.open(target);
    }

    public void hideMainPopup(AjaxRequestTarget target) {
        getMainPopup().close(target);
    }

    private VisibleEnableBehaviour getShoppingCartVisibleBehavior() {
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !isErrorPage() && isSideMenuVisible() &&
                        (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_SELF_REQUESTS_ASSIGNMENTS_URL, PageSelf.AUTH_SELF_ALL_URI)
                                && getSessionStorage().getRoleCatalog().getAssignmentShoppingCart().size() > 0);
            }
        };
    }

    private VisibleEnableBehaviour createUserStatusBehaviour() {
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !isErrorPage() && isSideMenuVisible();
            }
        };
    }

    protected boolean isSideMenuVisible() {
        return SecurityUtils.getPrincipalUser() != null;
    }

    private void initDebugBarLayout() {
        DebugBar debugPanel = new DebugBar(ID_DEBUG_PANEL);
        add(debugPanel);

        WebMarkupContainer debugBar = new WebMarkupContainer(ID_DEBUG_BAR);
        debugBar.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                RuntimeConfigurationType runtime = getApplication().getConfigurationType();
                return RuntimeConfigurationType.DEVELOPMENT.equals(runtime);
            }
        });
        add(debugBar);

        AjaxButton clearCache = new AjaxButton(ID_CLEAR_CACHE, createStringResource("PageBase.clearCssCache")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearLessJsCache(target);
            }
        };
        debugBar.add(clearCache);

        AjaxButton dumpPageTree = new AjaxButton(ID_DUMP_PAGE_TREE, createStringResource("PageBase.dumpPageTree")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                dumpPageTree(target);
            }
        };
        debugBar.add(dumpPageTree);
    }

    private void dumpPageTree(AjaxRequestTarget target) {
        try (PrintWriter pw = new PrintWriter(System.out)) {
            new PageStructureDump().dumpStructure(this, pw);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void clearLessJsCache(AjaxRequestTarget target) {
        try {
            ArrayList<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
            if (servers.size() > 1) {
                LOGGER.info("Too many mbean servers, cache won't be cleared.");
                for (MBeanServer server : servers) {
                    LOGGER.info(server.getDefaultDomain());
                }
                return;
            }
            MBeanServer server = servers.get(0);
            ObjectName objectName = ObjectName.getInstance(Wro4jConfig.WRO_MBEAN_NAME + ":type=WroConfiguration");
            server.invoke(objectName, "reloadCache", new Object[] {}, new String[] {});
            if (target != null) {
                target.add(PageBase.this);
            }
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't clear less/js cache", ex);
            error("Error occurred, reason: " + ex.getMessage());
            if (target != null) {
                target.add(getFeedbackPanel());
            }
        }
    }

    public WebMarkupContainer getFeedbackPanel() {
        return (WebMarkupContainer) get(ID_FEEDBACK_CONTAINER);
    }

    public SessionStorage getSessionStorage() {
        MidPointAuthWebSession session = (MidPointAuthWebSession) getSession();
        return session.getSessionStorage();
    }

    protected IModel<String> createPageTitleModel() {
        return () -> {
            BaseMenuItem activeMenu = getActiveMenu();
            String pageTitleKey = null;
            if (activeMenu != null) {
                pageTitleKey = activeMenu.getNameModel();
            }

            if (StringUtils.isEmpty(pageTitleKey)) {
                pageTitleKey = PageBase.this.getClass().getSimpleName() + ".title";
            }
            return createStringResource(pageTitleKey).getString();
        };
    }

    private <MI extends BaseMenuItem> MI getActiveMenu() {
        LeftMenuPanel sideBarMenu = getSideBarMenuPanel();
        if (sideBarMenu == null || !sideBarMenu.isVisible()) {
            return null;
        }

        List<SideBarMenuItem> sideMenuItems = sideBarMenu.getItems();
        if (CollectionUtils.isEmpty(sideMenuItems)) {
            return null;
        }

        for (SideBarMenuItem sideBarMenuItem : sideMenuItems) {
            MI activeMenu = sideBarMenuItem.getActiveMenu(PageBase.this);
            if (activeMenu != null) {
                return activeMenu;
            }
        }

        return null;
    }

    public void refreshTitle(AjaxRequestTarget target) {
        target.add(getTitleContainer());

        //TODO what about breadcrumbs and page title (header)?
        //target.add(getHeaderTitle()); cannot update component with rendetBodyOnly
    }

    public WebMarkupContainer getTitleContainer() {
        return (WebMarkupContainer) get(createComponentPath(ID_MAIN_HEADER, ID_NAVIGATION, ID_PAGE_TITLE_CONTAINER));
    }

    private Label getHeaderTitle() {
        return (Label) get(ID_TITLE);
    }

    public IModel<String> getPageTitleModel() {
        String title = (String) get(ID_TITLE).getDefaultModel().getObject();
        return Model.of(title);
    }

    public String getString(String resourceKey, Object... objects) {
        return createStringResource(resourceKey, objects).getString();
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey).setModel(new Model<String>()).setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(String resourceKey, IModel model, Object... objects) {
        return new StringResourceModel(resourceKey, model).setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(PolyString polystringKey, Object... objects) {
        String resourceKey = null;
        if (polystringKey != null) {
            resourceKey = localizationService.translate(polystringKey, WebComponentUtil.getCurrentLocale(), true);
        }
        return new StringResourceModel(resourceKey).setModel(new Model<String>()).setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(PolyStringType polystringKey, Object... objects) {
        String resourceKey = null;
        if (polystringKey != null) {
            resourceKey = localizationService.translate(PolyString.toPolyString(polystringKey), WebComponentUtil.getCurrentLocale(), true);
        }
        return new StringResourceModel(resourceKey).setModel(new Model<String>()).setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(Enum<?> e) {
        String resourceKey = e.getDeclaringClass().getSimpleName() + "." + e.name();
        return createStringResource(resourceKey);
    }

    @NotNull
    public static StringResourceModel createStringResourceStatic(String resourceKey,
            Object... objects) {
        return new StringResourceModel(resourceKey).setModel(new Model<String>())
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

        OpResult opResult = OpResult.getOpResult((PageBase) getPage(), result);
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
            if (values == null || values.isEmpty()) {
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
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
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

    // common result processing
    public void processResult(AjaxRequestTarget target, OperationResult result, boolean showSuccess) {
        result.computeStatusIfUnknown();
        if (!result.isSuccess()) {
            showResult(result, showSuccess);
            target.add(getFeedbackPanel());
        } else {
            showResult(result);
            redirectBack();
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

    // TODO untangle this brutal code (list vs objectable vs other cases)
    public <T> void parseObject(String lexicalRepresentation, final Holder<T> objectHolder,
            String language, boolean validateSchema, boolean skipChecks, Class<T> clazz, OperationResult result) {

        boolean isListOfObjects = List.class.isAssignableFrom(clazz);
        boolean isObjectable = Objectable.class.isAssignableFrom(clazz);
        if (skipChecks || language == null || PrismContext.LANG_JSON.equals(language) || PrismContext.LANG_YAML.equals(language)
                || (!isObjectable && !isListOfObjects)) {
            T object;
            try {
                if (isListOfObjects) {
                    List<PrismObject<? extends Objectable>> prismObjects = getPrismContext().parserFor(lexicalRepresentation)
                            .language(language).parseObjects();
                    if (!skipChecks) {
                        for (PrismObject<? extends Objectable> prismObject : prismObjects) {
                            prismObject.checkConsistence();
                        }
                    }
                    object = (T) prismObjects;
                } else if (isObjectable) {
                    PrismObject<ObjectType> prismObject = getPrismContext().parserFor(lexicalRepresentation).language(language).parse();
                    if (!skipChecks) {
                        prismObject.checkConsistence();
                    }
                    object = (T) prismObject.asObjectable();
                } else {
                    object = getPrismContext().parserFor(lexicalRepresentation).language(language).type(clazz).parseRealValue();
                }
                objectHolder.setValue(object);
            } catch (RuntimeException | SchemaException e) {
                result.recordFatalError(createStringResource("PageBase.message.parseObject.fatalError", e.getMessage()).getString(), e);
            }
            return;
        }

        List<PrismObject<?>> list = new ArrayList<>();
        if (isListOfObjects) {
            objectHolder.setValue((T) list);
        }
        EventHandler<Objectable> handler = new EventHandler<>() {
            @Override
            public EventResult preMarshall(Element objectElement, Node postValidationTree,
                    OperationResult objectResult) {
                return EventResult.cont();
            }

            @Override
            public EventResult postMarshall(
                    Objectable object, Element objectElement, OperationResult objectResult) {
                if (isListOfObjects) {
                    list.add(object.asPrismObject());
                } else {
                    //noinspection unchecked
                    objectHolder.setValue((T) object);
                }
                return EventResult.cont();
            }

            @Override
            public void handleGlobalError(OperationResult currentResult) {
            }
        };
        LegacyValidator<?> validator = new LegacyValidator<>(getPrismContext(), handler);
        validator.setVerbose(true);
        validator.setValidateSchema(validateSchema);
        validator.validate(lexicalRepresentation, result, OperationConstants.IMPORT_OBJECT); // TODO the operation name

        result.computeStatus();
    }

    public long getItemsPerPage(UserProfileStorage.TableId tableId) {
        return getItemsPerPage(tableId.name());
    }

    public long getItemsPerPage(String tableIdName) {
        UserProfileStorage userProfile = getSessionStorage().getUserProfile();
        return userProfile.getPagingSize(tableIdName);
    }

    public PrismObject<? extends FocusType> loadFocusSelf() {
        Task task = createSimpleTask(OPERATION_LOAD_USER);
        OperationResult result = task.getResult();
        PrismObject<? extends FocusType> focus = WebModelServiceUtils.loadObject(FocusType.class,
                WebModelServiceUtils.getLoggedInFocusOid(), PageBase.this, task, result);
        result.computeStatus();

        showResult(result, null, false);

        return focus;
    }

    public boolean canRedirectBack() {
        return canRedirectBack(DEFAULT_BREADCRUMB_STEP);
    }

    /**
     * Checks if it's possible to make backStep steps back.
     */
    public boolean canRedirectBack(int backStep) {
        List<Breadcrumb> breadcrumbs = getBreadcrumbs();
        if (breadcrumbs.size() > backStep) {
            return true;
        }
        if (breadcrumbs.size() == backStep && (breadcrumbs.get(breadcrumbs.size() - backStep)) != null) {
            Breadcrumb br = breadcrumbs.get(breadcrumbs.size() - backStep);
            if (br instanceof BreadcrumbPageInstance || br instanceof BreadcrumbPageClass) {
                return true;
            }
        }

        return false;
    }

    public Breadcrumb redirectBack() {
        return redirectBack(DEFAULT_BREADCRUMB_STEP);
    }

    /**
     * @param backStep redirects back to page with backStep step
     */
    public Breadcrumb redirectBack(int backStep) {
        List<Breadcrumb> breadcrumbs = getBreadcrumbs();
        if (canRedirectBack(backStep)) {
            Breadcrumb breadcrumb = breadcrumbs.get(breadcrumbs.size() - backStep);
            redirectBackToBreadcrumb(breadcrumb);
            return breadcrumb;
        } else if (canRedirectBack(DEFAULT_BREADCRUMB_STEP)) {
            Breadcrumb breadcrumb = breadcrumbs.get(breadcrumbs.size() - DEFAULT_BREADCRUMB_STEP);
            redirectBackToBreadcrumb(breadcrumb);
            return breadcrumb;
        } else {
            setResponsePage(getMidpointApplication().getHomePage());
            return null;
        }
    }

    public void navigateToNext(Class<? extends WebPage> page) {
        navigateToNext(page, null);
    }

    public void navigateToNext(Class<? extends WebPage> pageType, PageParameters params) {
        WebPage page = createWebPage(pageType, params);
        navigateToNext(page);
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

    public void navigateToNext(WebPage page) {
        if (!(page instanceof PageBase)) {
            setResponsePage(page);
            return;
        }

        PageBase next = (PageBase) page;
        next.setBreadcrumbs(getBreadcrumbs());

        setResponsePage(next);
    }

    // TODO deduplicate with redirectBack
    public RestartResponseException redirectBackViaRestartResponseException() {
        List<Breadcrumb> breadcrumbs = getBreadcrumbs();
        if (breadcrumbs.size() < 2) {
            return new RestartResponseException(getApplication().getHomePage());
        }

        Breadcrumb breadcrumb = breadcrumbs.get(breadcrumbs.size() - 2);
        redirectBackToBreadcrumb(breadcrumb);
        return breadcrumb.getRestartResponseException();
    }

    public void redirectBackToBreadcrumb(Breadcrumb breadcrumb) {
        Validate.notNull(breadcrumb, "Breadcrumb must not be null");

        boolean found = false;

        //we remove all breadcrumbs that are after "breadcrumb"
        List<Breadcrumb> breadcrumbs = getBreadcrumbs();
        Iterator<Breadcrumb> iterator = breadcrumbs.iterator();
        while (iterator.hasNext()) {
            Breadcrumb b = iterator.next();
            if (found) {
                iterator.remove();
            } else if (b.equals(breadcrumb)) {
                found = true;
            }
        }
        WebPage page = breadcrumb.redirect();
        if (page instanceof PageBase) {
            PageBase base = (PageBase) page;
            base.setBreadcrumbs(breadcrumbs);
        }

        setResponsePage(page);
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
        return securityContextManager.runPrivileged(producer);
    }

    public <T> T runAsChecked(CheckedProducer<T> producer, PrismObject<UserType> user) throws CommonException {
        return securityContextManager.runAsChecked(producer, user);
    }

    @NotNull
    public PrismObject<UserType> getAdministratorPrivileged(OperationResult parentResult) throws CommonException {
        OperationResult result = parentResult.createSubresult(OPERATION_LOAD_USER);
        try {
            return securityContextManager.runPrivilegedChecked(() -> {
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

    public void setBreadcrumbs(List<Breadcrumb> breadcrumbs) {
        getBreadcrumbs().clear();

        if (breadcrumbs != null) {
            getBreadcrumbs().addAll(breadcrumbs);
        }
    }

    public List<Breadcrumb> getBreadcrumbs() {
        if (breadcrumbs == null) {
            breadcrumbs = new ArrayList<>();
        }
        return breadcrumbs;
    }

    public void addBreadcrumb(Breadcrumb breadcrumb) {
        Validate.notNull(breadcrumb, "Breadcrumb must not be null");

        Breadcrumb last = getBreadcrumbs().isEmpty() ?
                null : getBreadcrumbs().get(getBreadcrumbs().size() - 1);
        if (last != null && last.equals(breadcrumb)) {
            return;
        }

        getBreadcrumbs().add(breadcrumb);
    }

    public Breadcrumb getLastBreadcrumb() {
        if (getBreadcrumbs().isEmpty()) {
            return null;
        }

        return getBreadcrumbs().get(getBreadcrumbs().size() - 1);
    }

    public Breadcrumb getPreviousBreadcrumb() {
        if (getBreadcrumbs().isEmpty() || getBreadcrumbs().size() < 2) {
            return null;
        }

        return getBreadcrumbs().get(getBreadcrumbs().size() - 2);
    }

    public void clearBreadcrumbs() {
        getBreadcrumbs().clear();
    }

    private boolean isCustomLogoVisible() {
        DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
        if (info == null || info.getLogo() == null) {
            return false;
        }

        IconType logo = info.getLogo();
        return StringUtils.isNotEmpty(logo.getImageUrl()) || StringUtils.isNotEmpty(logo.getCssClass());
    }

    protected boolean isLogoLinkEnabled() {
        return true;
    }

    private String getSubscriptionId() {
        DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
        return info != null ? info.getSubscriptionIdentifier() : null;
    }

    private VisibleEnableBehaviour getFooterVisibleBehaviour() {
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isFooterVisible();
            }
        };
    }

    private boolean isFooterVisible() {
        String subscriptionId = getSubscriptionId();
        if (StringUtils.isEmpty(subscriptionId)) {
            return true;
        }
        return !WebComponentUtil.isSubscriptionIdCorrect(subscriptionId) ||
                (SubscriptionType.DEMO_SUBSRIPTION.getSubscriptionType().equals(subscriptionId.substring(0, 2))
                        && WebComponentUtil.isSubscriptionIdCorrect(subscriptionId));
    }

    protected String determineDataLanguage() {
        CompiledGuiProfile config = getCompiledGuiProfile();
        if (config.getPreferredDataLanguage() != null) {
            if (PrismContext.LANG_JSON.equals(config.getPreferredDataLanguage())) {
                return PrismContext.LANG_JSON;
            } else if (PrismContext.LANG_YAML.equals(config.getPreferredDataLanguage())) {
                return PrismContext.LANG_YAML;
            } else {
                return PrismContext.LANG_XML;
            }
        } else {
            return PrismContext.LANG_XML;
        }
    }

    public void reloadShoppingCartIcon(AjaxRequestTarget target) {
        target.add(get(createComponentPath(ID_MAIN_HEADER, ID_NAVIGATION)));
        target.add(get(createComponentPath(ID_MAIN_HEADER, ID_NAVIGATION, ID_CART_BUTTON)));
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

    @Override
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

    @Override
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

    private LeftMenuPanel getSideBarMenuPanel() {
        return (LeftMenuPanel) get(ID_SIDEBAR_MENU);
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
