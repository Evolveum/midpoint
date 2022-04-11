/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.*;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.component.PendingOperationPanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceTasksPanel;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.CompiledShadowCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.ObjectLinkColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.ResourceContentTabPanel.Operation;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
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
public abstract class ResourceContentPanel extends BasePanel<PrismObject<ResourceType>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ResourceContentPanel.class);

    private static final String DOT_CLASS = ResourceContentTabPanel.class.getName() + ".";
    private static final String OPERATION_SEARCH_TASKS_FOR_RESOURCE = DOT_CLASS + "searchTasks";
    private static final String OPERATION_CHANGE_OWNER = DOT_CLASS + "changeOwner";
    private static final String OPERATION_LOAD_SHADOW_OWNER = DOT_CLASS + "loadOwner";
    private static final String OPERATION_UPDATE_STATUS = DOT_CLASS + "updateStatus";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    private static final String OPERATION_IMPORT_OBJECT = DOT_CLASS + "importObject";

    private static final String ID_TABLE = "table";
    private static final String ID_LABEL = "label";

    private static final String ID_IMPORT = "import";
    private static final String ID_RECONCILIATION = "reconciliation";
    private static final String ID_LIVE_SYNC = "liveSync";
    private static final String ID_TOTALS = "totals";

    private final ShadowKindType kind;
    private final String intent;
    private final QName objectClass;

    private String searchMode;
    private SelectableBeanObjectDataProvider<ShadowType> provider;

    IModel<PrismObject<ResourceType>> resourceModel;

    private final ContainerPanelConfigurationType config;

    public ResourceContentPanel(String id, IModel<PrismObject<ResourceType>> resourceModel, QName objectClass,
            ShadowKindType kind, String intent, String searchMode, ContainerPanelConfigurationType config) {
        super(id);
        this.kind = kind;
        this.searchMode = searchMode;
        this.resourceModel = resourceModel;
        this.intent = intent;
        this.objectClass = objectClass;
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

    public ShadowKindType getKind() {
        return kind;
    }

    public String getIntent() {
        return intent;
    }

    public IModel<PrismObject<ResourceType>> getResourceModel() {
        return resourceModel;
    }

    public QName getObjectClass() {
        return objectClass;
    }

    public ResourceObjectDefinition getDefinitionByKind() throws SchemaException {
        ResourceSchema refinedSchema = getRefinedSchema();
        if (refinedSchema == null) {
            warn("No schema found in resource. Please check your configuration and try to test connection for the resource.");
            return null;
        }
        String intent = getIntent();
        if (ShadowUtil.isKnown(intent)) {
            return refinedSchema.findObjectDefinition(getKind(), intent);
        } else {
            // TODO: Can be intent unknown or null here? If so, what should we do with that?
            return refinedSchema.findObjectDefinition(getKind(), null);
        }
    }

    public ResourceObjectDefinition getDefinitionByObjectClass() throws SchemaException {
        ResourceSchema refinedSchema = getRefinedSchema();
        if (refinedSchema == null) {
            warn("No schema found in resource. Please check your configuration and try to test connection for the resource.");
            return null;
        }
        return refinedSchema.findDefinitionForObjectClass(getObjectClass());

    }

    protected ResourceSchema getRefinedSchema() throws SchemaException {
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
                case ACCOUNT:
                    return UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL_REPOSITORY_MODE;
                case GENERIC:
                    return UserProfileStorage.TableId.PAGE_RESOURCE_GENERIC_PANEL_REPOSITORY_MODE;
                case ENTITLEMENT:
                    return UserProfileStorage.TableId.PAGE_RESOURCE_ENTITLEMENT_PANEL_REPOSITORY_MODE;

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

        MainObjectListPanel<ShadowType> shadowListPanel =
                new MainObjectListPanel<>(ID_TABLE,
                        ShadowType.class, createSearchOptions(), getPanelConfiguration()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected List<InlineMenuItem> createInlineMenu() {
                        return ResourceContentPanel.this.createRowMenuItems();
                    }

                    @Override
                    protected List<IColumn<SelectableBean<ShadowType>, String>> createDefaultColumns() {
                        return ResourceContentPanel.this.initColumns();
                    }

                    @Override
                    protected void objectDetailsPerformed(AjaxRequestTarget target, ShadowType object) {
                        shadowDetailsPerformed(target, WebComponentUtil.getName(object), object.getOid());
                    }

                    @Override
                    protected boolean isCreateNewObjectEnabled() {
                        return false;
                    }

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
                        provider = createSelectableBeanObjectDataProvider(() -> getResourceContentQuery(), null);
                        provider.setEmptyListOnNullQuery(true);
                        provider.setSort(null);
                        provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                        return provider;
                    }

                    @Override
                    protected Search createSearch(Class<ShadowType> type) {
                        return ResourceContentPanel.this.createSearch();
                    }

                    @Override
                    protected CompiledObjectCollectionView getObjectCollectionView() {
                        CompiledShadowCollectionView compiledView = findContainerPanelConfig();
                        if (compiledView != null) {
                            return compiledView;
                        }
                        return super.getObjectCollectionView();
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

        Label label = new Label(ID_LABEL, "Nothing to show. Select intent to search");
        add(label);
        label.setOutputMarkupId(true);
        label.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return createQuery() == null;
            }
        });

        initButton(ID_IMPORT, "Import", " fa-download", SystemObjectsType.ARCHETYPE_IMPORT_TASK.value());
        initButton(ID_RECONCILIATION, "Reconciliation", " fa-link", SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value());
        initButton(ID_LIVE_SYNC, "Live Sync", " fa-refresh", SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value());

        initCustomLayout();
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

    private void initButton(String id, String label, String icon, String archetypeOid) {

        ObjectQuery existingTasksQuery = getExistingTasksQuery(archetypeOid);
        OperationResult result = new OperationResult(OPERATION_SEARCH_TASKS_FOR_RESOURCE);
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
                        redirectToTasksListPage(createInTaskOidQuery(filteredByKindIntentTasks), archetypeOid);
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
                        newTaskPerformed(target, archetypeOid);
                    }
                };
            }
        };
        items.add(item);

        DropdownButtonPanel button = new DropdownButtonPanel(id,
                new DropdownButtonDto(String.valueOf(tasksList.size()), icon, label, items)) {
            @Override
            protected String getSpecialDropdownMenuClass() {
                return "pull-left";
            }
        };
        add(button);

    }

    private void newTaskPerformed(AjaxRequestTarget target, String archetypeOid) {
        List<ObjectReferenceType> archetypeRef = Arrays.asList(
                new ObjectReferenceType()
                        .oid(archetypeOid)
                        .type(ArchetypeType.COMPLEX_TYPE));
        try {
            TaskType newTask = ResourceTasksPanel.createResourceTask(getPrismContext(), getResourceModel().getObject(), archetypeRef);

            WebComponentUtil.initNewObjectWithReference(getPageBase(), newTask, archetypeRef);
        } catch (SchemaException ex) {
            getPageBase().getFeedbackMessages().error(ResourceContentPanel.this, ex.getUserFriendlyMessage());
            target.add(getPageBase().getFeedbackPanel());
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

    private void redirectToTasksListPage(ObjectQuery tasksQuery, String archetypeOid) {
        String taskCollectionViewName = getTaskCollectionViewNameByArchetypeOid(archetypeOid);
        PageParameters pageParameters = new PageParameters();
        if (StringUtils.isNotEmpty(taskCollectionViewName)) {
            pageParameters.add(PageBase.PARAMETER_OBJECT_COLLECTION_NAME, taskCollectionViewName);
            PageTasks pageTasks = new PageTasks(tasksQuery, pageParameters);
            getPageBase().setResponsePage(pageTasks);
        } else {
            PageTasks pageTasks = new PageTasks(tasksQuery, pageParameters);
            getPageBase().setResponsePage(pageTasks);
        }

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
                        } catch (SchemaException e) {
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
                return ObjectQueryUtil.createResourceAndObjectClassQuery(resourceModel.getObject().getOid(),
                        objectClass, getPageBase().getPrismContext());
            }

            ResourceObjectDefinition rOcDef = getDefinitionByKind();
            if (rOcDef != null) {
                baseQuery = rOcDef.createShadowSearchQuery(resourceModel.getObject().getOid());
            }
        } catch (SchemaException ex) {
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Could not crate query for shadows: " + ex.getMessage(), ex);
        }
        return baseQuery;
    }

    protected abstract Search createSearch();

    private Collection<SelectorOptions<GetOperationOptions>> createSearchOptions() {
        GetOperationOptionsBuilder builder = getPageBase().getOperationOptionsBuilder()
                .item(ShadowType.F_ASSOCIATION).dontRetrieve();
        builder = addAdditionalOptions(builder);
        return builder.build();
    }

    private StringResourceModel createStringResource(String key) {
        return getPageBase().createStringResource(key);
    }

    private List<IColumn<SelectableBean<ShadowType>, String>> initColumns() {

        List<ColumnTypeDto<String>> columnDefs = Arrays.asList(
                new ColumnTypeDto<>("ShadowType.synchronizationSituation",
                        SelectableBeanImpl.F_VALUE + ".synchronizationSituation",
                        ShadowType.F_SYNCHRONIZATION_SITUATION.getLocalPart()),
                new ColumnTypeDto<>("ShadowType.intent", SelectableBeanImpl.F_VALUE + ".intent",
                        ShadowType.F_INTENT.getLocalPart()));

        List<IColumn<SelectableBean<ShadowType>, String>> columns = new ArrayList<>();

        IColumn<SelectableBean<ShadowType>, String> identifiersColumn = new AbstractColumn<>(
                createStringResource("pageContentAccounts.identifiers")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ShadowType>>> cellItem,
                    String componentId, IModel<SelectableBean<ShadowType>> rowModel) {

                SelectableBean<ShadowType> dto = rowModel.getObject();
                RepeatingView repeater = new RepeatingView(componentId);

                ShadowType value = dto.getValue();
                if (value != null) {
                    for (ResourceAttribute<?> attr : ShadowUtil.getAllIdentifiers(value)) {
                        repeater.add(new Label(repeater.newChildId(),
                                attr.getElementName().getLocalPart() + ": " + attr.getRealValue()));

                    }
                }
                cellItem.add(repeater);

            }
        };
        columns.add(identifiersColumn);

        columns.addAll(ColumnUtils.createColumns(columnDefs));

        ObjectLinkColumn<SelectableBean<ShadowType>> ownerColumn = new ObjectLinkColumn<>(
                createStringResource("pageContentAccounts.owner")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<FocusType> createLinkModel(final IModel<SelectableBean<ShadowType>> rowModel) {

                return new IModel<>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public FocusType getObject() {
                        return loadShadowOwner(rowModel);
                    }

                };
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ShadowType>> rowModel,
                    ObjectType targetObjectType) {
                ownerDetailsPerformed((FocusType) targetObjectType);
            }
        };
        columns.add(ownerColumn);

        columns.add(new AbstractColumn<>(
                createStringResource("PageAccounts.accounts.pendingOperations")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ShadowType>>> cellItem,
                    String componentId, IModel<SelectableBean<ShadowType>> rowModel) {
                cellItem.add(new PendingOperationPanel(componentId,
                        new PropertyModel<>(rowModel, SelectableBeanImpl.F_VALUE + "." + ShadowType.F_PENDING_OPERATION.getLocalPart())));
            }
        });
        return columns;
    }

    private void ownerDetailsPerformed(FocusType owner) {
        if (owner == null) {
            return;
        }
        WebComponentUtil.dispatchToObjectDetailsPage(owner.getClass(), owner.getOid(), this, true);
    }

    private <F extends FocusType> F loadShadowOwner(IModel<SelectableBean<ShadowType>> model) {
        ShadowType shadow = model.getObject().getValue();
        String shadowOid;
        if (shadow != null) {
            shadowOid = shadow.getOid();
        } else {
            return null;
        }

        return loadShadowOwner(shadowOid);
    }

    private void shadowDetailsPerformed(AjaxRequestTarget target, String accountName, String accountOid) {
        if (StringUtils.isEmpty(accountOid)) {
            error(getPageBase().getString("pageContentAccounts.message.cantShowAccountDetails", accountName,
                    accountOid));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        WebComponentUtil.dispatchToObjectDetailsPage(ShadowType.class, accountOid, this, false);
    }

    private <F extends FocusType> F loadShadowOwner(String shadowOid) {

        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_SHADOW_OWNER);
        OperationResult result = new OperationResult(OPERATION_LOAD_SHADOW_OWNER);

        try {
            PrismObject<? extends FocusType> prismOwner = getPageBase().getModelService()
                    .searchShadowOwner(shadowOid, null, task, result);

            if (prismOwner != null) {
                return (F) prismOwner.asObjectable();
            }
        } catch (ObjectNotFoundException exception) {
            // owner was not found, it's possible and it's ok on unlinked
            // accounts
        } catch (Exception ex) {
            result.recordFatalError(getPageBase().getString("PageAccounts.message.ownerNotFound", shadowOid), ex);
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Could not load owner of account with oid: " + shadowOid, ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (WebComponentUtil.showResultInPage(result)) {
            getPageBase().showResult(result, false);
        }

        return null;
    }

    private List<InlineMenuItem> createRowMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.enableAccount"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            updateResourceObjectStatusPerformed(null, target, true);
                        } else {
                            SelectableBeanImpl<ShadowType> shadow = getRowModel().getObject();
                            updateResourceObjectStatusPerformed(shadow.getValue(), target, true);
                        }
                    }
                };
            }
        });

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.disableAccount"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            updateResourceObjectStatusPerformed(null, target, false);
                        } else {
                            SelectableBeanImpl<ShadowType> shadow = getRowModel().getObject();
                            updateResourceObjectStatusPerformed(shadow.getValue(), target, false);
                        }
                    }
                };
            }
        });

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.deleteAccount"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            deleteResourceObjectPerformed(null, target);
                        } else {
                            SelectableBeanImpl<ShadowType> shadow = getRowModel().getObject();
                            deleteResourceObjectPerformed(shadow.getValue(), target);
                        }
                    }
                };
            }
        });

        items.add(new ButtonInlineMenuItem(createStringResource("pageContentAccounts.menu.importAccount"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            importResourceObject(null, target);
                        } else {
                            SelectableBeanImpl<ShadowType> shadow = getRowModel().getObject();
                            importResourceObject(shadow.getValue(), target);
                        }
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_IMPORT_MENU_ITEM);
            }
        });

