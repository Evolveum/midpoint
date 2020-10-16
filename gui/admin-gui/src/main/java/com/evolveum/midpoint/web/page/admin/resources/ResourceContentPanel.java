/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.PendingOperationPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.server.PageTask;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.ObjectLinkColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.search.PropertySearchItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.ResourceContentTabPanel.Operation;
import com.evolveum.midpoint.web.page.admin.resources.content.PageAccount;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

/**
 * Implementation classes : ResourceContentResourcePanel,
 * ResourceContentRepositoryPanel
 *
 * @author katkav
 * @author semancik
 */
public abstract class ResourceContentPanel extends Panel {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ResourceContentPanel.class);

    private static final String DOT_CLASS = ResourceContentTabPanel.class.getName() + ".";
    private static final String OPERATION_SEARCH_TASKS_FOR_RESOURCE = DOT_CLASS + "seachTasks";
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

    private PageBase pageBase;
    private ShadowKindType kind;
    private String searchMode;
    private String intent;
    private QName objectClass;
    private SelectableBeanObjectDataProvider<ShadowType> provider;

    IModel<PrismObject<ResourceType>> resourceModel;

    public ResourceContentPanel(String id, IModel<PrismObject<ResourceType>> resourceModel, QName objectClass,
            ShadowKindType kind, String intent, String searchMode, PageBase pageBase) {
        super(id);
        this.pageBase = pageBase;
        this.kind = kind;
        this.searchMode = searchMode;
        this.resourceModel = resourceModel;
        this.intent = intent;
        this.objectClass = objectClass;
        initLayout();
    }

    public PageBase getPageBase() {
        return pageBase;
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

    public RefinedObjectClassDefinition getDefinitionByKind() throws SchemaException {
        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl
                .getRefinedSchema(resourceModel.getObject(), getPageBase().getPrismContext());
        if (refinedSchema == null) {
            warn("No schema found in resource. Please check your configuration and try to test connection for the resource.");
            return null;
        }
        return refinedSchema.getRefinedDefinition(getKind(), getIntent());
    }

    public RefinedObjectClassDefinition getDefinitionByObjectClass() throws SchemaException {
        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl
                .getRefinedSchema(resourceModel.getObject(), getPageBase().getPrismContext());
        if (refinedSchema == null) {
            warn("No schema found in resource. Please check your configuration and try to test connection for the resource.");
            return null;
        }
        return refinedSchema.getRefinedDefinition(getObjectClass());

    }

    private String getTableIdKey() {
        if (kind == null) {
            return SessionStorage.KEY_RESOURCE_OBJECT_CLASS_CONTENT;
        }

        if (searchMode == null) {
            searchMode = SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT;
        }

//        if (searchMode.equals(SessionStorage.KEY_RESOURCE_PAGE_REPOSITORY_CONTENT)) {
            switch (kind) {
                case ACCOUNT:
                    return SessionStorage.KEY_RESOURCE_ACCOUNT_CONTENT + searchMode;
                case GENERIC:
                    return SessionStorage.KEY_RESOURCE_GENERIC_CONTENT + searchMode;
                case ENTITLEMENT:
                    return SessionStorage.KEY_RESOURCE_ENTITLEMENT_CONTENT + searchMode;

                default:
                    return SessionStorage.KEY_RESOURCE_OBJECT_CLASS_CONTENT;
            }
//        } else if (searchMode.equals(SessionStorage.KEY_RESOURCE_PAGE_RESOURCE_CONTENT)){
//            switch (kind) {
//                case ACCOUNT:
//                    return TableId.PAGE_RESOURCE_ACCOUNTS_PANEL_RESOURCE_MODE;
//                case GENERIC:
//                    return TableId.PAGE_RESOURCE_GENERIC_PANEL_RESOURCE_MODE;
//                case ENTITLEMENT:
//                    return TableId.PAGE_RESOURCE_ENTITLEMENT_PANEL_RESOURCE_MODE;
//
//                default:
//                    return TableId.PAGE_RESOURCE_OBJECT_CLASS_PANEL;
//            }
//        }
//        return TableId.PAGE_RESOURCE_OBJECT_CLASS_PANEL;
    }

    private void initLayout() {

        WebMarkupContainer totals = new WebMarkupContainer(ID_TOTALS);
        totals.setOutputMarkupId(true);
        add(totals);
        initShadowStatistics(totals);

        MainObjectListPanel<ShadowType> shadowListPanel =
                new MainObjectListPanel<ShadowType>(ID_TABLE,
                ShadowType.class, createSearchOptions()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return ResourceContentPanel.this.createRowMenuItems();
            }

            @Override
            protected List<IColumn> createDefaultColumns() {
                return (List) ResourceContentPanel.this.initColumns();
            }

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, ShadowType object) {
                shadowDetailsPerformed(target, WebComponentUtil.getName(object), object.getOid());

            }

            @Override
            protected boolean isCreateNewObjectEnabled(){
                return false;
            }

            @Override
            protected ISelectableDataProvider<ShadowType, SelectableBean<ShadowType>> createProvider() {
                provider = (SelectableBeanObjectDataProvider<ShadowType>) super.createProvider();
                provider.setEmptyListOnNullQuery(true);
                provider.setSort(null);
                provider.setUseObjectCounting(isUseObjectCounting());
                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                return provider;
            }

            @Override
            protected ObjectQuery createQuery() {
                ObjectQuery parentQuery = super.createQuery();
                QueryFactory queryFactory = pageBase.getPrismContext().queryFactory();

                List<ObjectFilter> filters = new ArrayList<>();
                if (parentQuery != null) {
                    filters.add(parentQuery.getFilter());
                }

                ObjectQuery customQuery = ResourceContentPanel.this.createQuery();
                if (customQuery != null && customQuery.getFilter() != null) {
                    filters.add(customQuery.getFilter());
                }
                if (filters.size() == 1) {
                    return queryFactory.createQuery(filters.iterator().next());
                }
                if (filters.size() == 0) {
                    return null;
                }
                return queryFactory.createQuery(queryFactory.createAnd(filters));
            }

            @Override
            protected LoadableModel<Search> createSearchModel() {
                return new LoadableModel<Search>(false) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Search load() {
                        String storageKey = getStorageKey();
                        Search search = null;
                        if (org.apache.commons.lang3.StringUtils.isNotEmpty(storageKey)) {
                            PageStorage storage = getPageStorage(storageKey);
                            if (storage != null) {
                                search = storage.getSearch();
                            }
                        }
                        Search newSearch = createSearch();
                        if (search == null
                                || !search.getAvailableDefinitions().containsAll(newSearch.getAvailableDefinitions())) {
                            search = newSearch;
                        }

                        String searchByName = getSearchByNameParameterValue();
                        if (searchByName != null) {
                            for (PropertySearchItem item : search.getPropertyItems()) {
                                if (ItemPath.create(ObjectType.F_NAME).equivalent(item.getPath())) {
                                    item.setValue(new SearchValue(searchByName));
                                }
                            }
                        }
                        return search;
                    }
                };
            }

            @Override
            protected Search createSearch() {
                return ResourceContentPanel.this.createSearch();

            }

                    @Override
                    protected String getTableIdKeyValue() {
                        return getTableIdKey();
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

        initButton(ID_IMPORT, "Import", " fa-download", TaskCategory.IMPORTING_ACCOUNTS, SystemObjectsType.ARCHETYPE_IMPORT_TASK.value());
        initButton(ID_RECONCILIATION, "Reconciliation", " fa-link", TaskCategory.RECONCILIATION, SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value());
        initButton(ID_LIVE_SYNC, "Live Sync", " fa-refresh", TaskCategory.LIVE_SYNCHRONIZATION, SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value());

        initCustomLayout();
    }

    protected abstract void initShadowStatistics(WebMarkupContainer totals);

    private void initButton(String id, String label, String icon, String category, String archetypeOid) {

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
                        redirectToTasksListPage(createInTaskOidQuery(filteredByKindIntentTasks), archetypeOid, target);
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
                        newTaskPerformed(category, archetypeOid, target);
                    }
                };
            }
        };
        items.add(item);

        DropdownButtonPanel button = new DropdownButtonPanel(id,
                new DropdownButtonDto(String.valueOf(tasksList != null ? tasksList.size() : 0), icon, label, items)) {
            @Override
            protected String getSpecialDropdownMenuClass() {
                return "pull-left";
            }
        };
        add(button);

    }

    private void newTaskPerformed(String category, String archetypeOid, AjaxRequestTarget target) {
        TaskType taskType = new TaskType(getPageBase().getPrismContext());
        PrismProperty<ShadowKindType> pKind;
        try {
            pKind = taskType.asPrismObject().findOrCreateProperty(
                    ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND));
            pKind.setRealValue(getKind());
        } catch (SchemaException e) {
            getSession().warn("Could not set kind for new task " + e.getMessage());
        }

        PrismProperty<String> pIntent;
        try {
            pIntent = taskType.asPrismObject().findOrCreateProperty(
                    ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
            pIntent.setRealValue(getIntent());
        } catch (SchemaException e) {
            getSession().warn("Could not set kind for new task " + e.getMessage());
        }

        PrismObject<ResourceType> resource = getResourceModel().getObject();
        taskType.setObjectRef(ObjectTypeUtil.createObjectRef(resource, getPageBase().getPrismContext()));

        taskType.setCategory(category); //todo no need in category here after tasks migration to archetype groups

        if (StringUtils.isNotEmpty(archetypeOid)) {
            AssignmentType archetypeAssignment = new AssignmentType();
            archetypeAssignment.setTargetRef(ObjectTypeUtil.createObjectRef(archetypeOid, ObjectTypes.ARCHETYPE));
            taskType.getAssignment().add(archetypeAssignment);
        }

        taskType.setOwnerRef(ObjectTypeUtil.createObjectRef(SecurityUtils.getPrincipalUser().getOid(), ObjectTypes.USER));

        getPageBase().navigateToNext(new PageTask(taskType.asPrismObject(), true));
    }

    private ObjectQuery createInTaskOidQuery(List<TaskType> tasksList){
        if (tasksList == null){
            tasksList = new ArrayList<>();
        }
        List<String> taskOids = new ArrayList<>();
        tasksList.forEach(task -> taskOids.add(task.getOid()));
        return getPageBase().getPrismContext().queryFor(TaskType.class)
                .id(taskOids.toArray(new String[0]))
                .build();
    }
    private void redirectToTasksListPage(ObjectQuery tasksQuery, String archetypeOid, AjaxRequestTarget target) {
        String taskCollectionViewName = getTaskCollectionViewNameByArchetypeOid(archetypeOid);
        PageParameters pageParameters = new PageParameters();
        if (StringUtils.isNotEmpty(taskCollectionViewName)){
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
                getPageBase().getCompiledGuiProfile().findAllApplicableArchetypeViews(TaskType.COMPLEX_TYPE);
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
            PrismProperty<ShadowKindType> taskKind = task
                    .findProperty(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND));
            ShadowKindType taskKindValue = null;

            if (taskKind == null) {
                PrismProperty<QName> taskObjectClass = task
                        .findProperty(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECTCLASS));

                if (taskObjectClass == null) {
                    LOGGER.warn("Bad task definition. Task {} doesn't contain definition either of objectClass or kind/intent", task.getOid());
                    continue;
                }

                QName objectClass = getObjectClass();
                if (objectClass == null) {
                    LOGGER.trace("Trying to determine objectClass for kind: {}, intent: {}", getKind(), getIntent());
                    RefinedObjectClassDefinition objectClassDef = null;
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

                if (QNameUtil.match(objectClass, taskObjectClass.getRealValue())) {
                    tasksForKind.add(task.asObjectable());
                }

                continue;
            }

            if (taskKind != null) {
                taskKindValue = taskKind.getRealValue();

                PrismProperty<String> taskIntent = task.findProperty(
                        ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
                String taskIntentValue = null;
                if (taskIntent != null) {
                    taskIntentValue = taskIntent.getRealValue();
                }
                if (StringUtils.isNotEmpty(getIntent())) {
                    if (getKind() == taskKindValue && getIntent().equals(taskIntentValue)) {
                        tasksForKind.add(task.asObjectable());
                    }
                } else if (getKind() == taskKindValue) {
                    tasksForKind.add(task.asObjectable());
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

            RefinedObjectClassDefinition rOcDef = getDefinitionByKind();
            if (rOcDef != null) {
                if (rOcDef.getKind() != null) {
                    baseQuery = ObjectQueryUtil.createResourceAndKindIntent(
                            resourceModel.getObject().getOid(), rOcDef.getKind(), rOcDef.getIntent(),
                            getPageBase().getPrismContext());
                } else {
                    baseQuery = ObjectQueryUtil.createResourceAndObjectClassQuery(
                            resourceModel.getObject().getOid(), rOcDef.getTypeName(),
                            getPageBase().getPrismContext());
                }
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
        return pageBase.createStringResource(key);
    }

    private List<IColumn<SelectableBean<ShadowType>, String>> initColumns() {

        List<ColumnTypeDto<String>> columnDefs = Arrays.asList(
                new ColumnTypeDto<String>("ShadowType.synchronizationSituation",
                        SelectableBeanImpl.F_VALUE + ".synchronizationSituation",
                        ShadowType.F_SYNCHRONIZATION_SITUATION.getLocalPart()),
                new ColumnTypeDto<String>("ShadowType.intent", SelectableBeanImpl.F_VALUE + ".intent",
                        ShadowType.F_INTENT.getLocalPart()));

        List<IColumn<SelectableBean<ShadowType>, String>> columns = new ArrayList<>();

        IColumn<SelectableBean<ShadowType>, String> identifiersColumn = new AbstractColumn<SelectableBean<ShadowType>, String>(
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

        columns.addAll((Collection) ColumnUtils.createColumns(columnDefs));

        ObjectLinkColumn<SelectableBean<ShadowType>> ownerColumn = new ObjectLinkColumn<SelectableBean<ShadowType>>(
                createStringResource("pageContentAccounts.owner")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<FocusType> createLinkModel(final IModel<SelectableBean<ShadowType>> rowModel) {

                return new IModel<FocusType>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public FocusType getObject() {
                        FocusType owner = loadShadowOwner(rowModel);
                        if (owner == null) {
                            return null;
                        }
                        return owner;
                    }

                };
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ShadowType>> rowModel,
                    ObjectType targetObjectType) {
                ownerDetailsPerformed(target, (FocusType) targetObjectType);
            }
        };
        columns.add(ownerColumn);

        columns.add(new AbstractColumn<SelectableBean<ShadowType>, String>(
                createStringResource("PageAccounts.accounts.pendingOperations")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ShadowType>>> cellItem,
                                     String componentId, IModel<SelectableBean<ShadowType>> rowModel) {
                cellItem.add(new PendingOperationPanel(componentId, new PropertyModel<List<PendingOperationType>>(rowModel, SelectableBeanImpl.F_VALUE + "." + ShadowType.F_PENDING_OPERATION.getLocalPart())));
            }
        });
        return columns;
    }

    private ShadowType getShadow(IModel<SelectableBean<ShadowType>> model) {
        if (model == null || model.getObject() == null || model.getObject().getValue() == null) {
            return null;
        }

        return (ShadowType) model.getObject().getValue();
    }

    private void ownerDetailsPerformed(AjaxRequestTarget target, FocusType owner) {
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
            error(pageBase.getString("pageContentAccounts.message.cantShowAccountDetails", accountName,
                    accountOid));
            target.add(pageBase.getFeedbackPanel());
            return;
        }

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, accountOid);
        getPageBase().navigateToNext(PageAccount.class, parameters);
    }

    private <F extends FocusType> F loadShadowOwner(String shadowOid) {

        Task task = pageBase.createSimpleTask(OPERATION_LOAD_SHADOW_OWNER);
        OperationResult result = new OperationResult(OPERATION_LOAD_SHADOW_OWNER);

        try {
            PrismObject<? extends FocusType> prismOwner = pageBase.getModelService()
                    .searchShadowOwner(shadowOid, null, task, result);

            if (prismOwner != null) {
                return (F) prismOwner.asObjectable();
            }
        } catch (ObjectNotFoundException exception) {
            // owner was not found, it's possible and it's ok on unlinked
            // accounts
        } catch (Exception ex) {
            result.recordFatalError(pageBase.getString("PageAccounts.message.ownerNotFound", shadowOid), ex);
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Could not load owner of account with oid: " + shadowOid, ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (WebComponentUtil.showResultInPage(result)) {
            pageBase.showResult(result, false);
        }

        return null;
    }

    @SuppressWarnings("serial")
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
                        if (getRowModel() == null){
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
                        if (getRowModel() == null){
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
                        if (getRowModel() == null){
                            deleteResourceObjectPerformed(null, target);
                        } else {
                            SelectableBeanImpl<ShadowType> shadow = getRowModel().getObject();
                            deleteResourceObjectPerformed(shadow.getValue(), target);
                        }
                    }
                };
            }
        });

