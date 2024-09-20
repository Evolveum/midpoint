/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.search.CollectionPanelType;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceTaskCreator;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.CompiledShadowCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.shadows.ShadowTablePanel;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Implementation classes : ResourceContentResourcePanel,
 * ResourceContentRepositoryPanel
 *
 * @author katkav
 * @author semancik
 */
//Do not use, use ResourceObjectsPanel or ResourceUncategorizedPanel instead
@Deprecated(forRemoval = true)
public abstract class ResourceContentPanel extends BasePanel<PrismObject<ResourceType>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ResourceContentPanel.class);

    private static final String DOT_CLASS = ResourceContentPanel.class.getName() + ".";
    private static final String OP_SEARCH_TASKS_FOR_RESOURCE = DOT_CLASS + "searchTasks";
    private static final String OP_CREATE_TASK = DOT_CLASS + "createTask";

    private static final String ID_TABLE = "table";
    private static final String ID_LABEL = "label";

    private static final String ID_TASK_BUTTONS_CONTAINER = "taskButtonsContainer";
    private static final String ID_IMPORT = "import";
    private static final String ID_RECONCILIATION = "reconciliation";
    private static final String ID_LIVE_SYNC = "liveSync";
    private static final String ID_SIMULATION = "simulation";
    private static final String ID_TOTALS = "totals";

    // FIXME why are these static?
    private static ShadowKindType kind;
    private static String intent;
    private static QName objectClass;

    private String searchMode;

    IModel<PrismObject<ResourceType>> resourceModel;

    private final ContainerPanelConfigurationType config;

    public ResourceContentPanel(String id, IModel<PrismObject<ResourceType>> resourceModel, QName objectClass,
            ShadowKindType kind, String intent, String searchMode, ContainerPanelConfigurationType config) {
        super(id);
        ResourceContentPanel.kind = kind;
        this.searchMode = searchMode;
        this.resourceModel = resourceModel;
        ResourceContentPanel.intent = intent;
        ResourceContentPanel.objectClass = objectClass;
        this.config = config;
    }

    public ContainerPanelConfigurationType getPanelConfiguration() {
        return config;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    public IModel<PrismObject<ResourceType>> getResourceModel() {
        return resourceModel;
    }

    public static QName getObjectClass() {
        return objectClass;
    }

    public static ShadowKindType getKind() {
        return kind;
    }

    public static String getIntent() {
        return intent;
    }

    ResourceObjectDefinition createAttributeSearchItemWrappers() {
        try {
            if (getKind() != null) {
                return getDefinitionByKind();
            }

            if (getObjectClass() != null) {
                return getDefinitionByObjectClass();

            }
        } catch (SchemaException | ConfigurationException e) {
            warn("Could not determine object definition");
        }
        return null;
    }

    public ResourceObjectDefinition getDefinitionByKind() throws SchemaException, ConfigurationException {
        ResourceSchema refinedSchema = getRefinedSchema();
        if (refinedSchema == null) {
            if (WebComponentUtil.isTemplateCategory(resourceModel.getObject().asObjectable())) {
                warn("No schema found in resource.");
            } else {
                warn("No schema found in resource. Please check your configuration and try to test connection for the resource.");
            }
            return null;
        }
        String intent = getIntent();
        if (ShadowUtil.isKnown(intent)) {
            return refinedSchema.findObjectDefinition(getKind(), intent);
        } else {
            // TODO: Can be intent unknown or null here? If so, what should we do with that?
            return refinedSchema.findDefaultDefinitionForKind(getKind());
        }
    }

    public ResourceObjectDefinition getDefinitionByObjectClass() throws SchemaException, ConfigurationException {
        ResourceSchema refinedSchema = getRefinedSchema();
        if (refinedSchema == null) {
            if (WebComponentUtil.isTemplateCategory(resourceModel.getObject().asObjectable())) {
                warn("No schema found in resource.");
            } else {
                warn("No schema found in resource. Please check your configuration and try to test connection for the resource.");
            }
            return null;
        }
        return refinedSchema.findDefinitionForObjectClass(getObjectClass());

    }

    protected ResourceSchema getRefinedSchema() throws SchemaException, ConfigurationException {
        return ResourceSchemaFactory.getCompleteSchema(resourceModel.getObject());
    }

    private UserProfileStorage.TableId getTableId() {
        if (kind == null) {
            return UserProfileStorage.TableId.PAGE_RESOURCE_OBJECT_CLASS_PANEL;
        }

        if (searchMode == null) {
            searchMode = SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT;
        }

        if (searchMode.equals(SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT)) {
            switch (kind) {
//                case ACCOUNT:
//                    return UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL_REPOSITORY_MODE;
//                case GENERIC:
//                    return UserProfileStorage.TableId.PAGE_RESOURCE_GENERIC_PANEL_REPOSITORY_MODE;
//                case ENTITLEMENT:
//                    return UserProfileStorage.TableId.PAGE_RESOURCE_ENTITLEMENT_PANEL_REPOSITORY_MODE;

                default:
                    return UserProfileStorage.TableId.PAGE_RESOURCE_OBJECT_CLASS_PANEL;
            }
        } else if (searchMode.equals(SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT)) {
            switch (kind) {
                case ACCOUNT:
                    return UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL_RESOURCE_MODE;
                case GENERIC:
                    return UserProfileStorage.TableId.PAGE_RESOURCE_GENERIC_PANEL_RESOURCE_MODE;
                case ENTITLEMENT:
                    return UserProfileStorage.TableId.PAGE_RESOURCE_ENTITLEMENT_PANEL_RESOURCE_MODE;

                default:
                    return UserProfileStorage.TableId.PAGE_RESOURCE_OBJECT_CLASS_PANEL;
            }
        }
        return UserProfileStorage.TableId.PAGE_RESOURCE_OBJECT_CLASS_PANEL;
    }

    private void initLayout() {

        WebMarkupContainer totals = new WebMarkupContainer(ID_TOTALS);
        totals.setOutputMarkupId(true);
        add(totals);
        initShadowStatistics(totals);

        ShadowTablePanel shadowListPanel = new ShadowTablePanel(ID_TABLE, getPanelConfiguration()) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return ResourceContentPanel.this.getTableId();
            }

            @Override
            public PageStorage getPageStorage() {
                return getPageBase().getSessionStorage().getResourceContentStorage(kind, searchMode);
            }

            @Override
            protected ISelectableDataProvider createProvider() {
                SelectableBeanObjectDataProvider<ShadowType> provider = createSelectableBeanObjectDataProvider(() -> getResourceContentQuery(), null,
                        createSearchOptions());
                provider.setEmptyListOnNullQuery(true);
                provider.setSort(null);
                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                customizeProvider(provider);
                return provider;
            }

            @Override
            protected SearchContext createAdditionalSearchContext() {
                SearchContext searchContext = new SearchContext();
                searchContext.setResourceObjectDefinition(createAttributeSearchItemWrappers());
                searchContext.setPanelType(searchMode == SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT ? CollectionPanelType.REPO_SHADOW : CollectionPanelType.RESOURCE_SHADOW);
                return searchContext;
            }

            @Override
            protected CompiledShadowCollectionView findContainerPanelConfig() {
                return ResourceContentPanel.this.findContainerPanelConfig();
            }

            @Override
            public CompiledObjectCollectionView getObjectCollectionView() {
                CompiledShadowCollectionView compiledView = findContainerPanelConfig();
                if (compiledView != null) {
                    return compiledView;
                }
                return super.getObjectCollectionView();
            }

            protected ModelExecuteOptions createModelExecuteOptions() {
                return createModelOptions();
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                List<Component> buttonsList = new ArrayList<>();
                buttonsList.addAll(ResourceContentPanel.this.createToolbarButtonsList(buttonId));
                buttonsList.addAll(super.createToolbarButtonsList(buttonId));
                return buttonsList;
            }

            @Override
            protected boolean isShadowDetailsEnabled(IModel<SelectableBean<ShadowType>> rowModel) {
                return ResourceContentPanel.this.isShadowDetailsEnabled(rowModel);
            }
        };
        shadowListPanel.setOutputMarkupId(true);
        shadowListPanel.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return createQuery() != null;
            }
        });
        shadowListPanel.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_SHADOW_BOX_CSS_CLASSES);
        add(shadowListPanel);

        Label label = new Label(ID_LABEL, createStringResource("ResourceContentPanel.message.nothingToShow"));
        add(label);
        label.setOutputMarkupId(true);
        label.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return createQuery() == null;
            }
        });

        WebMarkupContainer taskButtonsContainer = new WebMarkupContainer(ID_TASK_BUTTONS_CONTAINER);
        taskButtonsContainer.setOutputMarkupId(true);
        taskButtonsContainer.add(new VisibleBehaviour(this::isTaskButtonsContainerVisible));
        add(taskButtonsContainer);

        initButton(
                ID_IMPORT,
                "ResourceContentPanel.button.import",
                " fa-download",
                SynchronizationTaskFlavor.IMPORT,
                taskButtonsContainer);
        initButton(
                ID_RECONCILIATION,
                "ResourceContentPanel.button.reconciliation",
                " fa-link",
                SynchronizationTaskFlavor.RECONCILIATION,
                taskButtonsContainer);
        initButton(
                ID_LIVE_SYNC,
                "ResourceContentPanel.button.liveSync",
                " fa-sync-alt",
                SynchronizationTaskFlavor.LIVE_SYNC,
                taskButtonsContainer);

        initSimulationButton(
                ID_SIMULATION,
                "ResourceContentPanel.button.simulation",
                " fa-flask",
                taskButtonsContainer);

        initCustomLayout();
    }

    protected boolean isShadowDetailsEnabled(IModel<SelectableBean<ShadowType>> rowModel) {
        return true;
    }

    private void initSimulationButton(String id, String label, String icon, WebMarkupContainer taskButtonsContainer) {
        List<InlineMenuItem> items = new ArrayList<>();

        ObjectQuery existingTasksQuery = getExistingTasksQuery(
                SystemObjectsType.ARCHETYPE_IMPORT_TASK.value(),
                SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value(),
                SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value());
        OperationResult result = new OperationResult(OP_SEARCH_TASKS_FOR_RESOURCE);
        List<PrismObject<TaskType>> tasksList = WebModelServiceUtils.searchObjects(TaskType.class, existingTasksQuery,
                result, getPageBase());

        List<PrismObject<TaskType>> simulatedTasks = getSimulatedTasks(tasksList);

        InlineMenuItem item = new InlineMenuItem(
                getPageBase().createStringResource("ResourceContentResourcePanel.showExisting")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        List<TaskType> filteredByKindIntentTasks = getTasksForKind(simulatedTasks);
                        redirectToTasksListPage(createInTaskOidQuery(filteredByKindIntentTasks), null);
                    }
                };
            }
        };
        items.add(item);

        item = new InlineMenuItem(getPageBase().createStringResource("ResourceContentPanel.button.simulation.import")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        newTaskPerformed(target, SynchronizationTaskFlavor.IMPORT, true);
                    }
                };
            }
        };
        items.add(item);

        item = new InlineMenuItem(getPageBase().createStringResource("ResourceContentPanel.button.simulation.reconciliation")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        newTaskPerformed(target, SynchronizationTaskFlavor.RECONCILIATION, true);
                    }
                };
            }
        };
        items.add(item);

        item = new InlineMenuItem(getPageBase().createStringResource("ResourceContentPanel.button.simulation.liveSync")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        newTaskPerformed(target, SynchronizationTaskFlavor.LIVE_SYNC, true);
                    }
                };
            }
        };
        items.add(item);

        DropdownButtonPanel button = new DropdownButtonPanel(id,
                new DropdownButtonDto(String.valueOf(simulatedTasks.size()), icon, getString(label), items)) {
            @Override
            protected String getSpecialDropdownMenuClass() {
                return "dropdown-menu-left";
            }
        };
        taskButtonsContainer.add(button);
    }

    protected Collection<? extends Component> createToolbarButtonsList(String buttonId) {
        return new ArrayList<>();
    }

    protected void customizeProvider(SelectableBeanObjectDataProvider<ShadowType> provider) {
    }

    protected boolean isTaskButtonsContainerVisible() {
        return true;
    }

    private CompiledShadowCollectionView findContainerPanelConfig() {
        PrismObject<ResourceType> resource = getResourceModel().getObject();
        return getPageBase().getCompiledGuiProfile().findShadowCollectionView(resource.getOid(), getKind(), getIntent());
    }

    private ObjectQuery getResourceContentQuery() {
        ObjectQuery customQuery = ResourceContentPanel.this.createQuery();
        if (customQuery != null && customQuery.getFilter() != null) {
            return customQuery;
        }
        return null;
    }

    protected abstract void initShadowStatistics(WebMarkupContainer totals);

    private void initButton(
            String id, String label, String icon, SynchronizationTaskFlavor flavor, WebMarkupContainer taskButtonsContainer) {

        ObjectQuery existingTasksQuery = getExistingTasksQuery(flavor.getArchetypeOid());
        OperationResult result = new OperationResult(OP_SEARCH_TASKS_FOR_RESOURCE);
        List<PrismObject<TaskType>> tasksList = WebModelServiceUtils.searchObjects(TaskType.class, existingTasksQuery,
                result, getPageBase());

        List<InlineMenuItem> items = new ArrayList<>();

        InlineMenuItem item = new InlineMenuItem(
                getPageBase().createStringResource("ResourceContentResourcePanel.showExisting")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        List<TaskType> filteredByKindIntentTasks = getTasksForKind(tasksList);
                        redirectToTasksListPage(createInTaskOidQuery(filteredByKindIntentTasks), flavor.getArchetypeOid());
                    }
                };
            }
        };
        items.add(item);

        item = new InlineMenuItem(getPageBase().createStringResource("ResourceContentResourcePanel.newTask")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        newTaskPerformed(target, flavor, false);
                    }
                };
            }
        };
        items.add(item);

        DropdownButtonPanel button = new DropdownButtonPanel(id,
                new DropdownButtonDto(String.valueOf(tasksList.size()), icon, getString(label), items)) {
            @Override
            protected String getSpecialDropdownMenuClass() {
                return "dropdown-menu-left";
            }
        };
        taskButtonsContainer.add(button);

    }

    private void newTaskPerformed(
            @NotNull AjaxRequestTarget target, @NotNull SynchronizationTaskFlavor flavor, boolean isSimulation) {

        var newTask = getPageBase().taskAwareExecutor(target, OP_CREATE_TASK)
                .hideSuccessfulStatus()
                .run((task, result) -> {

                    if (Objects.isNull(getObjectClass())) {
                        updateDefinitions(); // FIXME not static
                    }

                    PrismObject<ResourceType> resource = getResourceModel().getObject();
                    ResourceTaskCreator creator =
                            ResourceTaskCreator.forResource(resource.asObjectable(), getPageBase())
                                    .ofFlavor(flavor)
                                    .withCoordinates(
                                            getKind(), // FIXME not static
                                            getIntent(), // FIXME not static
                                            getObjectClass()); // FIXME not static

                    if (isSimulation) {
                        creator = creator
                                .withExecutionMode(ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW)
                                .withPredefinedConfiguration(PredefinedConfigurationType.DEVELOPMENT)
                                .withSimulationResultDefinition(
                                        new SimulationDefinitionType().useOwnPartitionForProcessedObjects(false));
                    }

                    return creator.create(task, result);
                });

        if (newTask != null) {
            DetailsPageUtil.dispatchToNewObject(newTask, getPageBase());
        }
    }

    private ObjectQuery createInTaskOidQuery(List<TaskType> tasksList) {
        if (tasksList == null) {
            tasksList = new ArrayList<>();
        }
        List<String> taskOids = new ArrayList<>();
        tasksList.forEach(task -> taskOids.add(task.getOid()));
        return getPageBase().getPrismContext().queryFor(TaskType.class)
                .id(taskOids.toArray(new String[0]))
                .build();
    }

    private void redirectToTasksListPage(ObjectQuery tasksQuery, @Nullable String archetypeOid) {
        String taskCollectionViewName = getTaskCollectionViewNameByArchetypeOid(archetypeOid);
        PageParameters pageParameters = new PageParameters();

        if (StringUtils.isNotEmpty(taskCollectionViewName)) {
            pageParameters.add(PageBase.PARAMETER_OBJECT_COLLECTION_NAME, taskCollectionViewName);
        }

        PageTasks pageTasks = new PageTasks(tasksQuery, pageParameters);
        getPageBase().setResponsePage(pageTasks);
    }

    private String getTaskCollectionViewNameByArchetypeOid(String archetypeOid) {
        if (StringUtils.isEmpty(archetypeOid)) {
            return "";
        }
        List<CompiledObjectCollectionView> taskCollectionViews =
                getPageBase().getCompiledGuiProfile().findAllApplicableArchetypeViews(TaskType.class);
        for (CompiledObjectCollectionView view : taskCollectionViews) {
            if (archetypeOid.equals(view.getCollection().getCollectionRef().getOid())) {
                return view.getViewIdentifier();
            }
        }
        return "";
    }

    private List<TaskType> getTasksForKind(List<PrismObject<TaskType>> tasks) {
        List<TaskType> tasksForKind = new ArrayList<>();
        for (PrismObject<TaskType> task : tasks) {
            ShadowKindType taskKindValue;

            @Nullable ResourceObjectSetType resourceSet = ResourceObjectSetUtil.fromTask(task.asObjectable());
            if (!java.util.Objects.isNull(resourceSet)) {
                taskKindValue = resourceSet.getKind();
                if (Objects.isNull(taskKindValue)) {
                    QName taskObjectClass = resourceSet.getObjectclass();
                    if (Objects.isNull(taskObjectClass)) {
                        LOGGER.warn("Bad task definition. Task {} doesn't contain definition either of objectClass or kind/intent", task.getOid());
                        continue;
                    }

                    QName objectClass = getObjectClass();
                    if (Objects.isNull(objectClass)) {
                        LOGGER.trace("Trying to determine objectClass for kind: {}, intent: {}", getKind(), getIntent());
                        ResourceObjectDefinition objectClassDef = null;
                        try {
                            objectClassDef = getDefinitionByKind();
                        } catch (SchemaException | ConfigurationException e) {
                            LOGGER.error("Failed to search for objectClass definition. Reason: {}", e.getMessage(), e);
                        }
                        if (objectClassDef == null) {
                            LOGGER.warn("Cannot find any definition for kind: {}, intent: {}", getKind(), getIntent());
                            continue;
                        }

                        objectClass = objectClassDef.getTypeName();
                    }

                    if (QNameUtil.match(objectClass, taskObjectClass)) {
                        tasksForKind.add(task.asObjectable());
                    }

                } else {
                    String taskIntentValue = resourceSet.getIntent();
                    if (StringUtils.isNotEmpty(getIntent())) {
                        if (getKind() == taskKindValue && getIntent().equals(taskIntentValue)) {
                            tasksForKind.add(task.asObjectable());
                        }
                    } else if (getKind() == taskKindValue) {
                        tasksForKind.add(task.asObjectable());
                    }
                }
            }
        }
        return tasksForKind;
    }

    private List<PrismObject<TaskType>> getSimulatedTasks(List<PrismObject<TaskType>> tasks) {
        List<PrismObject<TaskType>> simulatedTasks = new ArrayList<>();
        for (PrismObject<TaskType> task : tasks) {
            if (task == null) {
                continue;
            }
            @NotNull TaskType bean = task.asObjectable();
            if (bean.getActivity() != null
                    && bean.getActivity().getReporting() != null
                    && bean.getActivity().getReporting().getSimulationResult() != null) {
                simulatedTasks.add(task);
            }
        }
        return simulatedTasks;
    }

    protected void initCustomLayout() {
        // Nothing to do, for subclass extension
    }

    protected ObjectQuery createQuery() {
        ObjectQuery baseQuery = null;

        try {
            if (kind == null) {
                if (objectClass == null) {
                    return null;
                }
                return ObjectQueryUtil.createResourceAndObjectClassQuery(resourceModel.getObject().getOid(), objectClass);
            }

            ResourceObjectDefinition rOcDef = getDefinitionByKind();
            if (rOcDef != null) {
                baseQuery = rOcDef.createShadowSearchQuery(resourceModel.getObject().getOid());
            }
        } catch (SchemaException | ConfigurationException ex) {
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Could not crate query for shadows: " + ex.getMessage(), ex);
        }
        return baseQuery;
    }

    private Collection<SelectorOptions<GetOperationOptions>> createSearchOptions() {
        GetOperationOptionsBuilder builder = getPageBase().getOperationOptionsBuilder()
                .item(ShadowType.F_ASSOCIATIONS).dontRetrieve();
        builder = addAdditionalOptions(builder);
        return builder.build();
    }

    protected abstract ModelExecuteOptions createModelOptions();

    private ObjectQuery getExistingTasksQuery(String... archetypeRefOids) {
        return getPageBase().getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(resourceModel.getObject().getOid())
                .and().item(AssignmentHolderType.F_ARCHETYPE_REF).ref(archetypeRefOids)
                .build();
    }

    protected abstract GetOperationOptionsBuilder addAdditionalOptions(GetOperationOptionsBuilder builder);

    protected abstract boolean isUseObjectCounting();

    public ShadowTablePanel getTable() {
        return (ShadowTablePanel) get(ID_TABLE);
    }

    protected void updateDefinitions() {
        LOGGER.trace("Trying to update definitions for kind: {}, intent: {}", getKind(), getIntent());
        ResourceObjectDefinition objectClassDef = null;
        try {
            objectClassDef = getDefinitionByKind();
        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Failed to search for objectClass definition. Reason: {}", e.getMessage(), e);
        }
        if (objectClassDef == null) {
            LOGGER.warn("Cannot find any definition for kind: {}, intent: {}", getKind(), getIntent());
        } else {
            objectClass = objectClassDef.getTypeName();
            if (getIntent() == null) {
                ResourceObjectTypeDefinition typeDefinition = objectClassDef.getTypeDefinition();
                if (typeDefinition != null) {
                    intent = typeDefinition.getIntent();
                }
            }
        }
    }
}
