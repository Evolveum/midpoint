/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.api.page;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.LocalizationService;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.SubscriptionType;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.common.CacheRegistry;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskManager;
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
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.cases.PageCase;
import com.evolveum.midpoint.web.page.admin.cases.PageCaseWorkItem;
import com.evolveum.midpoint.web.page.admin.cases.PageCaseWorkItemsAll;
import com.evolveum.midpoint.web.page.admin.cases.PageCaseWorkItemsAllocatedToMe;
import com.evolveum.midpoint.web.page.admin.cases.PageCasesAll;
import com.evolveum.midpoint.web.page.admin.cases.PageCasesAllocatedToMe;
import com.evolveum.midpoint.web.page.admin.certification.PageCertCampaigns;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDecisions;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDefinition;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDefinitions;
import com.evolveum.midpoint.web.page.admin.configuration.*;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.reports.*;
import com.evolveum.midpoint.web.page.admin.resources.*;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.PageTasksCertScheduling;
import com.evolveum.midpoint.web.page.admin.services.PageService;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.admin.valuePolicy.PageValuePolicies;
import com.evolveum.midpoint.web.page.admin.valuePolicy.PageValuePolicy;
import com.evolveum.midpoint.web.page.admin.workflow.*;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.page.self.*;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.NewWindowNotifyingBehavior;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorRegistry;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.util.QueryUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.*;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessages;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.io.Serializable;
import java.util.*;

/**
 * @author lazyman
 * @author semancik
 */
public abstract class PageBase extends WebPage implements ModelServiceLocator {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageBase.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
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
    private static final String ID_SIDEBAR_MENU = "sidebarMenu";
    private static final String ID_RIGHT_MENU = "rightMenu";
    private static final String ID_LOCALE = "locale";
    private static final String ID_MENU_TOGGLE = "menuToggle";
    private static final String ID_BREADCRUMBS = "breadcrumbs";
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
    
    @SpringBean(name = "modelController")
    private CaseManagementService caseManagementService;

    @SpringBean(name = "accessDecisionManager")
    private SecurityEnforcer securityEnforcer;
    
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

    private List<Breadcrumb> breadcrumbs;

    private boolean initialized = false;

    private LoadableModel<Integer> workItemCountModel;
    private LoadableModel<Integer> certWorkItemCountModel;
    private LoadableModel<DeploymentInformationType> deploymentInfoModel;

