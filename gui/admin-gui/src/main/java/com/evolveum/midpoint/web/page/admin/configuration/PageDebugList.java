/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.session.PageStorage;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.RepositoryObjectDataProvider;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.data.column.TwoValueLinkPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteAllDto;
import com.evolveum.midpoint.web.component.dialog.DeleteAllPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.page.admin.configuration.component.DebugButtonPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.DebugSearchFragment;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.configuration.component.PageDebugDownloadBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugConfDialogDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugObjectItem;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugSearchDto;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.server.PageTask;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.session.ConfigurationStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/debugs", matchUrlForSecurity = "/admin/config/debugs") },
        action = {
                @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL, label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_DEBUGS_URL, label = "PageDebugList.auth.debugs.label", description = "PageDebugList.auth.debugs.description") })
public class PageDebugList extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageDebugList.class);
    private static final String DOT_CLASS = PageDebugList.class.getName() + ".";
    private static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";

    private static final String OPERATION_LAXATIVE_DELETE = DOT_CLASS + "laxativeDelete";

    private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "loadResources";
    private static final String OPERATION_LOAD_RESOURCE_OBJECT = DOT_CLASS + "loadResourceObject";
    private static final String OPERATION_DELETE_SHADOWS = DOT_CLASS + "deleteShadows";
    private static final String OPERATION_LOAD_OBJECT_BY_OID = DOT_CLASS + "searchObjectByOid";

    private static final String ID_MAIN_FORM = "mainForm";

    private static final String ID_TABLE = "table";

    private static final String ID_TABLE_HEADER = "tableHeader";

    // search form model;
    private final IModel<DebugSearchDto> searchModel;
    // todo make this persistent (in user session)
    private final IModel<Boolean> showAllItemsModel = Model.of(true);
    // confirmation dialog model
    private final IModel<DebugConfDialogDto> confDialogModel;
    private final IModel<List<ObjectViewDto<ResourceType>>> resourcesModel;
    private final IModel<List<QName>> objectClassListModel;

    public PageDebugList() {
        searchModel = new LoadableModel<DebugSearchDto>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected DebugSearchDto load() {
                ConfigurationStorage storage = getSessionStorage().getConfiguration();

                DebugSearchDto dto = storage.getDebugSearchDto();
                if (dto == null) {
                    dto = new DebugSearchDto();
                    dto.setType(ObjectTypes.SYSTEM_CONFIGURATION);
                    setupSearchDto(dto);
                }

                return dto;
            }
        };

        confDialogModel = new LoadableModel<DebugConfDialogDto>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected DebugConfDialogDto load() {
                return new DebugConfDialogDto();
            }
        };

        resourcesModel = new LoadableModel<List<ObjectViewDto<ResourceType>>>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<ObjectViewDto<ResourceType>> load() {
                return loadResources();
            }
        };
        objectClassListModel = new LoadableModel<List<QName>>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<QName> load() {
                if (searchModel.getObject() != null && searchModel.getObject().getResource() != null) {
                    ObjectViewDto objectViewDto = searchModel.getObject().getResource();
                    OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE_OBJECT);
                    PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(ResourceType.class, objectViewDto.getOid(), PageDebugList.this,
                            createSimpleTask(OPERATION_LOAD_RESOURCE_OBJECT), result);
                    if (resource != null) {
                        return WebComponentUtil.loadResourceObjectClassValues(resource.asObjectable(), PageDebugList.this);
                    }
                }
                return new ArrayList<>();
            }
        };

        initLayout();
    }

    private List<ObjectViewDto<ResourceType>> loadResources() {
        List<ObjectViewDto<ResourceType>> objects = new ArrayList<>();

        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
        try {
            List<PrismObject<ResourceType>> list = WebModelServiceUtils.searchObjects(ResourceType.class,
                    null, SelectorOptions.createCollection(GetOperationOptions.createRaw()), result, this,
                    null);

            objects = list.stream().map(obj -> new ObjectViewDto<ResourceType>(obj.getOid(), WebComponentUtil.getName(obj))).collect(Collectors.toList());
        } catch (Exception ex) {
            result.recordFatalError(createStringResource("PageDebugList.listResources.fataError", ex.getMessage()).getString(), ex);
            LOGGER.error("Failed to load resources {}", ex.getMessage(), ex);
        }

        objects.sort((r1, r2) -> {

            if (r1 == null || r2 == null) {
                return 0;
            }

            Collator collator = WebComponentUtil.getCollator();
            return collator.compare(r1.getName(), r2.getName());

        });
        result.computeStatusIfUnknown();
        showResult(result, false);
        return objects;
    }

    private void initLayout() {
        Form main = new MidpointForm(ID_MAIN_FORM);
        add(main);

        DebugSearchDto dto = searchModel.getObject();
//        Class type = dto.getType().getClassDefinition();
        Class type = ObjectType.class;
        RepositoryObjectDataProvider provider = new RepositoryObjectDataProvider(this, type) {
            private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return getSessionStorage().getConfiguration();
            }
        };
        DebugSearchDto search = searchModel.getObject();
        ObjectQuery query = search.getSearch().createObjectQuery(this);
        provider.setQuery(createQuery(query));

        create(provider);

        PageDebugDownloadBehaviour ajaxDownloadBehavior = new PageDebugDownloadBehaviour();
        main.add(ajaxDownloadBehavior);
    }

    private void initDownload(AjaxRequestTarget target, Class<? extends ObjectType> type, ObjectQuery query) {
        List<PageDebugDownloadBehaviour> list = get(ID_MAIN_FORM)
                .getBehaviors(PageDebugDownloadBehaviour.class);
        PageDebugDownloadBehaviour downloadBehaviour = list.get(0);

        downloadBehaviour.setType(type);
        downloadBehaviour.setQuery(query);
        downloadBehaviour.setUseZip(hasToZip());
        downloadBehaviour.setShowAllItems(showAllItemsModel.getObject());
        downloadBehaviour.initiate(target);
    }

    private void create(RepositoryObjectDataProvider provider) {
        Form mainForm = (Form) get(ID_MAIN_FORM);

        BoxedTablePanel<DebugObjectItem> table = new BoxedTablePanel<DebugObjectItem>(ID_TABLE, provider, createColumns(),
                UserProfileStorage.TableId.CONF_DEBUG_LIST_PANEL) {
            private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                DebugSearchFragment headerFragment = new DebugSearchFragment(headerId, ID_TABLE_HEADER, PageDebugList.this, searchModel,
                        resourcesModel, objectClassListModel, showAllItemsModel) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void searchPerformed(AjaxRequestTarget target, boolean oidFilter) {
                        listObjectsPerformed(target, oidFilter);
                    }
                };
                headerFragment.setOutputMarkupId(true);
                return headerFragment;
            }

        };
        table.setOutputMarkupId(true);

        ConfigurationStorage storage = getSessionStorage().getConfiguration();
        table.setCurrentPage(storage.getPaging());

        mainForm.addOrReplace(table);
    }

    private List<IColumn<DebugObjectItem, String>> createColumns() {
        List<IColumn<DebugObjectItem, String>> columns = new ArrayList<>();

        IColumn<DebugObjectItem, String> column = new CheckBoxHeaderColumn<>();
        columns.add(column);

        column = new AjaxLinkColumn<DebugObjectItem>(createStringResource("pageDebugList.name"),
                DebugObjectItem.F_NAME, DebugObjectItem.F_NAME) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<DebugObjectItem>> cellItem, String componentId,
                    final IModel<DebugObjectItem> rowModel) {

                TwoValueLinkPanel panel = new TwoValueLinkPanel(componentId,
                        new IModel<String>() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public String getObject() {
                                DebugObjectItem object = rowModel.getObject();
                                if (object == null) {
                                    return null;
                                }
                                StringBuilder sb = new StringBuilder();
                                sb.append(object.getName());
                                if (object.getStatus() != null && object.getStatus() != OperationResultStatusType.SUCCESS
                                        && object.getStatus() != OperationResultStatusType.HANDLED_ERROR) {
                                    sb.append(" (");
                                    sb.append(object.getStatus());
                                    sb.append(")");
                                }
                                return sb.toString();
                            }
                        },
                        new PropertyModel<String>(rowModel, DebugObjectItem.F_OID)) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        DebugObjectItem object = rowModel.getObject();
                        objectEditPerformed(object.getOid(), object.getType());
                    }

                    @Override
                    public boolean isEnabled() {
                        return rowModel.getObject().getOid() != null;
                    }
                };

                cellItem.add(panel);
                cellItem.add(new AttributeModifier("class", "col-md-3"));

            }

        };

        columns.add(column);
        columns.add(new PropertyColumn<>(createStringResource("pageDebugList.description"),
                DebugObjectItem.F_DESCRIPTION));

        if (ObjectTypes.SHADOW.equals(searchModel.getObject().getType())) {
            columns.add(new PropertyColumn<>(createStringResource("pageDebugList.resourceName"),
                    DebugObjectItem.F_RESOURCE_NAME));
            columns.add(new PropertyColumn<>(createStringResource("pageDebugList.resourceType"),
                    DebugObjectItem.F_RESOURCE_TYPE));
        }

        column = new AbstractColumn<DebugObjectItem, String>(new Model<>(), null) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return "debug-list-buttons";
            }

            @Override
            public void populateItem(Item<ICellPopulator<DebugObjectItem>> cellItem, String componentId,
                    IModel<DebugObjectItem> rowModel) {
                cellItem.add(new DebugButtonPanel<DebugObjectItem>(componentId, rowModel) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void deletePerformed(AjaxRequestTarget target, IModel<DebugObjectItem> model) {
                        deleteSelected(target, model.getObject());
                    }

                    @Override
                    public void exportPerformed(AjaxRequestTarget target, IModel<DebugObjectItem> model) {
                        exportSelected(target, model.getObject());
                    }
                });
            }
        };

        columns.add(column);

        InlineMenuHeaderColumn<DebugObjectItem> menuColumn = new InlineMenuHeaderColumn<DebugObjectItem>(initInlineMenu()) {

            @Override
            public void populateItem(Item<ICellPopulator<DebugObjectItem>> cellItem, String componentId, IModel<DebugObjectItem> rowModel) {
                // we don't need row inline menu
                cellItem.add(new Label(componentId));
            }

        };
        columns.add(menuColumn);

        return columns;
    }

    private List<InlineMenuItem> initInlineMenu() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.exportSelected"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageDebugList.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        exportSelected(target, null);
                    }
                };
            }
        });

        headerMenuItems
                .add(new InlineMenuItem(createStringResource("pageDebugList.menu.exportAllSelectedType"), true) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public InlineMenuItemAction initAction() {
                        return new HeaderMenuAction(PageDebugList.this) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onSubmit(AjaxRequestTarget target) {
                                exportAllType(target);
                            }
                        };
                    }
                });

        headerMenuItems
                .add(new InlineMenuItem(createStringResource("pageDebugList.menu.exportShadowsOnResource")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public InlineMenuItemAction initAction() {
                        return new HeaderMenuAction(PageDebugList.this) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                exportAllShadowsOnResource(target);
                            }
                        };
                    }

                    @Override
                    public IModel<Boolean> getVisible() {
                        DebugSearchDto dto = searchModel.getObject();
                        return Model.of(ObjectTypes.SHADOW.equals(dto.getType()));
                    }

                });

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.exportAll"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageDebugList.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        exportAll(target);
                    }
                };
            }
        });

