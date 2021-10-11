/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.page;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;

import com.evolveum.midpoint.web.page.admin.certification.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.wicket.*;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessages;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
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
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.LegacyValidator;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.SubscriptionType;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.factory.ItemWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValuePanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryConverter;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.CounterManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.CheckedProducer;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AsyncWebProcessManager;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.web.boot.Wro4jConfig;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.breadcrumbs.BreadcrumbPageClass;
import com.evolveum.midpoint.web.component.breadcrumbs.BreadcrumbPageInstance;
import com.evolveum.midpoint.web.component.dialog.MainPopupDialog;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.menu.*;
import com.evolveum.midpoint.web.component.menu.top.LocalePanel;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.archetype.PageArchetype;
import com.evolveum.midpoint.web.page.admin.archetype.PageArchetypes;
import com.evolveum.midpoint.web.page.admin.cases.*;
import com.evolveum.midpoint.web.page.admin.configuration.*;
import com.evolveum.midpoint.web.page.admin.home.PageDashboardConfigurable;
import com.evolveum.midpoint.web.page.admin.home.PageDashboardInfo;
import com.evolveum.midpoint.web.page.admin.reports.*;
import com.evolveum.midpoint.web.page.admin.resources.*;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageNodes;
import com.evolveum.midpoint.web.page.admin.server.PageTask;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.PageTasksCertScheduling;
import com.evolveum.midpoint.web.page.admin.services.PageService;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.admin.workflow.PageAttorneySelection;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItemsAttorney;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.page.self.*;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.NewWindowNotifyingBehavior;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorRegistry;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.util.QueryUtils;
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
    protected static final String OPERATION_LOAD_VIEW_COLLECTION_REF = DOT_CLASS + "loadViewCollectionRef";
    private static final String OPERATION_LOAD_WORK_ITEM_COUNT = DOT_CLASS + "loadWorkItemCount";
    private static final String OPERATION_LOAD_CERT_WORK_ITEM_COUNT = DOT_CLASS + "loadCertificationWorkItemCount";

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

    @SpringBean(name = "auditService")
    private AuditService auditService;

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

    // @SpringBean(name = "certificationManager")
    // private CertificationManager certificationManager;

    @SpringBean(name = "modelController")
    private AccessCertificationService certficationService;

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

    @SpringBean private CounterManager counterManager;

    @SpringBean private ClusterExecutionHelper clusterExecutionHelper;

    private List<Breadcrumb> breadcrumbs;

    private boolean initialized = false;

    private LoadableModel<Integer> workItemCountModel;
    private LoadableModel<Integer> certWorkItemCountModel;

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

        initializeModel();

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

    private void initializeModel() {
        workItemCountModel = new LoadableModel<Integer>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected Integer load() {
                try {
                    Task task = createSimpleTask(OPERATION_LOAD_WORK_ITEM_COUNT);
                    S_FilterEntryOrEmpty q = getPrismContext().queryFor(CaseWorkItemType.class);
                    ObjectQuery query = QueryUtils.filterForAssignees(q, getPrincipal(),
                            OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS, getRelationRegistry())
                            .and()
                            .item(CaseWorkItemType.F_CLOSE_TIMESTAMP)
                            .isNull()
                            .build();
                    return getModelService().countContainers(CaseWorkItemType.class, query, null, task, task.getResult());
                } catch (Exception e) {
                    LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't load work item count", e);
                    return null;
                }
            }
        };
        certWorkItemCountModel = new LoadableModel<Integer>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected Integer load() {
                try {
                    AccessCertificationService acs = getCertificationService();
                    Task task = createSimpleTask(OPERATION_LOAD_CERT_WORK_ITEM_COUNT);
                    OperationResult result = task.getResult();
                    return acs.countOpenWorkItems(getPrismContext().queryFactory().createQuery(), true, null, task, result);
                } catch (Exception e) {
                    LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't load certification work item count", e);
                    return null;
                }
            }
        };
    }

    public void resetWorkItemCountModel() {
        if (workItemCountModel != null) {
            workItemCountModel.reset();
        }
    }

    public void resetCertWorkItemCountModel() {
        if (certWorkItemCountModel != null) {
            certWorkItemCountModel.reset();
        }
    }

    protected void createBreadcrumb() {
        BreadcrumbPageClass bc = new BreadcrumbPageClass(new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getPageTitleModel().getObject();
            }
        }, this.getClass(), getPageParameters());

        addBreadcrumb(bc);
    }

    protected void createInstanceBreadcrumb() {
        BreadcrumbPageInstance bc = new BreadcrumbPageInstance(new IModel<String>() {
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

    public MidpointFunctions getMidpointFunctions() {
        return midpointFunctions;
    }

   public CounterManager getCounterManager() {
        return counterManager;
    }

    @Contract(pure = true)
    public PrismContext getPrismContext() {
        return getMidpointApplication().getPrismContext();
    }

    public SchemaHelper getSchemaHelper() {
        return getMidpointApplication().getSchemaHelper();
    }

    public GetOperationOptionsBuilder getOperationOptionsBuilder() {
        return getSchemaHelper().getOperationOptionsBuilder();
    }

    public Collection<SelectorOptions<GetOperationOptions>> retrieveItemsNamed(Object... items) {
        return getOperationOptionsBuilder().items(items).retrieve().build();
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

    public AuditService getAuditService() {
        return auditService;
    }

    public ClusterExecutionHelper getClusterExecutionHelper() {
        return clusterExecutionHelper;
    }

    public AccessCertificationService getCertificationService() {
        return certficationService;
    }

    @Override
    public ModelService getModelService() {
        return modelService;
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

    public GuiComponentRegistry getRegistry() {
        return registry;
    }

    public CacheDispatcher getCacheDispatcher() {
        return cacheDispatcher;
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

    // TODO reconsider this method
    public boolean isFullyAuthorized() {
        try {
            return isAuthorized(AuthorizationConstants.AUTZ_ALL_URL);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't check the authorization", t);
            return false;
        }
    }

    public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
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
        return createStringResourceStatic(component, resourceKey);
    }

    public static String createEnumResourceKey(Enum<?> e) {
        return e.getDeclaringClass().getSimpleName() + "." + e.name();
    }

    public Task createAnonymousTask(String operation) {
        TaskManager manager = getTaskManager();
        Task task = manager.createTaskInstance(operation);

        task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);

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
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        String skinCssString = CLASS_DEFAULT_SKIN;
        DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
        if (info != null && StringUtils.isNotEmpty(info.getSkin())) {
            skinCssString = info.getSkin();
        }

        String skinCssPath = String.format("../../../../../../webjars/adminlte/2.3.11/dist/css/skins/%s.min.css", skinCssString);
        response.render(CssHeaderItem.forReference(
                new CssResourceReference(
                        PageBase.class, skinCssPath)
                )
        );

        // this attaches jquery.js as first header item, which is used in our scripts.
        CoreLibrariesContributor.contribute(getApplication(), response);
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
        menuToggle.add(createUserStatusBehaviour(true));
        container.add(menuToggle);

        UserMenuPanel rightMenu = new UserMenuPanel(ID_RIGHT_MENU, this);
        rightMenu.add(createUserStatusBehaviour(true));
        container.add(rightMenu);

        LocalePanel locale = new LocalePanel(ID_LOCALE);
//        locale.add(createUserStatusBehaviour(false));
        container.add(locale);
    }

    private void initTitleLayout(WebMarkupContainer mainHeader) {
        WebMarkupContainer pageTitleContainer = new WebMarkupContainer(ID_PAGE_TITLE_CONTAINER);
        pageTitleContainer.add(createUserStatusBehaviour(true));
        pageTitleContainer.setOutputMarkupId(true);
        mainHeader.add(pageTitleContainer);

        WebMarkupContainer pageTitle = new WebMarkupContainer(ID_PAGE_TITLE);
        pageTitleContainer.add(pageTitle);

        IModel<String> deploymentNameModel = new IModel<String>() {

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

        IModel<List<Breadcrumb>> breadcrumbsModel = new IModel<List<Breadcrumb>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public List<Breadcrumb> getObject() {
                return getBreadcrumbs();
            }
        };

        ListView<Breadcrumb> breadcrumbs = new ListView<Breadcrumb>(ID_BREADCRUMB, breadcrumbsModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Breadcrumb> item) {
                final Breadcrumb dto = item.getModelObject();

                AjaxLink<String> bcLink = new AjaxLink<String>(ID_BC_LINK) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        redirectBackToBreadcrumb(dto);
                    }
                };
                item.add(bcLink);
                bcLink.add(new VisibleEnableBehaviour() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isEnabled() {
                        return dto.isUseLink();
                    }
                });

                WebMarkupContainer bcIcon = new WebMarkupContainer(ID_BC_ICON);
                bcIcon.add(new VisibleEnableBehaviour() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        return dto.getIcon() != null && dto.getIcon().getObject() != null;
                    }
                });
                bcIcon.add(AttributeModifier.replace("class", dto.getIcon()));
                bcLink.add(bcIcon);

                Label bcName = new Label(ID_BC_NAME, dto.getLabel());
                bcLink.add(bcName);

                item.add(new VisibleEnableBehaviour() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        return dto.isVisible();
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
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                attributes.setChannel(new AjaxChannel("blocking", AjaxChannel.Type.ACTIVE));
            }

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                navigateToNext(new PageAssignmentsList(true));
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
        body.add(new AttributeAppender("class", "hold-transition ", " "));
        body.add(new AttributeAppender("class", "custom-hold-transition ", " "));

        body.add(new AttributeAppender("class", new IModel<String>() {

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

        AjaxLink<String> logo = new AjaxLink<String>(ID_LOGO) {

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

        AjaxLink<String> customLogo = new AjaxLink<String>(ID_CUSTOM_LOGO) {
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


        IModel<IconType> logoModel = new IModel<IconType>() {

            private static final long serialVersionUID = 1L;

            @Override
            public IconType getObject() {
                DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
                return info != null ? info.getLogo() : null;
            }
        };

        ExternalImage customLogoImgSrc = new ExternalImage(ID_CUSTOM_LOGO_IMG_SRC){

            @Override
            protected void buildSrcAttribute(ComponentTag tag, IModel<?> srcModel) {
                tag.put("src", WebComponentUtil.getIconUrlModel(logoModel != null ? logoModel.getObject() : (IconType) null).getObject());
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

        SideBarMenuPanel sidebarMenu = new SideBarMenuPanel(ID_SIDEBAR_MENU, new LoadableModel<List<SideBarMenuItem>>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<SideBarMenuItem> load() {
                return createMenuItems();
            }
        });
        sidebarMenu.add(createUserStatusBehaviour(true));
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
        mainPopup.setOutputMarkupId(true);
        mainPopup.setOutputMarkupPlaceholderTag(true);
        mainPopup.showUnloadConfirmation(false);
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
        return getMainPopup().getContentId();
    }

    public void showMainPopup(Popupable popupable, AjaxRequestTarget target) {
        getMainPopup().setTitle(popupable.getTitle());
        getMainPopup().setInitialHeight(popupable.getHeight());
        getMainPopup().setInitialWidth(popupable.getWidth());
        getMainPopup().setHeightUnit(popupable.getHeightUnit());
        getMainPopup().setWidthUnit(popupable.getWidthUnit());
        getMainPopup().setContent(popupable.getComponent());
        getMainPopup().setResizable(false);
        getMainPopup().setMaskType(ModalWindow.MaskType.TRANSPARENT);
        getMainPopup().show(target);
    }

    public void hideMainPopup(AjaxRequestTarget target) {
        getMainPopup().close(target);
    }

    private VisibleEnableBehaviour getShoppingCartVisibleBehavior(){
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !isErrorPage() && isSideMenuVisible(true) &&
                        (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_SELF_REQUESTS_ASSIGNMENTS_URL, PageSelf.AUTH_SELF_ALL_URI));
            }
        };
    }

    private VisibleEnableBehaviour createUserStatusBehaviour(final boolean visibleIfLoggedIn) {
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !isErrorPage() && isSideMenuVisible(visibleIfLoggedIn);
            }
        };
    }

    protected boolean isSideMenuVisible(boolean visibleIfLoggedIn) {
        return SecurityUtils.getPrincipalUser() != null ? visibleIfLoggedIn : !visibleIfLoggedIn;
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
            server.invoke(objectName, "reloadCache", new Object[]{}, new String[]{});
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
        return new IModel<String>() {
            @Override
            public String getObject() {
                BaseMenuItem activeMenu = getActiveMenu();
                String pageTitleKey = null;
                if (activeMenu != null) {
                    pageTitleKey = activeMenu.getNameModel().getObject();
                }

                if (StringUtils.isEmpty(pageTitleKey)) {
                    pageTitleKey = PageBase.this.getClass().getSimpleName() + ".title";
                }
                return createStringResource(pageTitleKey).getString();
            }
        };
    }

    private <MI extends BaseMenuItem> MI getActiveMenu() {
        SideBarMenuPanel sideBarMenu = getSideBarMenuPanel();
        if (sideBarMenu == null || !sideBarMenu.isVisible()) {
            return null;
        }

        List<SideBarMenuItem> sideMenuItems = sideBarMenu.getModelObject();
        if (CollectionUtils.isEmpty(sideMenuItems)) {
            return null;
        }

        MI activeMenu = null;
        for (SideBarMenuItem sideBarMenuItem : sideMenuItems) {
            List<MainMenuItem> mainMenuItems = sideBarMenuItem.getItems();
            activeMenu = (MI) getActiveMenu(mainMenuItems);
            if (activeMenu != null) {
                return activeMenu;
            }
        }

        return activeMenu;
    }

    private <MI extends BaseMenuItem> MI getActiveMenu(List<MI> mainMenuItems) {
        if (CollectionUtils.isEmpty(mainMenuItems)) {
            return null;
        }
        for (MI menuItem : mainMenuItems) {
            if (menuItem.isMenuActive(PageBase.this)) {
                return menuItem;
            }

            if (!(menuItem instanceof MainMenuItem)) {
                continue;
            }

            List<MenuItem> menuItems = ((MainMenuItem) menuItem).getItems();
            MI activeMenuItem = (MI) getActiveMenu(menuItems);
            if (activeMenuItem != null) {
                return activeMenuItem;
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
        return (IModel) get(ID_TITLE).getDefaultModel();
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
            // TODO later: try polystringKey.getKey()
            resourceKey = polystringKey.getOrig();
        }
        return new StringResourceModel(resourceKey, this).setModel(new Model<String>()).setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(PolyStringType polystringKey, Object... objects) {
        String resourceKey = null;
        if (polystringKey != null) {
            // TODO later: try polystringKey.getKey()
            resourceKey = polystringKey.getOrig();
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

            ExpressionVariables variables = new ExpressionVariables();

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
        EventHandler handler = new EventHandler() {

            @Override
            public EventResult preMarshall(Element objectElement, Node postValidationTree,
                                           OperationResult objectResult) {
                return EventResult.cont();
            }

            @Override
            public <O extends Objectable> EventResult postMarshall(PrismObject<O> object, Element objectElement,
                                                                   OperationResult objectResult) {
                if (isListOfObjects) {
                    list.add(object);
                } else {
                    @SuppressWarnings({"unchecked", "raw"})
                    T value = (T) object.asObjectable();
                    objectHolder.setValue(value);
                }
                return EventResult.cont();
            }

            @Override
            public void handleGlobalError(OperationResult currentResult) {
            }
        };
        LegacyValidator validator = new LegacyValidator(getPrismContext(), handler);
        validator.setVerbose(true);
        validator.setValidateSchema(validateSchema);
        validator.validate(lexicalRepresentation, result, OperationConstants.IMPORT_OBJECT);        // TODO the operation name

        result.computeStatus();
    }

    public long getItemsPerPage(UserProfileStorage.TableId tableId) {
        UserProfileStorage userProfile = getSessionStorage().getUserProfile();
        return userProfile.getPagingSize(tableId);
    }

    protected List<SideBarMenuItem> createMenuItems() {
        List<SideBarMenuItem> menus = new ArrayList<>();

        SideBarMenuItem menu = new SideBarMenuItem(createStringResource("PageAdmin.menu.selfService"));
        menus.add(menu);
        createSelfServiceMenu(menu);

        menu = new SideBarMenuItem(createStringResource("PageAdmin.menu.mainNavigation"));
        menus.add(menu);
        List<MainMenuItem> items = menu.getItems();


        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CONFIGURATION_URL,
                AuthorizationConstants.AUTZ_UI_CONFIGURATION_DEBUG_URL,
                AuthorizationConstants.AUTZ_UI_CONFIGURATION_DEBUGS_URL,
                AuthorizationConstants.AUTZ_UI_CONFIGURATION_IMPORT_URL,
                AuthorizationConstants.AUTZ_UI_CONFIGURATION_LOGGING_URL,
                AuthorizationConstants.AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL,
                AuthorizationConstants.AUTZ_UI_CONFIGURATION_ABOUT_URL,
                AuthorizationConstants.AUTZ_UI_CONFIGURATION_REPOSITORY_QUERY_URL,
                AuthorizationConstants.AUTZ_UI_CONFIGURATION_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {

            menu = new SideBarMenuItem(createStringResource("PageAdmin.menu.top.configuration"));
            menus.add(menu);
            createConfigurationMenu(menu);
        }

        menu = new SideBarMenuItem(createStringResource("PageAdmin.menu.additional"));
        menus.add(menu);
        createAdditionalMenu(menu);


        // todo fix with visible behaviour [lazyman]
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                AuthorizationConstants.AUTZ_UI_HOME_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createHomeItems());
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_USERS_URL,
                AuthorizationConstants.AUTZ_UI_USERS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_UI_USERS_VIEW_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createUsersItems());
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ORG_STRUCT_URL,
                AuthorizationConstants.AUTZ_UI_ORG_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createOrganizationsMenu());
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ROLES_URL,
                AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_UI_ROLES_VIEW_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createRolesItems());
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_SERVICES_URL,
                AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_UI_SERVICES_VIEW_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createServicesItems());
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ARCHETYPES_URL,
                AuthorizationConstants.AUTZ_UI_ARCHETYPES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_UI_ARCHETYPES_VIEW_URL)) {
            items.add(createArchetypesItems());
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_RESOURCES_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL, AuthorizationConstants.AUTZ_UI_RESOURCE_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCE_EDIT_URL, AuthorizationConstants.AUTZ_UI_RESOURCES_VIEW_URL)) {
            items.add(createResourcesItems());
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_MY_WORK_ITEMS_URL,
                AuthorizationConstants.AUTZ_UI_ATTORNEY_WORK_ITEMS_URL,
                AuthorizationConstants.AUTZ_UI_ALL_WORK_ITEMS_URL,
                AuthorizationConstants.AUTZ_UI_CLAIMABLE_WORK_ITEMS_URL,
                AuthorizationConstants.AUTZ_UI_WORK_ITEM_URL,
                AuthorizationConstants.AUTZ_UI_CASES_ALL_URL,
                AuthorizationConstants.AUTZ_UI_CASES_URL,
                AuthorizationConstants.AUTZ_UI_CASE_URL,
                AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_UI_CASES_VIEW_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            if (getWorkflowManager().isEnabled()) {
                items.add(createWorkItemsItems());
            }
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CERTIFICATION_ALL_URL,
                AuthorizationConstants.AUTZ_UI_CERTIFICATION_DEFINITIONS_URL,
                AuthorizationConstants.AUTZ_UI_CERTIFICATION_NEW_DEFINITION_URL,
                AuthorizationConstants.AUTZ_UI_CERTIFICATION_CAMPAIGNS_URL,
                AuthorizationConstants.AUTZ_UI_CERTIFICATION_DECISIONS_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createCertificationItems());
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_TASKS_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createServerTasksItems());
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_REPORTS_URL,
                AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createReportsItems());
        }

        return menus;
    }


    private void createConfigurationMenu(SideBarMenuItem item) {
        addMainMenuItem(item, "fa fa-bullseye", "PageAdmin.menu.top.configuration.bulkActions", PageBulkAction.class);
        addMainMenuItem(item, "fa fa-upload", "PageAdmin.menu.top.configuration.importObject", PageImportObject.class);

        MainMenuItem debugs = addMainMenuItem(item, "fa fa-file-text", "PageAdmin.menu.top.configuration.repositoryObjects", null);

        addMenuItem(debugs, "PageAdmin.menu.top.configuration.repositoryObjectsList", PageDebugList.class);

        MenuItem menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.repositoryObjectView"),
                PageDebugView.class, null, createVisibleDisabledBehaviorForEditMenu(PageDebugView.class));
        debugs.getItems().add(menu);

        MainMenuItem systemItemNew = addMainMenuItem(item, "fa fa-cog", "PageAdmin.menu.top.configuration.basic", null);

        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.basic",
                PageSystemConfiguration.CONFIGURATION_TAB_BASIC);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.objectPolicy",
                PageSystemConfiguration.CONFIGURATION_TAB_OBJECT_POLICY);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.globalPolicyRule",
                PageSystemConfiguration.CONFIGURATION_TAB_GLOBAL_POLICY_RULE);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.globalAccountSynchronization",
                PageSystemConfiguration.CONFIGURATION_TAB_GLOBAL_ACCOUNT_SYNCHRONIZATION);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.cleanupPolicy",
                PageSystemConfiguration.CONFIGURATION_TAB_CLEANUP_POLICY);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.notifications",
                PageSystemConfiguration.CONFIGURATION_TAB_NOTIFICATION);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.logging",
                PageSystemConfiguration.CONFIGURATION_TAB_LOGGING);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.profiling",
                PageSystemConfiguration.CONFIGURATION_TAB_PROFILING);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.adminGui",
                PageSystemConfiguration.CONFIGURATION_TAB_ADMIN_GUI);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.workflow",
                PageSystemConfiguration.CONFIGURATION_TAB_WORKFLOW);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.roleManagement",
                PageSystemConfiguration.CONFIGURATION_TAB_ROLE_MANAGEMENT);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.internals",
                PageSystemConfiguration.CONFIGURATION_TAB_INTERNALS);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.deploymentInformation",
                PageSystemConfiguration.CONFIGURATION_TAB_DEPLOYMENT_INFORMATION);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.accessCertification",
                PageSystemConfiguration.CONFIGURATION_TAB_ACCESS_CERTIFICATION);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.infrastructure",
                PageSystemConfiguration.CONFIGURATION_TAB_INFRASTRUCTURE);
        addSystemMenuItem(systemItemNew, "PageAdmin.menu.top.configuration.fullTextSearch",
                PageSystemConfiguration.CONFIGURATION_TAB_FULL_TEXT_SEARCH);

        addMainMenuItem(item, "fa fa-archive", "PageAdmin.menu.top.configuration.internals", PageInternals.class);
        addMainMenuItem(item, "fa fa-search", "PageAdmin.menu.top.configuration.repoQuery", PageRepositoryQuery.class);
        if (WebModelServiceUtils.isEnableExperimentalFeature(this)) {
            addMainMenuItem(item, "fa fa-cog", "PageAdmin.menu.top.configuration.evaluateMapping", PageEvaluateMapping.class);
        }
        addMainMenuItem(item, "fa fa-info-circle", "PageAdmin.menu.top.configuration.about", PageAbout.class);
    }

    private void addSystemMenuItem(MainMenuItem mainItem, String key, int tabIndex) {
        PageParameters params = new PageParameters();
        params.add(PageSystemConfiguration.SELECTED_TAB_INDEX, tabIndex);
        MenuItem menu = new MenuItem(createStringResource(key), PageSystemConfiguration.class, params, null) {

            @Override
            public boolean isMenuActive(WebPage page) {
                if (!PageSystemConfiguration.class.equals(page.getClass())) {
                    return false;
                }

                int index = getSelectedTabForConfiguration(page);
                return tabIndex == index ? true : false;
            }
        };
        mainItem.getItems().add(menu);
    }

    private MainMenuItem addMainMenuItem(SideBarMenuItem item, String icon, String key, Class<? extends PageBase> page) {
        MainMenuItem mainItem = new MainMenuItem(icon, createStringResource(key), page);
        item.getItems().add(mainItem);

        return mainItem;
    }

    private void addMenuItem(MainMenuItem item, String key, Class<? extends PageBase> page) {
        addMenuItem(item, key, "", page);
    }

    private void addMenuItem(MainMenuItem item, String key, String iconClass, Class<? extends PageBase> page) {
        MenuItem menu = new MenuItem(createStringResource(key), iconClass, page);
        item.getItems().add(menu);
    }

    private MainMenuItem createWorkItemsItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.EVO_CASE_OBJECT_ICON,
                createStringResource("PageAdmin.menu.top.cases"), null) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getBubbleLabel() {
                Integer workItemCount = workItemCountModel.getObject();
                if (workItemCount == null || workItemCount == 0) {
                    return null;
                } else {
                    return workItemCount.toString();
                }
            }
        };

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CASES_ALL_URL, AuthorizationConstants.AUTZ_UI_CASES_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_URL)) {
            addObjectListPageMenuItem(item, "PageAdmin.menu.top.cases.listAll", GuiStyleConstants.EVO_CASE_OBJECT_ICON, PageCases.class);
        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CASES_ALL_URL, AuthorizationConstants.AUTZ_UI_CASES_VIEW_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_URL)) {
            addCollectionsMenuItems(item.getItems(), CaseType.COMPLEX_TYPE, PageCases.class);
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ALL_WORK_ITEMS_URL, AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_CASES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL)){
                addMenuItem(item, "PageAdmin.menu.top.caseWorkItems.listAll", GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_ICON, PageCaseWorkItemsAll.class);
    }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_MY_WORK_ITEMS_URL, AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_CASES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL)) {
            addMenuItem(item, "PageAdmin.menu.top.caseWorkItems.list", PageCaseWorkItemsAllocatedToMe.class);
        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ATTORNEY_WORK_ITEMS_URL, AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_CASES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL)) {
            addMenuItem(item, "PageAdmin.menu.top.workItems.selectAttorney", PageAttorneySelection.class);
            createFocusPageViewMenu(item.getItems(), "PageAdmin.menu.top.workItems.listAttorney", PageWorkItemsAttorney.class);

        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CLAIMABLE_WORK_ITEMS_URL, AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_CASES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL)) {
            addMenuItem(item, "PageWorkItemsClaimable.title", PageWorkItemsClaimable.class);
        }
        createFocusPageViewMenu(item.getItems(), "PageAdmin.menu.top.case.view", PageCase.class);
        createFocusPageViewMenu(item.getItems(), "PageAdmin.menu.top.caseWorkItems.view", PageCaseWorkItem.class);

        return item;
    }

    private MainMenuItem createServerTasksItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.CLASS_OBJECT_TASK_ICON_COLORED,
                createStringResource("PageAdmin.menu.top.serverTasks"), null);

        addObjectListPageMenuItem(item, "PageAdmin.menu.top.serverTasks.list", GuiStyleConstants.CLASS_SHADOW_ICON_GENERIC, PageTasks.class);

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_VIEW_URL)) {

            addCollectionsMenuItems(item.getItems(), TaskType.COMPLEX_TYPE, PageTasks.class);
        }

        addMenuItem(item, "PageAdmin.menu.top.serverTasks.nodes", PageNodes.class);

        createFocusPageNewEditMenu(item.getItems(), "PageAdmin.menu.top.serverTasks.new", "PageAdmin.menu.top.serverTasks.edit",
                PageTask.class, false);


        return item;
    }

    private MainMenuItem createResourcesItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON_COLORED,
                createStringResource("PageAdmin.menu.top.resources"), null);

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_RESOURCES_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL)) {
            addObjectListPageMenuItem(item, "PageAdmin.menu.top.resources.list", GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON, PageResources.class);
        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_RESOURCES_VIEW_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL)) {
            addCollectionsMenuItems(item.getItems(), ResourceType.COMPLEX_TYPE, PageResources.class);
        }
        createFocusPageViewMenu(item.getItems(), "PageAdmin.menu.top.resources.view", PageResource.class);
        createFocusPageNewEditMenu(item.getItems(), "PageAdmin.menu.top.resources.new",
                "PageAdmin.menu.top.resources.edit", PageResourceWizard.class, false);

        addMenuItem(item, "PageAdmin.menu.top.resources.import", PageImportResource.class);
        addMenuItem(item, "PageAdmin.menu.top.connectorHosts.list", PageConnectorHosts.class);

        return item;
    }

    private MainMenuItem createReportsItems() {
        MainMenuItem item = new MainMenuItem("fa fa-pie-chart", createStringResource("PageAdmin.menu.top.reports"),
                null);

        addMenuItem(item, "PageAdmin.menu.top.reports.list", PageReports.class);

        MenuItem configure = new MenuItem(createStringResource("PageAdmin.menu.top.reports.configure"),
                PageReport.class, null, createVisibleDisabledBehaviorForEditMenu(PageReport.class));
        item.getItems().add(configure);

        addMenuItem(item, "PageAdmin.menu.top.reports.created", PageCreatedReports.class);
        addMenuItem(item, "PageAdmin.menu.top.reports.new", PageNewReport.class);

        if (WebComponentUtil.isAuthorized(ModelAuthorizationAction.AUDIT_READ.getUrl())) {
            addMenuItem(item, "PageAuditLogViewer.menuName", PageAuditLogViewer.class);
        }

        return item;
    }

    private MainMenuItem createCertificationItems() {
        MainMenuItem item = new MainMenuItem("fa fa-certificate",
                createStringResource("PageAdmin.menu.top.certification"), null) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getBubbleLabel() {
                Integer certWorkItemCount = certWorkItemCountModel.getObject();
                if (certWorkItemCount == null || certWorkItemCount == 0) {
                    return null;
                } else {
                    return certWorkItemCount.toString();
                }
            }
        };

        addMenuItem(item, "PageAdmin.menu.top.certification.definitions", PageCertDefinitions.class);
        addMenuItem(item, "PageAdmin.menu.top.certification.campaigns", PageCertCampaigns.class);

        PageParameters params = new PageParameters();
        params.add(PageTasks.SELECTED_CATEGORY, TaskCategory.ACCESS_CERTIFICATION);
        MenuItem menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.scheduling"),
                PageTasksCertScheduling.class, params, null);
        item.getItems().add(menu);

        if (isFullyAuthorized()) {  // workaround for MID-5917
            addMenuItem(item, "PageAdmin.menu.top.certification.allDecisions", PageCertDecisionsAll.class);
        }
        addMenuItem(item, "PageAdmin.menu.top.certification.decisions", PageCertDecisions.class);

        MenuItem newCertificationMenu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.newDefinition"), GuiStyleConstants.CLASS_PLUS_CIRCLE, PageCertDefinition.class, null,
                new VisibleEnableBehaviour());
        item.getItems().add(newCertificationMenu);

        return item;
    }

    private int getSelectedTabForConfiguration(WebPage page) {
        PageParameters params = page.getPageParameters();
        StringValue val = params.get(PageSystemConfiguration.SELECTED_TAB_INDEX);
        String value = null;
        if (val != null && !val.isNull()) {
            value = val.toString();
        }

        return StringUtils.isNumeric(value) ? Integer.parseInt(value) : PageSystemConfiguration.CONFIGURATION_TAB_BASIC;
    }

    private void createSelfServiceMenu(SideBarMenuItem menu) {
        addMainMenuItem(menu, GuiStyleConstants.CLASS_ICON_DASHBOARD, "PageAdmin.menu.selfDashboard",
                PageSelfDashboard.class);
        addMainMenuItem(menu, GuiStyleConstants.CLASS_ICON_PROFILE, "PageAdmin.menu.profile",
                WebComponentUtil.resolveSelfPage());
        addMainMenuItem(menu, GuiStyleConstants.CLASS_ICON_CREDENTIALS, "PageAdmin.menu.credentials",
                PageSelfCredentials.class);
        if (WebModelServiceUtils.getLoggedInFocus() instanceof UserType) {
            addMainMenuItem(menu, GuiStyleConstants.CLASS_ICON_REQUEST, "PageAdmin.menu.request",
                    PageAssignmentShoppingCart.class);
        }

        //GDPR feature.. temporary disabled MID-4281
        if (WebModelServiceUtils.isEnableExperimentalFeature(this)) {
            addMainMenuItem(menu, GuiStyleConstants.CLASS_ICON_CONSENT, "PageAdmin.menu.consent",
                    PageSelfConsents.class);
        }
    }

    private void createAdditionalMenu(SideBarMenuItem menu) {
        CompiledGuiProfile userProfile = getCompiledGuiProfile();
        List<RichHyperlinkType> menuList = userProfile.getAdditionalMenuLink();

        Map<String, Class> urlClassMap = DescriptorLoader.getUrlClassMap();
        if (menuList != null && menuList.size() > 0 && urlClassMap != null && urlClassMap.size() > 0) {
            for (RichHyperlinkType link : menuList) {
                if (link.getTargetUrl() != null && !link.getTargetUrl().trim().equals("")) {
                    AdditionalMenuItem item = new AdditionalMenuItem(link.getIcon() == null ? "" : link.getIcon().getCssClass(),
                            new Model<String>(link.getLabel()),
                            link.getTargetUrl(), urlClassMap.get(link.getTargetUrl()));
                    menu.getItems().add(item);
                }
            }
        }
    }

    private MainMenuItem createHomeItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.CLASS_DASHBOARD_ICON,
                createStringResource("PageAdmin.menu.dashboard"), null);

        addMenuItem(item, "PageAdmin.menu.dashboard.info", PageDashboardInfo.class);

        OperationResult result = new OperationResult("Search Dashboard");
        List<PrismObject<DashboardType>> dashboards = WebModelServiceUtils.searchObjects(DashboardType.class, null, result, this);
        dashboards.forEach(prismObject -> {
            Validate.notNull(prismObject, "PrismObject<Dashboard> is null");
            DashboardType dashboard = prismObject.getRealValue();
            Validate.notNull(dashboard, "Dashboard object is null");

            StringResourceModel label;
            if(dashboard.getDisplay() != null && dashboard.getDisplay().getLabel() != null) {
                label = createStringResource(dashboard.getDisplay().getLabel().getOrig());
            } else {
                label = createStringResource(dashboard.getName());
            }
            PageParameters pageParameters = new PageParameters();
            pageParameters.add(OnePageParameterEncoder.PARAMETER, dashboard.getOid());
            MenuItem menu = new MenuItem(label, "", PageDashboardConfigurable.class, pageParameters, null, null){
                @Override
                protected boolean isMenuActive() {
                    StringValue dashboardOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
                    return dashboard.getOid().equals(dashboardOid.toString());
                }
            };
            item.getItems().add(menu);
        });

        return item;
    }

    private MainMenuItem createUsersItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.CLASS_OBJECT_USER_ICON_COLORED,
                createStringResource("PageAdmin.menu.top.users"), null);

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_USERS_URL,
                AuthorizationConstants.AUTZ_UI_USERS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL)) {

            addObjectListPageMenuItem(item, "PageAdmin.menu.top.users.list", GuiStyleConstants.CLASS_OBJECT_USER_ICON, PageUsers.class);
        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_USERS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_UI_USERS_VIEW_URL)) {

            addCollectionsMenuItems(item.getItems(), UserType.COMPLEX_TYPE, PageUsers.class);
        }
        createFocusPageNewEditMenu(item.getItems(), "PageAdmin.menu.top.users.new",
                "PageAdmin.menu.top.users.edit", PageUser.class, true);
        return item;
    }

    private void createFocusPageNewEditMenu(List<MenuItem> submenu, String newKey, String editKey,
                                            final Class<? extends PageAdmin> newPageClass, boolean checkAuthorization) {
        MenuItem edit = new MenuItem(createStringResource(editKey), newPageClass, null, new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public boolean isVisible() {
                if (!getPage().getClass().equals(newPageClass)) {
                    return false;
                }

                if (getPage() instanceof PageAdminObjectDetails) {
                    PageAdminObjectDetails page = (PageAdminObjectDetails) getPage();
                    return page.isOidParameterExists() || page.isEditingFocus();
                } else if (getPage() instanceof PageResourceWizard) {
                    PageResourceWizard page = (PageResourceWizard) getPage();
                    return !page.isNewResource();
                } else {
                    return false;
                }
            }
        });
        submenu.add(edit);
        MenuItem newMenu = new MenuItem(createStringResource(newKey), GuiStyleConstants.CLASS_PLUS_CIRCLE, newPageClass, null, new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !checkAuthorization || isMenuItemAuthorized(newPageClass);
            }
        }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean isMenuActive() {
                if (!PageBase.this.getPage().getClass().equals(newPageClass)) {
                    return false;
                }

                if (PageBase.this.getPage() instanceof PageAdminObjectDetails) {
                    PageAdminObjectDetails page = (PageAdminObjectDetails) PageBase.this.getPage();
                    return !page.isOidParameterExists() && !page.isEditingFocus();
                } else if (PageBase.this.getPage() instanceof PageResourceWizard) {
                    PageResourceWizard page = (PageResourceWizard) PageBase.this.getPage();
                    return page.isNewResource();
                } else {
                    return false;
                }
            }
        };
        submenu.add(newMenu);
    }

    private boolean isMenuItemAuthorized(Class<? extends PageAdmin> newPageClass) {
        try {
            ObjectType object = null;
            if (PageOrgUnit.class.equals(newPageClass)) {
                object = new OrgType(getPrismContext());
            } else if (PageUser.class.equals(newPageClass)) {
                object = new UserType(getPrismContext());
            } else if (PageRole.class.equals(newPageClass)) {
                object = new RoleType(getPrismContext());
            } else if (PageService.class.equals(newPageClass)) {
                object = new ServiceType(getPrismContext());
            }

            // TODO: the modify authorization here is probably wrong.
            // It is a model autz. UI autz should be here instead?
            return isAuthorized(ModelAuthorizationAction.ADD.getUrl(),
                    AuthorizationPhaseType.REQUEST, object == null ? null : object.asPrismObject(),
                    null, null, null);
        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't solve authorization for New organization menu item", ex);
        }
        return false;
    }

    private void  createFocusPageViewMenu(List<MenuItem> submenu, String viewKey,
                                         final Class<? extends PageBase> newPageType) {
        MenuItem view = new MenuItem(createStringResource(viewKey), newPageType, null, new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public boolean isVisible() {
                if (!getPage().getClass().equals(newPageType)) {
                    return false;
                }

                return true;
            }
        });
        submenu.add(view);
    }

    private MainMenuItem createOrganizationsMenu() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.CLASS_OBJECT_ORG_ICON_COLORED,
                createStringResource("PageAdmin.menu.top.users.org"), null);

        MenuItem orgTree = new MenuItem(createStringResource("PageAdmin.menu.top.users.org.tree"),
                GuiStyleConstants.CLASS_OBJECT_ORG_ICON, PageOrgTree.class);
        item.getItems().add(orgTree);
        //todo should we have org list page for collection/archetype view?
