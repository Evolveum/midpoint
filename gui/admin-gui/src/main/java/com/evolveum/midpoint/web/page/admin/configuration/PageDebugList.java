/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.data.RepositoryObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.dialog.DeleteAllDialog;
import com.evolveum.midpoint.web.component.dialog.DeleteAllDto;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.configuration.component.DebugButtonPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.configuration.component.PageDebugDownloadBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugConfDialogDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugObjectItem;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugSearchDto;
import com.evolveum.midpoint.web.session.ConfigurationStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/config/debugs", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#debugs",
                label = "PageDebugList.auth.debugs.label", description = "PageDebugList.auth.debugs.description")})
public class PageDebugList extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageDebugList.class);
    private static final String DOT_CLASS = PageDebugList.class.getName() + ".";
    private static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";

    private static final String OPERATION_LAXATIVE_DELETE = DOT_CLASS + "laxativeDelete";

    private static final String ID_CONFIRM_DELETE_POPUP = "confirmDeletePopup";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_ZIP_CHECK = "zipCheck";
    private static final String ID_TABLE = "table";
    private static final String ID_CHOICE = "choice";
    private static final String ID_EXPORT = "export";
    private static final String ID_EXPORT_ALL = "exportAll";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_BASIC_SEARCH = "basicSearch";
    private static final String ID_DELETE_ALL_DIALOG = "confirmDeleteAll";

    private static final Integer DELETE_LOG_INTERVAL = 50;

    // search form model;
    private IModel<DebugSearchDto> searchModel;
    // confirmation dialog model
    private IModel<DebugConfDialogDto> confDialogModel;

    private int objectsDeleted = 0;

    public PageDebugList() {
        searchModel = new LoadableModel<DebugSearchDto>(false) {

            @Override
            protected DebugSearchDto load() {
                ConfigurationStorage storage = getSessionStorage().getConfiguration();
                return storage.getDebugSearchDto();
            }
        };

        confDialogModel = new LoadableModel<DebugConfDialogDto>() {

            @Override
            protected DebugConfDialogDto load() {
                return new DebugConfDialogDto();
            }
        };

        initLayout();
    }

    private void initLayout() {
        DeleteAllDialog deleteAllDialog = new DeleteAllDialog(ID_DELETE_ALL_DIALOG,
                createStringResource("pageDebugList.dialog.title.deleteAll")){

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);

                Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
                GetOperationOptions opt = GetOperationOptions.createRaw();
                options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

                Task task = createSimpleTask(OPERATION_LAXATIVE_DELETE);
                OperationResult result = new OperationResult(OPERATION_LAXATIVE_DELETE);

                objectsDeleted = 0;
                DeleteAllDto dto = getModel().getObject();

                if(dto.getDeleteUsers()){
                    deleteAllUsers(task, result, options);
                }
                if(dto.getDeleteOrgs()){
                    deleteAllOrgUnits(task, result, options);
                }
                if(dto.getDeleteAccountShadow()){
                    deleteAllAccountShadows(task, result, options, true);
                }
                if(dto.getDeleteNonAccountShadow()){
                    deleteAllAccountShadows(task, result, options, false);
                }

                LOGGER.info("Deleted {} out of {} objects.", objectsDeleted, getObjectsToDelete());

                target.add(getListTable());
                target.add(getFeedbackPanel());

                result.recomputeStatus();
                showResult(result);
            }
        };
        add(deleteAllDialog);

        ConfirmationDialog deleteConfirm = new ConfirmationDialog(ID_CONFIRM_DELETE_POPUP,
                createStringResource("pageDebugList.dialog.title.confirmDelete"), createDeleteConfirmString()) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);

                DebugConfDialogDto dto = confDialogModel.getObject();
                switch (dto.getOperation()) {
                    case DELETE_ALL_TYPE:
                        deleteAllTypeConfirmed(target);
                        break;
                    case DELETE_SELECTED:
                        deleteSelectedConfirmed(target, dto.getObjects());
                        break;
                }
            }

            @Override
            public boolean getLabelEscapeModelStrings(){
                return false;
            }
        };
        add(deleteConfirm);

        Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);
        searchForm.setOutputMarkupId(true);
        initSearchForm(searchForm);

        Form main = new Form(ID_MAIN_FORM);
        add(main);

        DebugSearchDto dto = searchModel.getObject();
        Class type = dto.getType().getClassDefinition();
        addOrReplaceTable(new RepositoryObjectDataProvider(this, type));

        AjaxCheckBox zipCheck = new AjaxCheckBox(ID_ZIP_CHECK, new Model<Boolean>(false)) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        main.add(zipCheck);

        PageDebugDownloadBehaviour ajaxDownloadBehavior = new PageDebugDownloadBehaviour();
        main.add(ajaxDownloadBehavior);
    }

    private void initDownload(AjaxRequestTarget target, Class<? extends ObjectType> type, ObjectQuery query) {
        List<PageDebugDownloadBehaviour> list = get(ID_MAIN_FORM).getBehaviors(PageDebugDownloadBehaviour.class);
        PageDebugDownloadBehaviour downloadBehaviour = list.get(0);

        downloadBehaviour.setType(type);
        downloadBehaviour.setQuery(query);
        downloadBehaviour.setUseZip(hasToZip());
        downloadBehaviour.initiate(target);
    }

    private void addOrReplaceTable(RepositoryObjectDataProvider provider) {
        provider.setQuery(createQuery());
        Form mainForm = (Form) get(ID_MAIN_FORM);

        TablePanel table = new TablePanel(ID_TABLE, provider, initColumns(provider.getType()), UserProfileStorage.TableId.CONF_DEBUG_LIST_PANEL);
        table.setOutputMarkupId(true);
        mainForm.addOrReplace(table);
    }

    private List<IColumn> initColumns(final Class<? extends ObjectType> type) {
        List<IColumn> columns = new ArrayList<>();

        IColumn column = new CheckBoxHeaderColumn<ObjectType>();
        columns.add(column);

        column = new LinkColumn<DebugObjectItem>(createStringResource("pageDebugList.name"),
                DebugObjectItem.F_NAME, DebugObjectItem.F_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<DebugObjectItem> rowModel) {
                DebugObjectItem object = rowModel.getObject();
                objectEditPerformed(target, object.getOid(), type);
            }
        };
        columns.add(column);

        columns.add(new PropertyColumn(createStringResource("pageDebugList.description"),
                DebugObjectItem.F_DESCRIPTION));

        if (ShadowType.class.isAssignableFrom(type)) {
            columns.add(new PropertyColumn(createStringResource("pageDebugList.resourceName"),
                    DebugObjectItem.F_RESOURCE_NAME));
            columns.add(new PropertyColumn(createStringResource("pageDebugList.resourceType"),
                    DebugObjectItem.F_RESOURCE_TYPE));
        }

        column = new AbstractColumn<DebugObjectItem, String>(new Model(), null) {

            @Override
            public String getCssClass() {
                return "debug-list-buttons";
            }

            @Override
            public void populateItem(Item<ICellPopulator<DebugObjectItem>> cellItem, String componentId,
                                     IModel<DebugObjectItem> rowModel) {
                cellItem.add(new DebugButtonPanel<DebugObjectItem>(componentId, rowModel) {

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

        column = new InlineMenuHeaderColumn<InlineMenuable>(initInlineMenu()) {

            @Override
            public void populateItem(Item<ICellPopulator<InlineMenuable>> cellItem, String componentId,
                                     IModel<InlineMenuable> rowModel) {
                //we don't need row inline menu
                cellItem.add(new Label(componentId));
            }
        };
        columns.add(column);

        return columns;
    }

    private List<InlineMenuItem> initInlineMenu() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.exportSelected"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        exportSelected(target, null);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.exportAllSelectedType"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        exportAllType(target);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.exportAll"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        exportAll(target);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem());

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.deleteSelected"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        deleteSelected(target, null);
                    }
                }));

