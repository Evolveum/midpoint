/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;

import com.evolveum.midpoint.schema.util.task.ActivityDefinitionBuilder;

import org.apache.catalina.util.ServerInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.init.InitialDataImport;
import com.evolveum.midpoint.init.StartupConfiguration;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.LabeledString;
import com.evolveum.midpoint.schema.ProvisioningDiag;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ObjectFactory;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/about", matchUrlForSecurity = "/admin/config/about")
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_ABOUT_URL,
                        label = "PageAbout.auth.configAbout.label", description = "PageAbout.auth.configAbout.description") })
public class PageAbout extends PageAdminConfiguration {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageAbout.class);

    private static final String DOT_CLASS = PageAbout.class.getName() + ".";
    private static final String OPERATION_TEST_REPOSITORY = DOT_CLASS + "testRepository";
    private static final String OPERATION_TEST_REPOSITORY_CHECK_ORG_CLOSURE = DOT_CLASS + "testRepositoryCheckOrgClosure";
    private static final String OPERATION_GET_REPO_DIAG = DOT_CLASS + "getRepoDiag";
    private static final String OPERATION_SUBMIT_REINDEX = DOT_CLASS + "submitReindex";
    private static final String OPERATION_GET_PROVISIONING_DIAG = DOT_CLASS + "getProvisioningDiag";
    private static final String OPERATION_DELETE_ALL_OBJECTS = DOT_CLASS + "deleteAllObjects";
    private static final String OPERATION_LOAD_NODE = DOT_CLASS + "loadNode";
    private static final String OPERATION_INITIAL_IMPORT = DOT_CLASS + "initialImport";

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
    private static final String ID_COPY_ENVIRONMENT_INFO = "copyEnvironmentInfo";
    private static final String ID_FACTORY_DEFAULT = "factoryDefault";
    private static final String ID_NODE_NAME = "nodeName";
    private static final String ID_NODE_ID = "nodeId";
    private static final String ID_NODE_URL = "nodeUrl";

    private static final String[] PROPERTIES = new String[] { "file.separator", "java.class.path",
            "java.home", "java.vendor", "java.vendor.url", "java.version", "line.separator", "os.arch",
            "os.name", "os.version", "path.separator", "user.dir", "user.home", "user.name" };

    private IModel<RepositoryDiag> repoDiagModel;
    private IModel<ProvisioningDiag> provisioningDiagModel;

    private IModel<NodeType> nodeModel;

    @Autowired RepositoryCache repositoryCache;
    @Autowired protected SystemObjectCache systemObjectCache;

    public PageAbout() {
        initModels();
        initLayout();
    }

    private void initModels() {
        repoDiagModel = new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected RepositoryDiag load() {
                return loadRepoDiagModel();
            }
        };
        provisioningDiagModel = new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ProvisioningDiag load() {
                return loadProvisioningDiagModel();
            }
        };
        nodeModel = new LoadableDetachableModel<>() {

            @Override
            protected NodeType load() {
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

                return node.asObjectable();
            }
        };
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

        ListView<LabeledString> listSystemItems = new ListView<>(ID_LIST_SYSTEM_ITEMS, getItems()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<LabeledString> item) {
                LabeledString systemItem = item.getModelObject();

                Label property = new Label(ID_PROPERTY, systemItem.getLabel());
                property.setRenderBodyOnly(true);
                item.add(property);

                Label value = new Label(ID_VALUE, systemItem.getData());
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

        ListView<LabeledString> additionalDetails = new ListView<>(ID_ADDITIONAL_DETAILS,
                new PropertyModel<>(repoDiagModel, "additionalDetails")) {
            private static final long serialVersionUID = 1L;

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

        ListView<LabeledString> provisioningAdditionalDetails = new ListView<>(ID_PROVISIONING_ADDITIONAL_DETAILS,
                new PropertyModel<>(provisioningDiagModel, "additionalDetails")) {
            private static final long serialVersionUID = 1L;

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

        Label nodeName = new Label(ID_NODE_NAME, () -> WebComponentUtil.getName(nodeModel.getObject()));
        nodeName.setRenderBodyOnly(true);
        add(nodeName);

        Label nodeIdValue = new Label(ID_NODE_ID, () -> nodeModel.getObject().getNodeIdentifier());
        nodeIdValue.setRenderBodyOnly(true);
        add(nodeIdValue);

        Label nodeUrl = new Label(ID_NODE_URL, () -> nodeModel.getObject().getUrl());
        nodeUrl.setRenderBodyOnly(true);
        add(nodeUrl);

        Label jvmProperties = new Label(ID_JVM_PROPERTIES, new LoadableModel<String>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                try {
                    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
                    final List<String> arguments = runtimeMxBean.getInputArguments();

                    final List<String> updatedArguments = arguments.stream().map(a -> escapeJVMArgument(a)).collect(Collectors.toList());

                    return StringUtils.join(updatedArguments, "<br/>");
                } catch (Exception ex) {
                    return PageAbout.this.getString("PageAbout.message.couldntObtainJvmParams");
                }
            }
        });
        jvmProperties.setEscapeModelStrings(false);
        add(jvmProperties);

        initButtons();
    }

    private String escapeJVMArgument(String argument) {
        boolean matches = StartupConfiguration.SENSITIVE_CONFIGURATION_VARIABLES.stream().anyMatch(p -> argument.startsWith("-D" + p));
        if (!matches || StartupConfiguration.isPrintSensitiveValues()) {
            return argument;
        }

        int index = argument.indexOf("=");
        if (index < 0) {
            return argument;
        }

        return argument.substring(0, index) + "=" + StartupConfiguration.SENSITIVE_VALUE_OUTPUT;
    }

    private void addLabel(String id, String propertyName) {
        Label label = new Label(id, new PropertyModel<String>(repoDiagModel, propertyName));
        label.setRenderBodyOnly(true);
        add(label);
    }

    private void initButtons() {
        AjaxButton testRepository = new AjaxButton(ID_TEST_REPOSITORY, createStringResource("PageAbout.button.testRepository")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                testRepositoryPerformed(target);
            }
        };
        add(testRepository);

        AjaxButton testRepositoryCheckOrgClosure = new AjaxButton(ID_TEST_REPOSITORY_CHECK_ORG_CLOSURE,
                createStringResource("PageAbout.button.testRepositoryCheckOrgClosure")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                testRepositoryCheckOrgClosurePerformed(target);
            }
        };
        add(testRepositoryCheckOrgClosure);

        AjaxButton reindexRepositoryObjects = new AjaxButton(ID_REINDEX_REPOSITORY_OBJECTS,
                createStringResource("PageAbout.button.reindexRepositoryObjects")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                reindexRepositoryObjectsPerformed(target);
            }
        };
        add(reindexRepositoryObjects);

        AjaxButton testProvisioning = new AjaxButton(ID_TEST_PROVISIONING,
                createStringResource("PageAbout.button.testProvisioning")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                testProvisioningPerformed(target);
            }
        };
        add(testProvisioning);

        AjaxButton copyEnvironmentInfo = new AjaxButton(ID_COPY_ENVIRONMENT_INFO,
                createStringResource("PageAbout.button.copyEnvironmentInfo")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                copyEnvironmentInfoPerformed(target);
                success(createStringResource("PageAbout.button.copyEnvironmentMessage").getString());
                target.add(getFeedbackPanel());
            }
        };
        copyEnvironmentInfo.setOutputMarkupId(true);
        add(copyEnvironmentInfo);

        AjaxButton factoryDefault = new AjaxButton(ID_FACTORY_DEFAULT,
                createStringResource("PageAbout.button.factoryDefault")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showMainPopup(getDeleteAllObjectsConfirmationPanel(), target);
            }
        };
        add(factoryDefault);
    }

    private void copyEnvironmentInfoPerformed(AjaxRequestTarget target) {
        target.appendJavaScript("var $tempInput = document.createElement('INPUT');\n"
                + "document.body.appendChild($tempInput);\n"
                + "$tempInput.setAttribute('value', '" + getEnvironmentInfo() + "');\n"
                + "$tempInput.select();\n"
                + "document.execCommand('copy');\n"
                + "document.body.removeChild($tempInput);");
    }

    private String getEnvironmentInfo() {
        String nodesCount = createStringResource("PageAbout.environmentInfo.nodesCount",
                WebModelServiceUtils.countObjects(NodeType.class, null, PageAbout.this)).getString();
        Runtime runtime = Runtime.getRuntime();
        String processorsCount = createStringResource("PageAbout.environmentInfo.processorsCount", runtime.availableProcessors()).getString();
        String totalMemory = createStringResource("PageAbout.environmentInfo.totalMemory", runtime.totalMemory()).getString();
        String javaVersion = createStringResource("PageAbout.environmentInfo.javaVersion", System.getProperty("java.version")).getString();
        String tomcatVersion = createStringResource("PageAbout.environmentInfo.tomcatVersion", ServerInfo.getServerInfo()).getString();
        String mpVersion = createStringResource("PageAbout.environmentInfo.mpVersion",
                createStringResource("midpoint.system.version").getString()).getString();
        String dbInfo = createStringResource("PageAbout.environmentInfo.databaseInfo",
                repoDiagModel.getObject().getDriverShortName() + " " + repoDiagModel.getObject().getDriverVersion()).getString();
        String osInfo = createStringResource("PageAbout.environmentInfo.osInfo",
                System.getProperty("os.name") + " " + System.getProperty("os.version")).getString();
        return nodesCount + processorsCount + totalMemory + javaVersion + tomcatVersion + mpVersion + dbInfo + osInfo;
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

    private IModel<List<LabeledString>> getItems() {
        return new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<LabeledString> load() {
                List<LabeledString> items = new ArrayList<>();
                for (String property : PROPERTIES) {
                    items.add(new LabeledString(property, System.getProperty(property)));
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
        } catch (SchemaException | SecurityViolationException | ExpressionEvaluationException | ObjectNotFoundException |
                ConfigurationException | CommunicationException e) {
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
            Task task = getTaskManager().createTaskInstance();
            authorize(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null, result);
            getModelInteractionService().submit(
                    ActivityDefinitionBuilder.create(
                                    new ReindexingWorkDefinitionType())
                            .build(),
                    ActivitySubmissionOptions.create().withTaskTemplate(
                            new TaskType()
                                    .name("Reindex repository objects")
                                    .channel(SchemaConstants.CHANNEL_USER_URI)),
                    task, result);
        } catch (CommonException | RuntimeException e) {
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
        return new DeleteConfirmationPanel(getMainPopupBodyId(), createStringResource("PageAbout.message.deleteAllObjects")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                resetStateToInitialConfig(target);
            }
        };
    }

    private ActivityDefinitionType createDeleteActivityForType(QName type, QueryType query, int order) {
        // @formatter:off
        return new ActivityDefinitionType()
                .order(order)
                .identifier("Delete all " + type.getLocalPart())
                .beginDistribution()
                .<ActivityDefinitionType>end()
                .beginWork()
                .beginDeletion()
                .beginObjects()
                .type(type)
                .query(query)
                .<DeletionWorkDefinitionType>end()
                .<WorkDefinitionsType>end()
                .end();
        // @formatter:on
    }

    private QueryType createTaskQuery(String taskIdentifier) throws SchemaException {
        // @formatter:off
        final ObjectQuery query = getPrismContext().queryFor(TaskType.class)
                .not().item(TaskType.F_TASK_IDENTIFIER).eq(taskIdentifier)
                .and()
                .not().item(TaskType.F_PARENT).eq(taskIdentifier).build();
        // @formatter:on
        return getPrismContext().getQueryConverter().createQueryType(query);
    }

    private <T extends ObjectType> QueryType createAllQuery(Class<T> type) throws SchemaException {
        return getPrismContext().getQueryConverter().createQueryType(
                getPrismContext().queryFor(type)
                        .all()
                        .build());
    }

    private List<ObjectTypes> createSortedTypes() {
        final List<ObjectTypes> first = new ArrayList<>();
        first.add(ObjectTypes.SHADOW);
        first.add(ObjectTypes.USER);
        first.add(ObjectTypes.ROLE);
        first.add(ObjectTypes.ORG);

        final List<ObjectTypes> last = new ArrayList<>();
        last.add(ObjectTypes.RESOURCE);
        last.add(ObjectTypes.CONNECTOR);
        last.add(ObjectTypes.MARK);
        last.add(ObjectTypes.OBJECT_TEMPLATE);
        last.add(ObjectTypes.OBJECT_COLLECTION);
        last.add(ObjectTypes.ARCHETYPE);
        last.add(ObjectTypes.SECURITY_POLICY);
        last.add(ObjectTypes.PASSWORD_POLICY);
        last.add(ObjectTypes.SYSTEM_CONFIGURATION);

        final List<ObjectTypes> types = new ArrayList<>();

        types.addAll(first);

        for (ObjectTypes type : ObjectTypes.values()) {
            if (first.contains(type) || last.contains(type)) {
                continue;
            }

            if (Modifier.isAbstract(type.getClassDefinition().getModifiers())) {
                continue;
            }

            if (ObjectTypes.NODE == type) {
                continue;
            }

            types.add(type);
        }

        types.addAll(last);

        return types;
    }

    private void createAndRunDeleteAllTask() {
        Task task = createSimpleTask(OPERATION_DELETE_ALL_OBJECTS);
        OperationResult result = task.getResult();

        try {
            // @formatter:off
            ActivityDefinitionType definition = new ActivityDefinitionType()
                    .identifier("Delete all")
                    .beginComposition()
                    .<ActivityDefinitionType>end()
                    .beginDistribution()
                    .workerThreads(4)
                    .end();
            // @formatter:on

            List<ActivityDefinitionType> activities = definition.getComposition().getActivity();
            int order = 1;
            for (ObjectTypes type : createSortedTypes()) {
                QueryType query;
                if (ObjectTypes.TASK == type) {
                    query = createTaskQuery(task.getTaskIdentifier());
                } else {
                    query = createAllQuery(type.getClassDefinition());
                }

                activities.add(createDeleteActivityForType(type.getTypeQName(), query, order));
                order++;
            }

            activities.add(createInitialImportActivity(order));

            getModelInteractionService().submit(
                    definition,
                    ActivitySubmissionOptions.create()
                            .withTaskTemplate(new TaskType()
                                    .name("Delete all objects")
                                    .cleanupAfterCompletion(XmlTypeConverter.createDuration("P1D")))
                            .withArchetypes(
                                    SystemObjectsType.ARCHETYPE_UTILITY_TASK.value(),
                                    SystemObjectsType.ARCHETYPE_OBJECTS_DELETE_TASK.value()),
                    task, result);
        } catch (Exception ex) {
            result.computeStatusIfUnknown();
            result.recordFatalError("Couldn't create delete all task", ex);
        }

        showResult(result);
    }

    private ActivityDefinitionType createInitialImportActivity(int order) {
        ExecuteScriptActionExpressionType execute = new ExecuteScriptActionExpressionType();
        ScriptExpressionEvaluatorType script = new ScriptExpressionEvaluatorType();
        script.setCode("\n"
                + PageAbout.class.getName() + ".runInitialDataImport(\n"
                + "\tcom.evolveum.midpoint.model.impl.expr.SpringApplicationContextHolder.getApplicationContext(),\n"
                + "\tmidpoint.getCurrentTask().getResult())\n"
                + "log.info(\"Repository factory reset finished\")\n"
        );
        execute.setScript(script);
        execute.setForWholeInput(true);

        ExecuteScriptType executeScript = new ExecuteScriptType()
                .scriptingExpression(new ObjectFactory().createExecute(execute));

        return new ActivityDefinitionType()
                .identifier("Initial import")
                .order(order)
                .beginWork()
                .beginNonIterativeScripting()
                .scriptExecutionRequest(executeScript)
                .<WorkDefinitionsType>end()
                .end();
    }

    private void resetStateToInitialConfig(AjaxRequestTarget target) {
        hideMainPopup(target);

        createAndRunDeleteAllTask();

        target.add(getFeedbackPanel());
        // scroll page up
        target.appendJavaScript("$(function() { window.scrollTo(0, 0); });");
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

    /**
     * Used in delete all task as last activity. Do not remove!
     */
    public static void runInitialDataImport(ApplicationContext context, OperationResult parent) {
        OperationResult result = parent.createSubresult(OPERATION_INITIAL_IMPORT);

        ModelService modelService = context.getBean(ModelService.class);
        CacheDispatcher cacheDispatcher = context.getBean(CacheDispatcher.class);
        TaskManager taskManager = context.getBean(TaskManager.class);
        PrismContext prismContext = context.getBean(PrismContext.class);
        MidpointConfiguration midpointConfiguration = context.getBean(MidpointConfiguration.class);

        try {
            InitialDataImport initialDataImport = new InitialDataImport();
            initialDataImport.setModel(modelService);
            initialDataImport.setTaskManager(taskManager);
            initialDataImport.setPrismContext(prismContext);
            initialDataImport.setConfiguration(midpointConfiguration);
            initialDataImport.init(true);

            // TODO consider if we need to go clusterwide here
            cacheDispatcher.dispatchInvalidation(null, null, true, null);

            modelService.shutdown();

            modelService.postInit(result);

            result.recomputeStatus();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't run initial data import", ex);
        }
    }
}
