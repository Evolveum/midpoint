/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.focus.component;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.component.PendingOperationPanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.ProjectionDisplayNamePanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismReferenceWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.ShadowPanel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.web.component.util.ProjectionsListProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 * @author skublik
 */
@PanelType(name = "projections")
@PanelInstance(identifier = "projections", applicableForType = FocusType.class,
        display = @PanelDisplay(label = "pageAdminFocus.projections", icon = GuiStyleConstants.CLASS_SHADOW_ICON_ACCOUNT, order = 20))
@Counter(provider = FocusProjectionsCounter.class)
public class FocusProjectionsPanel<F extends FocusType> extends AbstractObjectMainPanel<F, FocusDetailsModels<F>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_SHADOW_TABLE = "shadowTable";
    private static final String ID_DEAD_SHADOWS = "deadShadows";

    private static final String DOT_CLASS = FocusProjectionsPanel.class.getName() + ".";
    private static final String OPERATION_ADD_ACCOUNT = DOT_CLASS + "addShadow";

    private static final String OPERATION_LOAD_SHADOW = DOT_CLASS + "loadShadow";

    private static final Trace LOGGER = TraceManager.getTrace(FocusProjectionsPanel.class);

    public FocusProjectionsPanel(String id, FocusDetailsModels<F> focusModel, ContainerPanelConfigurationType config) {
        super(id, focusModel, config);
    }

    private PrismObjectDefinition<ShadowType> getShadowDefinition() {
        return getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
    }

    protected void initLayout() {

        IModel<Integer> deadShadows = new ReadOnlyModel<>(this::countDeadShadows);
        Label label = new Label(ID_DEAD_SHADOWS, deadShadows);
        label.add(new VisibleBehaviour(() -> deadShadows.getObject() > 0));
        add(label);

        MultivalueContainerListPanelWithDetailsPanel<ShadowType> multivalueContainerListPanel =
                new MultivalueContainerListPanelWithDetailsPanel<ShadowType>(ID_SHADOW_TABLE, ShadowType.class, getPanelConfiguration()) {

            private static final long serialVersionUID = 1L;

                    @Override
                    protected ISelectableDataProvider<PrismContainerValueWrapper<ShadowType>> createProvider() {
                        return new ProjectionsListProvider(FocusProjectionsPanel.this, getSearchModel(), loadShadowModel()) {
                            @Override
                            protected PageStorage getPageStorage() {
                                PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(SessionStorage.KEY_FOCUS_PROJECTION_TABLE);
                                if (storage == null) {
                                    storage = getSession().getSessionStorage().initPageStorage(SessionStorage.KEY_FOCUS_PROJECTION_TABLE);
                                }
                                return storage;
                            }
                        };
                    }

                    @Override
                    protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation) {
                        List<QName> supportedTypes = Arrays.asList(ResourceType.COMPLEX_TYPE);
                        PageBase pageBase = FocusProjectionsPanel.this.getPageBase();
                        ObjectBrowserPanel<ResourceType> resourceSelectionPanel = new ObjectBrowserPanel<>(
                                pageBase.getMainPopupBodyId(), ResourceType.class, supportedTypes, true,
                                pageBase) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void addPerformed(AjaxRequestTarget target, QName type,
                                                        List<ResourceType> selected) {
                                FocusProjectionsPanel.this.addSelectedAccountPerformed(target,
                                        selected);
                                target.add(getPageBase().getFeedbackPanel());
                            }
                        };
                        resourceSelectionPanel.setOutputMarkupId(true);
                        pageBase.showMainPopup(resourceSelectionPanel,
                                target);
                    }

                    @Override
                    protected boolean isCreateNewObjectVisible() {
                        PrismObjectDefinition<F> def = FocusProjectionsPanel.this.getObjectWrapperModel().getObject().getObject().getDefinition();
                        PrismReferenceDefinition ref = def.findReferenceDefinition(UserType.F_LINK_REF);
                        return (ref.canRead() && ref.canAdd());
                    }

                    @Override
                    protected IModel<PrismContainerWrapper<ShadowType>> getContainerModel() {
                        return null;
                    }


                    @Override
                    protected TableId getTableId() {
                        return TableId.FOCUS_PROJECTION_TABLE;
                    }

                    @Override
                    protected List<IColumn<PrismContainerValueWrapper<ShadowType>, String>> createDefaultColumns() {
                        return initBasicColumns();
                    }

                    @Override
                    protected IColumn<PrismContainerValueWrapper<ShadowType>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ItemPath itemPath, ExpressionType expression) {
                        return createProjectionNameColumn(displayModel, customColumn, itemPath, expression);
                    }

                    @Override
                    protected IColumn<PrismContainerValueWrapper<ShadowType>, String> createIconColumn() {
                        return new CompositedIconColumn<>(Model.of("")) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            protected CompositedIcon getCompositedIcon(IModel<PrismContainerValueWrapper<ShadowType>> rowModel) {
                                if (rowModel == null || rowModel.getObject() == null || rowModel.getObject().getRealValue() == null) {
                                    return new CompositedIconBuilder().build();
                                }
                                ShadowType shadow = createShadowType(rowModel);
                                return WebComponentUtil.createAccountIcon(shadow, getPageBase(), true);
                            }

                        };
                    }

                    @Override
                    protected IColumn<PrismContainerValueWrapper<ShadowType>, String> createCheckboxColumn() {
                        return new CheckBoxHeaderColumn<>();
                    }

                    @Override
                    protected List<InlineMenuItem> createInlineMenu() {
                        return createShadowMenu();
                    }

                    @Override
                    public void editItemPerformed(AjaxRequestTarget target,
                            IModel<PrismContainerValueWrapper<ShadowType>> rowModel,
                            List<PrismContainerValueWrapper<ShadowType>> listItems) {

                        loadShadowIfNeeded(rowModel, target);

                        if (listItems != null) {
                            listItems.forEach(value -> {
                                if (((ShadowWrapper) value.getParent()).isLoadWithNoFetch()) {
                                    loadFullShadow((PrismObjectValueWrapper) value, target);
                                }
                            });
                        }
                        super.editItemPerformed(target, rowModel, listItems);
                    }

                    @Override
                    protected Search createSearch(Class<ShadowType> type) {
                        Search search = SearchFactory.createProjectionsTabSearch(getObjectCollectionView(), getPageBase());
                        return search;
                    }


                    @Override
                    protected MultivalueContainerDetailsPanel<ShadowType> getMultivalueContainerDetailsPanel(
                            ListItem<PrismContainerValueWrapper<ShadowType>> item) {
                        return FocusProjectionsPanel.this.getMultivalueContainerDetailsPanel(item);
                    }
                };
        add(multivalueContainerListPanel);
        setOutputMarkupId(true);
    }

    private IColumn<PrismContainerValueWrapper<ShadowType>, String> createProjectionNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ItemPath itemPath, ExpressionType expression) {
        if (expression != null) {
            return new AjaxLinkColumn<>(displayModel) {
                private static final long serialVersionUID = 1L;

                @Override
                public IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<ShadowType>> rowModel) {
                    return new LoadableModel<>() {
                        @Override
                        protected String load() {
                            Collection<String> evaluatedValues = getMultivalueContainerListPanel().loadExportableColumnDataModel(rowModel, customColumn, itemPath, expression);
                            if (CollectionUtils.isEmpty(evaluatedValues)) {
                                return "";
                            }
                            if (evaluatedValues.size() == 1) {
                                return evaluatedValues.iterator().next();
                            }
                            return String.join(", ", evaluatedValues);
                        }
                    };
                }


                @Override
                public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ShadowType>> rowModel) {
                    getMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
                    target.add(getPageBase().getFeedbackPanel());
                }
            };
        }

        IModel<PrismContainerDefinition<ShadowType>> shadowDef = Model.of(getShadowDefinition());
        return new PrismPropertyWrapperColumn<ShadowType, String>(shadowDef, ShadowType.F_NAME, ColumnType.LINK, getPageBase()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ShadowType>> rowModel) {
                getMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
                target.add(getPageBase().getFeedbackPanel());
            }

        };
    }

    private IModel<List<PrismContainerValueWrapper<ShadowType>>> loadShadowModel() {
        return () -> {
                List<PrismContainerValueWrapper<ShadowType>> items = new ArrayList<>();
                for (ShadowWrapper projection : getProjectionsModel().getObject()) {
                    items.add(projection.getValue());
                }
                return items;
        };
    }

    private LoadableDetachableModel<List<ShadowWrapper>> getProjectionsModel() {
        return getObjectDetailsModels().getProjectionModel();
    }

    private int countDeadShadows() {
        if (getProjectionsModel() == null) {
            return 0;
        }

        if (!getProjectionsModel().isAttached()) {
            return WebComponentUtil.countLinkForDeadShadows(getObjectWrapper().getObject().asObjectable().getLinkRef());
        }

        List<ShadowWrapper> projectionWrappers = getProjectionsModel().getObject();
        if (projectionWrappers == null) {
            return 0;
        }

        int dead = 0;
        for (ShadowWrapper projectionWrapper : projectionWrappers) {
            if (projectionWrapper.isDead()) {
                dead++;
            }
        }
        return dead;
    }

    private void addDeadSearchItem(Search search) {
        SearchItemDefinition def = new SearchItemDefinition(ShadowType.F_DEAD,
                getShadowDefinition().findPropertyDefinition(ShadowType.F_DEAD),
                Arrays.asList(new SearchValue<>(true), new SearchValue<>(false)));
        //todo create dead search item for refactored search
//        DeadShadowSearchItem deadShadowSearchItem = new DeadShadowSearchItem(search, def);
//        search.addSpecialItem(deadShadowSearchItem);
    }

    private void loadShadowIfNeeded(IModel<PrismContainerValueWrapper<ShadowType>> rowModel, AjaxRequestTarget target) {
        if (rowModel == null) {
            return;
        }

        PrismContainerValueWrapper<ShadowType> shadow = rowModel.getObject();
        if (shadow == null) {
            return;
        }

        ShadowWrapper shadowWrapper = shadow.getParent();
        if (shadowWrapper == null) {
            return;
        }

        if (shadowWrapper.isLoadWithNoFetch()) {
            loadFullShadow((PrismObjectValueWrapper)rowModel.getObject(), target);
        }
    }

    public void loadFullShadow(PrismObjectValueWrapper<ShadowType> shadowWrapperValue, AjaxRequestTarget target) {
        long start = System.currentTimeMillis();
        if (shadowWrapperValue.getRealValue() == null) {
            error(getString("pageAdminFocus.message.couldntCreateShadowWrapper"));
            LOGGER.error("Couldn't create shadow wrapper, because RealValue is null in " + shadowWrapperValue);
            return;
        }
        String oid = shadowWrapperValue.getRealValue().getOid();
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_SHADOW);
        OperationResult result = task.getResult();

        PrismObject<ShadowType> projection = getPrismObjectForShadowWrapper(oid, false, task,
                result, createLoadOptionForShadowWrapper());

        if (projection == null) {
            result.recordFatalError(getString("PageAdminFocus.message.loadFullShadow.fatalError", shadowWrapperValue.getRealValue()));
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        ShadowWrapper shadowWrapperNew;
        try {
            shadowWrapperNew = loadShadowWrapper(projection, task, result);
            shadowWrapperValue.clearItems();
            shadowWrapperValue.addItems((Collection) shadowWrapperNew.getValue().getItems());
            ((ShadowWrapper) shadowWrapperValue.getParent()).setLoadWithNoFetch(false);
        } catch (SchemaException e) {
            error(getString("pageAdminFocus.message.couldntCreateShadowWrapper"));
            LOGGER.error("Couldn't create shadow wrapper", e);
        }
    }

    private MultivalueContainerDetailsPanel<ShadowType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<ShadowType>> item) {
        MultivalueContainerDetailsPanel<ShadowType> detailsPanel = new MultivalueContainerDetailsPanel<ShadowType>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayNamePanel<ShadowType> createDisplayNamePanel(String displayNamePanelId) {
                IModel<ShadowType> shadowModel = () -> createShadowType(item.getModel());
                return new ProjectionDisplayNamePanel(displayNamePanelId, shadowModel);
            }

            @Override
            protected @NotNull List<ITab> createTabs() {
                List<ITab> tabs = super.createTabs();
                tabs.add(new PanelTab(createStringResource("ShadowType.basic")) {
                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        ContainerPanelConfigurationType config = getBasicShadowPanelConfiguration(getModelObject().getRealValue());
                        ShadowPanel shadowPanel = new ShadowPanel(panelId, getParentModel(getModel()), config);
                        return shadowPanel;
                    }
                });
                return tabs;
            }
        };
        return detailsPanel;
    }

    private ContainerPanelConfigurationType getBasicShadowPanelConfiguration(ShadowType shadowType) {
        List<ContainerPanelConfigurationType> panelConfigs = findShadowDetailsPageConfiguration(shadowType);
        if (panelConfigs.isEmpty()) {
            return null;
        }

        List<ContainerPanelConfigurationType> basicPanelConfig = panelConfigs.stream().filter(p -> p.getIdentifier().equals("shadowBasic")).collect(Collectors.toList());
        if (basicPanelConfig.size() == 1) {
            return basicPanelConfig.get(0);
        }

        //TODO at least log unexpected situation
        return null;
    }

    private List<ContainerPanelConfigurationType> findShadowDetailsPageConfiguration(ShadowType shadowType) {
        GuiShadowDetailsPageType shadowDetailsPage = getPageBase().getCompiledGuiProfile().findShadowDetailsConfiguration(createResourceShadowCoordinates(shadowType));
        if (shadowDetailsPage == null) {
            return Collections.emptyList();
        }
        return shadowDetailsPage.getPanel();
    }

    private ResourceShadowCoordinates createResourceShadowCoordinates(ShadowType shadow) {
        return new ResourceShadowCoordinates(shadow.getResourceRef().getOid(), shadow.getKind(), shadow.getIntent(), null);
    }

    private IModel<ShadowWrapper> getParentModel(IModel<PrismContainerValueWrapper<ShadowType>> model) {
        return new PropertyModel<>(model, "parent");
    }


    private List<IColumn<PrismContainerValueWrapper<ShadowType>, String>> initBasicColumns() {

        IModel<PrismContainerDefinition<ShadowType>> shadowDef = Model.of(getShadowDefinition());

        List<IColumn<PrismContainerValueWrapper<ShadowType>, String>> columns = new ArrayList<>();

        columns.add(new PrismReferenceWrapperColumn<>(shadowDef, ShadowType.F_RESOURCE_REF, ColumnType.STRING, getPageBase()));
        columns.add(new PrismPropertyWrapperColumn<ShadowType, String>(shadowDef, ShadowType.F_OBJECT_CLASS, ColumnType.STRING, getPageBase()));
        columns.add(new PrismPropertyWrapperColumn<ShadowType, String>(shadowDef, ShadowType.F_KIND, ColumnType.STRING, getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-xs-1";
            }
        });
        columns.add(new PrismPropertyWrapperColumn<ShadowType, String>(shadowDef, ShadowType.F_INTENT, ColumnType.STRING, getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-xs-1";
            }
        });
        columns.add(new PrismContainerWrapperColumn<>(shadowDef, ShadowType.F_PENDING_OPERATION, getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-xs-2";
            }

            @Override
            protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
                IW object = rowModel.getObject();
                if (object == null) {
                    return new WebMarkupContainer(componentId);
                }
                List<PrismValueWrapper<PendingOperationType>> values = object.getValues();
                List<PendingOperationType> pendingOperations = values.stream().map(operation -> operation.getRealValue()).collect(Collectors.toList());
                return new PendingOperationPanel(componentId, Model.ofList(pendingOperations));
            }
        });

        return columns;
    }

    private ShadowType createShadowType(IModel<PrismContainerValueWrapper<ShadowType>> rowModel) {
        ShadowType shadow = rowModel.getObject().getRealValue();
        try {
            PrismPropertyWrapper<Boolean> dead = rowModel.getObject().findProperty(ShadowType.F_DEAD);
            if (dead != null && !dead.isEmpty()) {
                shadow.setDead(dead.getValue().getRealValue());
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find property " + ShadowType.F_DEAD);
        }
        try {
            PrismContainerWrapper<ActivationType> activation = rowModel.getObject().findContainer(ShadowType.F_ACTIVATION);
            if (activation != null && !activation.isEmpty()) {
                shadow.setActivation(activation.getValue().getRealValue());
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find container " + ShadowType.F_ACTIVATION);
        }
        try {
            PrismContainerWrapper<TriggerType> triggers = rowModel.getObject().findContainer(ShadowType.F_TRIGGER);
            if (triggers != null && !triggers.isEmpty()) {
                for (PrismContainerValueWrapper<TriggerType> trigger : triggers.getValues()) {
                    if (trigger != null) {
                        shadow.getTrigger().add(trigger.getRealValue().cloneWithoutId());
                    }
                }
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find container " + ShadowType.F_TRIGGER);
        }
        return shadow;
    }

    private MultivalueContainerListPanelWithDetailsPanel<ShadowType> getMultivalueContainerListPanel() {
        return ((MultivalueContainerListPanelWithDetailsPanel<ShadowType>) get(ID_SHADOW_TABLE));
    }

    private void addSelectedAccountPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
        getPageBase().hideMainPopup(target);

        if (newResources.isEmpty()) {
            warn(getString("pageUser.message.noResourceSelected"));
            return;
        }

        for (ResourceType resource : newResources) {
            try {
                ResourceSchema refinedSchema = getRefinedSchema(resource);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Refined schema for {}\n{}", resource, refinedSchema.debugDump());
                }

                ResourceObjectDefinition accountDefinition = refinedSchema.findDefaultDefinitionForKind(ShadowKindType.ACCOUNT);
                if (accountDefinition == null) {
                    error(getString("pageAdminFocus.message.couldntCreateAccountNoAccountSchema",
                            resource.getName()));
                    continue;
                }
//                shadow.asPrismContainer().findOrCreateContainer(ShadowType.F_ATTRIBUTES).applyDefinition(accountDefinition.toResourceAttributeContainerDefinition());

                ShadowType shadow = createNewShadow(resource, accountDefinition);

                ShadowWrapper wrapperNew = createShadowWrapper(shadow);

                getProjectionsModel().getObject().add(wrapperNew);
            } catch (Exception ex) {
                error(getString("pageAdminFocus.message.couldntCreateAccount", resource.getName(),
                        ex.getMessage()));
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create account", ex);
            }
        }
        target.add(getMultivalueContainerListPanel());
    }

    private ShadowType createNewShadow(ResourceType resource, ResourceObjectDefinition accountDefinition) {
        ShadowType shadow = new ShadowType();
        shadow.setResourceRef(ObjectTypeUtil.createObjectRef(resource, SchemaConstants.ORG_DEFAULT));
        QName objectClass = accountDefinition.getTypeName();
        shadow.setObjectClass(objectClass);
        ResourceObjectTypeIdentification typeId = accountDefinition.getTypeIdentification();
        if (typeId != null) {
            shadow.setKind(typeId.getKind());
            shadow.setIntent(typeId.getIntent());
        } else if (accountDefinition.getObjectClassDefinition().isDefaultAccountDefinition()) {
            shadow.setKind(ShadowKindType.ACCOUNT);
            shadow.setIntent(SchemaConstants.INTENT_DEFAULT);
        } else {
            throw new IllegalStateException("No suitable account definition could be found");
        }
        return shadow;
    }

    private ResourceSchema getRefinedSchema(ResourceType resource) throws SchemaException, ConfigurationException {
        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource.asPrismObject(), LayerType.PRESENTATION);
        if (refinedSchema == null) {
            Task task = getPageBase().createSimpleTask(FocusProjectionsPanel.class.getSimpleName() + ".loadResource");
            OperationResult result = task.getResult();
            resource = WebModelServiceUtils.loadObject(ResourceType.class, resource.getOid(), getPageBase(), task, result).asObjectable();
            result.recomputeStatus();

            refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource, LayerType.PRESENTATION);

            if (refinedSchema == null) {
                error(getString("pageAdminFocus.message.couldntCreateAccountNoSchema",
                        resource.getName()));
            }
        }
        return refinedSchema;
    }

    private ShadowWrapper createShadowWrapper(ShadowType shadow) throws SchemaException {
        Task task = getPageBase().createSimpleTask(OPERATION_ADD_ACCOUNT);
        PrismObjectWrapperFactory<ShadowType> factory = getPageBase().findObjectWrapperFactory(shadow.asPrismContainer().getDefinition());
        WrapperContext context = new WrapperContext(task, task.getResult());
        ShadowWrapper wrapperNew = (ShadowWrapper) factory.createObjectWrapper(shadow.asPrismContainer(), ItemStatus.ADDED, context);

        if (task.getResult() != null
                && !WebComponentUtil.isSuccessOrHandledError(task.getResult())) {
            getPageBase().showResult(task.getResult(), false);
        }
        wrapperNew.setProjectionStatus(UserDtoStatus.ADD);
        return wrapperNew;
    }

    private List<InlineMenuItem> createShadowMenu() {
        List<InlineMenuItem> items = new ArrayList<>();

        PrismObjectDefinition<F> def = getObjectWrapperModel().getObject().getObject().getDefinition();
        PrismReferenceDefinition ref = def.findReferenceDefinition(UserType.F_LINK_REF);
        InlineMenuItem item;
        PrismPropertyDefinition<ActivationStatusType> administrativeStatus = def
                .findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        if (administrativeStatus.canRead() && administrativeStatus.canModify()) {
            item = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.enable")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            updateShadowActivation(target, getMultivalueContainerListPanel()
                                    .getPerformedSelectedItems(getRowModel()), true);
                        }
                    };
                }

                @Override
                public CompositedIconBuilder getIconCompositedBuilder() {
                    return getDefaultCompositedIconBuilder("fa fa-check");
                }
            };
            items.add(item);
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.disable")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            updateShadowActivation(target, getMultivalueContainerListPanel()
                                    .getPerformedSelectedItems(getRowModel()), false);
                        }
                    };
                }
            };
            items.add(item);
        }
        if (ref.canRead() && ref.canAdd()) {
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.unlink")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            unlinkProjectionPerformed(target, getMultivalueContainerListPanel()
                                    .getPerformedSelectedItems(getRowModel()));
                        }
                    };
                }
            };
            items.add(item);
        }
        PrismPropertyDefinition<LockoutStatusType> locakoutStatus = def.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
        if (locakoutStatus.canRead() && locakoutStatus.canModify()) {
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.unlock")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            unlockShadowPerformed(target, getMultivalueContainerListPanel()
                                    .getPerformedSelectedItems(getRowModel()));
                        }
                    };
                }
            };
            items.add(item);
        }
        if (administrativeStatus.canRead() && administrativeStatus.canModify()) {
//            items.add(new InlineMenuItem());
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            deleteProjectionPerformed(target, getMultivalueContainerListPanel()
                                    .getPerformedSelectedItems(getRowModel()));
                        }
                    };
                }
            };
            items.add(item);
        }
        item = new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        getMultivalueContainerListPanel().editItemPerformed(target,
                                getRowModel(), getMultivalueContainerListPanel().getSelectedObjects());
                        target.add(getPageBase().getFeedbackPanel());
                    }
                };
            }
        };
        items.add(item);
        return items;
    }

    private void deleteProjectionPerformed(AjaxRequestTarget target,
            List<PrismContainerValueWrapper<ShadowType>> selected) {
        if (!isAnyProjectionSelected(target, selected)) {
            return;
        }

        showModalWindow(getDeleteProjectionPopupContent(selected),
                target);
    }

    protected void showModalWindow(Popupable popupable, AjaxRequestTarget target) {
        getPageBase().showMainPopup(popupable, target);
        target.add(getPageBase().getFeedbackPanel());
    }

    private boolean isAnyProjectionSelected(AjaxRequestTarget target,
            List<PrismContainerValueWrapper<ShadowType>> selected) {
        if (selected.isEmpty()) {
            warn(getString("pageAdminFocus.message.noAccountSelected"));
            target.add(getPageBase().getFeedbackPanel());
            return false;
        }

        return true;
    }

