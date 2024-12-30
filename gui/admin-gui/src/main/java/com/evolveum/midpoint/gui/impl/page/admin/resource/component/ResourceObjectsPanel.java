/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import static com.evolveum.midpoint.common.LocalizationTestUtil.getLocalizationService;

import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic.AssociationDefinitionWrapper;
import com.evolveum.midpoint.gui.impl.util.AssociationChildWrapperUtil;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.web.session.ResourceContentStorage;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.button.ReloadableButton;
import com.evolveum.midpoint.gui.impl.component.data.provider.RepositoryShadowBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.input.LifecycleStatePanel;
import com.evolveum.midpoint.gui.impl.component.search.CollectionPanelType;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.ResourceObjectTypeWizardChoicePanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.CompiledShadowCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.ActivityDefinitionBuilder;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.ResourceObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.SynchronizationTaskFlavor;
import com.evolveum.midpoint.web.page.admin.shadows.ShadowTablePanel;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;

public abstract class ResourceObjectsPanel extends AbstractResourceObjectPanel {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectsPanel.class);

    private static final String DOT_CLASS = ResourceObjectsPanel.class.getName() + ".";
    private static final String OPERATION_GET_TOTALS = DOT_CLASS + "getTotals";
    private static final String ID_OBJECT_TYPE = "objectType";
    private static final String ID_TABLE = "table";
    private static final String ID_TITLE = "title";
    private static final String ID_DEFAULT_OPERATIONAL_POLICIES = "defaultOperationalPolicies";
    private static final String ID_CONFIGURATION = "configuration";
    private static final String ID_LIFECYCLE_STATE = "lifecycleState";
    private static final String OP_SET_LIFECYCLE_STATE_FOR_OBJECT_TYPE = DOT_CLASS + "setLyfecycleStateForObjectType";
    private static final String ID_CHART_CONTAINER = "chartContainer";
    private static final String ID_STATISTICS = "statistics";
    private static final String ID_SHOW_STATISTICS = "showStatistics";
    private static final String ID_TASKS = "tasks";

    private final IModel<Boolean> showStatisticsModel = Model.of(false);

    private LoadableDetachableModel<PrismPropertyWrapper<String>> lifecycleStateModel;

    public ResourceObjectsPanel(String id, ResourceDetailsModel resourceDetailsModel, ContainerPanelConfigurationType config) {
        super(id, resourceDetailsModel, config);
    }

    @Override
    protected void initLayout() {
        createPanelTitle();
        createObjectTypeChoice();

        createLifecycleStatePanel();
        createConfigureButton();
        createTasksButton(ID_TASKS);

        createShowStatistics();
        createStatisticsPanel();

        createDefaultOperationalPolicies();

        createShadowTable();

        //TODO tasks
    }

    private void createLifecycleStatePanel() {
        if (lifecycleStateModel == null) {
            lifecycleStateModel = new LoadableDetachableModel<>() {
                @Override
                protected PrismPropertyWrapper<String> load() {
                    if (getSelectedObjectType() != null) {
                        ItemPath pathToProperty = ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE)
                                .append(getSelectedObjectType().asPrismContainerValue().getPath())
                                .append(ResourceObjectTypeDefinitionType.F_LIFECYCLE_STATE);
                        try {
                            return getObjectWrapperModel().getObject().findProperty(pathToProperty);
                        } catch (SchemaException e) {
                            LOGGER.error("Couldn't find property with path " + pathToProperty);
                        }
                    }

                    return null;
                }
            };
        }

        LifecycleStatePanel panel = new LifecycleStatePanel(ID_LIFECYCLE_STATE, lifecycleStateModel) {
            @Override
            protected void onInitialize() {
                super.onInitialize();
                getBaseFormComponent().add(new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {

                        if (getSelectedObjectType() == null) {
                            return;
                        }

                        Task task = getPageBase().createSimpleTask(OP_SET_LIFECYCLE_STATE_FOR_OBJECT_TYPE);
                        OperationResult result = task.getResult();
                        ItemPath pathToProperty = ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE)
                                .append(getSelectedObjectType().asPrismContainerValue().getPath())
                                .append(ResourceObjectTypeDefinitionType.F_LIFECYCLE_STATE);

                        WebComponentUtil.saveLifeCycleStateOnPath(
                                getObjectWrapperObject(),
                                pathToProperty,
                                target,
                                task,
                                result,
                                getPageBase());

                        try {
                            if (result.isSuccess()) {
                                String realValue = lifecycleStateModel.getObject().getValue().getRealValue();
                                lifecycleStateModel.getObject().getValue().getOldValue().setValue(realValue);
                            }
                        } catch (SchemaException e) {
                            LOGGER.error("Couldn't get value of " + lifecycleStateModel.getObject());
                        }
                    }
                });
            }
        };
        panel.setOutputMarkupId(true);
        panel.add(new VisibleBehaviour(() -> getSelectedObjectTypeDefinition() != null));
        add(panel);
    }

    private void createPanelTitle() {
        Label title = new Label(ID_TITLE, getLabelModel());
        title.setOutputMarkupId(true);
        add(title);
    }

    private void createDefaultOperationalPolicies() {
        Label defaultOperationalPolicies = new Label(ID_DEFAULT_OPERATIONAL_POLICIES, getDefaultOperationPolicy());
        defaultOperationalPolicies.add(AttributeAppender.append("title", getDefaultOperationPolicyTitle()));
        defaultOperationalPolicies.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getDefaultOperationPolicy().getObject())));
        defaultOperationalPolicies.setOutputMarkupId(true);
        add(defaultOperationalPolicies);
    }

    private IModel<String> getDefaultOperationPolicyTitle() {
        IModel<String> objectTypeModel = () -> {
            ResourceObjectTypeDefinition def = getSelectedObjectTypeDefinition();
            if (def == null) {
                return null;
            }

            return GuiDisplayNameUtil.getDisplayName(getSelectedObjectTypeDefinition().getDefinitionBean());
        };

        return createStringResource(
                "ResourceObjectsPanel.defaultOperationPolicy",
                new Object[] {
                        objectTypeModel,
                        getDefaultOperationPolicy() }
        );
    }

    private IModel<String> getDefaultOperationPolicy() {
        return () -> {
            ResourceObjectTypeDefinition def = getSelectedObjectTypeDefinition();
            if (def == null) {
                return null;
            }

            @NotNull ResourceObjectTypeDefinitionType objectType = def.getDefinitionBean();
            if (objectType.getDefaultOperationPolicy().isEmpty()) {
                return null;
            }

            Object[] marks = objectType.getDefaultOperationPolicy().stream()
                    .filter(policy -> policy != null && policy.getPolicyRef() != null && !policy.getPolicyRef().asReferenceValue().isEmpty())
                    .map(policy -> new AbstractMap.SimpleEntry(WebModelServiceUtils.loadObject(policy.getPolicyRef(), getPageBase()), policy.getLifecycleState()))
                    .filter(pair -> pair.getKey() != null)
                    .map(pair -> WebComponentUtil.getDisplayNameOrName((PrismObject) pair.getKey())
                            + (((pair.getValue() != null && !SchemaConstants.LIFECYCLE_ACTIVE.equals(pair.getValue())) ?
                            (" (" + LocalizationUtil.translateLifecycleState((String) pair.getValue(), getPageBase()) + ")") : "")))
                    .toArray();

            return StringUtils.joinWith(", ", marks);
        };
    }

    private void createObjectTypeChoice() {

        LoadableDetachableModel<ResourceObjectTypeIdentification> objectTypeModel = new LoadableDetachableModel<>() {
            @Override
            protected ResourceObjectTypeIdentification load() {
                ResourceContentStorage storage = getPageStorage();
                String intent = null;
                if (storage != null) {
                    intent = storage.getContentSearch().getIntent();
                }
                if (intent == null) {
                    return null;
                }
                return getObjectDetailsModels().getObjectTypeDefinition(getKind(), intent).getTypeIdentification();
            }

            @Override
            public void setObject(ResourceObjectTypeIdentification object) {
                ResourceContentStorage storage = getPageStorage();
                if (storage != null) {
                    storage.getContentSearch().setIntent(object == null ? null : object.getIntent());
                }
            }
        };

        if (getPageStorage() == null || getPageStorage().getContentSearch().getIntent() == null) {
            ResourceObjectTypeDefinition defaultObjectTypeDef = getObjectDetailsModels().getDefaultObjectType(getKind());
            if (defaultObjectTypeDef != null) {
                objectTypeModel.setObject(defaultObjectTypeDef.getTypeIdentification());
            }
        }

        LoadableDetachableModel<List<? extends ResourceObjectTypeIdentification>> choices = new LoadableDetachableModel<>() {
            @Override
            protected List<? extends ResourceObjectTypeIdentification> load() {
                List<? extends ResourceObjectTypeDefinition> choices = getObjectDetailsModels()
                        .getResourceObjectTypesDefinitions(getKind());
                return choices != null ? choices.stream().map(ResourceObjectTypeDefinition::getTypeIdentification).toList() : Collections.emptyList();
            }
        };

        var objectTypes = new DropDownChoicePanel<>(
                ID_OBJECT_TYPE,
                objectTypeModel,
                choices,
                new ResourceObjectTypeChoiceRenderer(getObjectDetailsModels()), true) {

        };

        objectTypes.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(get(ID_LIFECYCLE_STATE));
                target.add(get(ID_LIFECYCLE_STATE).getParent());
                target.add(get(ID_CONFIGURATION));
                target.add(get(ID_TASKS));
                target.add(get(ID_DEFAULT_OPERATIONAL_POLICIES));
                target.add(getShadowTable());
                lifecycleStateModel.detach();
                objectTypeModel.detach();
            }
        });
        objectTypes.setOutputMarkupId(true);
        add(objectTypes);
    }

    private void createConfigureButton() {

        List<InlineMenuItem> items = new ArrayList<>();
        items.add(createWizardItemPanel(
                ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType.BASIC,
                "showResourceObjectTypeBasicWizard"));

        items.add(createWizardItemPanel(
                ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType.SYNCHRONIZATION,
                "showSynchronizationWizard"));

        items.add(createWizardItemPanel(
                ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType.ATTRIBUTE_MAPPING,
                "showAttributeMappingWizard"));

        items.add(createWizardItemPanel(
                ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType.CORRELATION,
                "showCorrelationWizard"));

        items.add(createWizardItemPanel(
                ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType.CAPABILITIES,
                "showCapabilitiesWizard"));

        items.add(createWizardItemPanel(
                ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType.CREDENTIALS,
                "showCredentialsWizard"));

        items.add(createWizardItemPanel(
                ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType.ACTIVATION,
                "showActivationsWizard"));

        items.add(createWizardItemPanel(
                ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType.POLICIES,
                "showPoliciesWizard"));

        addingAssociationButtonItems(items);

        DropdownButtonDto model = new DropdownButtonDto(null, "fa fa-cog", getString("ResourceObjectsPanel.button.configure"), items);
        DropdownButtonPanel configurationPanel = new DropdownButtonPanel(ID_CONFIGURATION, model) {
            @Override
            protected String getSpecialButtonClass() {
                return "btn-sm btn-primary";
            }

            protected String getSpecialDropdownMenuClass() {
                return "dropdown-menu-left";
            }

            @Override
            protected boolean showIcon() {
                return true;
            }
        };
        configurationPanel.setOutputMarkupId(true);
        configurationPanel.add(new VisibleBehaviour(() -> getSelectedObjectTypeDefinition() != null));
        add(configurationPanel);
    }

    private void addingAssociationButtonItems(List<InlineMenuItem> items) {
        ButtonInlineMenuItem menu = new ButtonInlineMenuItem(
                createStringResource("ResourceObjectsPanel.button.association.create")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            Method method = PageResource.class.getMethod(
                                    "showAssociationTypeWizard", AjaxRequestTarget.class, IModel.class);
                            method.invoke(getObjectDetailsModels().getPageResource(),
                                    target,
                                    createStringResource("ResourceObjectsPanel.associationWizard.exitLabel." + getKind()));
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                            LOGGER.error("Couldn't invoke method showAssociationTypeWizard in PageResource class");
                        }
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-code-compare");
            }
        };
        items.add(menu);

        menu = new ButtonInlineMenuItem(
                createStringResource("ResourceObjectsPanel.button.association.inbound")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ResourceObjectTypeDefinition selectedObjectType = getSelectedObjectTypeDefinition();
                        if (selectedObjectType == null) {
                            getPageBase().warn(LocalizationUtil.translate("PageResource.objectTypeIsNullForAssociation"));
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }

                        try {
                            List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> associations =
                                    AssociationChildWrapperUtil.findAssociationDefinitions(getObjectWrapper(), selectedObjectType);

                            if (associations.isEmpty()) {
                                return;
                            }

                            if (associations.size() == 1) {
                                showAssociationInboundWizard(associations.iterator().next(), target);
                            } else {
                                ChoiceAssociationPopupPanel popup = new ChoiceAssociationPopupPanel(
                                        getPageBase().getMainPopupBodyId(),
                                        getObjectDetailsModels(),
                                        associations) {
                                    @Override
                                    protected void onTileClickPerformed(AssociationDefinitionWrapper value, AjaxRequestTarget target) {
                                        showAssociationInboundWizard(value.getSourceValue(), target);
                                    }
                                };
                                getPageBase().showMainPopup(popup, target);
                            }

                        } catch (SchemaException e) {
                            LOGGER.error("Couldn't load association for " + selectedObjectType);
                        }
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-arrow-right-to-bracket");
            }
        };
        menu.setVisible(createAssociationMenuItemVisibilityModel());
        items.add(menu);

        menu = new ButtonInlineMenuItem(
                createStringResource("ResourceObjectsPanel.button.association.outbound")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ResourceObjectTypeDefinition selectedObjectType = getSelectedObjectTypeDefinition();
                        if (selectedObjectType == null) {
                            getPageBase().warn(LocalizationUtil.translate("PageResource.objectTypeIsNullForAssociation"));
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }

                        try {
                            List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> associations =
                                    AssociationChildWrapperUtil.findAssociationDefinitions(getObjectWrapper(), selectedObjectType);

                            if (associations.isEmpty()) {
                                return;
                            }

                            if (associations.size() == 1) {
                                showAssociationOutboundWizard(associations.iterator().next(), target);
                            } else {
                                ChoiceAssociationPopupPanel popup = new ChoiceAssociationPopupPanel(
                                        getPageBase().getMainPopupBodyId(),
                                        getObjectDetailsModels(),
                                        associations) {
                                    @Override
                                    protected void onTileClickPerformed(AssociationDefinitionWrapper value, AjaxRequestTarget target) {
                                        showAssociationOutboundWizard(value.getSourceValue(), target);
                                    }
                                };
                                getPageBase().showMainPopup(popup, target);
                            }

                        } catch (SchemaException e) {
                            LOGGER.error("Couldn't load association for " + selectedObjectType);
                        }
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-arrow-right-from-bracket");
            }
        };
        menu.setVisible(createAssociationMenuItemVisibilityModel());
        items.add(menu);
    }

    private IModel<Boolean> createAssociationMenuItemVisibilityModel() {
        return () -> {
            ResourceObjectTypeDefinition selectedObjectType = getSelectedObjectTypeDefinition();
            if (selectedObjectType == null) {
                return false;
            }
            try {
                List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> associations =
                        AssociationChildWrapperUtil.findAssociationDefinitions(getObjectWrapper(), selectedObjectType);
                return !associations.isEmpty();
            } catch (SchemaException e) {
                LOGGER.error("Couldn't load association for " + selectedObjectType);
                return false;
            }
        };
    }

    private void showAssociationInboundWizard(PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> associationWrapper, AjaxRequestTarget target) {
        ShadowAssociationTypeDefinitionType association = associationWrapper.getRealValue();
        if (association == null) {
            return;
        }

        if (association.getSubject() == null || association.getSubject().getAssociation() == null) {
            return;
        }

        PageResource page = getObjectDetailsModels().getPageResource();
        if (association.getSubject().getAssociation().getInbound().size() == 1) {
            ItemPath path = association.getSubject().getAssociation().getInbound().iterator().next().asPrismContainerValue().getPath();

            page.showAssociationInboundWizard(target, path, association, createExitLabelModel());
        } else {
            ItemPath path = association.asPrismContainerValue().getPath()
                    .append(ShadowAssociationTypeDefinitionType.F_SUBJECT)
                    .append(ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION);

            page.showAssociationInboundsWizard(target, path, association, createExitLabelModel());
        }
    }

    private IModel<String> createExitLabelModel() {
        return new StringResourceModel("ResourceObjectsPanel.associationWizard.exitLabel." + getKind());
    }

    private void showAssociationOutboundWizard(PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> associationWrapper, AjaxRequestTarget target) {
        ShadowAssociationTypeDefinitionType association = associationWrapper.getRealValue();
        if (association == null) {
            return;
        }

        if (association.getSubject() == null || association.getSubject().getAssociation() == null) {
            return;
        }

        PageResource page = getObjectDetailsModels().getPageResource();
        if (association.getSubject().getAssociation().getOutbound().size() == 1) {
            ItemPath path = association.getSubject().getAssociation().getOutbound().iterator().next().asPrismContainerValue().getPath();

            page.showAssociationOutboundWizard(target, path, association, createExitLabelModel());
        } else {
            ItemPath path = association.asPrismContainerValue().getPath()
                    .append(ShadowAssociationTypeDefinitionType.F_SUBJECT)
                    .append(ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION);

            page.showAssociationOutboundsWizard(target, path, association, createExitLabelModel());
        }
    }

    private ButtonInlineMenuItem createWizardItemPanel(
            @NotNull ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType wizardType,
            @NotNull String methodName
    ) {
        return new ButtonInlineMenuItem(
                createStringResource(wizardType)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ResourceObjectTypeDefinitionType selectedObjectType = getSelectedObjectType();
                        if (selectedObjectType != null) {
                            try {
                                Method method = PageResource.class.getMethod(
                                        methodName, AjaxRequestTarget.class, ItemPath.class);
                                method.invoke(
                                        getObjectDetailsModels().getPageResource(),
                                        target,
                                        ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE)
                                                .append(selectedObjectType.asPrismContainerValue().getPath()));
                            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                LOGGER.error("Couldn't invoke method " + methodName + " in PageResource class");
                            }
                        }
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(wizardType.getIcon());
            }
        };
    }

    private void createShowStatistics() {
        CheckBoxPanel showStatistics = new CheckBoxPanel(ID_SHOW_STATISTICS, showStatisticsModel,
                createStringResource("ResourceObjectsPanel.showStatistics")) {

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                super.onUpdate(target);
                target.add(getStatisticsPanel().getParent());
            }
        };
        showStatistics.setOutputMarkupId(true);
        add(showStatistics);
    }

    private void createStatisticsPanel() {
        WebMarkupContainer chartContainer = new WebMarkupContainer(ID_CHART_CONTAINER);
        chartContainer.setOutputMarkupId(true);
        chartContainer.add(new VisibleBehaviour(showStatisticsModel::getObject));
        add(chartContainer);

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
        chartContainer.add(shadowStatistics);
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
                return ResourceObjectsPanel.this.getPageStorage();
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<ShadowType>> createProvider() {
                return ResourceObjectsPanel.this.createProvider(getSearchModel(), (CompiledShadowCollectionView) getObjectCollectionView());
            }

            @Override
            protected SearchContext createAdditionalSearchContext() {
                SearchContext searchContext = new SearchContext();
                searchContext.setPanelType(CollectionPanelType.REPO_SHADOW);
                // MID-9569: selectedObjectDefinition has knowledge about detailed shadow type, so we can provide it
                // directly to search (since we are also adding coordinates to filter) so Axiom Query can access
                // additional attributes
                var resTypeDef = getSelectedObjectTypeDefinition();
                if (resTypeDef != null) {
                    searchContext.setDefinitionOverride(resTypeDef.getPrismObjectDefinition());
                }
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

    private ResourceContentStorage getPageStorage() {
        return getPageBase().getSessionStorage().getResourceContentStorage(getKind());
    }

    @Override
    protected void addViewMenuItemsForCreateTaskButton(List<InlineMenuItem> items) {
        items.add(createTaskViewMenuItem(
                createStringResource("ResourceObjectsPanel.button.viewSimulatedTasks"),
                null,
                true));

        items.add(createTaskViewMenuItem(
                createStringResource("ResourceObjectsPanel.button.viewImportTasks"),
                SystemObjectsType.ARCHETYPE_IMPORT_TASK.value(),
                false));

        items.add(createTaskViewMenuItem(
                createStringResource("ResourceObjectsPanel.button.viewLiveSyncTasks"),
                SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value(),
                false));

        items.add(createTaskViewMenuItem(
                createStringResource("ResourceObjectsPanel.button.viewReconciliationTasks"),
                SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value(),
                false));
    }

    protected ObjectQuery createQueryForTasks(boolean isSimulationTasks) {
        S_FilterExit filter = PrismContext.get()
                .queryFor(TaskType.class)
                .item(ItemPath.create(
                        TaskType.F_AFFECTED_OBJECTS,
                        TaskAffectedObjectsType.F_ACTIVITY,
                        ActivityAffectedObjectsType.F_RESOURCE_OBJECTS,
                        BasicResourceObjectSetType.F_RESOURCE_REF))
                .ref(getObjectDetailsModels().getObjectType().getOid())
                .and()
                .item(ItemPath.create(
                        TaskType.F_AFFECTED_OBJECTS,
                        TaskAffectedObjectsType.F_ACTIVITY,
                        ActivityAffectedObjectsType.F_RESOURCE_OBJECTS,
                        BasicResourceObjectSetType.F_KIND))
                .eq(getKind());
        ResourceObjectTypeDefinition objectType = getSelectedObjectTypeDefinition();
        if (objectType != null) {
            filter = filter
                    .and()
                    .item(ItemPath.create(
                            TaskType.F_AFFECTED_OBJECTS,
                            TaskAffectedObjectsType.F_ACTIVITY,
                            ActivityAffectedObjectsType.F_RESOURCE_OBJECTS,
                            BasicResourceObjectSetType.F_INTENT))
                    .eq(objectType.getIntent());
        }

        if (isSimulationTasks) {
            filter = addSimulationRule(
                    filter.and().block(),
                    false,
                    ExecutionModeType.PREVIEW);
            filter = addSimulationRule(
                    filter.or(),
                    false,
                    ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW);
            filter = filter.endBlock();
        } else {
            filter = addSimulationRule(
                    filter.and(),
                    true,
                    ExecutionModeType.PREVIEW);
            filter = addSimulationRule(
                    filter.and(),
                    true,
                    ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW);

        }

        return filter.build();
    }

    @Override
    protected TaskCreationPopup<?> createNewTaskPopup() {
        return new TaskCreationFoCategorizedObjectsPopup(getPageBase().getMainPopupBodyId()) {

            @Override
            protected void createNewTaskPerformed(SynchronizationTaskFlavor flavor, boolean simulate, AjaxRequestTarget target) {
                ResourceObjectsPanel.this.createNewTaskPerformed(flavor, simulate, target);
            }
        };
    }

    protected abstract UserProfileStorage.TableId getRepositorySearchTableId();

    protected abstract StringResourceModel getLabelModel();

    protected final RepositoryShadowBeanObjectDataProvider createProvider(IModel<Search<ShadowType>> searchModel, CompiledShadowCollectionView collection) {
        RepositoryShadowBeanObjectDataProvider provider = new RepositoryShadowBeanObjectDataProvider(
                getPageBase(), searchModel, null) {

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getResourceContentQuery();
            }

            @Override
            protected Integer countObjects(Class<ShadowType> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result) throws CommonException {
                Integer count = 0;
                ResourceType resource = getObjectDetailsModels().getObjectType();
                if (getSelectedObjectType() == null) {
                    StringResourceModel warnMessage = createStringResource("PageResource.warn.no.object.definition", getKind(), getIntent(), resource);
                    String localeWarnMessage = getLocalizationService()
                            .translate(PolyString.fromOrig(warnMessage.getString()), WebComponentUtil.getCurrentLocale(), true);
                    warn(localeWarnMessage);
                } else {
                    count = super.countObjects(type, query, currentOptions, task, result);
                }
                return count;
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

    private ReloadableButton createReloadButton(String buttonId) {

        ReloadableButton reload = new ReloadableButton(
                buttonId, getPageBase()) {

            @Override
            protected void refresh(AjaxRequestTarget target) {
                target.add(getShadowTable());
            }

            @Override
            protected ActivityDefinitionType createActivityDefinition() {

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
        DropDownChoicePanel<ResourceObjectTypeIdentification> objectTypeSelector = getObjectTypeSelector();
        if (objectTypeSelector != null && objectTypeSelector.getModel() != null && objectTypeSelector.getModel().getObject() != null) {
            ResourceObjectTypeIdentification objectTypeIdentifier = objectTypeSelector.getModel().getObject();
            return getObjectDetailsModels().getObjectTypeDefinition(objectTypeIdentifier.getKind(), objectTypeIdentifier.getIntent());
        }
        return null;
    }

    protected String getIntent() {
        ResourceObjectTypeDefinition objectType = getSelectedObjectTypeDefinition();
        if (objectType != null) {
            return objectType.getIntent();
        }
        return null;
    }

    protected QName getObjectClass() {
        ResourceObjectTypeDefinition objectType = getSelectedObjectTypeDefinition();
        if (objectType != null) {
            return objectType.getObjectClassName();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private DropDownChoicePanel<ResourceObjectTypeIdentification> getObjectTypeSelector() {
        return (DropDownChoicePanel<ResourceObjectTypeIdentification>) get(ID_OBJECT_TYPE);
    }

    private ShadowTablePanel getShadowTable() {
        return (ShadowTablePanel) get(ID_TABLE);
    }

    private WebMarkupContainer getStatisticsPanel() {
        return (WebMarkupContainer) get(ID_CHART_CONTAINER);
    }

}