//        headerMenuItems.add(new InlineMenuItem());

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.deleteSelected"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageDebugList.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        deleteSelected(target, null);
                    }
                };
            }
        });

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.deleteAllType"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageDebugList.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        deleteAllType(target);
                    }
                };
            }
        });

        headerMenuItems
                .add(new InlineMenuItem(createStringResource("pageDebugList.menu.deleteShadowsOnResource")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public InlineMenuItemAction initAction() {
                        return new HeaderMenuAction(PageDebugList.this) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                deleteAllShadowsOnResource(target);
                            }
                        };
                    }

                    @Override
                    public IModel<Boolean> getVisible() {
                        DebugSearchDto dto = searchModel.getObject();
                        return Model.of(ObjectTypes.SHADOW.equals(dto.getType()));
                    }
                });

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.deleteAllIdentities"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageDebugList.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        deleteAllIdentities(target);
                    }
                };
            }
        });

        return headerMenuItems;
    }

    private boolean hasToZip() {
        BoxedTablePanel table = (BoxedTablePanel) getListTable();
        DebugSearchFragment header = (DebugSearchFragment) table.getHeader();
        AjaxCheckBox zipCheck = header.getZipCheck();

        return zipCheck.getModelObject();
    }

    private Table getListTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private void listObjectsPerformed(AjaxRequestTarget target, boolean isOidSearch) {
        DebugSearchDto dto = searchModel.getObject();
        if (isOidSearch && StringUtils.isNotEmpty(dto.getOidFilter())) {
            OperationResult result = new OperationResult(OPERATION_LOAD_OBJECT_BY_OID);
            Task task = createSimpleTask(OPERATION_LOAD_OBJECT_BY_OID);
            PrismObject<ObjectType> objectToDisplay = WebModelServiceUtils.loadObject(ObjectType.class, dto.getOidFilter(), PageDebugList.this,
                    task, result);
            if (objectToDisplay != null && objectToDisplay.getCompileTimeClass() != null) {
                dto.setType(ObjectTypes.getObjectType(objectToDisplay.getCompileTimeClass()));
            }
        }
        setupSearchDto(dto);

        Search search = dto.getSearch();
        ObjectQuery query = search.createObjectQuery(this);

        listObjectsPerformed(query, isOidSearch, target);
    }

    private void setupSearchDto(DebugSearchDto dto) {
        ObjectTypes type = dto.getType();
        Search search = dto.getSearch();
        if (search == null) {
            search = SearchFactory.createSearch(type.getClassDefinition(), this);
            search.setCanConfigure(true);
            SchemaRegistry registry = getPrismContext().getSchemaRegistry();
            PrismObjectDefinition objDef = registry.findObjectDefinitionByCompileTimeClass(ObjectType.class);
            List<SearchItemDefinition> configuredSearchItemDefs =
                    SearchFactory.getConfiguredSearchItemDefinitions(search.getAvailableDefinitions(), this,
                            ObjectType.class, null, Search.PanelType.DEBUG_PAGE);
            if (!configuredSearchItemDefs.isEmpty()) {
                SearchFactory.processSearchItemDefFromCompiledView(configuredSearchItemDefs, search, objDef);
            }
        }
        dto.setSearch(search);
    }

    private void listObjectsPerformed(ObjectQuery query, boolean isOidSearch, AjaxRequestTarget target) {
        DebugSearchDto dto = searchModel.getObject();

        if (!isOidSearch) {
            searchModel.getObject().setOidFilter(null);
        }

        RepositoryObjectDataProvider provider = getTableDataProvider();
        provider.setQuery(createQuery(query));

//        ObjectTypes selected = dto.getType();
//        if (selected != null) {
//            provider.setType(selected.getClassDefinition());
//        }

        // save object type category to session storage, used by back button
        ConfigurationStorage storage = getSessionStorage().getConfiguration();
        storage.setDebugSearchDto(dto);

        Table table = getListTable();
        table.getDataTable().getColumns().clear();
        table.getDataTable().getColumns().addAll(createColumns());
        target.add((Component) table);
        target.add(getFeedbackPanel());
    }

    private ObjectQuery createQuery(ObjectQuery searchQuery) {
        DebugSearchDto dto = searchModel.getObject();

        List<ObjectFilter> filters = new ArrayList<>();
//        String oidFilterValue = dto.getOidFilter();
//        if (StringUtils.isNotEmpty(oidFilterValue)) {
//            ObjectFilter inOidFilter = getPrismContext().queryFor(ObjectType.class).id(oidFilterValue).buildFilter();
//            filters.add(inOidFilter);
//
//            if (searchQuery == null) {
//                ObjectQuery query = getPrismContext().queryFor(ObjectType.class)
//                        .build();
//                query.addFilter(inOidFilter);
//                return query;
//            } else {
//                searchQuery.addFilter(inOidFilter);
//                return searchQuery;
//            }
//        }
//        if (ObjectTypes.SHADOW.equals(dto.getType()) && dto.getResource() != null) {
//            String oid = dto.getResource().getOid();
//            QName objectClass = dto.getObjectClass();
//            ObjectFilter objectFilter;
//            if (objectClass != null) {
//                objectFilter = getPrismContext().queryFor(ShadowType.class)
//                        .item(ShadowType.F_RESOURCE_REF).ref(oid)
//                        .and()
//                        .item(ShadowType.F_OBJECT_CLASS)
//                        .eq(objectClass)
//                        .buildFilter();
//            } else {
//                objectFilter = getPrismContext().queryFor(ShadowType.class)
//                        .item(ShadowType.F_RESOURCE_REF).ref(oid)
//                        .buildFilter();
//            }
//            filters.add(objectFilter);
//        }

        if (searchQuery != null && searchQuery.getFilter() != null) {
            filters.add(searchQuery.getFilter());
        }

        if (filters.isEmpty()) {
            return null;
        }

        ObjectFilter filter = filters.size() > 1 ? getPrismContext().queryFactory().createAnd(filters) : filters.get(0);
        return getPrismContext().queryFactory().createQuery(filter);
    }

    private void objectEditPerformed(String oid, Class<? extends ObjectType> type) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageDebugView.PARAM_OBJECT_ID, oid);
        parameters.add(PageDebugView.PARAM_OBJECT_TYPE, type.getSimpleName());
        parameters.add(PageDebugView.PARAM_SHOW_ALL_ITEMS, showAllItemsModel.getObject());
        navigateToNext(PageDebugView.class, parameters);
    }

    private RepositoryObjectDataProvider getTableDataProvider() {
        Table tablePanel = getListTable();
        DataTable table = tablePanel.getDataTable();
        return (RepositoryObjectDataProvider) table.getDataProvider();
    }

    private IModel<String> createDeleteConfirmString() {
        return new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                DebugConfDialogDto dto = confDialogModel.getObject();

                switch (dto.getOperation()) {
                    case DELETE_ALL_TYPE:
                        String type = getTypeLocalized(dto);
                        return createStringResource("pageDebugList.message.deleteAllType", type).getString();
                    case DELETE_SELECTED:
                        List<DebugObjectItem> selectedList = dto.getObjects();

                        if (selectedList.size() > 1) {
                            return createStringResource("pageDebugList.message.deleteSelectedConfirm",
                                    selectedList.size()).getString();
                        }

                        DebugObjectItem selectedItem = selectedList.get(0);
                        return createStringResource("pageDebugList.message.deleteObjectConfirm",
                                selectedItem.getName()).getString();
                    case DELETE_RESOURCE_SHADOWS:
                        DebugSearchDto search = searchModel.getObject();
                        return createStringResource("pageDebugList.messsage.deleteAllResourceShadows",
                                search.getResource().getName()).getString();
                }

                return "";
            }
        };
    }

    private String getTypeLocalized(DebugConfDialogDto dto) {
        String key;
        ObjectTypeGuiDescriptor descriptor = ObjectTypeGuiDescriptor.getDescriptor(dto.getType());
        if (descriptor == null) {
            key = "pageDebugList.descriptor.unknown";
        } else {
            key = descriptor.getLocalizationKey();
        }

        return createStringResource(key).getString();
    }

    private void deleteAllIdentitiesConfirmed(AjaxRequestTarget target, DeleteAllDto dto) {
        OperationResult result = new OperationResult(OPERATION_LAXATIVE_DELETE);
        String taskOid = null;
        try {
            if (dto.getDeleteUsers()) {
                ObjectQuery query = createDeleteAllUsersQuery();
                taskOid = deleteObjectsAsync(UserType.COMPLEX_TYPE, query, true, "Delete all users", result);
            }
            if (dto.getDeleteOrgs()) {
                taskOid = deleteObjectsAsync(OrgType.COMPLEX_TYPE, null, true, "Delete all orgs", result);
            }
            if (dto.getDeleteAccountShadow()) {
                taskOid = deleteAllShadowsConfirmed(result, true);
            }
            if (dto.getDeleteNonAccountShadow()) {
                taskOid = deleteAllShadowsConfirmed(result, false);
            }
        } catch (Exception ex) {
            result.computeStatus(getString("pageDebugList.message.laxativeProblem"));
            LoggingUtils.logUnexpectedException(LOGGER, getString("pageDebugList.message.laxativeProblem"), ex);
        }

        if (taskOid != null) {
            PageParameters parameters = new PageParameters();
            parameters.add(OnePageParameterEncoder.PARAMETER, taskOid);
            navigateToNext(PageTask.class, parameters);
        } else {
            navigateToNext(PageTasks.class);
        }
        target.add(getFeedbackPanel());

        showResult(result);
    }

    private ObjectQuery createDeleteAllUsersQuery() {
        return getPrismContext().queryFor(UserType.class).not().id(SystemObjectsType.USER_ADMINISTRATOR.value()).build();
    }

    private String deleteAllShadowsConfirmed(OperationResult result, boolean deleteAccountShadows)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {

        ObjectFilter kindFilter = getPrismContext().queryFor(ShadowType.class)
                .item(ShadowType.F_KIND).eq(ShadowKindType.ACCOUNT)
                .buildFilter();

        String taskName;
        ObjectQuery query;
        QueryFactory factory = getPrismContext().queryFactory();
        if (deleteAccountShadows) {
            taskName = "Delete all account shadows";
            query = factory.createQuery(kindFilter);
        } else {
            taskName = "Delete all non-account shadows";
            query = factory.createQuery(factory.createNot(kindFilter));
        }

        return deleteObjectsAsync(ShadowType.COMPLEX_TYPE, query, true, taskName, result);

    }

    private void exportSelected(AjaxRequestTarget target, DebugObjectItem item) {
        List<DebugObjectItem> selected = getSelectedData(item);
        if (selected.isEmpty()) {
            RepositoryObjectDataProvider provider = getTableDataProvider();
            selected = IteratorUtils.toList(provider.internalIterator(0, provider.size()));
        }

        List<String> oids = new ArrayList<>();
        for (DebugObjectItem dItem : selected) {
            oids.add(dItem.getOid());
        }

        DebugSearchDto searchDto = searchModel.getObject();
        ObjectQuery query = getPrismContext().queryFor(ObjectType.class).id(oids.toArray(new String[0])).build();
        initDownload(target, searchDto.getType().getClassDefinition(), query);
    }

    private void exportAllType(AjaxRequestTarget target) {
        DebugSearchDto searchDto = searchModel.getObject();
        initDownload(target, searchDto.getType().getClassDefinition(), null);
    }

    private void exportAll(AjaxRequestTarget target) {
        initDownload(target, ObjectType.class, null);
    }

    private void deleteAllType(AjaxRequestTarget target) {
        DebugSearchDto searchDto = searchModel.getObject();
        DebugConfDialogDto dto = new DebugConfDialogDto(DebugConfDialogDto.Operation.DELETE_ALL_TYPE, null,
                searchDto.getType().getClassDefinition());
        confDialogModel.setObject(dto);

        showMainPopup(getDeleteConfirmationPanel(), target);
    }

    private List<DebugObjectItem> getSelectedData(DebugObjectItem item) {
        if (item != null) {
            return Collections.singletonList(item);
        }

        return WebComponentUtil.getSelectedData(getListTable());
    }

    private void deleteSelected(AjaxRequestTarget target, DebugObjectItem item) {
        List<DebugObjectItem> selected = getSelectedData(item);
        if (selected.isEmpty()) {
            RepositoryObjectDataProvider provider = getTableDataProvider();
            selected = IteratorUtils.toList(provider.internalIterator(0, provider.size()));
        }

        DebugSearchDto searchDto = searchModel.getObject();
        DebugConfDialogDto dto = new DebugConfDialogDto(DebugConfDialogDto.Operation.DELETE_SELECTED,
                selected, searchDto.getType().getClassDefinition());
        confDialogModel.setObject(dto);

        showMainPopup(getDeleteConfirmationPanel(), target);
    }

    private void deleteAllIdentities(AjaxRequestTarget target) {
        DeleteAllPanel dialog = new DeleteAllPanel(getMainPopupBodyId()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                hideMainPopup(target);
                deleteAllIdentitiesConfirmed(target, getModel().getObject());
            }
        };
        showMainPopup(dialog, target);
    }

    private void deleteAllTypeConfirmed(AjaxRequestTarget target) {
        DebugSearchDto dto = searchModel.getObject();

        LOGGER.debug("Deleting all of type {}", dto.getType());

        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECTS);
        String taskOid = null;
        try {
            ObjectQuery query = null;
            if (ObjectTypes.USER.equals(dto.getType())) {
                query = createDeleteAllUsersQuery();
            }

            QName type = dto.getType().getTypeQName();

            taskOid = deleteObjectsAsync(type, query, true, "Delete all of type " + type.getLocalPart(),
                    result);

            info(getString("pageDebugList.messsage.deleteAllOfType", dto.getType()));
        } catch (Exception ex) {
            result.recomputeStatus();
            result.recordFatalError(getString("pageDebugList.messsage.notDeleteObjects", dto.getType()), ex);

            LoggingUtils.logUnexpectedException(LOGGER, "" + dto.getType(), ex);
        }

        finishOperation(result, taskOid, target);
    }

    private void deleteSelectedConfirmed(AjaxRequestTarget target, List<DebugObjectItem> items) {
        DebugConfDialogDto dto = confDialogModel.getObject();

        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECTS);
        for (DebugObjectItem bean : items) {
            WebModelServiceUtils.deleteObject(dto.getType(), bean.getOid(), executeOptions().raw(),
                    result, this);
        }
        result.computeStatusIfUnknown();

        RepositoryObjectDataProvider provider = getTableDataProvider();
        provider.clearCache();

        showResult(result);
        target.add((Component) getListTable());
        target.add(getFeedbackPanel());
    }

    private void deleteAllShadowsOnResource(AjaxRequestTarget target) {
        DebugSearchDto dto = searchModel.getObject();
        if (dto.getResource() == null) {
            error(getString("pageDebugList.message.resourceNotSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        LOGGER.debug("Displaying delete all shadows on resource {} confirmation dialog",
                dto.getResource().getName());

        DebugConfDialogDto dialogDto = new DebugConfDialogDto(
                DebugConfDialogDto.Operation.DELETE_RESOURCE_SHADOWS, null, null);
        confDialogModel.setObject(dialogDto);

        showMainPopup(getDeleteConfirmationPanel(), target);
    }

    private void exportAllShadowsOnResource(AjaxRequestTarget target) {
        DebugSearchDto dto = searchModel.getObject();
        if (dto.getResource() == null) {
            error(getString("pageDebugList.message.resourceNotSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        ObjectQuery objectQuery = getPrismContext().queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(dto.getResource().getOid())
                .build();
        initDownload(target, dto.getType().getClassDefinition(), objectQuery);
    }

    private Popupable getDeleteConfirmationPanel() {
        return new ConfirmationPanel(getMainPopupBodyId(), createDeleteConfirmString()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                DebugConfDialogDto dto = confDialogModel.getObject();
                switch (dto.getOperation()) {
                    case DELETE_ALL_TYPE:
                        deleteAllTypeConfirmed(target);
                        break;
                    case DELETE_SELECTED:
                        deleteSelectedConfirmed(target, dto.getObjects());
                        break;
                    case DELETE_RESOURCE_SHADOWS:
                        deleteAllShadowsOnResourceConfirmed(target);
                        break;
                }
            }
        };
    }

    private void deleteAllShadowsOnResourceConfirmed(AjaxRequestTarget target) {
        DebugSearchDto dto = searchModel.getObject();
        String resourceOid = dto.getResource().getOid();

        LOGGER.debug("Deleting shadows on resource {}", resourceOid);

        OperationResult result = new OperationResult(OPERATION_DELETE_SHADOWS);
        String taskOid = null;
        try {
            ObjectQuery objectQuery = getPrismContext().queryFor(ShadowType.class)
                    .item(ShadowType.F_RESOURCE_REF).ref(dto.getResource().getOid())
                    .build();

            QName type = ShadowType.COMPLEX_TYPE;

            taskOid = deleteObjectsAsync(type, objectQuery, true,
                    "Delete shadows on " + dto.getResource().getName(), result);

            info(getString("pageDebugList.messsage.deleteAllShadowsStarted", dto.getResource().getName()));
        } catch (Exception ex) {
            result.recomputeStatus();
            result.recordFatalError(getString("pageDebugList.messsage.notDeleteShadows"), ex);

            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete shadows", ex);
        }

        finishOperation(result, taskOid, target);
    }

    private void finishOperation(OperationResult result, String taskOid, AjaxRequestTarget target) {
        showResult(result);
        if (taskOid != null) {
            PageParameters parameters = new PageParameters();
            parameters.add(OnePageParameterEncoder.PARAMETER, taskOid);
            navigateToNext(PageTask.class, parameters);
        } else {
            navigateToNext(PageTasks.class);
        }
        target.add(getFeedbackPanel());
    }

}
