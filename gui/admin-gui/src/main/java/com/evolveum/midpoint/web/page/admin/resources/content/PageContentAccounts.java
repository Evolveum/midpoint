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

package com.evolveum.midpoint.web.page.admin.resources.content;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.dialog.UserBrowserDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.resources.PageAdminResources;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountContentDataProvider;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountContentDto;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountContentSearchDto;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountOwnerChangeDto;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.dto.UserListItemDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.session.ResourcesStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/resources/content/accounts", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminResources.AUTH_RESOURCE_ALL,
                label = PageAdminResources.AUTH_RESOURCE_ALL_LABEL,
                description = PageAdminResources.AUTH_RESOURCE_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#resourcesContentAccounts",
                label = "PageContentAccounts.auth.resourcesContentAccounts.label",
                description = "PageContentAccounts.auth.resourcesContentAccounts.description")})
public class PageContentAccounts extends PageAdminResources {

    private static final Trace LOGGER = TraceManager.getTrace(PageContentAccounts.class);

    private static final String DOT_CLASS = PageContentAccounts.class.getName() + ".";
    private static final String OPERATION_CHANGE_OWNER = DOT_CLASS + "changeOwner";
    private static final String OPERATION_CREATE_USER_FROM_ACCOUNTS = DOT_CLASS + "createUserFromAccounts";
    private static final String OPERATION_CREATE_USER_FROM_ACCOUNT = DOT_CLASS + "createUserFromAccount";
    private static final String OPERATION_DELETE_ACCOUNT_FROM_RESOURCE = DOT_CLASS + "deleteAccountFromResource";
    private static final String OPERATION_ADJUST_ACCOUNT_STATUS = "changeAccountActivationStatus";
    private static final String MODAL_ID_OWNER_CHANGE = "ownerChangePopup";
    private static final String MODAL_ID_CONFIRM_DELETE = "confirmDeletePopup";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_BASIC_SEARCH = "basicSearch";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_NAME_CHECK = "nameCheck";
    private static final String ID_IDENTIFIERS_CHECK = "identifiersCheck";
    private static final String ID_TABLE = "table";

    private IModel<PrismObject<ResourceType>> resourceModel;
    private IModel<AccountContentSearchDto> searchModel;
    private LoadableModel<AccountOwnerChangeDto> ownerChangeModel;
    private AccountContentDto singleDelete;

    public PageContentAccounts() {
        searchModel = new LoadableModel<AccountContentSearchDto>() {

            @Override
            protected AccountContentSearchDto load() {
                ResourcesStorage storage = getSessionStorage().getResources();
                AccountContentSearchDto dto = storage.getAccountContentSearch();

                if(dto == null){
                    dto = new AccountContentSearchDto();
                }

                return dto;
            }
        };

        resourceModel = new LoadableModel<PrismObject<ResourceType>>(false) {

            @Override
            protected PrismObject<ResourceType> load() {
                if (!isResourceOidAvailable()) {
                    getSession().error(getString("pageContentAccounts.message.resourceOidNotDefined"));
                    throw new RestartResponseException(PageResources.class);
                }
                return loadResource(null);
            }
        };
        ownerChangeModel = new LoadableModel<AccountOwnerChangeDto>(false) {

            @Override
            protected AccountOwnerChangeDto load() {
                return new AccountOwnerChangeDto();
            }
        };

        initLayout();
    }