//TODO this is probably whole wrong .. we should not rely that the activation will be available. it is available only after full load..
    private void updateShadowActivation(AjaxRequestTarget target,
            List<PrismContainerValueWrapper<ShadowType>> accounts, boolean enabled) {

        if (!isAnyProjectionSelected(target, accounts)) {
            return;
        }

        int markedToChangeActivation = 0;
        for (PrismContainerValueWrapper<ShadowType> account : accounts) {
            try {
                PrismContainerWrapper<ActivationType> activation = account
                        .findContainer(ShadowType.F_ACTIVATION);
                if (activation == null) {
                    warn(getString("pageAdminFocus.message.noActivationFound", WebComponentUtil.getName(account.getRealValue())));
                    continue;
                }

                if (CollectionUtils.isEmpty(activation.getValues())) {
                    warn(getString("pageAdminFocus.message.noActivationFound", WebComponentUtil.getName(account.getRealValue())));
                    continue;
                }

                PrismPropertyWrapper<?> enabledProperty = activation.getValues().iterator().next()
                        .findProperty(ActivationType.F_ADMINISTRATIVE_STATUS);
                if (enabledProperty == null || enabledProperty.getValues().size() != 1) {
                    warn(getString("pageAdminFocus.message.noEnabledPropertyFound", account.getDisplayName()));
                    continue;
                }
                PrismValueWrapper<?> value = enabledProperty.getValues().get(0);
                ActivationStatusType status = enabled ? ActivationStatusType.ENABLED
                        : ActivationStatusType.DISABLED;
                ((PrismPropertyValue) value.getNewValue()).setValue(status);
                markedToChangeActivation++;

            } catch (SchemaException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (markedToChangeActivation > 0) {
            info(getString("pageAdminFocus.message.updated." + enabled));
        }
        target.add(getPageBase().getFeedbackPanel(), getMultivalueContainerListPanel());
    }

    private void unlockShadowPerformed(AjaxRequestTarget target,
            List<PrismContainerValueWrapper<ShadowType>> selected) {
        if (!isAnyProjectionSelected(target, selected)) {
            return;
        }

        for (PrismContainerValueWrapper<ShadowType> account : selected) {
//            if (!account.isLoadedOK()) {
//                continue;
//            }
            try {
//                ObjectWrapperOld<ShadowType> wrapper = account.getObjectOld();
//                PrismObjectWrapper<ShadowType> wrapper = account.getObject();
//                wrapper.setSelected(false);

                PrismContainerWrapper<ActivationType> activation = account.findContainer(ShadowType.F_ACTIVATION);
                if (activation == null) {
                    warn(getString("pageAdminFocus.message.noActivationFound", account.getDisplayName()));
                    continue;
                }

                PrismPropertyWrapper<?> lockedProperty = activation.getValues().iterator()
                        .next().findProperty(ActivationType.F_LOCKOUT_STATUS);
                if (lockedProperty == null || lockedProperty.getValues().size() != 1) {
                    warn(getString("pageAdminFocus.message.noLockoutStatusPropertyFound"));
                    continue;
                }
                PrismValueWrapper<?> value = lockedProperty.getValues().get(0);
                ((PrismPropertyValue) value.getNewValue()).setValue(LockoutStatusType.NORMAL);
            } catch (SchemaException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }// TODO only for really unlocked accounts
        }
        info(getString("pageAdminFocus.message.unlocked"));
        target.add(getPageBase().getFeedbackPanel(), getMultivalueContainerListPanel());
    }

    private void unlinkProjectionPerformed(AjaxRequestTarget target,
            List<PrismContainerValueWrapper<ShadowType>> selected) {
        if (!isAnyProjectionSelected(target, selected)) {
            return;
        }

        for (PrismContainerValueWrapper projection : selected) {
            if (UserDtoStatus.ADD.equals(((ShadowWrapper) projection.getParent()).getProjectionStatus())) {
                continue;
            }
            ((ShadowWrapper) projection.getParent()).setProjectionStatus(UserDtoStatus.UNLINK);
        }
        target.add(getMultivalueContainerListPanel());
    }

    private Popupable getDeleteProjectionPopupContent(List<PrismContainerValueWrapper<ShadowType>> selected) {
        ConfirmationPanel dialog = new DeleteConfirmationPanel(getPageBase().getMainPopupBodyId(),
                new IModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        return createStringResource("pageAdminFocus.message.deleteAccountConfirm",
                                selected.size()).getString();
                    }
                }) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteAccountConfirmedPerformed(target, selected);
            }
        };
        return dialog;
    }

    private void deleteAccountConfirmedPerformed(AjaxRequestTarget target,
            List<PrismContainerValueWrapper<ShadowType>> selected) {
        List<ShadowWrapper> accounts = getProjectionsModel().getObject();
        for (PrismContainerValueWrapper<ShadowType> account : selected) {
            if (UserDtoStatus.ADD.equals(((ShadowWrapper) account.getParent()).getProjectionStatus())) {
                accounts.remove(account.getParent());
            } else {
                ((ShadowWrapper) account.getParent()).setProjectionStatus(UserDtoStatus.DELETE);
            }
        }
        target.add(getMultivalueContainerListPanel());
    }

    private Collection<SelectorOptions<GetOperationOptions>> createLoadOptionForShadowWrapper() {
        return getPageBase().getOperationOptionsBuilder()
                .resolveNames()
                .build();
    }

    @NotNull
    public ShadowWrapper loadShadowWrapper(PrismObject<ShadowType> projection, Task task, OperationResult result) throws SchemaException {
        PrismObjectWrapperFactory<ShadowType> factory = getPageBase().findObjectWrapperFactory(projection.getDefinition());
        WrapperContext context = new WrapperContext(task, result);
        context.setCreateIfEmpty(true);
        context.setDetailsPageTypeConfiguration(findShadowDetailsPageConfiguration(projection.asObjectable()));
        ShadowWrapper wrapper = (ShadowWrapper) factory.createObjectWrapper(projection, ItemStatus.NOT_CHANGED, context);
        wrapper.setProjectionStatus(UserDtoStatus.MODIFY);
        return wrapper;
    }

    private PrismObject<ShadowType> getPrismObjectForShadowWrapper(String oid, boolean noFetch,
            Task task, OperationResult subResult, Collection<SelectorOptions<GetOperationOptions>> loadOptions) {
        if (oid == null) {
            return null;
        }

        if (noFetch) {
            GetOperationOptions rootOptions = SelectorOptions.findRootOptions(loadOptions);
            if (rootOptions == null) {
                loadOptions.add(new SelectorOptions<>(GetOperationOptions.createNoFetch()));
            } else {
                rootOptions.setNoFetch(true);
            }
        }

        PrismObject<ShadowType> projection = WebModelServiceUtils.loadObject(ShadowType.class, oid, loadOptions, getPageBase(), task, subResult);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Loaded projection {} ({}):\n{}", oid, loadOptions, projection == null ? null : projection.debugDump());
        }

        return projection;
    }

}