    // No need to store this in the session. Retrieval is cheap.
    private transient AdminGuiConfigurationType adminGuiConfiguration;

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
                    S_FilterEntryOrEmpty q = QueryBuilder.queryFor(WorkItemType.class, getPrismContext());
                    ObjectQuery query = QueryUtils.filterForAssignees(q, getPrincipal(),
                            OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS).build();
                    return getModelService().countContainers(WorkItemType.class, query, null, task, task.getResult());
                } catch (SchemaException | SecurityViolationException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
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
                    return acs.countOpenWorkItems(new ObjectQuery(), true, null, task, result);
                } catch (SchemaException | SecurityViolationException | ObjectNotFoundException
                        | ConfigurationException | CommunicationException | ExpressionEvaluationException e) {
                    LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't load certification work item count", e);
                    return null;
                }
            }
        };
        deploymentInfoModel = new LoadableModel<DeploymentInformationType>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected DeploymentInformationType load() {
                return loadDeploymentInformationType();
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
        BreadcrumbPageClass bc = new BreadcrumbPageClass(new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getPageTitleModel().getObject();
            }
        }, this.getClass(), getPageParameters());

        addBreadcrumb(bc);
    }

    protected void createInstanceBreadcrumb() {
        BreadcrumbPageInstance bc = new BreadcrumbPageInstance(new AbstractReadOnlyModel<String>() {
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

    public PrismContext getPrismContext() {
        return getMidpointApplication().getPrismContext();
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


    public AccessCertificationService getCertificationService() {
        return certficationService;
    }
    
    public CaseManagementService getCaseManagementService() {
		return caseManagementService;
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

    protected ModelDiagnosticService getModelDiagnosticService() {
        return modelDiagnosticService;
    }
    
    public CacheDispatcher getCacheDispatcher() {
    		return cacheDispatcher;
    }

    @NotNull
    @Override
    public AdminGuiConfigurationType getAdminGuiConfiguration() {
        if (adminGuiConfiguration == null) {
            Task task = createSimpleTask(PageBase.DOT_CLASS + "getAdminGuiConfiguration");
            try {
                adminGuiConfiguration = modelInteractionService.getAdminGuiConfiguration(task, task.getResult());
            } catch (ObjectNotFoundException | SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot retrieve admin GUI configuration", e);
                if (InternalsConfig.nonCriticalExceptionsAreFatal()) {
                    throw new SystemException("Cannot retrieve admin GUI configuration: " + e.getMessage(), e);
                } else {
                    // Just return empty admin GUI config, so the GUI can go on (and the problem may get fixed)
                    return new AdminGuiConfigurationType();
                }
            }
        }
        return adminGuiConfiguration;
    }

    @Override
    public Task getPageTask() {
        if (pageTask == null) {
            pageTask = createSimpleTask(this.getClass().getName());
        }
        return pageTask;
    }
    
    public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	return isAuthorized(operationUrl, null, null, null, null, null);
    }
    
    public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl, AuthorizationPhaseType phase,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	Task task = getPageTask();
    	AuthorizationParameters<O,T> params = new AuthorizationParameters.Builder<O,T>()
    		.object(object)
    		.delta(delta)
    		.target(target)
    		.build();
    	return getSecurityEnforcer().isAuthorized(operationUrl, phase, params, ownerResolver, task, task.getResult());
    }

    public <O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, AuthorizationPhaseType phase,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver, OperationResult result)
			throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
    	AuthorizationParameters<O,T> params = new AuthorizationParameters.Builder<O,T>()
        		.object(object)
        		.delta(delta)
        		.target(target)
        		.build();
    	getSecurityEnforcer().authorize(operationUrl, phase, params, ownerResolver, getPageTask(), result);
    }

    public <O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, AuthorizationPhaseType phase,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver)
			throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
    	Task task = getPageTask();
    	AuthorizationParameters<O,T> params = new AuthorizationParameters.Builder<O,T>()
        		.object(object)
        		.delta(delta)
        		.target(target)
        		.build();
    	getSecurityEnforcer().authorize(operationUrl, phase, params, ownerResolver, task, task.getResult());
    }

    
    public MidpointFormValidatorRegistry getFormValidatorRegistry() {
        return formValidatorRegistry;
    }

    public MidPointPrincipal getPrincipal() {
        return SecurityUtils.getPrincipalUser();
    }

    public static StringResourceModel createStringResourceStatic(Component component, Enum e) {
        String resourceKey = createEnumResourceKey(e);
        return createStringResourceStatic(component, resourceKey);
    }

    public static String createEnumResourceKey(Enum e) {
        return e.getDeclaringClass().getSimpleName() + "." + e.name();
    }

    public Task createAnonymousTask(String operation) {
        TaskManager manager = getTaskManager();
        Task task = manager.createTaskInstance(operation);

        task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);

        return task;
    }

    public Task createSimpleTask(String operation) {
        MidPointPrincipal user = SecurityUtils.getPrincipalUser();
        if (user == null) {
            throw new RestartResponseException(PageLogin.class);
        }
        return WebModelServiceUtils.createSimpleTask(operation, user.getUser().asPrismObject(), getTaskManager());
    }

    public MidpointConfiguration getMidpointConfiguration() {
        return midpointConfiguration;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        String skinCssString = CLASS_DEFAULT_SKIN;
        if (deploymentInfoModel != null && deploymentInfoModel.getObject() != null &&
                StringUtils.isNotEmpty(deploymentInfoModel.getObject().getSkin())) {
            skinCssString = deploymentInfoModel.getObject().getSkin();
        }

        String skinCssPath = String.format("../../../../../../webjars/adminlte/2.3.11/dist/css/skins/%s.min.css", skinCssString);
        response.render(CssHeaderItem.forReference(
                new CssResourceReference(
                        PageBase.class, skinCssPath)
                )
        );
        // this attaches jquery.js as first header item, which is used in our
        // scripts.
        CoreLibrariesContributor.contribute(getApplication(), response);

//		response.render(JavaScriptHeaderItem.forScript("alert(window.name);", "windowNameScript"));
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

        UserMenuPanel rightMenu = new UserMenuPanel(ID_RIGHT_MENU);
        rightMenu.add(createUserStatusBehaviour(true));
        container.add(rightMenu);

        LocalePanel locale = new LocalePanel(ID_LOCALE);
        locale.add(createUserStatusBehaviour(false));
        container.add(locale);
    }

    private void initTitleLayout(WebMarkupContainer mainHeader) {
        WebMarkupContainer pageTitleContainer = new WebMarkupContainer(ID_PAGE_TITLE_CONTAINER);
        pageTitleContainer.add(createUserStatusBehaviour(true));
        mainHeader.add(pageTitleContainer);

        WebMarkupContainer pageTitle = new WebMarkupContainer(ID_PAGE_TITLE);
        pageTitleContainer.add(pageTitle);

        String environmentName = "";
        if (deploymentInfoModel != null && deploymentInfoModel.getObject() != null &&
                StringUtils.isNotEmpty(deploymentInfoModel.getObject().getName())) {
            environmentName = deploymentInfoModel.getObject().getName();
        }
        Model<String> deploymentNameModel = new Model<>(StringUtils.isNotEmpty(environmentName) ? environmentName + ": " : "");
        Label deploymentName = new Label(ID_DEPLOYMENT_NAME, deploymentNameModel);
        deploymentName.add(new VisibleEnableBehaviour() {
            public boolean isVisible() {
                return StringUtils.isNotEmpty(deploymentNameModel.getObject());
            }
        });
        deploymentName.setRenderBodyOnly(true);
        pageTitle.add(deploymentName);

        Label pageTitleReal = new Label(ID_PAGE_TITLE_REAL, createPageTitleModel());
        pageTitleReal.setRenderBodyOnly(true);
        pageTitle.add(pageTitleReal);

        ListView breadcrumbs = new ListView<Breadcrumb>(ID_BREADCRUMB,
                new AbstractReadOnlyModel<List<Breadcrumb>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public List<Breadcrumb> getObject() {
                        return getBreadcrumbs();
                    }
                }) {

            @Override
            protected void populateItem(ListItem<Breadcrumb> item) {
                final Breadcrumb dto = item.getModelObject();

                AjaxLink bcLink = new AjaxLink(ID_BC_LINK) {
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
        mainHeader.add(breadcrumbs);
    }

    private void initLayout() {
        TransparentWebMarkupContainer body = new TransparentWebMarkupContainer(ID_BODY);
        body.add(new AttributeAppender("class", "hold-transition ", " "));
        body.add(new AttributeAppender("class", "custom-hold-transition ", " "));

        Boolean usingSkin = deploymentInfoModel != null && deploymentInfoModel.getObject() != null &&
                StringUtils.isNotEmpty(deploymentInfoModel.getObject().getSkin());

        if (usingSkin) {
            body.add(new AttributeAppender("class", deploymentInfoModel.getObject().getSkin(), " "));
        } else {
            body.add(new AttributeAppender("class", CLASS_DEFAULT_SKIN, " "));
        }
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

        AjaxLink customLogo = new AjaxLink(ID_CUSTOM_LOGO) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                //TODO may be this should lead to customerUrl ?
                Class<? extends Page> page = MidPointApplication.get().getHomePage();
                setResponsePage(page);
            }
        };
        customLogo.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return isCustomLogoVisible();
            }
        });
        mainHeader.add(customLogo);

        WebMarkupContainer navigation = new WebMarkupContainer(ID_NAVIGATION);
        mainHeader.add(navigation);


        WebMarkupContainer customLogoImgSrc = new WebMarkupContainer(ID_CUSTOM_LOGO_IMG_SRC);
        WebMarkupContainer customLogoImgCss = new WebMarkupContainer(ID_CUSTOM_LOGO_IMG_CSS);
        if (deploymentInfoModel != null && deploymentInfoModel.getObject() != null &&
                deploymentInfoModel.getObject().getLogo() != null) {
            if (StringUtils.isNotEmpty(deploymentInfoModel.getObject().getLogo().getCssClass())) {
                customLogoImgCss.add(new AttributeAppender("class", deploymentInfoModel.getObject().getLogo().getCssClass()));
                customLogoImgSrc.setVisible(false);
            } else {
                customLogoImgSrc.add(new AttributeAppender("src",
                        deploymentInfoModel.getObject().getLogo().getImageUrl()));
                customLogoImgCss.setVisible(false);
            }
            mainHeader.add(new AttributeAppender("style",
                    "background-color: " + GuiStyleConstants.DEFAULT_BG_COLOR + "; !important;"));
        }
        customLogo.add(customLogoImgSrc);
        customLogo.add(customLogoImgCss);

        Label title = new Label(ID_TITLE, createPageTitleModel());
        title.setRenderBodyOnly(true);
        add(title);

        initHeaderLayout(navigation);
        initTitleLayout(navigation);

        if (deploymentInfoModel != null && deploymentInfoModel.getObject() != null &&
                StringUtils.isNotEmpty(deploymentInfoModel.getObject().getHeaderColor())) {
            logo.add(new AttributeAppender("style",
                    "background-color: " + deploymentInfoModel.getObject().getHeaderColor() + "; !important;"));
            customLogo.add(new AttributeAppender("style",
                    "background-color: " + deploymentInfoModel.getObject().getHeaderColor() + "; !important;"));
            mainHeader.add(new AttributeAppender("style",
                    "background-color: " + deploymentInfoModel.getObject().getHeaderColor() + "; !important;"));
            //using a skin overrides the navigation color
            if (!usingSkin) {
                navigation.add(new AttributeAppender("style",
                        "background-color: " + deploymentInfoModel.getObject().getHeaderColor() + "; !important;"));
            }
        }
        initDebugBarLayout();

        List<SideBarMenuItem> menuItems = createMenuItems();
        SideBarMenuPanel sidebarMenu = new SideBarMenuPanel(ID_SIDEBAR_MENU, new Model((Serializable) menuItems));
        sidebarMenu.add(createUserStatusBehaviour(true));
        add(sidebarMenu);

        WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
        footerContainer.setOutputMarkupId(true);
        footerContainer.add(getFooterVisibleBehaviour());
        add(footerContainer);

        WebMarkupContainer version = new WebMarkupContainer(ID_VERSION) {
            private static final long serialVersionUID = 1L;

            @Deprecated
            public String getDescribe() {
                return PageBase.this.getDescribe();
            }
        };
        version.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return RuntimeConfigurationType.DEVELOPMENT.equals(getApplication().getConfigurationType());
            }
        });
        footerContainer.add(version);

        WebMarkupContainer copyrightMessage = new WebMarkupContainer(ID_COPYRIGHT_MESSAGE);
        copyrightMessage.add(getFooterVisibleBehaviour());
        footerContainer.add(copyrightMessage);

        Label subscriptionMessage = new Label(ID_SUBSCRIPTION_MESSAGE,
                new AbstractReadOnlyModel<String>() {
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
        add(feedbackContainer);

        FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK);
        feedbackList.setOutputMarkupId(true);
        feedbackContainer.add(feedbackList);

        MainPopupDialog mainPopup = new MainPopupDialog(ID_MAIN_POPUP);
        mainPopup.setOutputMarkupId(true);
        add(mainPopup);
    }

    public MainPopupDialog getMainPopup() {
        return (MainPopupDialog) get(ID_MAIN_POPUP);
    }

    public String getMainPopupBodyId() {
        return ID_MAIN_POPUP_BODY;
    }

    public void setMainPopupTitle(IModel<String> title) {
        getMainPopup().setTitle(title);
    }

    public void showMainPopup(Popupable popupable, AjaxRequestTarget target) {
        getMainPopup().setBody(popupable);
        getMainPopup().show(target);
    }

    public void hideMainPopup(AjaxRequestTarget target) {
        getMainPopup().close(target);
    }

    private VisibleEnableBehaviour createUserStatusBehaviour(final boolean visibleIfLoggedIn) {
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isSideMenuVisible(visibleIfLoggedIn);
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
        String key = getClass().getSimpleName() + ".title";
        return createStringResource(key);
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

    public StringResourceModel createStringResource(Enum e) {
        String resourceKey = e.getDeclaringClass().getSimpleName() + "." + e.name();
        return createStringResource(resourceKey);
    }

    @NotNull
    public static StringResourceModel createStringResourceStatic(Component component, String resourceKey,
                                                                 Object... objects) {
        return new StringResourceModel(resourceKey, component).setModel(new Model<String>())
                .setDefaultValue(resourceKey).setParameters(objects);
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

    public OpResult showResult(OperationResult result, String errorMessageKey, boolean showSuccess) {
        Validate.notNull(result, "Operation result must not be null.");
        Validate.notNull(result.getStatus(), "Operation result status must not be null.");

        OperationResult scriptResult = executeResultScriptHook(result);
        if (scriptResult == null) {
            return null;
        }

        result = scriptResult;

        OpResult opResult = OpResult.getOpResult((PageBase) getPage(), result);
        opResult.determineBackgroundTaskVisibility(this);
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
        AdminGuiConfigurationType adminGuiConfiguration = getAdminGuiConfiguration();
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
            PrismPropertyDefinition<String> outputDefinition = new PrismPropertyDefinitionImpl<>(
                    ExpressionConstants.OUTPUT_ELEMENT_NAME, OperationResultType.COMPLEX_TYPE, getPrismContext());
            Expression expression = factory.makeExpression(expressionType, outputDefinition, contextDesc, task, topResult);

            ExpressionVariables variables = new ExpressionVariables();

            OperationResultType resultType = result.createOperationResultType();

            variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, resultType);

            ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, contextDesc, task, topResult);
            PrismValueDeltaSetTriple<PrismPropertyValue<?>> outputTriple = expression.evaluate(context);
            if (outputTriple == null) {
                return null;
            }

            Collection<PrismPropertyValue<?>> values = outputTriple.getNonNegativeValues();
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
    protected void processResult(AjaxRequestTarget target, OperationResult result, boolean showSuccess) {
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

    protected ModalWindow createModalWindow(final String id, IModel<String> title, int width, int height) {
        final ModalWindow modal = new ModalWindow(id);
        add(modal);

        modal.setResizable(false);
        modal.setTitle(title);
        modal.setCookieName(PageBase.class.getSimpleName() + ((int) (Math.random() * 100)));

        modal.setInitialWidth(width);
        modal.setWidthUnit("px");
        modal.setInitialHeight(height);
        modal.setHeightUnit("px");

        modal.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                return true;
            }
        });

        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            @Override
            public void onClose(AjaxRequestTarget target) {
                modal.close(target);
            }
        });

        modal.add(new AbstractDefaultAjaxBehavior() {

            @Override
            public void renderHead(Component component, IHeaderResponse response) {
                response.render(OnDomReadyHeaderItem.forScript("Wicket.Window.unloadConfirmation = false;"));
                response.render(JavaScriptHeaderItem
                        .forScript("$(document).ready(function() {\n" + "  $(document).bind('keyup', function(evt) {\n"
                                + "    if (evt.keyCode == 27) {\n" + getCallbackScript() + "\n"
                                + "        evt.preventDefault();\n" + "    }\n" + "  });\n" + "});", id));
            }

            @Override
            protected void respond(AjaxRequestTarget target) {
                modal.close(target);
            }
        });

        return modal;
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
                result.recordFatalError("Couldn't parse object: " + e.getMessage(), e);
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
        Validator validator = new Validator(getPrismContext(), handler);
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
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createUsersItems());
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ORG_STRUCT_URL,
                AuthorizationConstants.AUTZ_UI_ORG_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createOrganizationsMenu());
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ROLES_URL,
                AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createRolesItems());
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_SERVICES_URL,
                AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createServicesItems());
        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_RESOURCES_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL, AuthorizationConstants.AUTZ_UI_RESOURCE_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCE_EDIT_URL)) {
            items.add(createResourcesItems());
        }
 //TODO uncomment after ValuePolicies pages are finished