    private void initDialog() {
        UserBrowserDialog dialog = new UserBrowserDialog(MODAL_ID_OWNER_CHANGE) {

            @Override
            public void userDetailsPerformed(AjaxRequestTarget target, UserType user) {
                super.userDetailsPerformed(target, user);

                ownerChangePerformed(target, user);
                target.add(getTable());
            }
        };
        add(dialog);

        add(new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE, createStringResource("pageContentAccounts.dialog.title.confirmDelete"),
                createDeleteConfirmString()){

            @Override
            public void yesPerformed(AjaxRequestTarget target){
                close(target);
                deleteConfirmedPerformed(target);
            }
        });
    }

    private IModel<String> createDeleteConfirmString(){
        return new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                if(singleDelete == null){
                    return createStringResource("pageContentAccounts.message.deleteConfirmation", getSelectedAccounts(null).size()).getString();
                } else{
                    return createStringResource("pageContentAccounts.message.deleteConfirmationSingle", singleDelete.getAccountName()).getString();
                }
            }
        };
    }

    private void initLayout() {
        Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);

        CheckBox nameCheck = new CheckBox(ID_NAME_CHECK, new PropertyModel(searchModel, AccountContentSearchDto.F_NAME));
        searchForm.add(nameCheck);

        CheckBox identifiersCheck = new CheckBox(ID_IDENTIFIERS_CHECK,
                new PropertyModel(searchModel, AccountContentSearchDto.F_IDENTIFIERS));
        searchForm.add(identifiersCheck);

        BasicSearchPanel<AccountContentSearchDto> basicSearch = new BasicSearchPanel<AccountContentSearchDto>(ID_BASIC_SEARCH) {

            @Override
            protected IModel<String> createSearchTextModel() {
                return new PropertyModel<>(searchModel, AccountContentSearchDto.F_SEARCH_TEXT);
            }

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                PageContentAccounts.this.searchPerformed(target);
            }

            @Override
            protected void clearSearchPerformed(AjaxRequestTarget target) {
                PageContentAccounts.this.clearSearchPerformed(target);
            }
        };
        searchForm.add(basicSearch);

        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        AccountContentDataProvider provider = new AccountContentDataProvider(this,
                new PropertyModel<String>(resourceModel, "oid"), createObjectClassModel(), createUseObjectCountingModel()) {

            @Override
            protected void addInlineMenuToDto(AccountContentDto dto) {
                addRowMenuToTable(dto);
            }
        };
        provider.setQuery(createQuery());

        List<IColumn> columns = initColumns();
        TablePanel table = new TablePanel(ID_TABLE, provider, columns,
                UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL, getItemsPerPage(UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL));
        table.setOutputMarkupId(true);
        mainForm.add(table);

        initDialog();
    }

    private List<IColumn> initColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();

        IColumn column = new CheckBoxColumn(new Model<String>(), AccountContentDto.F_SELECTED);
        columns.add(column);

        column = new LinkColumn<AccountContentDto>(
                createStringResource("pageContentAccounts.name"), AccountContentDto.F_ACCOUNT_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<AccountContentDto> rowModel) {
                AccountContentDto dto = rowModel.getObject();
                accountDetailsPerformed(target, dto.getAccountName(), dto.getAccountOid());
            }
        };
        columns.add(column);

        column = new AbstractColumn<AccountContentDto, String>(
                createStringResource("pageContentAccounts.identifiers")) {

            @Override
            public void populateItem(Item<ICellPopulator<AccountContentDto>> cellItem,
                                     String componentId, IModel<AccountContentDto> rowModel) {

                AccountContentDto dto = rowModel.getObject();
                List values = new ArrayList();
                for (ResourceAttribute<?> attr : dto.getIdentifiers()) {
                    values.add(attr.getElementName().getLocalPart() + ": " + attr.getRealValue());
                }
                cellItem.add(new Label(componentId, new Model<>(StringUtils.join(values, ", "))));
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageContentAccounts.kind"), AccountContentDto.F_KIND);
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageContentAccounts.intent"), AccountContentDto.F_INTENT);
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageContentAccounts.objectClass"),
                AccountContentDto.F_OBJECT_CLASS);
        columns.add(column);

        column = new EnumPropertyColumn(createStringResource("pageContentAccounts.situation"), AccountContentDto.F_SITUATION) {

            @Override
            protected String translate(Enum en) {
                return createStringResource(en).getString();
            }
        };
        columns.add(column);

        column = new LinkColumn<AccountContentDto>(createStringResource("pageContentAccounts.owner")) {

            @Override
            protected IModel<String> createLinkModel(final IModel<AccountContentDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        AccountContentDto dto = rowModel.getObject();
                        if (StringUtils.isNotBlank(dto.getOwnerName())) {
                            return dto.getOwnerName();
                        }

                        return dto.getOwnerOid();
                    }
                };
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<AccountContentDto> rowModel) {
                AccountContentDto dto = rowModel.getObject();

                ownerDetailsPerformed(target, dto.getOwnerName(), dto.getOwnerOid());
            }
        };
        columns.add(column);

        column = new InlineMenuHeaderColumn(createHeaderMenuItems());
        columns.add(column);

        return columns;
    }

    private List<InlineMenuItem> createHeaderMenuItems() {
        List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.enableAccount"), true,
                new HeaderMenuAction(this){

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form){
                        updateAccountStatusPerformed(target, null, true);
                    }
                }));

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.disableAccount"), true,
                new HeaderMenuAction(this){

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form){
                        updateAccountStatusPerformed(target, null, false);
                    }
                }));

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.deleteAccount"), true,
                new HeaderMenuAction(this){

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form){
                        deleteAccountPerformed(target, null);
                    }
                }));

        items.add(new InlineMenuItem());

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.importAccount"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        importAccount(target, null);
                    }
                }));

        items.add(new InlineMenuItem());

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.removeOwner"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        removeOwnerPerformed(target, null);
                    }
                }));

        return items;
    }

    private void addRowMenuToTable(final AccountContentDto dto) {
        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.enableAccount"), true,
                new HeaderMenuAction(this){

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form){
                        updateAccountStatusPerformed(target, dto, true);
                    }
                }));

        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.disableAccount"), true,
                new HeaderMenuAction(this){

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form){
                        updateAccountStatusPerformed(target, dto, false);
                    }
                }));

        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.deleteAccount"), true,
                new HeaderMenuAction(this){

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form){
                        deleteAccountPerformed(target, dto);
                    }
                }));

        dto.getMenuItems().add(new InlineMenuItem());

        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.importAccount"),
                new ColumnMenuAction<UserListItemDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        importAccount(target, dto);
                    }
                }));

        dto.getMenuItems().add(new InlineMenuItem());

        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.changeOwner"),
                new ColumnMenuAction<UserListItemDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        changeOwnerPerformed(target, dto);
                    }
                }));

        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.removeOwner"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        removeOwnerPerformed(target, dto);
                    }
                }));
    }

    @Override
    protected IModel<String> createPageSubTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                String name = WebMiscUtil.getName(resourceModel.getObject());
                return new StringResourceModel("page.subTitle", PageContentAccounts.this, null, null, name).getString();
            }
        };
    }

    private void ownerDetailsPerformed(AjaxRequestTarget target, String ownerName, String ownerOid) {
        if (StringUtils.isEmpty(ownerOid)) {
            error(getString("pageContentAccounts.message.cantShowUserDetails", ownerName, ownerOid));
            target.add(getFeedbackPanel());
            return;
        }

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, ownerOid);
        setResponsePage(PageUser.class, parameters);
    }

    private void changeOwnerPerformed(AjaxRequestTarget target, AccountContentDto dto) {
        reloadOwnerChangeModel(dto.getAccountOid(), dto.getOwnerOid());

        showModalWindow(MODAL_ID_OWNER_CHANGE, target);
    }

    private void reloadOwnerChangeModel(String accountOid, String ownerOid) {
        ownerChangeModel.reset();

        AccountOwnerChangeDto changeDto = ownerChangeModel.getObject();

        changeDto.setAccountOid(accountOid);
        changeDto.setAccountType(ShadowType.COMPLEX_TYPE);

        changeDto.setOldOwnerOid(ownerOid);
    }

    private void importAccount(AjaxRequestTarget target, AccountContentDto row) {
        List<AccountContentDto> accounts = isAnythingSelected(target, row);
        if (accounts.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_CREATE_USER_FROM_ACCOUNTS);
        for (AccountContentDto dto : accounts) {
            OperationResult subResult = result.createMinorSubresult(OPERATION_CREATE_USER_FROM_ACCOUNT);
            try {
                getModelService().importFromResource(dto.getAccountOid(),
                        createSimpleTask(OPERATION_CREATE_USER_FROM_ACCOUNT), subResult);
            } catch (Exception ex) {
                subResult.computeStatus(getString("pageContentAccounts.message.cantImportAccount", dto.getAccountOid()));
                LoggingUtils.logException(LOGGER, "Can't import account {},oid={}", ex, dto.getAccountName(), dto.getAccountOid());
            } finally {
                subResult.computeStatusIfUnknown();
            }
        }

        result.computeStatus();
        showResult(result);

        target.add(getFeedbackPanel());
        target.add(getTable());
    }

    private TablePanel getTable() {
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private void searchPerformed(AjaxRequestTarget target) {
        ObjectQuery query = createQuery();

        TablePanel panel = getTable();
        DataTable table = panel.getDataTable();
        AccountContentDataProvider provider = (AccountContentDataProvider) table.getDataProvider();
        provider.setQuery(query);
        table.setCurrentPage(0);

        target.add(panel);
        target.add(getFeedbackPanel());
    }

    private ObjectQuery createQuery() {
        AccountContentSearchDto dto = searchModel.getObject();
        if (StringUtils.isEmpty(dto.getSearchText())) {
            return null;
        }

        try {
            ObjectQuery query = null;

            List<ObjectFilter> conditions = new ArrayList<>();
            ObjectClassComplexTypeDefinition def = getAccountDefinition();
            if (dto.isIdentifiers()) {

                List<ResourceAttributeDefinition> identifiers = new ArrayList<>();
                if (def.getIdentifiers() != null) {
                    identifiers.addAll(def.getIdentifiers());
                }

                //TODO set matching rule instead fo null
                for (ResourceAttributeDefinition attrDef : identifiers) {
                    conditions.add(EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, attrDef.getName()),
                            attrDef, dto.getSearchText()));
                }
            }

            if (dto.isName()) {
                List<ResourceAttributeDefinition> secondaryIdentifiers = new ArrayList<>();
                if (def.getNamingAttribute() != null) {
                    secondaryIdentifiers.add(def.getNamingAttribute());
                } else if (def.getSecondaryIdentifiers() != null){
                	secondaryIdentifiers.addAll(def.getSecondaryIdentifiers());
                }
                for (ResourceAttributeDefinition attrDef : secondaryIdentifiers) {
                    conditions.add(SubstringFilter.createSubstring(
                            new ItemPath(ShadowType.F_ATTRIBUTES, attrDef.getName()), attrDef, dto.getSearchText()));
                }
            }

            if (!conditions.isEmpty()) {
                if (conditions.size() > 1) {
                    query = ObjectQuery.createObjectQuery(OrFilter.createOr(conditions));
                } else {
                    query = ObjectQuery.createObjectQuery(conditions.get(0));
                }
            }

            return query;
        } catch (Exception ex) {
            error(getString("pageUsers.message.queryError") + " " + ex.getMessage());
            LoggingUtils.logException(LOGGER, "Couldn't create query filter.", ex);
        }

        return null;
    }

    private IModel<QName> createObjectClassModel() {
        return new LoadableModel<QName>(false) {

            @Override
            protected QName load() {
                try {
                    return getObjectClassDefinition();
                } catch (Exception ex) {
                    throw new SystemException(ex.getMessage(), ex);
                }
            }
        };
    }

    private IModel<Boolean> createUseObjectCountingModel() {
        return new LoadableModel<Boolean>(false) {

            @Override
            protected Boolean load() {
                try {
                    return isUseObjectCounting();
                } catch (Exception ex) {
                    throw new SystemException(ex.getMessage(), ex);
                }
            }
        };
    }


    private QName getObjectClassDefinition() throws SchemaException {
        ObjectClassComplexTypeDefinition def = getAccountDefinition();
        return def != null ? def.getTypeName() : null;
    }

    private ObjectClassComplexTypeDefinition getAccountDefinition() throws SchemaException {
        MidPointApplication application = (MidPointApplication) getApplication();
        PrismObject<ResourceType> resource = resourceModel.getObject();
        ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, application.getPrismContext());
        Collection<ObjectClassComplexTypeDefinition> list = resourceSchema.getObjectClassDefinitions();
        if (list != null) {
            for (ObjectClassComplexTypeDefinition def : list) {
                if (def.isDefaultInAKind()) {
                    return def;
                }
            }
        }

        return null;
    }

    private boolean isUseObjectCounting() throws SchemaException {
        MidPointApplication application = (MidPointApplication) getApplication();
        PrismObject<ResourceType> resource = resourceModel.getObject();
        RefinedResourceSchema resourceSchema = RefinedResourceSchema.getRefinedSchema(resource, application.getPrismContext());

        // hacking this for now ... in future, we get the type definition (and maybe kind+intent) directly from GUI model
        // TODO here we should deal with the situation that one object class is mentioned in different
        // kind/intent sections -- we would want to avoid mentioning paged search information in all
        // these sections
        ObjectClassComplexTypeDefinition typeDefinition = getAccountDefinition();
        if (typeDefinition == null) {
            // should not occur
            LOGGER.warn("ObjectClass definition couldn't be found");
            return false;
        }

        RefinedObjectClassDefinition refinedObjectClassDefinition = resourceSchema.getRefinedDefinition(typeDefinition.getTypeName());
        if (refinedObjectClassDefinition == null) {
            return false;
        }
        return refinedObjectClassDefinition.isObjectCountingEnabled();
    }

    private void showModalWindow(String id, AjaxRequestTarget target) {
        ModalWindow window = (ModalWindow) get(id);
        window.show(target);
    }

    private void accountDetailsPerformed(AjaxRequestTarget target, String accountName, String accountOid) {
        if (StringUtils.isEmpty(accountOid)) {
            error(getString("pageContentAccounts.message.cantShowAccountDetails", accountName, accountOid));
            target.add(getFeedbackPanel());
            return;
        }

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, accountOid);
        setResponsePage(PageAccount.class, parameters);
    }

    private List<AccountContentDto> getSelectedAccounts(AccountContentDto dto){
        List<AccountContentDto> accounts;
        if (dto != null) {
            accounts = new ArrayList<>();
            accounts.add(dto);
        } else {
            accounts = WebMiscUtil.getSelectedData(getTable());
        }

        return accounts;
    }

    private List<AccountContentDto> isAnythingSelected(AjaxRequestTarget target, AccountContentDto dto) {
        List<AccountContentDto> accounts;
        if (dto != null) {
            accounts = new ArrayList<>();
            accounts.add(dto);
        } else {
            accounts = WebMiscUtil.getSelectedData(getTable());
            if (accounts.isEmpty()) {
                warn(getString("pageContentAccounts.message.noAccountSelected"));
                target.add(getFeedbackPanel());
            }
        }

        return accounts;
    }

    private void removeOwnerPerformed(AjaxRequestTarget target, AccountContentDto row) {
        List<AccountContentDto> accounts = isAnythingSelected(target, row);
        if (accounts.isEmpty()) {
            return;
        }

        for (AccountContentDto dto : accounts) {
            reloadOwnerChangeModel(dto.getAccountOid(), dto.getOwnerOid());
            ownerChangePerformed(target, null);
        }

        target.add(getTable());
        target.add(getFeedbackPanel());
    }

    private void ownerChangePerformed(AjaxRequestTarget target, UserType user) {
        AccountOwnerChangeDto dto = ownerChangeModel.getObject();
        OperationResult result = new OperationResult(OPERATION_CHANGE_OWNER);
        try {
            Task task = createSimpleTask(OPERATION_CHANGE_OWNER);
            if (StringUtils.isNotEmpty(dto.getOldOwnerOid())) {
                ObjectDelta delta = new ObjectDelta(UserType.class, ChangeType.MODIFY, getPrismContext());
                delta.setOid(dto.getOldOwnerOid());
                PrismReferenceValue refValue = new PrismReferenceValue(dto.getAccountOid());
                refValue.setTargetType(dto.getAccountType());
                delta.addModification(ReferenceDelta.createModificationDelete(UserType.class,
                        UserType.F_LINK_REF, getPrismContext(), refValue));
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            }

            if (user != null) {
                ObjectDelta delta = new ObjectDelta(UserType.class, ChangeType.MODIFY, getPrismContext());
                delta.setOid(user.getOid());
                PrismReferenceValue refValue = new PrismReferenceValue(dto.getAccountOid());
                refValue.setTargetType(dto.getAccountType());
                delta.addModification(ReferenceDelta.createModificationAdd(UserType.class,
                        UserType.F_LINK_REF, getPrismContext(), refValue));

                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);

            }
            result.recomputeStatus();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't submit user.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't submit user", ex);
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }

    private void clearSearchPerformed(AjaxRequestTarget target){
        searchModel.setObject(new AccountContentSearchDto());

        TablePanel panel = getTable();
        DataTable table = panel.getDataTable();
        AccountContentDataProvider provider = (AccountContentDataProvider)table.getDataProvider();
        provider.setQuery(null);

        ResourcesStorage storage = getSessionStorage().getResources();
        storage.setAccountContentSearch(searchModel.getObject());
        storage.setAccountContentPaging(null);
        panel.setCurrentPage(null);

        target.add(get(ID_SEARCH_FORM));
        target.add(panel);
    }

    private void deleteAccountPerformed(AjaxRequestTarget target, AccountContentDto dto){
        singleDelete = dto;
        List<AccountContentDto> accounts = isAnythingSelected(target, dto);

        if (accounts.isEmpty()) {
            return;
        }

        showModalWindow(MODAL_ID_CONFIRM_DELETE, target);
    }

    private void deleteConfirmedPerformed(AjaxRequestTarget target){
        List<AccountContentDto> selected = new ArrayList<AccountContentDto>();

        if(singleDelete != null){
            selected.add(singleDelete);
        } else {
            selected = isAnythingSelected(target, null);
        }

        OperationResult result = new OperationResult(OPERATION_DELETE_ACCOUNT_FROM_RESOURCE);

        for(AccountContentDto acc: selected){
            String accOid = acc.getAccountOid();

            try{
                Task task = createSimpleTask(OPERATION_DELETE_ACCOUNT_FROM_RESOURCE);

                ObjectDelta delta = ObjectDelta.createDeleteDelta(ShadowType.class, accOid, getPrismContext());
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);

            } catch (Exception e){
                result.recordPartialError("Couldn't delete account from resource.", e);
                LoggingUtils.logException(LOGGER, "Couldn't delete account from resource", e);
            }
        }

        if(result.isUnknown()){
            result.recomputeStatus("Error occurred during resource account deletion.");
        }

        if(result.isSuccess()){
            result.recordStatus(OperationResultStatus.SUCCESS, "Selected accounts have been successfully deleted.");
        }

        AccountContentDataProvider provider = (AccountContentDataProvider)getTable().getDataTable().getDataProvider();
        provider.clearCache();

        TablePanel table = getTable();
        target.add(table);
        showResult(result);
        target.add(getFeedbackPanel());
    }

    private void updateAccountStatusPerformed(AjaxRequestTarget target, AccountContentDto dto, boolean enabled){
        List<AccountContentDto> accounts = isAnythingSelected(target, dto);
        OperationResult result = new OperationResult(OPERATION_ADJUST_ACCOUNT_STATUS);

        if (accounts.isEmpty()) {
            return;
        }

        ActivationStatusType status = enabled ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED;

        for(AccountContentDto acc: accounts){

            ObjectDelta delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, acc.getAccountOid(),
                    new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), getPrismContext(), status);

            try{
                Task task = createSimpleTask(OPERATION_ADJUST_ACCOUNT_STATUS);
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            } catch(Exception e){
                LoggingUtils.logException(LOGGER, "Couldn't enable/disable account(s) on resource", e);
                result.recordPartialError("Couldn't enable/disable account(s) on resource", e);
            }
        }
        result.recomputeStatus();
        showResult(result);
        target.add(getFeedbackPanel());
    }




}
