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

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromFile;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.dto.AccountDetailsSearchDto;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ResourceItemDto;
import com.evolveum.midpoint.web.session.ConfigurationStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.SearchFormEnterBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;

import javax.xml.namespace.QName;

import java.io.*;
import java.util.*;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/config/sync/accounts", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#configSyncAccounts",
                label = "PageAccounts.auth.configSyncAccounts.label", description = "PageAccounts.auth.configSyncAccounts.description")})
public class PageAccounts extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageAccounts.class);

    private static final String DOT_CLASS = PageAccounts.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "loadResources";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
    private static final String OPERATION_EXPORT = DOT_CLASS + "export";
    private static final String OPERATION_EXPORT_ACCOUNT = DOT_CLASS + "exportAccount";
    private static final String OPERATION_GET_TOTALS = DOT_CLASS + "getTotals";
    private static final String OPERATION_GET_INTENTS = DOT_CLASS + "getResourceIntentList";
    private static final String OPERATION_GET_OBJECT_CLASS = DOT_CLASS + "getResourceObjectClassList";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_FORM_ACCOUNT = "accountForm";
    private static final String ID_RESOURCES = "resources";
    private static final String ID_LIST_SYNC_DETAILS = "listSyncDetails";
    private static final String ID_EXPORT = "export";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_CLEAR_EXPORT = "clearExport";
    private static final String ID_FILES_CONTAINER = "filesContainer";
    private static final String ID_FILES = "files";
    private static final String ID_FILE = "file";
    private static final String ID_FILE_NAME = "fileName";
    private static final String ID_TOTALS = "totals";
    private static final String ID_TOTAL = "total";
    private static final String ID_DELETED = "deleted";
    private static final String ID_UNMATCHED = "unmatched";
    private static final String ID_DISPUTED = "disputed";
    private static final String ID_LINKED = "linked";
    private static final String ID_UNLINKED = "unlinked";
    private static final String ID_ACCOUNTS_CONTAINER = "accountsContainer";
    private static final String ID_NOTHING = "nothing";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH_KIND = "kindSearch";
    private static final String ID_SEARCH_INTENT = "intentSearch";
    private static final String ID_SEARCH_OBJECT_CLASS = "objectClassSearch";
    private static final String ID_SEARCH_BASIC = "basicSearch";

    private static final Integer AUTO_COMPLETE_LIST_SIZE = 10;

    private IModel<List<ResourceItemDto>> resourcesModel;
    private LoadableModel<List<String>> filesModel;
    private IModel<AccountDetailsSearchDto> searchModel;

    private IModel<ResourceItemDto> resourceModel = new Model<ResourceItemDto>();
    private LoadableModel<Integer> totalModel;
    private LoadableModel<Integer> deletedModel;
    private LoadableModel<Integer> unmatchedModel;
    private LoadableModel<Integer> disputedModel;
    private LoadableModel<Integer> linkedModel;
    private LoadableModel<Integer> unlinkedModel;
    private LoadableModel<Integer> nothingModel;

    private File downloadFile;

    public PageAccounts() {
        searchModel = new LoadableModel<AccountDetailsSearchDto>() {

            @Override
            protected AccountDetailsSearchDto load() {
                ConfigurationStorage storage = getSessionStorage().getConfiguration();
                AccountDetailsSearchDto dto = storage.getAccountSearchDto();

                if(dto == null){
                    dto = new AccountDetailsSearchDto();
                }

                return dto;
            }
        };

        resourcesModel = new LoadableModel<List<ResourceItemDto>>() {

            @Override
            protected List<ResourceItemDto> load() {
                return loadResources();
            }
        };

        initLayout();
    }

    private void initLayout() {
        Form form = new Form(ID_MAIN_FORM);
        form.setOutputMarkupId(true);
        add(form);

        Form accForm = new Form(ID_FORM_ACCOUNT);
        accForm.setOutputMarkupId(true);
        add(accForm);

        Form searchForm = new Form(ID_SEARCH_FORM);
        initSearchForm(searchForm);
        searchForm.setOutputMarkupPlaceholderTag(true);
        searchForm.setOutputMarkupId(true);
        searchForm.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return resourceModel.getObject() != null;
            }
        });
        add(searchForm);

        DropDownChoice<ResourceItemDto> resources = new DropDownChoice<ResourceItemDto>(
                ID_RESOURCES, resourceModel, resourcesModel,
                new IChoiceRenderer<ResourceItemDto>() {

                    @Override
                    public Object getDisplayValue(ResourceItemDto object) {
                        if (object == null) {
                            return "";
                        }

                        return object.getName();
                    }

                    @Override
                    public String getIdValue(ResourceItemDto object, int index) {
                        return Integer.toString(index);
                    }
                });
        form.add(resources);

        initLinks(form, accForm);
        initTotals(form);

        final AjaxDownloadBehaviorFromFile ajaxDownloadBehavior = new AjaxDownloadBehaviorFromFile(true) {

            @Override
            protected File initFile() {
                return downloadFile;
            }
        };
        ajaxDownloadBehavior.setRemoveFile(false);
        form.add(ajaxDownloadBehavior);

        WebMarkupContainer filesContainer = new WebMarkupContainer(ID_FILES_CONTAINER);
        filesContainer.setOutputMarkupId(true);
        accForm.add(filesContainer);

        filesModel = createFilesModel();
        ListView<String> files = new ListView<String>(ID_FILES, filesModel) {

            @Override
            protected void populateItem(final ListItem<String> item) {
                AjaxLink file = new AjaxLink(ID_FILE) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        downloadPerformed(target, item.getModelObject(), ajaxDownloadBehavior);
                    }
                };
                file.add(new Label(ID_FILE_NAME, item.getModelObject()));
                item.add(file);
            }
        };
        files.setRenderBodyOnly(true);
        filesContainer.add(files);

        WebMarkupContainer accountsContainer = new WebMarkupContainer(ID_ACCOUNTS_CONTAINER);
        accountsContainer.setOutputMarkupId(true);
        accForm.add(accountsContainer);

        ObjectDataProvider provider = new ObjectDataProvider(this, ShadowType.class);
        provider.setOptions(SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        provider.setQuery(ObjectQuery.createObjectQuery(createResourceQueryFilter()));
        TablePanel accounts = new TablePanel(ID_ACCOUNTS, provider, createAccountsColumns(), UserProfileStorage.TableId.CONF_PAGE_ACCOUNTS);
        accounts.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return resourceModel.getObject() != null;
            }
        });
        accounts.setItemsPerPage(50);
        accountsContainer.add(accounts);
    }

    private void initSearchForm(Form searchForm){
        BasicSearchPanel<AccountDetailsSearchDto> basicSearch = new BasicSearchPanel<AccountDetailsSearchDto>(ID_SEARCH_BASIC) {

            @Override
            protected IModel<String> createSearchTextModel() {
                return new PropertyModel<>(searchModel, AccountDetailsSearchDto.F_SEARCH_TEXT);
            }

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                PageAccounts.this.searchPerformed(target);
            }

            @Override
            protected void clearSearchPerformed(AjaxRequestTarget target) {
                PageAccounts.this.clearSearchPerformed(target);
            }
        };
        basicSearch.setOutputMarkupId(true);
        searchForm.add(basicSearch);

        DropDownChoice kind = new DropDownChoice(ID_SEARCH_KIND,
                new PropertyModel<ShadowKindType>(searchModel, AccountDetailsSearchDto.F_KIND),
                WebMiscUtil.createReadonlyModelFromEnum(ShadowKindType.class), new EnumChoiceRenderer(this));
        kind.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                searchPerformed(target);
            }
        });
        kind.setOutputMarkupId(true);
        kind.setNullValid(true);
        searchForm.add(kind);

        DropDownChoice intent = new DropDownChoice(ID_SEARCH_INTENT,
                new PropertyModel<String>(searchModel, AccountDetailsSearchDto.F_INTENT),
                createIntentChoices(), new IChoiceRenderer<String>() {

            @Override
            public String getDisplayValue(String object) {
                return object;
            }

            @Override
            public String getIdValue(String object, int index) {
                return Integer.toString(index);
            }
        });
        intent.setNullValid(true);
        intent.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                searchPerformed(target);
            }
        });
        intent.setOutputMarkupId(true);
        searchForm.add(intent);

        AutoCompleteTextField<String> objectClass = new AutoCompleteTextField<String>(ID_SEARCH_OBJECT_CLASS,
                new PropertyModel<String>(searchModel, AccountDetailsSearchDto.F_OBJECT_CLASS)) {

            @Override
            protected Iterator<String> getChoices(String input) {
                if(Strings.isEmpty(input)){
                    List<String> emptyList = Collections.emptyList();
                    return emptyList.iterator();
                }

                AccountDetailsSearchDto dto = searchModel.getObject();
                List<QName> accountObjectClassList = dto.getObjectClassList();
                List<String> choices = new ArrayList<>(AUTO_COMPLETE_LIST_SIZE);

                for(QName s: accountObjectClassList){
                    if(s.getLocalPart().toLowerCase().startsWith(input.toLowerCase())){
                        choices.add(s.getLocalPart());

                        if(choices.size() == AUTO_COMPLETE_LIST_SIZE){
                            break;
                        }
                    }
                }

                return choices.iterator();
            }
        };
        objectClass.add(AttributeModifier.replace("placeholder", createStringResource("PageAccounts.accounts.objectClass")));
        objectClass.setOutputMarkupId(true);
        objectClass.add(createObjectClassValidator());
        objectClass.add(new SearchFormEnterBehavior(basicSearch.getSearchButton()));
        searchForm.add(objectClass);
    }

    private IValidator<String> createObjectClassValidator(){
        return new IValidator<String>() {

            @Override
            public void validate(IValidatable<String> validatable) {
                String value = validatable.getValue();
                AccountDetailsSearchDto dto = searchModel.getObject();
                List<QName> accountObjectClassList = dto.getObjectClassList();
                List<String> accountObjectClassListString = new ArrayList<>();

                for(QName objectClass: accountObjectClassList){
                    accountObjectClassListString.add(objectClass.getLocalPart());
                }

                if(!accountObjectClassListString.contains(value)){
                    error(createStringResource("PageAccounts.message.validationError", value).getString());
                }
            }
        };
    }

    private IModel<List<String>> createIntentChoices(){
        return new AbstractReadOnlyModel<List<String>>() {

            @Override
            public List<String> getObject() {
                List<String> intentList = new ArrayList<>();
                ResourceItemDto dto = resourceModel.getObject();
                PrismObject<ResourceType> resourcePrism;

                if(dto == null){
                    return intentList;
                }

                String oid = dto.getOid();
                OperationResult result = new OperationResult(OPERATION_GET_INTENTS);

                try {
                    resourcePrism = getModelService().getObject(ResourceType.class, oid, null,
                            createSimpleTask(OPERATION_GET_INTENTS), result);
                    ResourceType resource = resourcePrism.asObjectable();
                    SchemaHandlingType schemaHandling = resource.getSchemaHandling();

                    for(ResourceObjectTypeDefinitionType r: schemaHandling.getObjectType()){
                        intentList.add(r.getIntent());
                    }

                } catch (Exception e){
                    LoggingUtils.logException(LOGGER, "Couldn't load intents from resource.", e);
                    error("Couldn't load intents from resource.");
                    return null;
                }

                return intentList;
            }
        };
    }

    private void initTotals(Form form) {
        WebMarkupContainer totals = new WebMarkupContainer(ID_TOTALS);
        totals.setOutputMarkupId(true);

        totalModel = createTotalModel();
        deletedModel = createTotalsModel(SynchronizationSituationType.DELETED);
        unmatchedModel = createTotalsModel(SynchronizationSituationType.UNMATCHED);
        disputedModel = createTotalsModel(SynchronizationSituationType.DISPUTED);
        linkedModel = createTotalsModel(SynchronizationSituationType.LINKED);
        unlinkedModel = createTotalsModel(SynchronizationSituationType.UNLINKED);
        nothingModel = createTotalsModel(null);

        totals.add(new Label(ID_TOTAL, totalModel));
        totals.add(new Label(ID_DELETED, deletedModel));
        totals.add(new Label(ID_UNMATCHED, unmatchedModel));
        totals.add(new Label(ID_DISPUTED, disputedModel));
        totals.add(new Label(ID_LINKED, linkedModel));
        totals.add(new Label(ID_UNLINKED, unlinkedModel));
        totals.add(new Label(ID_NOTHING, nothingModel));

        form.add(totals);
    }

    private LoadableModel<Integer> createTotalModel() {
        return new LoadableModel<Integer>(false) {

            @Override
            protected Integer load() {
                int total = 0;

                total += deletedModel.getObject();
                total += unmatchedModel.getObject();
                total += disputedModel.getObject();
                total += linkedModel.getObject();
                total += unlinkedModel.getObject();
                total += nothingModel.getObject();

                return total;
            }
        };
    }

    private void refreshSyncTotalsModels() {
        totalModel.reset();
        deletedModel.reset();
        unmatchedModel.reset();
        disputedModel.reset();
        linkedModel.reset();
        unlinkedModel.reset();
        nothingModel.reset();
    }

    private LoadableModel<Integer> createTotalsModel(final SynchronizationSituationType situation) {
        return new LoadableModel<Integer>(false) {

            @Override
            protected Integer load() {
//                ObjectFilter resourceFilter = createResourceQueryFilter();
                ObjectFilter resourceFilter = createObjectQuery().getFilter();

                if (resourceFilter == null) {
                    return 0;
                }

                Collection<SelectorOptions<GetOperationOptions>> options =
                        SelectorOptions.createCollection(GetOperationOptions.createRaw());
                Task task = createSimpleTask(OPERATION_GET_TOTALS);
                OperationResult result = new OperationResult(OPERATION_GET_TOTALS);
                try {
                    EqualFilter situationFilter = EqualFilter.createEqual(ShadowType.F_SYNCHRONIZATION_SITUATION, ShadowType.class,
                            getPrismContext(), null, situation);

                    AndFilter andFilter = AndFilter.createAnd(resourceFilter, situationFilter);
                    ObjectQuery query = ObjectQuery.createObjectQuery(andFilter);

                    return getModelService().countObjects(ShadowType.class, query, options, task, result);
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't count shadows", ex);
                }

                return 0;
            }
        };
    }

    private LoadableModel<List<String>> createFilesModel() {
        return new LoadableModel<List<String>>(false) {

            @Override
            protected List<String> load() {
                String[] filesArray;
                try {
                    MidpointConfiguration config = getMidpointConfiguration();
                    File exportFolder = new File(config.getMidpointHome() + "/export");
                    filesArray = exportFolder.list(new FilenameFilter() {

                        @Override
                        public boolean accept(java.io.File dir, String name) {
                            if (name.endsWith("xml")) {
                                return true;
                            }

                            return false;
                        }
                    });
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't list files", ex);
                    getSession().error("Couldn't list files, reason: " + ex.getMessage());

                    throw new RestartResponseException(PageDashboard.class);
                }

                if (filesArray == null) {
                    return new ArrayList<>();
                }

                List<String> list = Arrays.asList(filesArray);
                Collections.sort(list);

                return list;
            }
        };
    }

    private void initLinks(Form form, Form accForm) {
        AjaxSubmitLink listSyncDetails = new AjaxSubmitLink(ID_LIST_SYNC_DETAILS) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                listSyncDetailsPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(listSyncDetails);

        AjaxSubmitLink export = new AjaxSubmitLink(ID_EXPORT) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                exportPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(export);

        AjaxLink clearExport = new AjaxLink(ID_CLEAR_EXPORT) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearExportPerformed(target);
            }
        };
        accForm.add(clearExport);
    }

    private List<IColumn> createAccountsColumns() {
        List<IColumn> columns = new ArrayList<>();

        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.oid"),
                SelectableBean.F_VALUE + ".oid"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.name"),
                ShadowType.F_NAME.getLocalPart(), SelectableBean.F_VALUE + ".name"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.kind"),
                ShadowType.F_KIND.getLocalPart(), SelectableBean.F_VALUE + ".kind"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.intent"),
                ShadowType.F_INTENT.getLocalPart(), SelectableBean.F_VALUE + ".intent"));
        columns.add(new PropertyColumn<QName, String>(createStringResource("PageAccounts.accounts.objectClass"),
                ShadowType.F_OBJECT_CLASS.getLocalPart(), SelectableBean.F_VALUE + ".objectClass.localPart"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.synchronizationSituation"),
                ShadowType.F_SYNCHRONIZATION_SITUATION.getLocalPart(), SelectableBean.F_VALUE + ".synchronizationSituation"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.synchronizationTimestamp"),
                ShadowType.F_SYNCHRONIZATION_TIMESTAMP.getLocalPart(), SelectableBean.F_VALUE + ".synchronizationTimestamp"));

        return columns;
    }

    private ObjectFilter createResourceQueryFilter() {
        ResourceItemDto dto = resourceModel.getObject();
        if (dto == null) {
            return null;
        }
        OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
        String oid = dto.getOid();
        try {
            RefFilter resourceRef = RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class,
                    getPrismContext(), oid);

            return resourceRef;
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't create query", ex);
            error("Couldn't create query, reason: " + ex.getMessage());
        } finally {
            result.recomputeStatus();
        }

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return null;
    }

    private List<ResourceItemDto> loadResources() {
        List<ResourceItemDto> resources = new ArrayList<ResourceItemDto>();

        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
        try {
            List<PrismObject<ResourceType>> objects = getModelService().searchObjects(ResourceType.class, null, null,
                    createSimpleTask(OPERATION_LOAD_RESOURCES), result);

            if (objects != null) {
                for (PrismObject<ResourceType> object : objects) {
                    resources.add(new ResourceItemDto(object.getOid(), WebMiscUtil.getName(object)));
                }
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load resources", ex);
            result.recordFatalError("Couldn't load resources, reason: " + ex.getMessage(), ex);
        } finally {
            if (result.isUnknown()) {
                result.recomputeStatus();
            }
        }

        Collections.sort(resources);

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResultInSession(result);
            throw new RestartResponseException(PageDashboard.class);
        }

        return resources;
    }

    private Component getAccountsContainer(){
        return get(createComponentPath(ID_FORM_ACCOUNT, ID_ACCOUNTS_CONTAINER));
    }

    private TablePanel getAccountsTable(){
        return (TablePanel) get(createComponentPath(ID_FORM_ACCOUNT, ID_ACCOUNTS_CONTAINER, ID_ACCOUNTS));
    }

    private Form getSearchPanel(){
        return (Form) get(ID_SEARCH_FORM);
    }

    private Component getTotalsPanel(){
        return get(createComponentPath(ID_MAIN_FORM, ID_TOTALS));
    }

    private void loadResourceObjectClass(){
        AccountDetailsSearchDto dto = searchModel.getObject();
        PrismObject<ResourceType> resourcePrism;
        OperationResult result = new OperationResult(OPERATION_GET_OBJECT_CLASS);
        List<QName> accountObjectClassList = new ArrayList<>();

        ResourceItemDto resourceDto = resourceModel.getObject();
        String oid = resourceDto.getOid();

        try {
            resourcePrism = getModelService().getObject(ResourceType.class, oid, null,
                    createSimpleTask(OPERATION_GET_INTENTS), result);

            ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resourcePrism, getPrismContext());
            schema.getObjectClassDefinitions();

            for(Definition def: schema.getDefinitions()){
                accountObjectClassList.add(def.getTypeName());
            }

            dto.setObjectClassList(accountObjectClassList);
        } catch (Exception e){
            LoggingUtils.logException(LOGGER, "Couldn't load object class list from resource.", e);
            error("Couldn't load object class list from resource.");
        }
    }

    private void listSyncDetailsPerformed(AjaxRequestTarget target) {
        refreshSyncTotalsModels();

        if(resourceModel.getObject() == null){
            warn(getString("pageAccounts.message.resourceNotSelected"));
            refreshEverything(target);
            return;
        }

        loadResourceObjectClass();

        TablePanel table = getAccountsTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
        provider.setQuery(ObjectQuery.createObjectQuery(createResourceQueryFilter()));
        table.getDataTable().setCurrentPage(0);

        refreshEverything(target);
    }

    private void refreshEverything(AjaxRequestTarget target){
        target.add(getAccountsContainer(),
                getTotalsPanel(),
                getFeedbackPanel(),
                getSearchPanel(),
                get(createComponentPath(ID_SEARCH_FORM, ID_SEARCH_INTENT)));
    }

    private void exportPerformed(AjaxRequestTarget target) {
        String fileName = "accounts-" + WebMiscUtil.formatDate("yyyy-MM-dd-HH-mm-ss", new Date()) + ".xml";

        OperationResult result = new OperationResult(OPERATION_EXPORT);
        Writer writer = null;
        try {
            Task task = createSimpleTask(OPERATION_EXPORT);

            writer = createWriter(fileName);
            writeHeader(writer);

            final Writer handlerWriter = writer;
            ResultHandler handler = new AbstractSummarizingResultHandler() {

                @Override
                protected boolean handleObject(PrismObject object, OperationResult parentResult) {
                    OperationResult result = parentResult.createMinorSubresult(OPERATION_EXPORT_ACCOUNT);
                    try {
                        String xml = getPrismContext().serializeObjectToString(object, PrismContext.LANG_XML);
                        handlerWriter.write(xml);

                        result.computeStatus();
                    } catch (Exception ex) {
                        LoggingUtils.logException(LOGGER, "Couldn't serialize account", ex);
                        result.recordFatalError("Couldn't serialize account.", ex);

                        return false;
                    }

                    return true;
                }
            };

            try {
                ObjectQuery query = ObjectQuery.createObjectQuery(createResourceQueryFilter());
                getModelService().searchObjectsIterative(ShadowType.class, query, handler, null, task, result);
            } finally {
                writeFooter(writer);
            }

            result.recomputeStatus();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't export accounts", ex);
            error(getString("PageAccounts.exportException", ex.getMessage()));
        } finally {
            IOUtils.closeQuietly(writer);
        }

        filesModel.reset();
        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_FILES_CONTAINER)));
    }

    private Writer createWriter(String fileName) throws IOException {
        //todo improve!!!!!!!

        MidpointConfiguration config = getMidpointConfiguration();
        File exportFolder = new File(config.getMidpointHome() + "/export/");
        File file = new File(exportFolder, fileName);
        try {
            if (!exportFolder.exists() || !exportFolder.isDirectory()) {
                exportFolder.mkdir();
            }

            file.createNewFile();
        } catch (Exception ex) {
            error(getString("PageAccounts.exportFileDoesntExist", file.getAbsolutePath()));
            throw ex;
        }

        return new OutputStreamWriter(new FileOutputStream(file), "utf-8");
    }

    private void writeHeader(Writer writer) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n");
        sb.append("<objects xmlns=\"");
        sb.append(SchemaConstantsGenerated.NS_COMMON);
        sb.append("\">\n");

        writer.write(sb.toString());
    }

    private void writeFooter(Writer writer) throws IOException {
        writer.write("</objects>\n");
    }

    private void clearExportPerformed(AjaxRequestTarget target) {
        try {
            MidpointConfiguration config = getMidpointConfiguration();
            File exportFolder = new File(config.getMidpointHome() + "/export");
            java.io.File[] files = exportFolder.listFiles();
            if (files == null) {
                return;
            }

            for (java.io.File file : files) {
                file.delete();
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't delete export files", ex);
            error("Couldn't delete export files, reason: " + ex.getMessage());
        }

        filesModel.reset();
        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_FILES_CONTAINER)));
    }

    private void downloadPerformed(AjaxRequestTarget target, String fileName,
                                   AjaxDownloadBehaviorFromFile downloadBehavior) {
        MidpointConfiguration config = getMidpointConfiguration();
        downloadFile = new File(config.getMidpointHome() + "/export/" + fileName);

        downloadBehavior.initiate(target);
    }

    private void searchPerformed(AjaxRequestTarget target){
        refreshSyncTotalsModels();

        ObjectQuery query = createObjectQuery();
        TablePanel panel = getAccountsTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider)table.getDataProvider();
        provider.setQuery(query);

        ConfigurationStorage storage = getSessionStorage().getConfiguration();
        storage.setAccountSearchDto(searchModel.getObject());
        storage.setAccountDetailsPaging(null);

        panel.setCurrentPage(null);

        target.add(getTotalsPanel());
        target.add(getFeedbackPanel());
        target.add(getAccountsContainer());
    }

    private ObjectQuery createObjectQuery(){
        AccountDetailsSearchDto dto = searchModel.getObject();
        ObjectQuery query = new ObjectQuery();

        List<ObjectFilter> filters = new ArrayList<>();
        String searchText = dto.getText();
        ShadowKindType kind = dto.getKind();
        String intent = dto.getIntent();
        String objectClass = dto.getObjectClass();

        if(StringUtils.isNotEmpty(searchText)){
            PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
            String normalized = normalizer.normalize(searchText);

            ObjectFilter substring = SubstringFilter.createSubstring(ShadowType.F_NAME, ShadowType.class, getPrismContext(),
                    PolyStringNormMatchingRule.NAME, normalized);
            filters.add(substring);
        }

        if(kind != null){
            ObjectFilter kindFilter = EqualFilter.createEqual(ShadowType.F_KIND, ShadowType.class, getPrismContext(),
                    null, kind);
            filters.add(kindFilter);
        }

        if(StringUtils.isNotEmpty(intent)){
            ObjectFilter intentFilter = EqualFilter.createEqual(ShadowType.F_INTENT, ShadowType.class, getPrismContext(),
                    null, intent);
            filters.add(intentFilter);
        }

        if(StringUtils.isNotEmpty(objectClass)){
            QName objClass = new QName(objectClass);

            for(QName q: dto.getObjectClassList()){
                if(objectClass.equals(q.getLocalPart())){
                    objClass = q;
                }
            }

            ObjectFilter objectClassFilter = EqualFilter.createEqual(ShadowType.F_OBJECT_CLASS, ShadowType.class, getPrismContext(),
                    null, objClass);
            filters.add(objectClassFilter);
        }

        AndFilter searchFilter;
        if(!filters.isEmpty()){
            searchFilter = AndFilter.createAnd(filters);
            query.setFilter(AndFilter.createAnd(searchFilter, createResourceQueryFilter()));
        } else {
            query.setFilter(createResourceQueryFilter());
        }

        return query;
    }

    private void clearSearchPerformed(AjaxRequestTarget target){
        refreshSyncTotalsModels();
        searchModel.setObject(new AccountDetailsSearchDto());

        TablePanel panel = getAccountsTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(ObjectQuery.createObjectQuery(createResourceQueryFilter()));

        ConfigurationStorage storage = getSessionStorage().getConfiguration();
        storage.setAccountSearchDto(searchModel.getObject());
        storage.setAccountDetailsPaging(null);

        panel.setCurrentPage(storage.getAccountDetailsPaging());

        target.add(getTotalsPanel());
        target.add(getSearchPanel());
        target.add(getAccountsContainer());
    }
}