//        items.add(new InlineMenuItem());

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.removeOwner"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            changeOwner(null, target, null, Operation.REMOVE);
                        } else {
                            SelectableBeanImpl<ShadowType> shadow = getRowModel().getObject();
                            changeOwner(shadow.getValue(), target, null, Operation.REMOVE);
                        }
                    }
                };
            }
        });

        items.add(new ButtonInlineMenuItem(createStringResource("pageContentAccounts.menu.changeOwner"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        final SelectableBeanImpl<ShadowType> shadow = getRowModel().getObject();
                        ObjectBrowserPanel<FocusType> browser = new ObjectBrowserPanel<>(
                                getPageBase().getMainPopupBodyId(), UserType.class,
                                WebComponentUtil.createFocusTypeList(), false, getPageBase()) {

                            @Override
                            protected void onSelectPerformed(AjaxRequestTarget target, FocusType focus) {
                                changeOwner(shadow.getValue(), target, focus, Operation.MODIFY);
                            }

                        };

                        getPageBase().showMainPopup(browser, target);

                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

        });

        return items;
    }

    protected void importResourceObject(ShadowType selected, AjaxRequestTarget target) {
        List<ShadowType> selectedShadows;
        if (selected != null) {
            selectedShadows = new ArrayList<>();
            selectedShadows.add(selected);
        } else {
            selectedShadows = getTable().getSelectedRealObjects();
        }

        OperationResult result = new OperationResult(OPERATION_IMPORT_OBJECT);
        Task task = getPageBase().createSimpleTask(OPERATION_IMPORT_OBJECT);

        if (selectedShadows == null || selectedShadows.isEmpty()) {
            result.recordWarning(createStringResource("ResourceContentPanel.message.importResourceObject.warning").getString());
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        for (ShadowType shadow : selectedShadows) {
            try {
                getPageBase().getModelService().importFromResource(shadow.getOid(), task, result);
            } catch (Exception e) {
                result.recordPartialError(getPageBase().createStringResource("ResourceContentPanel.message.importResourceObject.partialError", shadow).getString(), e);
                LOGGER.error("Could not import account {} ", shadow, e);
            }
        }

        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        getTable().refreshTable(target);
        target.add(getPageBase().getFeedbackPanel());
    }

    // TODO: as a task?
    protected void deleteResourceObjectPerformed(ShadowType selected, AjaxRequestTarget target) {
        final List<ShadowType> selectedShadow = getSelectedShadowsList(selected);
        final OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);

        if (selectedShadow == null || selectedShadow.isEmpty()) {
            result.recordWarning(createStringResource("ResourceContentPanel.message.deleteResourceObjectPerformed.warning").getString());
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        ConfirmationPanel dialog = new DeleteConfirmationPanel(((PageBase) getPage()).getMainPopupBodyId(),
                createDeleteConfirmString(selected, "pageContentAccounts.message.deleteConfirmationSingle",
                        "pageContentAccounts.message.deleteConfirmation")) {
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteAccountConfirmedPerformed(target, result, selectedShadow);
            }
        };
        ((PageBase) getPage()).showMainPopup(dialog, target);

    }

    private void deleteAccountConfirmedPerformed(AjaxRequestTarget target, OperationResult result,
            List<ShadowType> selected) {
        Task task = getPageBase().createSimpleTask(OPERATION_DELETE_OBJECT);
        ModelExecuteOptions opts = createModelOptions();

        for (ShadowType shadow : selected) {
            try {
                ObjectDelta<ShadowType> deleteDelta = getPageBase().getPrismContext().deltaFactory().object().createDeleteDelta(ShadowType.class,
                        shadow.getOid());
                getPageBase().getModelService().executeChanges(
                        MiscUtil.createCollection(deleteDelta), opts, task, result);
            } catch (Throwable e) {
                result.recordPartialError("Could not delete " + shadow + ", reason: " + e.getMessage(), e);
                LOGGER.error("Could not delete {}, using option {}", shadow, opts, e);
            }
        }

        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        getTable().refreshTable(target);
        target.add(getPageBase().getFeedbackPanel());

    }

    private IModel<String> createDeleteConfirmString(final ShadowType selected, final String oneDeleteKey,
            final String moreDeleteKey) {
        return () -> {
            List<ShadowType> selectedShadow = getSelectedShadowsList(selected);
            if (selectedShadow.size() == 1) {
                ShadowType first = selectedShadow.get(0);
                String name = WebComponentUtil.getName(first);
                return getPageBase().createStringResource(oneDeleteKey, name).getString();
            }
            return getPageBase().createStringResource(moreDeleteKey, selectedShadow.size())
                    .getString();
        };
    }

    protected abstract ModelExecuteOptions createModelOptions();

    protected void updateResourceObjectStatusPerformed(ShadowType selected, AjaxRequestTarget target,
            boolean enabled) {
        List<ShadowType> selectedShadow = getSelectedShadowsList(selected);

        OperationResult result = new OperationResult(OPERATION_UPDATE_STATUS);
        Task task = getPageBase().createSimpleTask(OPERATION_UPDATE_STATUS);

        if (selectedShadow == null || selectedShadow.isEmpty()) {
            result.recordWarning(createStringResource("updateResourceObjectStatusPerformed.warning").getString());
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        ModelExecuteOptions opts = createModelOptions();

        for (ShadowType shadow : selectedShadow) {
            ActivationStatusType status = enabled ? ActivationStatusType.ENABLED
                    : ActivationStatusType.DISABLED;
            try {
                ObjectDelta<ShadowType> deleteDelta = getPageBase().getPrismContext().deltaFactory().object().createModificationReplaceProperty(
                        ShadowType.class, shadow.getOid(),
                        SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                        status);
                getPageBase().getModelService().executeChanges(
                        MiscUtil.createCollection(deleteDelta), opts, task, result);
            } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
                    | ExpressionEvaluationException | CommunicationException | ConfigurationException
                    | PolicyViolationException | SecurityViolationException e) {
                result.recordPartialError(
                        getPageBase().createStringResource(
                                        "ResourceContentPanel.message.updateResourceObjectStatusPerformed.partialError", status, shadow)
                                .getString(),
                        e);
                LOGGER.error("Could not update status (to {}) for {}, using option {}",
                        status, shadow, opts, e);
            }
        }

        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        getTable().refreshTable(target);
        target.add(getPageBase().getFeedbackPanel());

    }

    private PrismObjectDefinition<FocusType> getFocusDefinition() {
        return getPageBase().getPrismContext().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(FocusType.class);
    }

    private MainObjectListPanel<ShadowType> getTable() {
        return (MainObjectListPanel<ShadowType>) get(getPageBase().createComponentPath(ID_TABLE));
    }

    private void changeOwner(ShadowType selected, AjaxRequestTarget target, FocusType ownerToChange,
            Operation operation) {

        getPageBase().hideMainPopup(target);

        List<ShadowType> selectedShadow = getSelectedShadowsList(selected);

        Collection<? extends ItemDelta> modifications = new ArrayList<>();

        ReferenceDelta delta;
        switch (operation) {

            case REMOVE:
                for (ShadowType shadow : selectedShadow) {
                    modifications = new ArrayList<>();
                    FocusType owner = loadShadowOwner(shadow.getOid());
                    if (owner != null) {
                        delta = getPageBase().getPrismContext().deltaFactory().reference().createModificationDelete(FocusType.F_LINK_REF,
                                getFocusDefinition(),
                                ObjectTypeUtil.createObjectRef(shadow, getPageBase().getPrismContext()).asReferenceValue());

                        ((Collection) modifications).add(delta);
                        changeOwnerInternal(owner.getOid(), owner.getClass(), modifications, target);
                    }
                }
                break;
            case MODIFY:
                if (!isSatisfyConstraints(selectedShadow)) {
                    break;
                }

                ShadowType shadow = selectedShadow.iterator().next();
                FocusType owner = loadShadowOwner(shadow.getOid());
                if (owner != null) {
                    delta = getPageBase().getPrismContext().deltaFactory().reference().createModificationDelete(FocusType.F_LINK_REF,
                            getFocusDefinition(), ObjectTypeUtil.createObjectRef(shadow, getPageBase().getPrismContext()).asReferenceValue());

                    ((Collection) modifications).add(delta);
                    changeOwnerInternal(owner.getOid(), owner.getClass(), modifications, target);
                }
                modifications = new ArrayList<>();

                delta = getPageBase().getPrismContext().deltaFactory().reference().createModificationAdd(FocusType.F_LINK_REF, getFocusDefinition(),
                        ObjectTypeUtil.createObjectRef(shadow, getPageBase().getPrismContext()).asReferenceValue());
                ((Collection) modifications).add(delta);
                changeOwnerInternal(ownerToChange.getOid(), ownerToChange.getClass(), modifications, target);

                break;
        }

    }

    private boolean isSatisfyConstraints(List selected) {
        if (selected.size() > 1) {
            error("Could not link to more than one owner");
            return false;
        }

        if (selected.isEmpty()) {
            warn("Could not link to more than one owner");
            return false;
        }

        return true;
    }

    private void changeOwnerInternal(String ownerOid, Class<? extends FocusType> ownerType, Collection<? extends ItemDelta> modifications,
            AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_CHANGE_OWNER);
        Task task = getPageBase().createSimpleTask(OPERATION_CHANGE_OWNER);
        ObjectDelta<? extends ObjectType> objectDelta =
                getPageBase().getPrismContext().deltaFactory().object()
                        .createModifyDelta(ownerOid, modifications, ownerType);
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        deltas.add(objectDelta);
        try {
            getPageBase().getModelService().executeChanges(deltas, null, task, result);

        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
                | ExpressionEvaluationException | CommunicationException | ConfigurationException
                | PolicyViolationException | SecurityViolationException e) {
            // nothing?
        }

        result.computeStatusIfUnknown();

        getPageBase().showResult(result);
        target.add(getPageBase().getFeedbackPanel());
        getTable().refreshTable(target);
        target.add(ResourceContentPanel.this);
    }

    private List<ShadowType> getSelectedShadowsList(ShadowType shadow) {
        List<ShadowType> selectedShadow;
        if (shadow != null) {
            selectedShadow = new ArrayList<>();
            selectedShadow.add(shadow);
        } else {
            provider.clearSelectedObjects();
            selectedShadow = getTable().getSelectedRealObjects();
        }
        return selectedShadow;
    }

    private ObjectQuery getExistingTasksQuery(String archetypeRefOid) {
        return getPageBase().getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(resourceModel.getObject().getOid())
                .and()
                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(archetypeRefOid)
                .build();
    }

    protected abstract GetOperationOptionsBuilder addAdditionalOptions(GetOperationOptionsBuilder builder);

    protected abstract boolean isUseObjectCounting();
}