//        items.add(new InlineMenuItem());

        items.add(new ButtonInlineMenuItem(createStringResource("pageContentAccounts.menu.importAccount"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        if (getRowModel() == null){
                            importResourceObject(null, target);
                        } else {
                            SelectableBeanImpl<ShadowType> shadow = getRowModel().getObject();
                            importResourceObject(shadow.getValue(), target);
                        }
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder(){
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
                        if (getRowModel() == null){
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
                        ObjectBrowserPanel<FocusType> browser = new ObjectBrowserPanel<FocusType>(
                                pageBase.getMainPopupBodyId(), UserType.class,
                                WebComponentUtil.createFocusTypeList(), false, pageBase) {

                            @Override
                            protected void onSelectPerformed(AjaxRequestTarget target, FocusType focus) {
                                changeOwner(shadow.getValue(), target, focus, Operation.MODIFY);
                            }

                        };

                        pageBase.showMainPopup(browser, target);

                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder(){
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM);
            }

            @Override
            public boolean isHeaderMenuItem(){
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
            selectedShadows = getTable().getSelectedObjects();
        }

        OperationResult result = new OperationResult(OPERATION_IMPORT_OBJECT);
        Task task = pageBase.createSimpleTask(OPERATION_IMPORT_OBJECT);

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
                result.recordPartialError(pageBase.createStringResource("ResourceContentPanel.message.importResourceObject.partialError", shadow).getString(), e);
                LOGGER.error("Could not import account {} ", shadow, e);
            }
        }

        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        getTable().refreshTable(null, target);
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

        ConfirmationPanel dialog = new ConfirmationPanel(((PageBase) getPage()).getMainPopupBodyId(),
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
        Task task = pageBase.createSimpleTask(OPERATION_DELETE_OBJECT);
        ModelExecuteOptions opts = createModelOptions();

        for (ShadowType shadow : selected) {
            try {
                ObjectDelta<ShadowType> deleteDelta = getPageBase().getPrismContext().deltaFactory().object().createDeleteDelta(ShadowType.class,
                        shadow.getOid());
                getPageBase().getModelService().executeChanges(
                        MiscUtil.createCollection(deleteDelta), opts, task, result);
            } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
                    | ExpressionEvaluationException | CommunicationException | ConfigurationException
                    | PolicyViolationException | SecurityViolationException e) {
                result.recordPartialError("Could not delete object " + shadow, e);
                LOGGER.error("Could not delete {}, using option {}", shadow, opts, e);
                continue;
            }
        }

        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        getTable().refreshTable(null, target);
        target.add(getPageBase().getFeedbackPanel());

    }

    private IModel<String> createDeleteConfirmString(final ShadowType selected, final String oneDeleteKey,
            final String moreDeleteKey) {
        return new IModel<String>() {

            @Override
            public String getObject() {
                List<ShadowType> selectedShadow = getSelectedShadowsList(selected);
                switch (selectedShadow.size()) {
                    case 1:
                        Object first = selectedShadow.get(0);
                        String name = WebComponentUtil.getName(((ShadowType) first));
                        return getPageBase().createStringResource(oneDeleteKey, name).getString();
                    default:
                        return getPageBase().createStringResource(moreDeleteKey, selectedShadow.size())
                                .getString();
                }
            }
        };
    }

    protected abstract ModelExecuteOptions createModelOptions();

    protected void updateResourceObjectStatusPerformed(ShadowType selected, AjaxRequestTarget target,
            boolean enabled) {
        List<ShadowType> selectedShadow = getSelectedShadowsList(selected);

        OperationResult result = new OperationResult(OPERATION_UPDATE_STATUS);
        Task task = pageBase.createSimpleTask(OPERATION_UPDATE_STATUS);

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
                // TODO Auto-generated catch block
                result.recordPartialError(pageBase.createStringResource("ResourceContentPanel.message.updateResourceObjectStatusPerformed.partialError", status, shadow).getString(), e);
                LOGGER.error("Could not update status (to {}) for {}, using option {}", status, shadow, opts,
                        e);
                continue;
            }
        }

        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        getTable().refreshTable(null, target);
        target.add(getPageBase().getFeedbackPanel());

    }

    private PrismObjectDefinition<FocusType> getFocusDefinition() {
        return pageBase.getPrismContext().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(FocusType.class);
    }

    private MainObjectListPanel<ShadowType> getTable() {
        return (MainObjectListPanel<ShadowType>) get(pageBase.createComponentPath(ID_TABLE));
    }

    private void changeOwner(ShadowType selected, AjaxRequestTarget target, FocusType ownerToChange,
            Operation operation) {

        pageBase.hideMainPopup(target);

        List<ShadowType> selectedShadow = getSelectedShadowsList(selected);

        Collection<? extends ItemDelta> modifications = new ArrayList<>();

        ReferenceDelta delta = null;
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
        Task task = pageBase.createSimpleTask(OPERATION_CHANGE_OWNER);
        ObjectDelta objectDelta = pageBase.getPrismContext().deltaFactory().object()
                .createModifyDelta(ownerOid, modifications, ownerType);
        Collection deltas = new ArrayList<>();
        deltas.add(objectDelta);
        try {
            if (!deltas.isEmpty()) {
                pageBase.getModelService().executeChanges(deltas, null, task, result);

            }
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
                | ExpressionEvaluationException | CommunicationException | ConfigurationException
                | PolicyViolationException | SecurityViolationException e) {

        }

        result.computeStatusIfUnknown();

        pageBase.showResult(result);
        target.add(pageBase.getFeedbackPanel());
        getTable().refreshTable(null, target);
        target.add(ResourceContentPanel.this);
    }

    private List<ShadowType> getSelectedShadowsList(ShadowType shadow) {
        List<ShadowType> selectedShadow = null;
        if (shadow != null) {
            selectedShadow = new ArrayList<>();
            selectedShadow.add(shadow);
        } else {
            provider.clearSelectedObjects();
            selectedShadow = getTable().getSelectedObjects();
        }
        return selectedShadow;
    }

    private ObjectQuery getExistingTasksQuery(String archetypeRefOid){
        return getPageBase().getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(resourceModel.getObject().getOid())
                .and()
                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(archetypeRefOid)
                .build();
    }

    private QName getTaskObjectClass(){
        QName objectClass = getObjectClass();
        if (objectClass == null) {
            LOGGER.trace("Trying to determine objectClass for kind: {}, intent: {}", getKind(), getIntent());
            RefinedObjectClassDefinition objectClassDef = null;
            try {
                objectClassDef = getDefinitionByKind();
            } catch (SchemaException e) {
                LOGGER.error("Failed to search for objectClass definition. Reason: {}", e.getMessage(), e);
            }
            if (objectClassDef == null) {
                LOGGER.warn("Cannot find any definition for kind: {}, intent: {}", getKind(), getIntent());
                return null;
            }

            objectClass = objectClassDef.getTypeName();
        }
        return objectClass;
    }

    protected abstract GetOperationOptionsBuilder addAdditionalOptions(GetOperationOptionsBuilder builder);

    protected abstract boolean isUseObjectCounting();
}
