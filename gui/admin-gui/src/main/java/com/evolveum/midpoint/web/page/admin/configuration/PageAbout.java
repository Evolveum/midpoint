/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.init.InitialDataImport;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.LabeledString;
import com.evolveum.midpoint.schema.ProvisioningDiag;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Producer;
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
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import com.evolveum.midpoint.repo.cache.RepositoryCache;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/config/about", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_ABOUT_URL,
                label = "PageAbout.auth.configAbout.label", description = "PageAbout.auth.configAbout.description")})
public class PageAbout extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageAbout.class);

    private static final String DOT_CLASS = PageAbout.class.getName() + ".";
    private static final String OPERATION_TEST_REPOSITORY = DOT_CLASS + "testRepository";
    private static final String OPERATION_TEST_REPOSITORY_CHECK_ORG_CLOSURE = DOT_CLASS + "testRepositoryCheckOrgClosure";
    private static final String OPERATION_GET_REPO_DIAG = DOT_CLASS + "getRepoDiag";
    private static final String OPERATION_SUBMIT_REINDEX = DOT_CLASS + "submitReindex";
    private static final String OPERATION_CHECK_WORKFLOW_PROCESSES = DOT_CLASS + "checkWorkflowProcesses";
    private static final String OPERATION_GET_PROVISIONING_DIAG = DOT_CLASS + "getProvisioningDiag";
    private static final String OPERATION_DELETE_ALL_OBJECTS = DOT_CLASS + "deleteAllObjects";
    private static final String OPERATION_DELETE_TASK = DOT_CLASS + "deleteTask";
    private static final String OPERATION_LOAD_NODE = DOT_CLASS + "loadNode";
    private static final String POST_INIT = DOT_CLASS + "postInit";

    private static final String ID_BUILD_TIMESTAMP = "buildTimestamp";
    private static final String ID_BUILD = "build";
    private static final String ID_BRANCH = "branch";
    private static final String ID_PROPERTY = "property";
    private static final String ID_VALUE = "value";
    private static final String ID_LIST_SYSTEM_ITEMS = "listSystemItems";
    private static final String ID_TEST_REPOSITORY = "testRepository";
    private static final String ID_TEST_REPOSITORY_CHECK_ORG_CLOSURE = "testRepositoryCheckOrgClosure";
    private static final String ID_REINDEX_REPOSITORY_OBJECTS = "reindexRepositoryObjects";
    private static final String ID_TEST_PROVISIONING = "testProvisioning";
    private static final String ID_CHECK_WORKFLOW_PROCESSES = "checkWorkflowProcesses";
    private static final String ID_IMPLEMENTATION_SHORT_NAME = "implementationShortName";
    private static final String ID_IMPLEMENTATION_DESCRIPTION = "implementationDescription";
    private static final String ID_IS_EMBEDDED = "isEmbedded";
    private static final String ID_DRIVER_SHORT_NAME = "driverShortName";
    private static final String ID_DRIVER_VERSION = "driverVersion";
    private static final String ID_REPOSITORY_URL = "repositoryUrl";
    private static final String ID_ADDITIONAL_DETAILS = "additionalDetails";
    private static final String ID_DETAIL_NAME = "detailName";
    private static final String ID_DETAIL_VALUE = "detailValue";
    private static final String ID_PROVISIONING_ADDITIONAL_DETAILS = "provisioningAdditionalDetails";
    private static final String ID_PROVISIONING_DETAIL_NAME = "provisioningDetailName";
    private static final String ID_PROVISIONING_DETAIL_VALUE = "provisioningDetailValue";
    private static final String ID_JVM_PROPERTIES = "jvmProperties";
    private static final String ID_CLEAR_CSS_JS_CACHE = "clearCssJsCache";
    private static final String ID_FACTORY_DEFAULT = "factoryDefault";
    private static final String ID_NODE_NAME = "nodeName";
    private static final String ID_NODE_ID = "nodeId";
    private static final String ID_NODE_URL = "nodeUrl";

    private static final String[] PROPERTIES = new String[]{"file.separator", "java.class.path",
            "java.home", "java.vendor", "java.vendor.url", "java.version", "line.separator", "os.arch",
            "os.name", "os.version", "path.separator", "user.dir", "user.home", "user.name"};

    private IModel<RepositoryDiag> repoDiagModel;
    private IModel<ProvisioningDiag> provisioningDiagModel;

    @Autowired RepositoryCache repositoryCache;
    @Autowired protected SystemObjectCache systemObjectCache;

    public PageAbout() {
        repoDiagModel = new LoadableModel<RepositoryDiag>(false) {

            @Override
            protected RepositoryDiag load() {
                return loadRepoDiagModel();
            }
        };
        provisioningDiagModel = new LoadableModel<ProvisioningDiag>(false) {

            @Override
            protected ProvisioningDiag load() {
                return loadProvisioningDiagModel();
            }
        };
        initLayout();
    }

    private void initLayout() {
        Label branch = new Label(ID_BRANCH, createStringResource("midpoint.system.branch"));
        branch.setRenderBodyOnly(true);
        add(branch);

        Label revision = new Label(ID_BUILD, createStringResource("midpoint.system.build"));
        revision.setRenderBodyOnly(true);
        add(revision);

        Label build = new Label(ID_BUILD_TIMESTAMP, createStringResource("midpoint.system.buildTimestamp"));
        build.setRenderBodyOnly(true);
        add(build);

        ListView<SystemItem> listSystemItems = new ListView<SystemItem>(ID_LIST_SYSTEM_ITEMS, getItems()) {

            @Override
            protected void populateItem(ListItem<SystemItem> item) {
                SystemItem systemItem = item.getModelObject();

                Label property = new Label(ID_PROPERTY, systemItem.getProperty());
                property.setRenderBodyOnly(true);
                item.add(property);

                Label value = new Label(ID_VALUE, systemItem.getValue());
                value.setRenderBodyOnly(true);
                item.add(value);
            }
        };
        add(listSystemItems);

        addLabel(ID_IMPLEMENTATION_SHORT_NAME, "implementationShortName");
        addLabel(ID_IMPLEMENTATION_DESCRIPTION, "implementationDescription");
        addLabel(ID_IS_EMBEDDED, "isEmbedded");
        addLabel(ID_DRIVER_SHORT_NAME, "driverShortName");
        addLabel(ID_DRIVER_VERSION, "driverVersion");
        addLabel(ID_REPOSITORY_URL, "repositoryUrl");

        ListView<LabeledString> additionalDetails = new ListView<LabeledString>(ID_ADDITIONAL_DETAILS,
            new PropertyModel<>(repoDiagModel, "additionalDetails")) {

            @Override
            protected void populateItem(ListItem<LabeledString> item) {
                LabeledString labeledString = item.getModelObject();

                Label property = new Label(ID_DETAIL_NAME, labeledString.getLabel());
                property.setRenderBodyOnly(true);
                item.add(property);

                Label value = new Label(ID_DETAIL_VALUE, labeledString.getData());
                value.setRenderBodyOnly(true);
                item.add(value);
            }
        };
        add(additionalDetails);

        ListView<LabeledString> provisioningAdditionalDetails = new ListView<LabeledString>(ID_PROVISIONING_ADDITIONAL_DETAILS,
            new PropertyModel<>(provisioningDiagModel, "additionalDetails")) {

            @Override
            protected void populateItem(ListItem<LabeledString> item) {
                LabeledString labeledString = item.getModelObject();

                Label property = new Label(ID_PROVISIONING_DETAIL_NAME, labeledString.getLabel());
                property.setRenderBodyOnly(true);
                item.add(property);

                Label value = new Label(ID_PROVISIONING_DETAIL_VALUE, labeledString.getData());
                value.setRenderBodyOnly(true);
                item.add(value);
            }
        };
        add(provisioningAdditionalDetails);

        String nodeId = getTaskManager().getNodeId();
        OperationResult result = new OperationResult(OPERATION_LOAD_NODE);
        List<PrismObject<NodeType>> nodes = WebModelServiceUtils.searchObjects(NodeType.class,
                getPrismContext().queryFor(NodeType.class)
                        .item(NodeType.F_NODE_IDENTIFIER).eq(nodeId)
                        .build(),
                result, PageAbout.this);

        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Couldn't find NodeType with identifier '" + nodeId + "'");
        }

        if (nodes.size() > 1) {
            throw new IllegalArgumentException("Found more as one NodeType with identifier '" + nodeId + "'");
        }

        PrismObject<NodeType> node = nodes.get(0);

        if (node == null) {
            throw new IllegalArgumentException("Found NodeType with identifier '" + nodeId + "' is null");
        }

        NodeType nodeType = node.asObjectable();

        Label nodeName = new Label(ID_NODE_NAME, nodeType.getName() != null ? nodeType.getName() : "");
        nodeName.setRenderBodyOnly(true);
        add(nodeName);

        Label nodeIdValue = new Label(ID_NODE_ID, nodeType.getNodeIdentifier());
        nodeIdValue.setRenderBodyOnly(true);
        add(nodeIdValue);

        Label nodeUrl = new Label(ID_NODE_URL, nodeType.getUrl() != null ? nodeType.getUrl() : "");
        nodeUrl.setRenderBodyOnly(true);
        add(nodeUrl);

        Label jvmProperties = new Label(ID_JVM_PROPERTIES, new LoadableModel<String>(false) {

            @Override
            protected String load() {
                try {
                    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
                    List<String> arguments = runtimeMxBean.getInputArguments();

                    return StringUtils.join(arguments, "<br/>");
                } catch (Exception ex) {
                    return PageAbout.this.getString("PageAbout.message.couldntObtainJvmParams");
                }
            }
        });
        jvmProperties.setEscapeModelStrings(false);
        add(jvmProperties);

        initButtons();
    }

    private void addLabel(String id, String propertyName) {
        Label label = new Label(id, new PropertyModel<String>(repoDiagModel, propertyName));
        label.setRenderBodyOnly(true);
        add(label);
    }

    private void initButtons() {
        AjaxButton testRepository = new AjaxButton(ID_TEST_REPOSITORY,
                createStringResource("PageAbout.button.testRepository")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                testRepositoryPerformed(target);
            }
        };
        add(testRepository);

        AjaxButton testRepositoryCheckOrgClosure = new AjaxButton(ID_TEST_REPOSITORY_CHECK_ORG_CLOSURE,
                createStringResource("PageAbout.button.testRepositoryCheckOrgClosure")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                testRepositoryCheckOrgClosurePerformed(target);
            }
        };
        add(testRepositoryCheckOrgClosure);

        AjaxButton reindexRepositoryObjects = new AjaxButton(ID_REINDEX_REPOSITORY_OBJECTS,
                createStringResource("PageAbout.button.reindexRepositoryObjects")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                reindexRepositoryObjectsPerformed(target);
            }
        };
        add(reindexRepositoryObjects);

        AjaxButton testProvisioning = new AjaxButton(ID_TEST_PROVISIONING,
                createStringResource("PageAbout.button.testProvisioning")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                testProvisioningPerformed(target);
            }
        };
        add(testProvisioning);

        AjaxButton checkWorkflowProcesses = new AjaxButton(ID_CHECK_WORKFLOW_PROCESSES,
                createStringResource("PageAbout.button.checkWorkflowProcesses")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
//                checkWorkflowProcessesPerformed(target);
            }
        };
        checkWorkflowProcesses.setVisible(false);
        add(checkWorkflowProcesses);

        AjaxButton clearCssJsCache = new AjaxButton(ID_CLEAR_CSS_JS_CACHE,
                createStringResource("PageAbout.button.clearCssJsCache")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearLessJsCache(target);
            }
        };
        add(clearCssJsCache);

        AjaxButton factoryDefault = new AjaxButton(ID_FACTORY_DEFAULT,
                createStringResource("PageAbout.button.factoryDefault")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showMainPopup(getDeleteAllObjectsConfirmationPanel(), target);
            }
        };
        add(factoryDefault);
    }

    private RepositoryDiag loadRepoDiagModel() {
        OperationResult result = new OperationResult(OPERATION_GET_REPO_DIAG);
        RepositoryDiag diag = null;
        try {
            Task task = createSimpleTask(OPERATION_GET_REPO_DIAG);
            diag = getModelDiagnosticService().getRepositoryDiag(task, result);

            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get repo diagnostics", ex);
            result.recordFatalError(getString("PageAbout.message.loadRepoDiagModel.fatalError"), ex);
        }
        result.recomputeStatus();

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return diag;
    }

    private ProvisioningDiag loadProvisioningDiagModel() {
        OperationResult result = new OperationResult(OPERATION_GET_PROVISIONING_DIAG);
        ProvisioningDiag diag = null;
        try {
            Task task = createSimpleTask(OPERATION_GET_PROVISIONING_DIAG);
            diag = getModelDiagnosticService().getProvisioningDiag(task, result);

            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get provisioning diagnostics", ex);
            result.recordFatalError(getString("PageAbout.message.loadProvisioningDiagModel.fatalError"), ex);
        }
        result.recomputeStatus();

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return diag;
    }

    private IModel<List<SystemItem>> getItems() {
        return new LoadableModel<List<SystemItem>>(false) {

            @Override
            protected List<SystemItem> load() {
                List<SystemItem> items = new ArrayList<>();
                for (String property : PROPERTIES) {
                    items.add(new SystemItem(property, System.getProperty(property)));
                }
                return items;
            }
        };
    }

    private void testRepositoryPerformed(AjaxRequestTarget target) {
        Task task = createSimpleTask(OPERATION_TEST_REPOSITORY);

        OperationResult result = getModelDiagnosticService().repositorySelfTest(task);
        showResult(result);

        target.add(getFeedbackPanel());
    }

    private void testRepositoryCheckOrgClosurePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_TEST_REPOSITORY_CHECK_ORG_CLOSURE);
        try {
            Task task = createSimpleTask(OPERATION_TEST_REPOSITORY_CHECK_ORG_CLOSURE);
            getModelDiagnosticService().repositoryTestOrgClosureConsistency(task, true, result);
        } catch (SchemaException | SecurityViolationException | ExpressionEvaluationException | ObjectNotFoundException | ConfigurationException | CommunicationException e) {
            result.recordFatalError(e);
        } finally {
            result.computeStatusIfUnknown();
        }
        showResult(result);

        target.add(getFeedbackPanel());
    }

    private void reindexRepositoryObjectsPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_SUBMIT_REINDEX);
        try {
            TaskManager taskManager = getTaskManager();
            Task task = taskManager.createTaskInstance();
            MidPointPrincipal user = SecurityUtils.getPrincipalUser();
            if (user == null) {
                throw new RestartResponseException(PageLogin.class);
            } else {
                task.setOwner(user.getFocus().asPrismObject());
            }
            authorize(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null, null, result);
            task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);
            task.setHandlerUri(ModelPublicConstants.REINDEX_TASK_HANDLER_URI);
            task.setName("Reindex repository objects");
            taskManager.switchToBackground(task, result);
            result.setBackgroundTaskOid(task.getOid());
        } catch (SecurityViolationException | SchemaException|RuntimeException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(e);
        } finally {
            result.computeStatusIfUnknown();
        }
        showResult(result);
        target.add(getFeedbackPanel());
    }

    private void testProvisioningPerformed(AjaxRequestTarget target) {
        Task task = createSimpleTask(OPERATION_TEST_REPOSITORY);

        OperationResult result = getModelDiagnosticService().provisioningSelfTest(task);
        showResult(result);

        target.add(getFeedbackPanel());
    }

    private Popupable getDeleteAllObjectsConfirmationPanel() {
        return new ConfirmationPanel(getMainPopupBodyId(), createStringResource("PageAbout.message.deleteAllObjects")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                resetStateToInitialConfig(target);
            }

        };
    }

    private void resetStateToInitialConfig(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_DELETE_ALL_OBJECTS);
        String taskOid = null;
        String taskName = "Delete all objects";

        QueryFactory factory = getPrismContext().queryFactory();

        TypeFilter nodeFilter = factory.createType(NodeType.COMPLEX_TYPE, factory.createAll());
        final ObjectFilter taskFilter = getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_NAME).eq(taskName).buildFilter();
        NotFilter notNodeFilter = factory.createNot(nodeFilter);
        NotFilter notTaskFilter = factory.createNot(taskFilter);

        try {
            QName type = ObjectType.COMPLEX_TYPE;
            taskOid = deleteObjectsAsync(type, factory.createQuery(
                    factory.createAnd(notTaskFilter, notNodeFilter)), true,
                    taskName, result);

        } catch (Exception ex) {
            result.recomputeStatus();
            result.recordFatalError(getString("PageAbout.message.resetStateToInitialConfig.allObject.fatalError"), ex);

            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete all objects", ex);
        }

        final String taskOidToRemoving = taskOid;

        try {
            while(!getTaskManager().getTaskPlain(taskOid, result).isClosed()) {TimeUnit.SECONDS.sleep(5);}

            runPrivileged(new Producer<Object>() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object run() {
                    Task task = createAnonymousTask(OPERATION_DELETE_TASK);
                    OperationResult result = new OperationResult(OPERATION_DELETE_TASK);
                    ObjectDelta<TaskType> delta = getPrismContext().deltaFactory().object()
                            .createDeleteDelta(TaskType.class, taskOidToRemoving);
                    Collection<ObjectDelta<? extends ObjectType>> deltaCollection = new ArrayList<ObjectDelta<? extends ObjectType>>() {{add(delta);}};
                    try {
                        getModelService().executeChanges(deltaCollection, null, task, result);
                    } catch (Exception ex) {
                        result.recomputeStatus();
                        result.recordFatalError(getString("PageAbout.message.resetStateToInitialConfig.task.fatalError"), ex);

                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete task", ex);
                    }
                    result.computeStatus();
                    return null;
                }

            });

            InitialDataImport initialDataImport = new InitialDataImport();
            initialDataImport.setModel(getModelService());
            initialDataImport.setTaskManager(getTaskManager());
            initialDataImport.setPrismContext(getPrismContext());
            initialDataImport.setConfiguration(getMidpointConfiguration());
            initialDataImport.init();

            // TODO consider if we need to go clusterwide here
            getCacheDispatcher().dispatchInvalidation(null, null, true, null);

            getModelService().shutdown();

            getModelService().postInit(result);


        } catch (Exception ex) {
            result.recomputeStatus();
            result.recordFatalError(getString("PageAbout.message.resetStateToInitialConfig.import.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't import initial objects", ex);
        }
        showResult(result);
        target.add(getFeedbackPanel());
    }

    /**
     * It's here only because of some IDEs - it's not properly filtering resources during maven build.
     * "describe" variable is not replaced.
     *
     * @return "unknown" instead of "git describe" for current build.
     */
    @Deprecated
    public String getDescribe() {
        return getString("PageAbout.unknownBuildNumber");
    }

    private static class SystemItem implements Serializable {

        private String property;
        private String value;

        private SystemItem(String property, String value) {
            this.property = property;
            this.value = value;
        }

        public String getProperty() {
            return property;
        }

        public String getValue() {
            return value;
        }
    }
}
