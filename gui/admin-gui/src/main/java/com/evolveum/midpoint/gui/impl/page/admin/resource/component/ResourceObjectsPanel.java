/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.button.ReloadableButton;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.util.task.ActivityDefinitionBuilder;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.RepositoryShadowBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.CollectionPanelType;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.CompiledShadowCollectionView;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.ResourceObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.resources.SynchronizationTaskFlavor;
import com.evolveum.midpoint.web.page.admin.shadows.ShadowTablePanel;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;

public abstract class ResourceObjectsPanel extends AbstractObjectMainPanel<ResourceType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectsPanel.class);
    private static final String DOT_CLASS = ResourceObjectsPanel.class.getName() + ".";
    private static final String OPERATION_GET_TOTALS = DOT_CLASS + "getTotals";
    private static final String ID_OBJECT_TYPE = "objectType";
    private static final String ID_TABLE = "table";
    private static final String ID_TITLE = "title";
    private static final String ID_CONFIGURATION = "configuration";
    private static final String ID_STATISTICS = "statistics";
    private static final String ID_SHOW_STATISTICS = "showStatistics";
    private static final String ID_CREATE_TASK = "createTask";
    private static final String OP_CREATE_TASK = DOT_CLASS + "createTask";

    private IModel<Boolean> showStatisticsModel = Model.of(false);

    public ResourceObjectsPanel(String id, ResourceDetailsModel resourceDetailsModel, ContainerPanelConfigurationType config) {
        super(id, resourceDetailsModel, config);
    }

    @Override
    protected void initLayout() {
        createPanelTitle();
        createObjectTypeChoice();
        createConfigureButton();
        createTaskCreateButton();

        createShowStatistics();
        createStatisticsPanel();

        createShadowTable();

        //TODO tasks
    }

    private void createPanelTitle() {
        Label title = new Label(ID_TITLE, getLabelModel());
        title.setOutputMarkupId(true);
        add(title);
    }

    private void createObjectTypeChoice() {
        var objectTypes = new DropDownChoicePanel<>(ID_OBJECT_TYPE,
                Model.of(getObjectDetailsModels().getDefaultObjectType(getKind())),
                () -> getObjectDetailsModels().getResourceObjectTypesDefinitions(getKind()),
                new ResourceObjectTypeChoiceRenderer(), true);
        objectTypes.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(getShadowTable());
            }
        });
        objectTypes.setOutputMarkupId(true);
        add(objectTypes);
    }

    private void createConfigureButton() {
        AjaxIconButton configuration = new AjaxIconButton(
                ID_CONFIGURATION,
                new Model<>("fa fa-cog"),
                createStringResource("ResourceObjectsPanel.button.configure")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PrismContainerValue<ResourceObjectTypeDefinitionType> selectedObjectType = getSelectedObjectType().asPrismContainerValue();
                if (selectedObjectType.isEmpty()) {
                    showNewObjectTypeWizard(target);
                } else {
                    showEditObjectTypeWizard(target, selectedObjectType);
                }
            }
        };
        configuration.showTitleAsLabel(true);
        add(configuration);
    }

    private void createShowStatistics() {
        CheckBoxPanel showStatistics = new CheckBoxPanel(ID_SHOW_STATISTICS, showStatisticsModel, createStringResource("ResourceObjectsPanel.showStatistics")) {

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                super.onUpdate(target);
                target.add(getStatisticsPanel());
            }
        };
        showStatistics.setOutputMarkupId(true);
        add(showStatistics);
    }

    private void createStatisticsPanel() {
        ShadowStatisticsModel statisticsModel = new ShadowStatisticsModel() {

            protected Integer createTotalsModel(final ObjectFilter situationFilter) {
                return countFor(situationFilter);
            }

            @Override
            protected String createSituationLabel(SynchronizationSituationType situation) {
                if (situation == null) {
                    return "N/A";
                }
                return getPageBase().getString(situation);
            }
        };

        ChartJsPanel<ChartConfiguration> shadowStatistics =
                new ChartJsPanel<>(ID_STATISTICS, statisticsModel);
        shadowStatistics.setOutputMarkupId(true);
        shadowStatistics.setOutputMarkupPlaceholderTag(true);
        shadowStatistics.add(new VisibleBehaviour(() -> showStatisticsModel.getObject()));
        add(shadowStatistics);
    }

    private Integer countFor(ObjectFilter situationFilter) {
        ObjectQuery resourceContentQuery = getResourceContentQuery();
        ObjectFilter filter = resourceContentQuery == null ? null : resourceContentQuery.getFilter();
        if (filter == null) {
            return 0;
        }
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createRaw());
        Task task = getPageBase().createSimpleTask(OPERATION_GET_TOTALS);
        OperationResult result = task.getResult();
        try {
            ObjectQuery query = PrismContext.get().queryFactory().createQuery(
                    PrismContext.get().queryFactory().createAnd(filter, situationFilter));
            return getPageBase().getModelService().countObjects(ShadowType.class, query, options, task, result);
        } catch (CommonException | RuntimeException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count shadows", ex);
        }

        return 0;
    }

    private void createShadowTable() {
        ShadowTablePanel shadowTablePanel = new ShadowTablePanel(ID_TABLE, getPanelConfiguration()) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return ResourceObjectsPanel.this.getRepositorySearchTableId();
            }

            @Override
            public PageStorage getPageStorage() {
                return getPageBase().getSessionStorage().getResourceContentStorage(getKind());
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<ShadowType>> createProvider() {
                return ResourceObjectsPanel.this.createProvider(getSearchModel(), (CompiledShadowCollectionView) getObjectCollectionView());
            }

            @Override
            protected SearchContext createAdditionalSearchContext() {
                SearchContext searchContext = new SearchContext();
                searchContext.setPanelType(CollectionPanelType.REPO_SHADOW);
                return searchContext;
            }

            @Override
            protected CompiledShadowCollectionView findContainerPanelConfig() {
                ResourceType resource = getObjectDetailsModels().getObjectType();
                return getPageBase().getCompiledGuiProfile()
                        .findShadowCollectionView(resource.getOid(), getKind(), getIntent());
            }

            @Override
            public CompiledObjectCollectionView getObjectCollectionView() {
                CompiledShadowCollectionView compiledView = findContainerPanelConfig();
                if (compiledView != null) {
                    return compiledView;
                }
                return super.getObjectCollectionView();
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                List<Component> buttonsList = new ArrayList<>();
                buttonsList.add(createReloadButton(buttonId));
                buttonsList.addAll(super.createToolbarButtonsList(buttonId));
                return buttonsList;
            }
        };
        shadowTablePanel.setOutputMarkupId(true);
        add(shadowTablePanel);
    }

    private void createTaskCreateButton() {
        AjaxIconButton createTask = new AjaxIconButton(ID_CREATE_TASK, new Model<>("fa fa-tasks"),
                createStringResource("ResourceObjectsPanel.button.createTask")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                createTaskPerformed(target);
            }
        };
        createTask.showTitleAsLabel(true);
        createTask.setOutputMarkupId(true);
        add(createTask);
    }

    private void createTaskPerformed(AjaxRequestTarget target) {
        TaskCreationPopup createTaskPopup = new TaskCreationPopup(getPageBase().getMainPopupBodyId(), () -> getSelectedObjectType()) {

            @Override
            protected void createNewTaskPerformed(SynchronizationTaskFlavor flavor, boolean simulate, AjaxRequestTarget target) {
                ResourceObjectsPanel.this.createNewTaskPerformed(flavor, simulate, target);
            }
        };
        getPageBase().showMainPopup(createTaskPopup, target);

    }

    private void createNewTaskPerformed(SynchronizationTaskFlavor flavor, boolean isSimulation, AjaxRequestTarget target) {
        var newTask = getPageBase().taskAwareExecutor(target, OP_CREATE_TASK)
                .hideSuccessfulStatus()
                .run((task, result) -> {

                    ResourceType resource = getObjectDetailsModels().getObjectType();
                    ResourceTaskCreator creator =
                            ResourceTaskCreator.forResource(resource, getPageBase())
                                    .ofFlavor(flavor)
                                    .ownedByCurrentUser()
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

    protected abstract UserProfileStorage.TableId getRepositorySearchTableId();
    protected abstract StringResourceModel getLabelModel();

    protected final RepositoryShadowBeanObjectDataProvider createProvider(IModel<Search<ShadowType>> searchModel, CompiledShadowCollectionView collection) {
        RepositoryShadowBeanObjectDataProvider provider = new RepositoryShadowBeanObjectDataProvider(
                getPageBase(), searchModel, null) {
            @Override
            protected PageStorage getPageStorage() {
                return getPageBase().getSessionStorage().getResourceContentStorage(getKind());
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getResourceContentQuery();
            }

        };
        provider.setCompiledObjectCollectionView(collection);
        return provider;
    }

    protected abstract ShadowKindType getKind();

    private ObjectQuery getResourceContentQuery() {
        ResourceObjectTypeDefinition objectType = getSelectedObjectTypeDefinition();
        if (objectType == null) {
            return ObjectQueryUtil.createResourceAndKind(getObjectDetailsModels().getObjectType().getOid(), getKind());
        }
        try {
            return ObjectQueryUtil.createResourceAndKindIntent(
                    getObjectDetailsModels().getObjectType().getOid(),
                    getKind(),
                    objectType.getIntent());
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot create query for resource content", e);
        }
        return null;
    }

    private void showNewObjectTypeWizard(AjaxRequestTarget target) {
        var objectTypeWrapper = PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(),
                ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));

        getObjectDetailsModels().getPageResource().showObjectTypeWizard(target, objectTypeWrapper.getObject().getPath());
    }

    private void showEditObjectTypeWizard(AjaxRequestTarget target, PrismContainerValue<ResourceObjectTypeDefinitionType> selectedObjectType) {
        try {
            PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> def = getObjectWrapperModel().getObject()
                    .findContainerValue(ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE, selectedObjectType.getPath()));
            getObjectDetailsModels().getPageResource().showResourceObjectTypePreviewWizard(
                    target,
                    def.getPath());
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot find object type definition", e);
            target.add(getPageBase().getFeedbackPanel());
        }

    }

    private ReloadableButton createReloadButton(String buttonId) {

        ReloadableButton reload = new ReloadableButton(
                buttonId, getPageBase()) {

            @Override
            protected void refresh(AjaxRequestTarget target) {
                target.add(getShadowTable());
            }

            @Override
            protected ActivityDefinitionType createActivityDefinition() throws SchemaException {

                ResourceObjectTypeDefinition objectType = getSelectedObjectTypeDefinition();
                ShadowKindType kind;
                String resourceOid;
                String intent = null;
                if (objectType == null) {
                    resourceOid = getObjectDetailsModels().getObjectType().getOid();
                    kind = getKind();
                } else {
                    resourceOid = objectType.getResourceOid();
                    kind = objectType.getKind();
                    intent = objectType.getIntent();
                }

                return ActivityDefinitionBuilder.create(new WorkDefinitionsType()
                                ._import(new ImportWorkDefinitionType()
                                        .resourceObjects(new ResourceObjectSetType()
                                                .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                                                .kind(kind)
                                                .intent(intent))))
                        .withExecutionMode(ExecutionModeType.NONE)
                        .build();
            }

            @Override
            protected String getTaskName() {
                return "Reload objects on " + getObjectWrapperObject().asObjectable();
            }
        };
        reload.add(new VisibleBehaviour(() -> getSelectedObjectTypeDefinition() != null));
        return reload;
    }

    private ResourceObjectTypeDefinitionType getSelectedObjectType() {
        ResourceObjectTypeDefinition objectTypeDefinition = getSelectedObjectTypeDefinition();
        return objectTypeDefinition == null ? null : objectTypeDefinition.getDefinitionBean();
    }

    private ResourceObjectTypeDefinition getSelectedObjectTypeDefinition() {
        DropDownChoicePanel<ResourceObjectTypeDefinition> objectTypeSelector = getObjectTypeSelector();
        if (objectTypeSelector != null && objectTypeSelector.getModel() != null) {
            return objectTypeSelector.getModel().getObject();
        }
        return null;
    }

    private String getIntent() {
        ResourceObjectTypeDefinition objectType = getSelectedObjectTypeDefinition();
        if (objectType != null) {
            return objectType.getIntent();
        }
        return null;
    }

    private QName getObjectClass() {
        ResourceObjectTypeDefinition objectType = getSelectedObjectTypeDefinition();
        if (objectType != null) {
            return objectType.getObjectClassName();
        }
        return null;
    }

    private DropDownChoicePanel<ResourceObjectTypeDefinition> getObjectTypeSelector() {
        return (DropDownChoicePanel<ResourceObjectTypeDefinition>) get(ID_OBJECT_TYPE);
    }

    private ShadowTablePanel getShadowTable() {
        return (ShadowTablePanel) get(ID_TABLE);
    }

    private ChartJsPanel getStatisticsPanel() {
        return (ChartJsPanel) get(ID_STATISTICS);
    }

}
