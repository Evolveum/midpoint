/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.page;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.gui.api.DefaultGuiConfigurationCompiler;
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
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.correlator.CorrelationService;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.QueryConverter;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.ClusterExecutionHelper;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.CheckedProducer;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.web.util.NewWindowNotifyingBehavior;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorRegistry;
import com.evolveum.midpoint.wf.api.ApprovalsManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DeploymentInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class PageAdminLTE extends WebPage implements ModelServiceLocator {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageAdminLTE.class.getName() + ".";

    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";

    private static final Trace LOGGER = TraceManager.getTrace(PageAdminLTE.class);

    private static final String ID_TITLE = "title";
    private static final String ID_BODY = "body";
    private static final String ID_DEBUG_BAR = "debugBar";
    private static final String ID_DUMP_PAGE_TREE = "dumpPageTree";
    private static final String ID_DEBUG_PANEL = "debugPanel";


    private static final String CLASS_DEFAULT_SKIN = "skin-blue-light";

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

    @SpringBean(name = "auditController")
    private ModelAuditService modelAuditService;

    @SpringBean(name = "modelController")
    private CaseService caseService;

    @SpringBean(name = "caseManager")
    private CaseManager caseManager;

    @SpringBean(name = "approvalsManager")
    private ApprovalsManager approvalsManager;

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

    @SpringBean
    private GuiComponentRegistry registry;

    @SpringBean
    private DefaultGuiConfigurationCompiler guiConfigurationRegistry;

    @SpringBean
    private ClusterExecutionHelper clusterExecutionHelper;

    @SpringBean
    private AdminGuiConfigurationMergeManager adminGuiConfigurationMergeManager;

    @SpringBean
    private CorrelationService correlationService;

    // No need for this to store in session. It is used only during single init and render.
    private transient Task pageTask;

    // No need to store this in the session. Retrieval is cheap.
    private transient CompiledGuiProfile compiledGuiProfile;

    public PageAdminLTE(PageParameters parameters) {
        super(parameters);

        setStatelessHint(false);

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

        Label title = new Label(ID_TITLE, createPageTitleModel());
        title.setRenderBodyOnly(true);
        add(title);

        initDebugBarLayout();
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

    @Override
    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    @Override
    public ExpressionFactory getExpressionFactory() {
        return getMidpointApplication().getExpressionFactory();
    }

    public WebApplicationConfiguration getWebApplicationConfiguration() {
        MidPointApplication application = getMidpointApplication();
        return application.getWebApplicationConfiguration();
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

    public MatchingRuleRegistry getMatchingRuleRegistry() {
        return getMidpointApplication().getMatchingRuleRegistry();
    }

    public RepositoryService getRepositoryService() {
        return getMidpointApplication().getRepositoryService();
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public CaseService getCaseService() {
        return caseService;
    }

    public CaseManager getCaseManager() {
        return caseManager;
    }

    public ApprovalsManager getApprovalsManager() {
        return approvalsManager;
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

    public CacheDispatcher getCacheDispatcher() {
        return cacheDispatcher;
    }

    @Override
    public AdminGuiConfigurationMergeManager getAdminGuiConfigurationMergeManager() {
        return adminGuiConfigurationMergeManager;
    }

    @Override
    public CorrelationService getCorrelationService() {
        return correlationService;
    }

    public MidPointApplication getMidpointApplication() {
        return (MidPointApplication) getApplication();
    }

    @Override
    public Task createSimpleTask(String operation) {
        return createSimpleTask(operation, null);
    }

    public Task createSimpleTask(String operation, String channel) {
        MidPointPrincipal user = AuthUtil.getPrincipalUser();
        if (user == null) {
            throw new RestartResponseException(PageLogin.class);
        }
        return WebModelServiceUtils.createSimpleTask(operation, channel, user.getFocus().asPrismObject(), getTaskManager());
    }

    public MidpointConfiguration getMidpointConfiguration() {
        return midpointConfiguration;
    }

    @Override
    public Task getPageTask() {
        if (pageTask == null) {
            pageTask = createSimpleTask(this.getClass().getName());
        }
        return pageTask;
    }

    @Contract(pure = true)
    public PrismContext getPrismContext() {
        return getMidpointApplication().getPrismContext();
    }

    @NotNull
    @Override
    public CompiledGuiProfile getCompiledGuiProfile() {
        // TODO: may need to always go to ModelInteractionService to make sure the setting is up to date
        if (compiledGuiProfile == null) {
            Task task = createSimpleTask(PageAdminLTE.DOT_CLASS + "getCompiledGuiProfile");
            try {
                compiledGuiProfile = modelInteractionService.getCompiledGuiProfile(task, task.getResult());
            } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException |
                    SecurityViolationException | ExpressionEvaluationException e) {
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
    public <I extends Item, IW extends ItemWrapper> IW createItemWrapper(I item, ItemStatus status, WrapperContext ctx) throws SchemaException {

        ItemWrapperFactory<IW, ?, ?> factory = registry.findWrapperFactory(item.getDefinition(), null);

        ctx.setCreateIfEmpty(true);
        return factory.createWrapper(null, item, status, ctx);
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

    @Override
    public <O extends ObjectType> PrismObjectWrapperFactory<O> findObjectWrapperFactory(PrismObjectDefinition<O> objectDef) {
        return registry.getObjectWrapperFactory(objectDef);
    }

    @Override
    public MidpointFormValidatorRegistry getFormValidatorRegistry() {
        return formValidatorRegistry;
    }

    protected IModel<String> createPageTitleModel() {
        return createStringResource(getClass().getSimpleName() + ".title");
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

    public static StringResourceModel createStringResourceStatic(Component component, Enum<?> e) {
        String resourceKey = createEnumResourceKey(e);
        return createStringResourceStatic(resourceKey);
    }

    public static String createEnumResourceKey(Enum<?> e) {
        return WebComponentUtil.createEnumResourceKey(e);
    }

    private Label getHeaderTitle() {
        return (Label) get(ID_TITLE);
    }

    public IModel<String> getPageTitleModel() {
        String title = (String) get(ID_TITLE).getDefaultModel().getObject();
        return Model.of(title);
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

    public <ID extends ItemDefinition, C extends Containerable> ItemWrapperFactory<?, ?, ?> findWrapperFactory(ID def, PrismContainerValue<C> parentValue) {
        return registry.findWrapperFactory(def, parentValue);
    }

    public <C extends Containerable> PrismContainerWrapperFactory<C> findContainerWrapperFactory(PrismContainerDefinition<C> def) {
        return registry.findContainerWrapperFactory(def);
    }

    public <ID extends ItemDefinition, IW extends ItemWrapper> IW createItemWrapper(ID def, PrismContainerValueWrapper<?> parent, WrapperContext ctx) throws SchemaException {

        ItemWrapperFactory<IW, ?, ?> factory = registry.findWrapperFactory(def, parent.getNewValue());
        ctx.setShowEmpty(true);
        ctx.setCreateIfEmpty(true);
        return factory.createWrapper(parent, def, ctx);
    }

    private Class<?> getWrapperPanel(QName typeName) {
        return registry.getPanelClass(typeName);
    }

    public Task createAnonymousTask(String operation) {
        TaskManager manager = getTaskManager();
        Task task = manager.createTaskInstance(operation);

        task.setChannel(SchemaConstants.CHANNEL_USER_URI);

        return task;
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
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException |
                IllegalArgumentException | InvocationTargetException e) {
            throw new SystemException("Cannot instantiate " + panelClass, e);
        }
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