//        addCollectionsMenuItems(item.getItems(), OrgType.COMPLEX_TYPE);

        createFocusPageNewEditMenu(item.getItems(), "PageAdmin.menu.top.users.org.new", "PageAdmin.menu.top.users.org.edit",
                PageOrgUnit.class, true);

        return item;
    }

    private MainMenuItem createRolesItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON_COLORED,
                createStringResource("PageAdmin.menu.top.roles"), null);

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ROLES_URL,
                AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL)) {

            addObjectListPageMenuItem(item, "PageAdmin.menu.top.roles.list", GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, PageRoles.class);
        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_UI_ROLES_VIEW_URL)) {
            addCollectionsMenuItems(item.getItems(), RoleType.COMPLEX_TYPE, PageRoles.class);
        }

        createFocusPageNewEditMenu(item.getItems(), "PageAdmin.menu.top.roles.new", "PageAdmin.menu.top.roles.edit",
                PageRole.class, true);

        return item;
    }

    private MainMenuItem createServicesItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON_COLORED,
                createStringResource("PageAdmin.menu.top.services"), null);

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_SERVICES_URL,
                AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL)) {
            addObjectListPageMenuItem(item, "PageAdmin.menu.top.services.list", GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON, PageServices.class);
        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_UI_SERVICES_VIEW_URL)) {
            addCollectionsMenuItems(item.getItems(), ServiceType.COMPLEX_TYPE, PageServices.class);
        }
        createFocusPageNewEditMenu(item.getItems(), "PageAdmin.menu.top.services.new", "PageAdmin.menu.top.services.edit",
                PageService.class, true);

        return item;
    }

    private MainMenuItem createArchetypesItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON,
                createStringResource("PageAdmin.menu.top.archetypes"), null);
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ARCHETYPES_URL,
                AuthorizationConstants.AUTZ_UI_ARCHETYPES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL)) {
            addObjectListPageMenuItem(item, "PageAdmin.menu.top.archetypes.list", GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON, PageArchetypes.class);
        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ARCHETYPES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_UI_ARCHETYPES_VIEW_URL)) {
            addCollectionsMenuItems(item.getItems(), ArchetypeType.COMPLEX_TYPE, PageArchetypes.class);
        }

        createFocusPageNewEditMenu(item.getItems(), "PageAdmin.menu.top.archetypes.new", "PageAdmin.menu.top.archetypes.edit",
                PageArchetype.class, true);

        return item;
    }

    private void addObjectListPageMenuItem(MainMenuItem item, String key, String iconClass, Class<? extends PageBase> menuItemPage){
        MenuItem menu = new MenuItem(createStringResource(key),  iconClass, menuItemPage){
            @Override
            public boolean isMenuActive(WebPage page) {
                if (!page.getClass().equals(this.getPageClass()) || getPageParameters() != null && getPageParameters().get(PARAMETER_OBJECT_COLLECTION_NAME) != null
                        && StringUtils.isNotEmpty(getPageParameters().get(PARAMETER_OBJECT_COLLECTION_NAME).toString())
                        && !getPageParameters().get(PARAMETER_OBJECT_COLLECTION_NAME).toString().equals("null")){
                    return false;
                } else {
                    return super.isMenuActive(page);
                }
            }
        };
        item.getItems().add(menu);
    }

    private void addCollectionsMenuItems(List<MenuItem> menu, QName type, Class<? extends PageBase> redirectToPage) {
        List<CompiledObjectCollectionView> objectViews = getCompiledGuiProfile().findAllApplicableObjectCollectionViews(type);
        List<MenuItem> collectionMenuItems = new ArrayList<>(objectViews.size());
        objectViews.forEach(objectView -> {
            CollectionRefSpecificationType collectionRefSpec = objectView.getCollection();
            if (collectionRefSpec == null) {
                return;
            }

            ObjectReferenceType collectionRef = collectionRefSpec.getCollectionRef();
            if (collectionRef == null) {
                return;
            }

            OperationResult result = new OperationResult(OPERATION_LOAD_VIEW_COLLECTION_REF);
            Task task = createSimpleTask(OPERATION_LOAD_VIEW_COLLECTION_REF);
            PrismObject<? extends ObjectType> collectionObject = WebModelServiceUtils.resolveReferenceNoFetch(collectionRef, this,
                    task, result);
            if (collectionObject == null) {
                return;
            }
            ObjectType objectType = collectionObject.asObjectable();
            if (!(objectType instanceof ArchetypeType) && !(objectType instanceof ObjectCollectionType)) {
                return;
            }
            DisplayType viewDisplayType = objectView.getDisplay();

            PageParameters pageParameters = new PageParameters();
            pageParameters.add(PARAMETER_OBJECT_COLLECTION_NAME, objectView.getViewIdentifier());

            MenuItem userViewMenu = new MenuItem(
                    createStringResourceDefault("MenuItem.noName", WebComponentUtil.getCollectionLabel(viewDisplayType, collectionRefSpec, objectType)),
                    WebComponentUtil.getIconCssClass(viewDisplayType), redirectToPage, pageParameters, null) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isMenuActive(WebPage page) {
                    PageParameters params = getPageParameters();
                    if (params != null && params.get(PARAMETER_OBJECT_COLLECTION_NAME) != null){
                        StringValue collectionName = params.get(PARAMETER_OBJECT_COLLECTION_NAME);
                        if (objectView.getViewIdentifier().equals(collectionName.toString())) {
                            return true;
                        }
                    }
                    return false;
                }
            };
            userViewMenu.setDisplayOrder(objectView.getDisplayOrder());
            collectionMenuItems.add(userViewMenu);
        });

        // We need to sort after we get all the collections. Only then we have correct collection labels.
        // We do not want to determine the labels twice.

        // TODO: can this be combined in a single sort?
        collectionMenuItems.sort(Comparator.comparing(o -> o.getNameModel().getObject()));
        collectionMenuItems.sort(Comparator.comparingInt(o -> ObjectUtils.defaultIfNull(o.getDisplayOrder(), Integer.MAX_VALUE)));
        menu.addAll(collectionMenuItems);
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

    private VisibleEnableBehaviour createVisibleDisabledBehaviorForEditMenu(final Class<? extends WebPage> page) {
        return new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return getPage().getClass().equals(page);
            }

            @Override
            public boolean isEnabled() {
                return false;
            }
        };
    }

    public boolean canRedirectBack() {
        return canRedirectBack(DEFAULT_BREADCRUMB_STEP);
    }

    /**
     * checks if it's possible to make backStep steps back
     *
     * @param backStep
     * @return
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
     * @return
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

    protected void setTimeZone(PageBase page) {
        PrismObject<? extends FocusType> focus = loadFocusSelf();
        String timeZone = null;
        GuiProfiledPrincipal principal = SecurityUtils.getPrincipalUser();
        if (focus != null && focus.asObjectable().getTimezone() != null) {
            timeZone = focus.asObjectable().getTimezone();
        } else if (principal != null && principal.getCompiledGuiProfile() != null) {
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

    @NotNull public PrismObject<UserType> getAdministratorPrivileged(OperationResult parentResult) throws CommonException {
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

    public <ID extends ItemDefinition> ItemWrapperFactory<?, ?, ?> findWrapperFactory(ID def) {
        return registry.findWrapperFactory(def);
    }

    public <IW extends ItemWrapper, VW extends PrismValueWrapper, PV extends PrismValue> VW createValueWrapper(IW parentWrapper, PV newValue, ValueStatus status, WrapperContext context) throws SchemaException {

        ItemWrapperFactory<IW, VW, PV> factory = (ItemWrapperFactory<IW, VW, PV>) registry.findWrapperFactory(parentWrapper);

        return factory.createValueWrapper(parentWrapper, newValue, status, context);

    }

    public <ID extends ItemDefinition, IW extends ItemWrapper> IW createItemWrapper(ID def, PrismContainerValueWrapper<?> parent, WrapperContext ctx) throws SchemaException {

        ItemWrapperFactory<IW, ?,?> factory = (ItemWrapperFactory<IW, ?,?>) registry.findWrapperFactory(def);
        ctx.setShowEmpty(true);
        ctx.setCreateIfEmpty(true);
        return factory.createWrapper(parent, def, ctx);

    }

    public <I extends Item, IW extends ItemWrapper> IW createItemWrapper(I item, ItemStatus status, WrapperContext ctx) throws SchemaException {

        ItemWrapperFactory<IW, ?,?> factory = (ItemWrapperFactory<IW, ?,?>) registry.findWrapperFactory(item.getDefinition());

        ctx.setCreateIfEmpty(true);
        return factory.createWrapper(item, status, ctx);

    }

    private Class<?> getWrapperPanel(QName typeName) {
        return registry.getPanelClass(typeName);
    }


    public <IW extends ItemWrapper> Panel initItemPanel(String panelId, QName typeName, IModel<IW> wrapperModel, ItemPanelSettings itemPanelSettings) throws SchemaException{
        Class<?> panelClass = getWrapperPanel(typeName);
        if (panelClass == null) {
            ErrorPanel errorPanel = new ErrorPanel(panelId, () -> "Cannot create panel for " + typeName);
            errorPanel.add(new VisibleBehaviour(() -> getApplication().usesDevelopmentConfig()));
            return errorPanel;
        }

        Constructor<?> constructor;
        try {
            constructor = panelClass.getConstructor(String.class, IModel.class, ItemPanelSettings.class);
            Panel panel = (Panel) constructor.newInstance(panelId, wrapperModel, itemPanelSettings);
            return panel;
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new SystemException("Cannot instantiate " + panelClass, e);
        }
    }

    public <CVW extends PrismContainerValueWrapper<C>, C extends Containerable> Panel initContainerValuePanel(String id, IModel<CVW> model,
            ItemPanelSettings settings) {
        //TODO find from registry first
        return new PrismContainerValuePanel<>(id, model, settings);
    }

    public Clock getClock() {
        return clock;
    }

    private SideBarMenuPanel getSideBarMenuPanel(){
        return (SideBarMenuPanel) get(ID_SIDEBAR_MENU);
    }
}