//        headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.deleteAllType"), true,
//                new HeaderMenuAction(this) {
//
//                    @Override
//                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
//                        deleteAllType(target);
//                    }
//                }));

        headerMenuItems.add(new InlineMenuItem());

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageDebugList.menu.deleteAllIdentities"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        deleteAllIdentities(target);
                    }
                }));

        return headerMenuItems;
    }

    private boolean hasToZip() {
        AjaxCheckBox zipCheck = (AjaxCheckBox) get(createComponentPath(ID_MAIN_FORM, ID_ZIP_CHECK));
        return zipCheck.getModelObject();
    }

    private void initSearchForm(Form searchForm) {
        IChoiceRenderer<ObjectTypes> renderer = new IChoiceRenderer<ObjectTypes>() {

            @Override
            public Object getDisplayValue(ObjectTypes object) {
                ObjectTypeGuiDescriptor descr = ObjectTypeGuiDescriptor.getDescriptor(object);
                String key = descr != null ? descr.getLocalizationKey() : ObjectTypeGuiDescriptor.ERROR_LOCALIZATION_KEY;

                return new StringResourceModel(key, PageDebugList.this, null).getString();
            }

            @Override
            public String getIdValue(ObjectTypes object, int index) {
                return object.getClassDefinition().getSimpleName();
            }
        };

        DropDownChoice choice = new DropDownChoice(ID_CHOICE, new PropertyModel(searchModel, DebugSearchDto.F_TYPE),
                createChoiceModel(renderer), renderer);
        searchForm.add(choice);
        choice.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                listObjectsPerformed(target);
            }
        });

        BasicSearchPanel<DebugSearchDto> basicSearch = new BasicSearchPanel<DebugSearchDto>(ID_BASIC_SEARCH) {

            @Override
            protected IModel<String> createSearchTextModel() {
                return new PropertyModel<>(searchModel, DebugSearchDto.F_TEXT);
            }

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                listObjectsPerformed(target);
            }

            @Override
            protected void clearSearchPerformed(AjaxRequestTarget target) {
                PageDebugList.this.clearSearchPerformed(target);
            }
        };
        searchForm.add(basicSearch);
    }

    private IModel<List<ObjectTypes>> createChoiceModel(final IChoiceRenderer<ObjectTypes> renderer) {
        return new LoadableModel<List<ObjectTypes>>(false) {

            @Override
            protected List<ObjectTypes> load() {
                List<ObjectTypes> choices = new ArrayList<>();

                Collections.addAll(choices, ObjectTypes.values());
                choices.remove(ObjectTypes.OBJECT);

                Collections.sort(choices, new Comparator<ObjectTypes>() {

                    @Override
                    public int compare(ObjectTypes o1, ObjectTypes o2) {
                        String str1 = (String) renderer.getDisplayValue(o1);
                        String str2 = (String) renderer.getDisplayValue(o2);
                        return String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
                    }
                });

                return choices;
            }
        };
    }

    private TablePanel getListTable() {
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private void listObjectsPerformed(AjaxRequestTarget target) {
        DebugSearchDto dto = searchModel.getObject();
        ObjectTypes selected = dto.getType();

        RepositoryObjectDataProvider provider = getTableDataProvider();
        provider.setQuery(createQuery());

        if (selected != null) {
            provider.setType(selected.getClassDefinition());
            addOrReplaceTable(provider);
        }

        //save object type category to session storage, used by back button
        ConfigurationStorage storage = getSessionStorage().getConfiguration();
        storage.setDebugSearchDto(dto);

        TablePanel table = getListTable();
        target.add(table);
    }

    private ObjectQuery createQuery(){
        DebugSearchDto dto = searchModel.getObject();
        String nameText = dto.getText();
        ObjectQuery query = new ObjectQuery();

        if (StringUtils.isNotEmpty(nameText)) {
                PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
                String normalizedString = normalizer.normalize(nameText);

                ObjectFilter substring = SubstringFilter.createSubstring(ObjectType.F_NAME, ObjectType.class, getPrismContext(),
                        PolyStringNormMatchingRule.NAME, normalizedString);
                query.setFilter(substring);

            return  query;
        }

        return null;
    }

    private void objectEditPerformed(AjaxRequestTarget target, String oid, Class<? extends ObjectType> type) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageDebugView.PARAM_OBJECT_ID, oid);
        parameters.add(PageDebugView.PARAM_OBJECT_TYPE, type.getSimpleName());
        setResponsePage(PageDebugView.class, parameters);
    }

    private RepositoryObjectDataProvider getTableDataProvider() {
        TablePanel tablePanel = getListTable();
        DataTable table = tablePanel.getDataTable();
        return (RepositoryObjectDataProvider) table.getDataProvider();
    }

    private IModel<String> createDeleteConfirmString() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                DebugConfDialogDto dto = confDialogModel.getObject();

                switch (dto.getOperation()) {
                    case DELETE_ALL_TYPE:
                        String key = ObjectTypeGuiDescriptor.getDescriptor(dto.getType()).getLocalizationKey();
                        String type = createStringResource(key).getString();
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
                }

                return "";
            }
        };
    }

    private void deleteAllUsers(Task task, final OperationResult result, Collection<SelectorOptions<GetOperationOptions>> options){
        ResultHandler<UserType> userHandler = new ResultHandler<UserType>() {

            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
                if (!SystemObjectsType.USER_ADMINISTRATOR.value().equals(object.asObjectable().getOid())) {
                    ObjectDelta delta = ObjectDelta.createDeleteDelta(UserType.class, object.asObjectable().getOid(), getPrismContext());
                    Task task = createSimpleTask(OPERATION_LAXATIVE_DELETE);
                    OperationResult r = result.createMinorSubresult(OPERATION_LAXATIVE_DELETE);

                    try {
                        getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), ModelExecuteOptions.createRaw(), task, r);
                        objectsDeleted++;

                        if (objectsDeleted % DELETE_LOG_INTERVAL == 0)
                            LOGGER.info("Deleted {} out of {} objects.", objectsDeleted, getObjectsToDelete());

                        r.recordSuccess();
                    } catch (Exception ex) {
                        r.computeStatus(getString("pageDebugList.message.singleUserDeleteProblem"));
                        LoggingUtils.logException(LOGGER, getString("pageDebugList.message.singleUserDeleteProblem"), ex);
                    }
                    parentResult.addSubresult(r);
                }
                return true;
            }
        };

        try {
            getModelService().searchObjectsIterative(UserType.class, null, userHandler, options, task, result);
        } catch (Exception ex) {
            result.computeStatus(getString("pageDebugList.message.laxativeProblem"));
            LoggingUtils.logException(LOGGER, getString("pageDebugList.message.laxativeProblem"), ex);
        }
    }

    private void deleteAllOrgUnits(Task task, final OperationResult result, Collection<SelectorOptions<GetOperationOptions>> options){
        ResultHandler<OrgType> userHandler = new ResultHandler<OrgType>() {

            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
             ObjectDelta delta = ObjectDelta.createDeleteDelta(OrgType.class, object.asObjectable().getOid(), getPrismContext());
                Task task = createSimpleTask(OPERATION_LAXATIVE_DELETE);
                OperationResult r = result.createMinorSubresult(OPERATION_LAXATIVE_DELETE);

                try {
                    getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), ModelExecuteOptions.createRaw(), task, r);
                    objectsDeleted++;

                    if (objectsDeleted % DELETE_LOG_INTERVAL == 0)
                        LOGGER.info("Deleted {} out of {} objects.", objectsDeleted, getObjectsToDelete());

                    r.recordSuccess();
                } catch (Exception ex) {
                    r.computeStatus(getString("pageDebugList.message.singleOrgDeleteProblem"));
                    LoggingUtils.logException(LOGGER, getString("pageDebugList.message.singleOrgDeleteProblem"), ex);
                }
                parentResult.addSubresult(r);

                return true;
            }
        };

        try {
            getModelService().searchObjectsIterative(OrgType.class, null, userHandler, options, task, result);
        } catch (Exception ex) {
            result.computeStatus(getString("pageDebugList.message.laxativeProblem"));
            LoggingUtils.logException(LOGGER, getString("pageDebugList.message.laxativeProblem"), ex);
        }
    }

    private void deleteAllAccountShadows(Task task, final OperationResult result, Collection<SelectorOptions<GetOperationOptions>> options,
                                         boolean deleteAccountShadows){
        ResultHandler<ShadowType> shadowHandler = new ResultHandler<ShadowType>() {

            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
                ObjectDelta delta = ObjectDelta.createDeleteDelta(ShadowType.class, object.asObjectable().getOid(), getPrismContext());
                Task task = createSimpleTask(OPERATION_LAXATIVE_DELETE);
                OperationResult r = result.createMinorSubresult(OPERATION_LAXATIVE_DELETE);

                try {
                    getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), ModelExecuteOptions.createRaw(), task, r);
                    objectsDeleted++;

                    if (objectsDeleted % DELETE_LOG_INTERVAL == 0)
                        LOGGER.info("Deleted {} out of {} objects.", objectsDeleted, getObjectsToDelete());

                    r.recordSuccess();
                } catch (Exception ex) {
                    r.computeStatus(getString("pageDebugList.message.singleShadowDeleteProblem"));
                    LoggingUtils.logException(LOGGER, getString("pageDebugList.message.singleShadowDeleteProblem"), ex);
                }
                parentResult.addSubresult(r);
                return true;
            }
        };

        try {
            if(deleteAccountShadows){
                ObjectFilter filter = EqualFilter.createEqual(ShadowType.F_KIND, ShadowType.class, getPrismContext(), null, ShadowKindType.ACCOUNT);
                ObjectQuery query = ObjectQuery.createObjectQuery(filter);
                getModelService().searchObjectsIterative(ShadowType.class, query, shadowHandler, options, task, result);
            } else {
                ObjectFilter filter = EqualFilter.createEqual(ShadowType.F_KIND, ShadowType.class, getPrismContext(), null, ShadowKindType.ACCOUNT);
                ObjectQuery query = ObjectQuery.createObjectQuery(NotFilter.createNot(filter));
                getModelService().searchObjectsIterative(ShadowType.class, query, shadowHandler, options, task, result);
            }

        } catch (Exception ex) {
            result.computeStatus(getString("pageDebugList.message.laxativeProblem"));
            LoggingUtils.logException(LOGGER, getString("pageDebugList.message.laxativeProblem"), ex);
        }
    }

    private int getObjectsToDelete(){
        DeleteAllDialog dialog = (DeleteAllDialog)get(ID_DELETE_ALL_DIALOG);
        return dialog.getObjectsToDelete();
    }

    private void exportSelected(AjaxRequestTarget target, DebugObjectItem item) {
        List<DebugObjectItem> selected = getSelectedData(target, item);
        if (selected.isEmpty()) {
            return;
        }

        List<String> oids = new ArrayList<>();
        for (DebugObjectItem dItem : selected) {
            oids.add(dItem.getOid());
        }

        ObjectFilter filter = InOidFilter.createInOid(oids);

        DebugSearchDto searchDto = searchModel.getObject();
        initDownload(target, searchDto.getType().getClassDefinition(), ObjectQuery.createObjectQuery(filter));
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

        ModalWindow dialog = (ModalWindow) get(ID_CONFIRM_DELETE_POPUP);
        dialog.show(target);
    }

    private List<DebugObjectItem> getSelectedData(AjaxRequestTarget target, DebugObjectItem item) {
        List<DebugObjectItem> items;
        if (item != null) {
            items = new ArrayList<>();
            items.add(item);
            return items;
        }

        items = WebMiscUtil.getSelectedData(getListTable());
        if (items.isEmpty()) {
            warn(getString("pageDebugList.message.nothingSelected"));
            target.add(getFeedbackPanel());
        }

        return items;
    }

    private void deleteSelected(AjaxRequestTarget target, DebugObjectItem item) {
        List<DebugObjectItem> selected = getSelectedData(target, item);
        if (selected.isEmpty()) {
            return;
        }

        DebugSearchDto searchDto = searchModel.getObject();
        DebugConfDialogDto dto = new DebugConfDialogDto(DebugConfDialogDto.Operation.DELETE_SELECTED, selected,
                searchDto.getType().getClassDefinition());
        confDialogModel.setObject(dto);

        ModalWindow dialog = (ModalWindow) get(ID_CONFIRM_DELETE_POPUP);
        dialog.show(target);
    }

    private void deleteAllIdentities(AjaxRequestTarget target) {
        DeleteAllDialog dialog = (DeleteAllDialog) get(ID_DELETE_ALL_DIALOG);
//        dialog.getModel().setObject(dialog.createMessage());
        dialog.show(target);
    }

    private void deleteAllTypeConfirmed(AjaxRequestTarget target) {
        //todo implement [lazyman] as background task...
        warn("Not implemented yet, will be implemented as background task.");
        target.add(getFeedbackPanel());
    }

    private void deleteSelectedConfirmed(AjaxRequestTarget target, List<DebugObjectItem> items) {
        DebugConfDialogDto dto = confDialogModel.getObject();

        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECTS);
        for (DebugObjectItem bean : items) {
            WebModelUtils.deleteObject(dto.getType(), bean.getOid(), ModelExecuteOptions.createRaw(), result, this);
        }
        result.computeStatusIfUnknown();

        RepositoryObjectDataProvider provider = getTableDataProvider();
        provider.clearCache();

        showResult(result);
        target.add(getListTable());
        target.add(getFeedbackPanel());
    }

    private void clearSearchPerformed(AjaxRequestTarget target){
        DebugSearchDto dto = new DebugSearchDto();

        TablePanel panel = getListTable();
        DataTable table = panel.getDataTable();
        RepositoryObjectDataProvider provider = (RepositoryObjectDataProvider) table.getDataProvider();
        provider.setQuery(null);

        provider.setType(dto.getType().getClassDefinition());
        searchModel.setObject(new DebugSearchDto());

        ConfigurationStorage storage = getSessionStorage().getConfiguration();
        storage.setDebugSearchDto(searchModel.getObject());

        target.add(get(ID_SEARCH_FORM));
        target.add(panel);
    }
}
