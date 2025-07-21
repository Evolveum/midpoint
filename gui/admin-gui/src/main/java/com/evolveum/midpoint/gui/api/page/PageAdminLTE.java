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
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.secrets.SecretsProviderManager;
import com.evolveum.midpoint.gui.impl.page.admin.certification.column.AbstractGuiColumn;
import com.evolveum.midpoint.gui.impl.page.login.AbstractPageLogin;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;

import com.evolveum.midpoint.model.api.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.model.common.MarkManager;
import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper;

import com.evolveum.midpoint.repo.common.subscription.SubscriptionState;
import com.evolveum.midpoint.schema.merger.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.schema.processor.ResourceSchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResultStatus;

import com.evolveum.midpoint.security.api.SecurityContextManager.ResultAwareCheckedProducer;
import com.evolveum.midpoint.gui.impl.component.action.AbstractGuiAction;

import com.evolveum.midpoint.smart.api.SmartIntegrationService;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
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
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.factory.wrapper.ItemWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.registry.DataProviderRegistry;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.page.login.module.PageLogin;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.handlers.CertGuiHandlerRegistry;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.util.NewWindowNotifyingBehavior;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorRegistry;
import com.evolveum.midpoint.wf.api.ApprovalsManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
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

    private static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_VERSION = "version";
    private static final String ID_SUBSCRIPTION_MESSAGE = "subscriptionMessage";
    private static final String ID_COPYRIGHT_MESSAGE = "copyrightMessage";

    public static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";
    private static final String ID_FEEDBACK = "feedback";

    // Strictly speaking following fields should be transient.
    // But making them transient is causing problems on some
    // JVM version or tomcat configurations (MID-3357).
    // It seems to be somehow related to session persistence.
    // But honestly I have no idea about the real cause.
    // Anyway, setting these fields to non-transient seems to
    // fix it. And surprisingly it does not affect the session
    // size.

    @SpringBean(name = "modelController")
    private BulkActionsService bulkActionsService;

    @SpringBean(name = "modelController")
    private ModelService modelService;

    @SpringBean(name = "modelInteractionService")
    private ModelInteractionService modelInteractionService;

    @SpringBean(name = "smartIntegrationService")
    private SmartIntegrationService smartIntegrationService;

    @SpringBean(name = "dashboardService")
    private DashboardService dashboardService;

    @SpringBean(name = "modelController")
    private TaskService taskService;

    @SpringBean(name = "modelDiagController")
    private ModelDiagnosticService modelDiagnosticService;

    @SpringBean(name = "taskManager")
    private TaskManager taskManager;

    @SpringBean(name = "objectOperationPolicyHelper")
    private ObjectOperationPolicyHelper objectOperationPolicyHelper;

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
    private GuiComponentRegistry registry;

    @SpringBean
    private DataProviderRegistry dataProviderRegistry;

    @SpringBean
    private DefaultGuiConfigurationCompiler guiConfigurationRegistry;

    @SpringBean
    private AdminGuiConfigurationMergeManager adminGuiConfigurationMergeManager;

    @SpringBean
    private CorrelationService correlationService;

    @SpringBean
    private SimulationResultManager simulationResultManager;

    @SpringBean
    private CertGuiHandlerRegistry certGuiHandlerRegistry;

    @SpringBean
    private SecretsProviderManager secretsProviderManager;

    @SpringBean
    private TriggerHandlerRegistry triggerHandlerRegistry;

    @SpringBean
    private ResourceSchemaRegistry resourceSchemaRegistry;

    @SpringBean
    private MarkManager markManager;

    // No need for this to store in session. It is used only during single init and render.
    private transient Task pageTask;

    // No need to store this in the session. Retrieval is cheap.
    private transient CompiledGuiProfile compiledGuiProfile;

    private static final String DEFAULT_SYSTEM_NAME = "midPoint";

    public PageAdminLTE(PageParameters parameters) {
        super(parameters);

        setStatelessHint(false);

        LOGGER.debug("Initializing page {}", this.getClass());

        Injector.get().inject(this);
        Validate.notNull(modelService, "Model service was not injected.");
        Validate.notNull(taskManager, "Task manager was not injected.");
        Validate.notNull(reportManager, "Report manager was not injected.");

        MidPointAuthWebSession.get().setClientCustomization();

        add(new NewWindowNotifyingBehavior());
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forScript(
                "document.documentElement.lang = '" + getSession().getLocale().getLanguage() + "';", "set-lang"));
    }

    private void initLayout() {
        TransparentWebMarkupContainer body = new TransparentWebMarkupContainer(ID_BODY);
        body.add(AttributeAppender.append("class", () -> getSessionStorage().getMode() == SessionStorage.Mode.DARK ? "dark-mode" : null));
        // body.add(AttributeAppender.append("class", () -> WebComponentUtil.getMidPointSkin().getAccentCss()));

        addDefaultBodyStyle(body);
        add(body);

        Label title = new Label(ID_TITLE, createPageTitleModel());
        title.add(getPageTitleBehaviour());
        title.setRenderBodyOnly(true);
        add(title);

        addFooter();
        initDebugBarLayout();
    }

    protected VisibleEnableBehaviour getPageTitleBehaviour() {
        return VisibleBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    protected void addDefaultBodyStyle(TransparentWebMarkupContainer body) {

    }

    private void addFooter() {
        WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
        footerContainer.add(new VisibleBehaviour(() -> !isErrorPage() && isFooterVisible()));
        add(footerContainer);

        WebMarkupContainer version = new WebMarkupContainer(ID_VERSION) {

            private static final long serialVersionUID = 1L;

            @Deprecated
            public String getDescribe() {
                return PageAdminLTE.this.getDescribe();
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
                        SubscriptionState subscription = getSubscriptionState();
                        if (!subscription.isActive()) {
                            return " " + createStringResource("PageBase.nonActiveSubscriptionMessage").getString();
                        } else if (subscription.isDemo()) {
                            return " " + createStringResource("PageBase.demoSubscriptionMessage").getString();
                        } else if (subscription.isInGracePeriod()) {
                            int daysToGracePeriodGone = subscription.getDaysToGracePeriodGone();
                            if(daysToGracePeriodGone < 2) {
                                return " " + createStringResource("PageBase.gracePeriodSubscriptionMessage.lastDay").getString();
                            }
                            return " " + createStringResource(
                                    "PageBase.gracePeriodSubscriptionMessage",
                                    subscription.getDaysToGracePeriodGone())
                                    .getString();
                        } else {
                            return "";
                        }
                    }
                });
        subscriptionMessage.setOutputMarkupId(true);
        subscriptionMessage.add(getFooterVisibleBehaviour());
        footerContainer.add(subscriptionMessage);
    }

    public SubscriptionState getSubscriptionState() {
        return MidPointApplication.get().getSubscriptionState();
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
        SubscriptionState subscription = getSubscriptionState();
        return subscription.isFooterVisible();
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

    public ObjectOperationPolicyHelper getObjectOperationPolicyHelper() {
        return objectOperationPolicyHelper;
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

    public BulkActionsService getBulkActionsService() {
        return bulkActionsService;
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
    public SmartIntegrationService getSmartIntegrationService() {
        return smartIntegrationService;
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

    @Override
    public CorrelationService getCorrelationService() {
        return correlationService;
    }

    @Override
    public SimulationResultManager getSimulationResultManager() {
        return simulationResultManager;
    }

    public RoleAnalysisService getRoleAnalysisService() {
        return getMidpointApplication().getRoleAnalysisService();
    }

    public CertGuiHandlerRegistry getCertGuiHandlerRegistry() {
        return certGuiHandlerRegistry;
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
        return WebModelServiceUtils.createSimpleTask(operation, channel, user.getFocusPrismObject(), getTaskManager());
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

    @Override
    @Contract(pure = true)
    public PrismContext getPrismContext() {
        return getMidpointApplication().getPrismContext();
    }

    @NotNull
    @Override
    public CompiledGuiProfile getCompiledGuiProfile() {
        // TODO: may need to always go to ModelInteractionService to make sure the setting is up to date
        if (compiledGuiProfile == null) {
            compiledGuiProfile = WebComponentUtil.getCompiledGuiProfile();
        }
        return compiledGuiProfile;
    }

    @Override
    public <I extends Item, IW extends ItemWrapper> IW createItemWrapper(I item, ItemStatus status, WrapperContext ctx) throws SchemaException {
        return createItemWrapper(item, null, status, ctx);
    }

    public <I extends Item, IW extends ItemWrapper> IW createItemWrapper(
            I item, PrismContainerValueWrapper<?> parent, ItemStatus status, WrapperContext ctx) throws SchemaException {

        ItemWrapperFactory<IW, ?, ?> factory = registry.findWrapperFactory(
                item.getDefinition(),
                parent == null ? null : parent.getNewValue());

        ctx.setCreateIfEmpty(true);
        return factory.createWrapper(parent, item, status, ctx);
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

    public String getString(Enum<?> e) {
        return createStringResource(e).getString();
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

    @NotNull
    public static StringResourceModel createStringResourceStatic(String resourceKey, String defaultValue, Object... objects) {
        return new StringResourceModel(resourceKey).setModel(new Model<String>())
                .setDefaultValue(defaultValue).setParameters(objects);
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

    public <T> T runAsChecked(
            ResultAwareCheckedProducer<T> producer,
            PrismObject<UserType> user,
            OperationResult result) throws CommonException {
        return securityContextManager.runAsChecked(producer, user, result);
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

    public <ID extends ItemDefinition, C extends Containerable> ItemWrapperFactory<?, ?, ?> findWrapperFactory(
            ID def, PrismContainerValue<C> parentValue) {
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
        return ModelExecuteOptions.create();
    }

    public Class<? extends Panel> findObjectPanel(String identifier) {
        return guiConfigurationRegistry.findPanel(identifier);
    }

    public Class<? extends AbstractGuiAction<?>> findGuiAction(String identifier) {
        return guiConfigurationRegistry.findAction(identifier);
    }

    public Class<? extends AbstractGuiColumn<?, ?>> findGuiColumn(String identifier) {
        return guiConfigurationRegistry.findColumn(identifier);
    }

    public List<Class<? extends AbstractGuiColumn<?, ?>>> findAllApplicableGuiColumns(Class<? extends Containerable> clazz) {
        return guiConfigurationRegistry.findAllApplicableColumns(clazz);
    }

    public SimpleCounter getCounterProvider(String identifier) {
        return guiConfigurationRegistry.findCounter(identifier);
    }

    public boolean isNativeRepo() {
        return getRepositoryService().isNative();
    }

    public String createComponentPath(String... components) {
        return StringUtils.join(components, ":");
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
        return showResult(result, errorMessageKey,
                OpResult.Options.create()
                        .withHideSuccess(!showSuccess));
    }

    // FIXME why the `errorMessageKey` is not used?
    public OpResult showResult(OperationResult result, String errorMessageKey, @NotNull OpResult.Options options) {
        Validate.notNull(result, "Operation result must not be null.");
        Validate.notNull(result.getStatus(), "Operation result status must not be null.");

        OperationResult processedResult = executeResultScriptHook(result);
        if (processedResult == null) {
            return null;
        }

        OpResult opResult = OpResult.getOpResult((PageAdminLTE) getPage(), processedResult);

        // Checking these options here to eliminate the rest of processing (visibility determination etc.) if not needed
        if (opResult.getStatus() == OperationResultStatus.SUCCESS && options.hideSuccess()
                || opResult.getStatus() == OperationResultStatus.IN_PROGRESS && options.hideInProgress()) {
            return opResult;
        }

        opResult.determineObjectsVisibility(this, options);

        switch (opResult.getStatus()) {
            case FATAL_ERROR, PARTIAL_ERROR -> getSession().error(opResult);
            case IN_PROGRESS, NOT_APPLICABLE -> getSession().info(opResult);
            case SUCCESS -> getSession().success(opResult);
            default -> getSession().warn(opResult); // includes unknown and warning
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
            PrismPropertyDefinition<OperationResultType> outputDefinition = getPrismContext().definitionFactory().newPropertyDefinition(
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
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException |
                ConfigurationException | SecurityViolationException e) {
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

    public <O extends ObjectType> boolean isAuthorized(ModelAuthorizationAction action, PrismObject<O> object) {
        try {
            return isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null)
                    || isAuthorized(action.getUrl(), null, object, null, null);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException |
                ConfigurationException | SecurityViolationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine authorization for {}", e, action);
            return true;            // it is only GUI thing
        }
    }

    public boolean isAuthorized(String operationUrl) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return isAuthorized(operationUrl, null, null, null, null);
    }

    public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(
            String operationUrl, AuthorizationPhaseType phase, PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        Task task = getPageTask();
        AuthorizationParameters<O, T> params = new AuthorizationParameters.Builder<O, T>()
                .oldObject(object)
                .delta(delta)
                .target(target)
                .build();
        SecurityEnforcer.Options options = SecurityEnforcer.Options.create();
        boolean isAuthorized = getSecurityEnforcer().isAuthorized(
                operationUrl, phase, params, options, task, task.getResult());
        if (!isAuthorized &&
                (ModelAuthorizationAction.GET.getUrl().equals(operationUrl)
                        || ModelAuthorizationAction.SEARCH.getUrl().equals(operationUrl))) {
            isAuthorized = getSecurityEnforcer().isAuthorized(
                    ModelAuthorizationAction.READ.getUrl(), phase, params, options, task, task.getResult());
        }
        return isAuthorized;
    }

    public void redirectToNotFoundPage() {
        PageError404 notFound = new PageError404();
        throw new RestartResponseException(notFound);
    }

    public GuiProfiledPrincipal getPrincipal() {
        return AuthUtil.getPrincipalUser();
    }

    // TODO should we throw "redirect to login" exception here?
    public FocusType getPrincipalFocus() {
        MidPointPrincipal principal = getPrincipal();
        if (principal == null) {
            return null;
        }
        return principal.getFocus();
    }

    //Used by subclasses
    public void addFeedbackPanel() {
        WebMarkupContainer feedbackContainer = new WebMarkupContainer(ID_FEEDBACK_CONTAINER);
        feedbackContainer.setOutputMarkupId(true);
        feedbackContainer.setOutputMarkupPlaceholderTag(true);
        add(feedbackContainer);

        FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK);
        feedbackList.setOutputMarkupId(true);
        feedbackList.setOutputMarkupPlaceholderTag(true);
        feedbackContainer.add(feedbackList);
    }

    public Component getFeedbackPanel() {
        return get(ID_FEEDBACK_CONTAINER);
    }

    public SessionStorage getSessionStorage() {
        MidPointAuthWebSession session = (MidPointAuthWebSession) getSession();
        return session.getSessionStorage();
    }

    public SecretsProviderManager getSecretsProviderManager() {
        return secretsProviderManager;
    }

    @Override
    public TriggerHandlerRegistry getTriggerHandlerRegistry() {
        return triggerHandlerRegistry;
    }

    public ResourceSchemaRegistry getResourceSchemaRegistry() {
        return resourceSchemaRegistry;
    }

    public MarkManager getMarkManager() {
        return markManager;
    }

    public void changeLocal(AjaxRequestTarget target) {
    }

    public IModel<String> getSystemNameModel() {
        return () -> {
            String customSystemName = WebComponentUtil.getMidpointCustomSystemName(PageAdminLTE.this, DEFAULT_SYSTEM_NAME);
            return StringUtils.isNotEmpty(customSystemName) ? customSystemName : DEFAULT_SYSTEM_NAME;
        };
    }
}