//        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_VALUE_POLICIES_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
//                AuthorizationConstants.AUTZ_UI_VALUE_POLICIES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
//            items.add(createValuePolicieItems());
//        }

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_MY_WORK_ITEMS_URL,
                AuthorizationConstants.AUTZ_UI_ATTORNEY_WORK_ITEMS_URL,
                AuthorizationConstants.AUTZ_UI_CASES_ALLOCATED_TO_ME_URL, AuthorizationConstants.AUTZ_UI_CASES_ALL_URL,
                AuthorizationConstants.AUTZ_UI_CASE_WORK_ITEMS_ALLOCATED_TO_ME_URL, AuthorizationConstants.AUTZ_UI_CASE_WORK_ITEMS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_APPROVALS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
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

        MainMenuItem systemItem = addMainMenuItem(item, "fa fa-cog", "PageAdmin.menu.top.configuration.basic", null);

        addSystemMenuItem(systemItem, "PageAdmin.menu.top.configuration.basic",
                PageSystemConfiguration.CONFIGURATION_TAB_BASIC);
        addSystemMenuItem(systemItem, "PageAdmin.menu.top.configuration.notifications",
                PageSystemConfiguration.CONFIGURATION_TAB_NOTIFICATION);
        addSystemMenuItem(systemItem, "PageAdmin.menu.top.configuration.logging",
                PageSystemConfiguration.CONFIGURATION_TAB_LOGGING);
        addSystemMenuItem(systemItem, "PageAdmin.menu.top.configuration.profiling",
                PageSystemConfiguration.CONFIGURATION_TAB_PROFILING);
        addSystemMenuItem(systemItem, "PageAdmin.menu.top.configuration.adminGui",
                PageSystemConfiguration.CONFIGURATION_TAB_ADMIN_GUI);

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
        MenuItem menu = new MenuItem(createStringResource(key), page);
        item.getItems().add(menu);
    }

    private MainMenuItem createWorkItemsItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_ICON_COLORED,
                createStringResource("PageAdmin.menu.top.workItems"), null) {

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

        addMenuItem(item, "PageAdmin.menu.top.workItems.list", PageWorkItemsAllocatedToMe.class);
        addMenuItem(item, "PageAdmin.menu.top.workItems.listClaimable", PageWorkItemsClaimable.class);
        addMenuItem(item, "PageAdmin.menu.top.workItems.listAttorney", PageAttorneySelection.class);
        addMenuItem(item, "PageAdmin.menu.top.workItems.listAll", PageWorkItemsAll.class);
        addMenuItem(item, "PageAdmin.menu.top.workItems.listProcessInstancesRequestedBy", PageProcessInstancesRequestedBy.class);
        addMenuItem(item, "PageAdmin.menu.top.workItems.listProcessInstancesRequestedFor", PageProcessInstancesRequestedFor.class);
        addMenuItem(item, "PageAdmin.menu.top.workItems.listProcessInstancesAll", PageProcessInstancesAll.class);
        addMenuItem(item, "PageAdmin.menu.top.cases.list", PageCasesAllocatedToMe.class);
        addMenuItem(item, "PageAdmin.menu.top.cases.listAll", PageCasesAll.class);
        addMenuItem(item, "PageAdmin.menu.top.caseWorkItems.list", PageCaseWorkItemsAllocatedToMe.class);
        addMenuItem(item, "PageAdmin.menu.top.caseWorkItems.listAll", PageCaseWorkItemsAll.class);

        createFocusPageViewMenu(item.getItems(), "PageAdmin.menu.top.caseWorkItems.view", PageCaseWorkItem.class);

        addMenuItem(item, "PageAdmin.menu.top.case.new", PageCase.class);

        return item;
    }

    private MainMenuItem createServerTasksItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.CLASS_OBJECT_TASK_ICON_COLORED,
                createStringResource("PageAdmin.menu.top.serverTasks"), null);

        addMenuItem(item, "PageAdmin.menu.top.serverTasks.list", PageTasks.class);
        addMenuItem(item, "PageAdmin.menu.top.serverTasks.new", PageTaskAdd.class);

        MenuItem menuItem = new MenuItem(createStringResource("PageAdmin.menu.top.serverTasks.edit"),
                PageTaskEdit.class, null, createVisibleDisabledBehaviorForEditMenu(PageTaskEdit.class));
        item.getItems().add(menuItem);

        return item;
    }

    private MainMenuItem createResourcesItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON_COLORED,
                createStringResource("PageAdmin.menu.top.resources"), null);

        addMenuItem(item, "PageAdmin.menu.top.resources.list", PageResources.class);

        createFocusPageViewMenu(item.getItems(), "PageAdmin.menu.top.resources.view", PageResource.class);
        createFocusPageNewEditMenu(item.getItems(), "PageAdmin.menu.top.resources.new",
                "PageAdmin.menu.top.resources.edit", PageResourceWizard.class, false);

        addMenuItem(item, "PageAdmin.menu.top.resources.import", PageImportResource.class);
        addMenuItem(item, "PageAdmin.menu.top.connectorHosts.list", PageConnectorHosts.class);

        return item;
    }

    // Izolated until the referenced value policies pages are fully implemented
    private MainMenuItem createValuePolicieItems(){
        MainMenuItem item = new MainMenuItem("fa fa-asterisk", createStringResource("PageAdmin.menu.top.valuePolicies"),null);

        addMenuItem(item, "PageAdmin.menu.top.valuePolicies.list", PageValuePolicies.class);
        addMenuItem(item,"PageAdmin.menu.top.valuePolicies.new", PageValuePolicy.class);

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
        addMenuItem(item, "PageAdmin.menu.top.certification.newDefinition", PageCertDefinition.class);
        addMenuItem(item, "PageAdmin.menu.top.certification.campaigns", PageCertCampaigns.class);

        PageParameters params = new PageParameters();
        params.add(PageTasks.SELECTED_CATEGORY, TaskCategory.ACCESS_CERTIFICATION);
        MenuItem menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.scheduling"),
                PageTasksCertScheduling.class, params, null);
        item.getItems().add(menu);

        addMenuItem(item, "PageAdmin.menu.top.certification.decisions", PageCertDecisions.class);

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
                PageSelfProfile.class);
        addMainMenuItem(menu, GuiStyleConstants.CLASS_ICON_CREDENTIALS, "PageAdmin.menu.credentials",
                PageSelfCredentials.class);
        addMainMenuItem(menu, GuiStyleConstants.CLASS_ICON_REQUEST, "PageAdmin.menu.request",
                PageAssignmentShoppingKart.class);
        
        //GDPR feature.. temporary disabled MID-4281
