/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.impl.component.search.*;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.FilterableSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.SearchConfigurationWrapper;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.web.session.GenericPageStorage;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.task.PageTask;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.RepositoryObjectDataProvider;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.TwoValueLinkPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteAllDto;
import com.evolveum.midpoint.web.component.dialog.DeleteAllPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.page.admin.configuration.component.DebugButtonPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.DebugSearchFragment;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.configuration.component.PageDebugDownloadBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugConfDialogDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugObjectItem;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.session.PageStorage;
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
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL, label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_DEBUGS_URL, label = "PageDebugList.auth.debugs.label", description = "PageDebugList.auth.debugs.description") })
public class PageDebugList extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageDebugList.class);
    private static final String DOT_CLASS = PageDebugList.class.getName() + ".";
    private static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";

    private static final String OPERATION_LAXATIVE_DELETE = DOT_CLASS + "laxativeDelete";
    private static final String OPERATION_DELETE_SHADOWS = DOT_CLASS + "deleteShadows";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";
    private static final String ID_TABLE_HEADER = "tableHeader";

    // search form model;
    private LoadableDetachableModel<Search<? extends ObjectType>> searchModel = null;
    // todo make this persistent (in user session)
    private final IModel<Boolean> showAllItemsModel = Model.of(true);
    // confirmation dialog model
    private IModel<DebugConfDialogDto> confDialogModel = null;


    public PageDebugList() {
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        DebugConfDialogDto searchDto = new DebugConfDialogDto();
        searchDto.setType(SystemConfigurationType.class);
        confDialogModel = Model.of(searchDto);

        searchModel = new LoadableDetachableModel<>() {

            @Override
            protected Search<? extends ObjectType> load() {
                GenericPageStorage storage = getSessionStorage().getConfiguration();
                Search search = storage.getSearch();

                if (search == null || search.isForceReload()) {
                    Class<? extends ObjectType> type = getType();


                    search = createSearch(type);
                //SearchFactory.createSearch(getSchemaService().findContainerDefinitionByCompileTimeClass(type), defaultSearchConfig, null, PageDebugList.this);
//                    search = SearchFactory.createSearch((Class) confDialogModel.getObject().getType(),  PageDebugList.this);
                    //TODO axiom?
                    search.setAllowedModeList(Arrays.asList(SearchBoxModeType.BASIC, SearchBoxModeType.ADVANCED, SearchBoxModeType.OID));

                    storage.setSearch(search);
                }
                return search;
            }
        };

        initLayout();
    }

    private Search<? extends ObjectType> createSearch(Class<? extends ObjectType> type) {
        SearchFactory<? extends ObjectType> factory = new SearchFactory<>(type)
                .additionalSearchContext(createAdditionalSearchContext())
                .modelServiceLocator(PageDebugList.this);

        return factory.createSearch();
    }

    private SearchContext createAdditionalSearchContext() {
        SearchContext ctx = new SearchContext();
        ctx.setPanelType(PredefinedSearchableItems.PanelType.DEBUG);
        return ctx;
    }


    private List<QName> getAllowedTypes() {
        List<QName> choices = new ArrayList<>();
        WebComponentUtil.createObjectTypesList().stream()
                .forEach(type -> choices.add(type.getTypeQName()));
        return choices;
    }

    private void initLayout() {
        Form<?> main = new MidpointForm<>(ID_MAIN_FORM);
        add(main);

        RepositoryObjectDataProvider<? extends ObjectType> provider = new RepositoryObjectDataProvider<>(
                this, (IModel) searchModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return getSessionStorage().getConfiguration();
            }
        };

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

        BoxedTablePanel<DebugObjectItem> table = new BoxedTablePanel<>(ID_TABLE, provider, createColumns(),
                UserProfileStorage.TableId.CONF_DEBUG_LIST_PANEL) {
            private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                DebugSearchFragment headerFragment = new DebugSearchFragment(headerId, ID_TABLE_HEADER, PageDebugList.this, searchModel,
                        showAllItemsModel) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void searchPerformed(AjaxRequestTarget target) {
                        listObjectsPerformed(target);
                    }
                };
                headerFragment.setOutputMarkupId(true);
                return headerFragment;
            }

        };
        table.setOutputMarkupId(true);

        GenericPageStorage storage = getSessionStorage().getConfiguration();
        table.setCurrentPage(storage.getPaging());

        mainForm.addOrReplace(table);
    }

    private List<IColumn<DebugObjectItem, String>> createColumns() {
        List<IColumn<DebugObjectItem, String>> columns = new ArrayList<>();

        IColumn<DebugObjectItem, String> column = new CheckBoxHeaderColumn<>();
        columns.add(column);

        column = new AjaxLinkColumn<>(createStringResource("pageDebugList.name"),
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

        if (ShadowType.class.equals(getType())) {
            columns.add(new PropertyColumn<>(createStringResource("pageDebugList.resourceName"),
                    DebugObjectItem.F_RESOURCE_NAME));
            columns.add(new PropertyColumn<>(createStringResource("pageDebugList.resourceType"),
                    DebugObjectItem.F_RESOURCE_TYPE));
        }

        column = new AbstractColumn<>(new Model<>(), null) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return "debug-list-buttons";
            }

            @Override
            public void populateItem(Item<ICellPopulator<DebugObjectItem>> cellItem, String componentId,
                    IModel<DebugObjectItem> rowModel) {
                cellItem.add(new DebugButtonPanel<>(componentId, rowModel) {
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

        columns.add(new InlineMenuHeaderColumn(initInlineMenu()));

        return columns;
    }

    @NotNull
    private Class<? extends ObjectType> getType() {
        return searchModel.isAttached() ? searchModel.getObject().getTypeClass() : SystemConfigurationType.class;
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
                        return Model.of(ShadowType.class.equals(getType()));
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
                        return Model.of(ShadowType.class.equals(getType()));
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
        CheckBoxPanel zipCheck = header.getZipCheck();

        return zipCheck.getValue();
    }

    private Table getListTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private <C extends Containerable> void listObjectsPerformed(AjaxRequestTarget target) {
        Table table = getListTable();


        Search<? extends ObjectType> search = searchModel.getObject();
        if (search.isForceReload()) {
            Class<? extends ObjectType> type = search.getTypeClass();

            Search<? extends ObjectType> newSearch = createSearch(type);
                        searchModel.setObject(newSearch);//TODO: this is veeery ugly, available definitions should refresh when the type changed
                        table.getDataTable().getColumns().clear();
                        table.getDataTable().getColumns().addAll(createColumns());
        }


        // save object type category to session storage, used by back button
        GenericPageStorage storage = getSessionStorage().getConfiguration();
        storage.setSearch(searchModel.getObject());

        target.add((Component) table);
        target.add(getFeedbackPanel());
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
        return new IModel<>() {
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
                        return createStringResource("pageDebugList.messsage.deleteAllResourceShadows",
                                WebComponentUtil.getReferencedObjectDisplayNamesAndNames(getResourceRefFromSearch(), false)
                        ).getString();
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
                taskOid = deleteObjectsAsync(UserType.COMPLEX_TYPE, query, "Delete all users", result);
            }
            if (dto.getDeleteOrgs()) {
                taskOid = deleteObjectsAsync(OrgType.COMPLEX_TYPE, null, "Delete all orgs", result);
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

        return deleteObjectsAsync(ShadowType.COMPLEX_TYPE, query, taskName, result);

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

        ObjectQuery query = getPrismContext().queryFor(ObjectType.class).id(oids.toArray(new String[0])).build();
        initDownload(target, getType(), query);
    }

    private void exportAllType(AjaxRequestTarget target) {
        initDownload(target, getType(), null);
    }

    private void exportAll(AjaxRequestTarget target) {
        initDownload(target, ObjectType.class, null);
    }

    private void deleteAllType(AjaxRequestTarget target) {
        DebugConfDialogDto dto = new DebugConfDialogDto(DebugConfDialogDto.Operation.DELETE_ALL_TYPE, null,
                getType());
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

        DebugConfDialogDto dto = new DebugConfDialogDto(DebugConfDialogDto.Operation.DELETE_SELECTED,
                selected, getType());
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

        LOGGER.debug("Deleting all of type {}", getType());

        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECTS);
        String taskOid = null;
        try {
            ObjectQuery query = null;
            if (ObjectTypes.USER.getClassDefinition().equals(getType())) {
                query = createDeleteAllUsersQuery();
            }

            QName type = ObjectTypes.getObjectType(getType()).getTypeQName();

            taskOid = deleteObjectsAsync(type, query, "Delete all of type " + type.getLocalPart(),
                    result);

            info(getString("pageDebugList.messsage.deleteAllOfType", getType()));
        } catch (Exception ex) {
            result.recomputeStatus();
            result.recordFatalError(getString("pageDebugList.messsage.notDeleteObjects", getType()), ex);

            LoggingUtils.logUnexpectedException(LOGGER, "" + getType(), ex);
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
        ObjectReferenceType resourceRef = getResourceRefFromSearch();
        if (resourceRef == null || resourceRef.getOid() == null) {
            error(getString("pageDebugList.message.resourceNotSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        LOGGER.debug("Displaying delete all shadows on resource {} confirmation dialog",
                WebComponentUtil.getReferencedObjectDisplayNamesAndNames(resourceRef, false));

        DebugConfDialogDto dialogDto = new DebugConfDialogDto(
                DebugConfDialogDto.Operation.DELETE_RESOURCE_SHADOWS, null, null);
        confDialogModel.setObject(dialogDto);

        showMainPopup(getDeleteConfirmationPanel(), target);
    }

    private boolean resourceReferenceIsNotEmpty() {
        ObjectReferenceType resourceRef = getResourceRefFromSearch();
        if (resourceRef == null) {
            return false;
        }

        return resourceRef.getOid() != null;
    }

    private ObjectReferenceType getResourceRefFromSearch() {
        Search search = searchModel.getObject();
        FilterableSearchItemWrapper searchItem = search.findPropertyItemByPath(ShadowType.F_RESOURCE_REF);
        DisplayableValue<ObjectReferenceType> displayableValue = searchItem.getValue();
        if (displayableValue != null) {
            return displayableValue.getValue();
        }
        return null;
    }

    private void exportAllShadowsOnResource(AjaxRequestTarget target) {
        ObjectReferenceType resourceRef = getResourceRefFromSearch();
        if (resourceRef == null || resourceRef.asReferenceValue().isEmpty()) {
            error(getString("pageDebugList.message.resourceNotSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        ObjectQuery objectQuery = getPrismContext().queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resourceRef.getOid())
                .build();
        initDownload(target, getType(), objectQuery);
    }

    private Popupable getDeleteConfirmationPanel() {
        return new DeleteConfirmationPanel(getMainPopupBodyId(), createDeleteConfirmString()) {
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
        ObjectReferenceType resourceRef = getResourceRefFromSearch();

        String resourceName = WebComponentUtil.getReferencedObjectDisplayNamesAndNames(resourceRef, false);
        if (resourceRef == null) {
            warn(getString("pageDebugList.messsage.deleteAllShadows.noResource", resourceName));
            return;
        }
        String resourceOid = resourceRef.getOid(); //TODO NPE

        LOGGER.debug("Deleting shadows on resource {}", resourceOid);

        OperationResult result = new OperationResult(OPERATION_DELETE_SHADOWS);
        String taskOid = null;
        try {
            ObjectQuery objectQuery = getPrismContext().queryFor(ShadowType.class)
                    .item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
                    .build();

            QName type = ShadowType.COMPLEX_TYPE;


            taskOid = deleteObjectsAsync(type, objectQuery,
                    "Delete shadows on " + resourceName, result);
            info(getString("pageDebugList.messsage.deleteAllShadowsStarted", resourceName));
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