//        addMainMenuItem(menu, GuiStyleConstants.CLASS_ICON_CONSENT, "PageAdmin.menu.consent",
//                PageSelfConsents.class);
    }

    private void createAdditionalMenu(SideBarMenuItem menu) {
        AdminGuiConfigurationType adminGuiConfig = loadAdminGuiConfiguration();
        List<RichHyperlinkType> menuList = loadAdminGuiConfiguration().getAdditionalMenuLink();

        Map<String, Class> urlClassMap = DescriptorLoader.getUrlClassMap();
        if (menuList != null && menuList.size() > 0 && urlClassMap != null && urlClassMap.size() > 0) {
            for (RichHyperlinkType link : menuList) {
                if (link.getTargetUrl() != null && !link.getTargetUrl().trim().equals("")) {
                    AdditionalMenuItem item = new AdditionalMenuItem(link.getIcon() == null ? "" : link.getIcon().getCssClass(),
                            getAdditionalMenuItemNameModel(link.getLabel()),
                            link.getTargetUrl(), urlClassMap.get(link.getTargetUrl()));
                    menu.getItems().add(item);
                }
            }
        }
    }

    private IModel<String> getAdditionalMenuItemNameModel(final String name) {
        return new IModel<String>() {
            @Override
            public String getObject() {
                return name;
            }

            @Override
            public void setObject(String s) {
            }

            @Override
            public void detach() {
            }
        };
    }

    private MainMenuItem createHomeItems() {
        MainMenuItem item = new MainMenuItem("fa fa-dashboard",
                createStringResource("PageAdmin.menu.dashboard"), PageDashboard.class);

        return item;
    }

    private MainMenuItem createUsersItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.CLASS_OBJECT_USER_ICON_COLORED,
                createStringResource("PageAdmin.menu.top.users"), null);

        addMenuItem(item, "PageAdmin.menu.top.users.list", PageUsers.class);

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

                if (getPage() instanceof PageAdminFocus) {
                    PageAdminFocus page = (PageAdminFocus) getPage();
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
        MenuItem newMenu = new MenuItem(createStringResource(newKey), newPageClass, null, new VisibleEnableBehaviour() {
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

                if (PageBase.this.getPage() instanceof PageAdminFocus) {
                    PageAdminFocus page = (PageAdminFocus) PageBase.this.getPage();
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

    private void createFocusPageViewMenu(List<MenuItem> submenu, String viewKey,
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

        addMenuItem(item, "PageAdmin.menu.top.users.org.tree", PageOrgTree.class);

        createFocusPageNewEditMenu(item.getItems(), "PageAdmin.menu.top.users.org.new", "PageAdmin.menu.top.users.org.edit",
                PageOrgUnit.class, true);

        return item;
    }

    private MainMenuItem createRolesItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON_COLORED,
                createStringResource("PageAdmin.menu.top.roles"), null);

        addMenuItem(item, "PageAdmin.menu.top.roles.list", PageRoles.class);

        createFocusPageNewEditMenu(item.getItems(), "PageAdmin.menu.top.roles.new", "PageAdmin.menu.top.roles.edit",
                PageRole.class, true);

        return item;
    }

    private MainMenuItem createServicesItems() {
        MainMenuItem item = new MainMenuItem(GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON_COLORED,
                createStringResource("PageAdmin.menu.top.services"), null);

        addMenuItem(item, "PageAdmin.menu.top.services.list", PageServices.class);

        createFocusPageNewEditMenu(item.getItems(), "PageAdmin.menu.top.services.new", "PageAdmin.menu.top.services.edit",
                PageService.class, true);

        return item;
    }

    public PrismObject<UserType> loadUserSelf() {
        Task task = createSimpleTask(OPERATION_LOAD_USER);
        OperationResult result = task.getResult();
        PrismObject<UserType> user = WebModelServiceUtils.loadObject(UserType.class,
                WebModelServiceUtils.getLoggedInUserOid(), PageBase.this, task, result);
        result.computeStatus();

        showResult(result, null, false);

        return user;
    }

    private VisibleEnableBehaviour createVisibleDisabledBehaviorForEditMenu(final Class<? extends WebPage> page) {
        return new VisibleEnableBehaviour() {

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

    @NotNull
    public AdminGuiConfigurationType loadAdminGuiConfiguration() {
        MidPointPrincipal user = SecurityUtils.getPrincipalUser();
        AdminGuiConfigurationType adminGuiConfig = new AdminGuiConfigurationType();
        if (user == null) {
            return adminGuiConfig;
        } else {
            OperationResult result = new OperationResult(OPERATION_GET_SYSTEM_CONFIG);
            Task task = createSimpleTask(OPERATION_GET_SYSTEM_CONFIG);
            try {
                adminGuiConfig = getModelInteractionService().getAdminGuiConfiguration(task, result);
                LOGGER.trace("Admin GUI config: {}", adminGuiConfig);
                result.recordSuccess();
            } catch (Exception ex) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load system configuration", ex);
                result.recordFatalError("Couldn't load system configuration.", ex);
            }
            return adminGuiConfig;
        }
    }

    public DeploymentInformationType loadDeploymentInformationType() {
        DeploymentInformationType deploymentInformationType = null;
        OperationResult result = new OperationResult(OPERATION_GET_DEPLOYMENT_INFORMATION);
        try {
            deploymentInformationType = getModelInteractionService().getDeploymentInformationConfiguration(result);
            LOGGER.trace("Deployment information : {}", deploymentInformationType);
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load deployment information", ex);
            result.recordFatalError("Couldn't load deployment information.", ex);
        }
        return deploymentInformationType;
    }

    public boolean canRedirectBack() {
        List<Breadcrumb> breadcrumbs = getBreadcrumbs();
        if (breadcrumbs.size() > 2) {
            return true;
        }
        if (breadcrumbs.size() == 2 && (breadcrumbs.get(breadcrumbs.size() - 2)) != null) {
                return true;
        }

        return false;
    }

    public Breadcrumb redirectBack() {
        List<Breadcrumb> breadcrumbs = getBreadcrumbs();
        if (!canRedirectBack()) {
            setResponsePage(getMidpointApplication().getHomePage());

            return null;
        }

        Breadcrumb breadcrumb = breadcrumbs.get(breadcrumbs.size() - 2);
        redirectBackToBreadcrumb(breadcrumb);
        return breadcrumb;
    }

    public void navigateToNext(Class<? extends WebPage> page) {
        navigateToNext(page, null);
    }

    public void navigateToNext(Class<? extends WebPage> pageType, PageParameters params) {
        IPageFactory pFactory = Session.get().getPageFactory();
        WebPage page;
        if (params == null) {
            page = pFactory.newPage(pageType);
        } else {
            page = pFactory.newPage(pageType, params);
        }

        navigateToNext(page);
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
        PrismObject<UserType> user = loadUserSelf();
        String timeZone = null;
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        if (user != null && user.asObjectable().getTimezone() != null) {
            timeZone = user.asObjectable().getTimezone();
        } else if (principal != null && principal.getAdminGuiConfiguration() != null) {
            timeZone = principal.getAdminGuiConfiguration().getDefaultTimezone();
        }
        if (timeZone != null) {
            WebSession.get().getClientInfo().getProperties().
                    setTimeZone(TimeZone.getTimeZone(timeZone));
        }
    }

    public <T> T runPrivileged(Producer<T> producer) {
        return securityContextManager.runPrivileged(producer);
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

    public void clearBreadcrumbs() {
        getBreadcrumbs().clear();
    }

    private boolean isCustomLogoVisible() {
        if (deploymentInfoModel != null && deploymentInfoModel.getObject() != null
                && deploymentInfoModel.getObject().getLogo() != null
                && (StringUtils.isNotEmpty(deploymentInfoModel.getObject().getLogo().getImageUrl())
                || StringUtils.isNotEmpty(deploymentInfoModel.getObject().getLogo().getCssClass()))) {
            return true;
        }
        return false;
    }
    
    protected boolean isLogoLinkEnabled() {
    	return true;
    }

    private String getSubscriptionId() {
        if (deploymentInfoModel == null || deploymentInfoModel.getObject() == null) {
            return null;
        }
        return deploymentInfoModel.getObject().getSubscriptionIdentifier();
    }

    private VisibleEnableBehaviour getFooterVisibleBehaviour() {
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                String subscriptionId = getSubscriptionId();
                if (StringUtils.isEmpty(subscriptionId)) {
                    return true;
                }
                return !WebComponentUtil.isSubscriptionIdCorrect(subscriptionId) ||
                        (SubscriptionType.DEMO_SUBSRIPTION.getSubscriptionType().equals(subscriptionId.substring(0, 2))
                                && WebComponentUtil.isSubscriptionIdCorrect(subscriptionId));
            }
        };
    }

    protected String determineDataLanguage() {
        AdminGuiConfigurationType config = loadAdminGuiConfiguration();
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

    public AsyncWebProcessManager getAsyncWebProcessManager() {
        return MidPointApplication.get().getAsyncWebProcessManager();
    }
    
    @Override
    public Locale getLocale() {
    	return getSession().getLocale();
    }
    
}
