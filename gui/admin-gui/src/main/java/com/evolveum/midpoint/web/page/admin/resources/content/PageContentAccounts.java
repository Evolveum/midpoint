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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.dialog.UserBrowserDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.option.OptionContent;
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
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
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
public class PageContentAccounts extends PageAdminResources {

    private static final Trace LOGGER = TraceManager.getTrace(PageContentAccounts.class);

    private static final String DOT_CLASS = PageContentAccounts.class.getName() + ".";
    private static final String OPERATION_CHANGE_OWNER = DOT_CLASS + "changeOwner";
    private static final String OPERATION_CREATE_USER_FROM_ACCOUNTS = DOT_CLASS + "createUserFromAccounts";
    private static final String OPERATION_CREATE_USER_FROM_ACCOUNT = DOT_CLASS + "createUserFromAccount";
    private static final String MODAL_ID_OWNER_CHANGE = "ownerChangePopup";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH_TEXT = "searchText";
    private static final String ID_NAME_CHECK = "nameCheck";
    private static final String ID_IDENTIFIERS_CHECK = "identifiersCheck";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_TABLE = "table";

    private IModel<PrismObject<ResourceType>> resourceModel;
    private LoadableModel<AccountContentSearchDto> model;
    private LoadableModel<AccountOwnerChangeDto> ownerChangeModel;

    public PageContentAccounts() {
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
        model = new LoadableModel<AccountContentSearchDto>(false) {

            @Override
            protected AccountContentSearchDto load() {
                return new AccountContentSearchDto();
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
    }

    private void initLayout() {
        Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);

        TextField searchText = new TextField(ID_SEARCH_TEXT);
        searchForm.add(searchText);

        CheckBox nameCheck = new CheckBox(ID_NAME_CHECK);
        searchForm.add(nameCheck);

        CheckBox identifiersCheck = new CheckBox(ID_IDENTIFIERS_CHECK);
        searchForm.add(identifiersCheck);

        AjaxSubmitButton searchButton = new AjaxSubmitButton(ID_SEARCH_BUTTON,
                createStringResource("pageContentAccounts.search")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                searchPerformed(target);
            }
        };
        searchForm.add(searchButton);

        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        List<IColumn> columns = initColumns();
        TablePanel table = new TablePanel(ID_TABLE, new AccountContentDataProvider(this,
                new PropertyModel<String>(resourceModel, "oid"), createObjectClassModel()) {

            @Override
            protected void addInlineMenuToDto(AccountContentDto dto) {
                addRowMenuToTable(dto);
            }
        }, columns);
        table.setOutputMarkupId(true);
        mainForm.add(table);

        initDialog();
    }

    private List<IColumn> initColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();

        IColumn column = new CheckBoxColumn(new Model<String>(), AccountContentDto.F_SELECTED);
        columns.add(column);

        column = new LinkColumn<AccountContentDto>(
                createStringResource("pageContentAccounts.name"), "accountName") {

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
                cellItem.add(new Label(componentId, new Model<String>(StringUtils.join(values, ", "))));
            }
        };
        columns.add(column);

        column = new EnumPropertyColumn(createStringResource("pageContentAccounts.situation"), "situation") {

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
        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.importAccount"),
                new ColumnMenuAction<UserListItemDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        importAccount(target, dto);
                    }
                }));
        dto.getMenuItems().add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.changeOwner"),
                new ColumnMenuAction<UserListItemDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        changeOwnerPerformed(target, dto);
                    }
                }));
        dto.getMenuItems().add(new InlineMenuItem());
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
        parameters.add(PageUser.PARAM_USER_ID, ownerOid);
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
            try {
                OperationResult subResult = result.createMinorSubresult(OPERATION_CREATE_USER_FROM_ACCOUNT);
                getModelService().importFromResource(dto.getAccountOid(),
                        createSimpleTask(OPERATION_CREATE_USER_FROM_ACCOUNT), subResult);
            } catch (Exception ex) {
                result.computeStatus(getString("pageContentAccounts.message.cantImportAccount", dto.getAccountOid()));
                LoggingUtils.logException(LOGGER, getString("pageContentAccounts.message.cantImportAccount", dto.getAccountName()), ex);
            }
        }

        result.computeStatus();
        showResult(result);

        target.add(getFeedbackPanel());
        target.add(getTable());
    }

    private TablePanel getTable() {
        OptionContent content = (OptionContent) get("mainForm:optionContent");
        return (TablePanel) content.getBodyContainer().get("table");
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
        AccountContentSearchDto dto = model.getObject();
        if (StringUtils.isEmpty(dto.getSearchText())) {
            return null;
        }

        try {
            ObjectQuery query = null;

            List<ObjectFilter> conditions = new ArrayList<ObjectFilter>();
            ObjectClassComplexTypeDefinition def = getAccountDefinition();
            if (dto.isIdentifiers()) {

                List<ResourceAttributeDefinition> identifiers = new ArrayList<ResourceAttributeDefinition>();
                if (def.getIdentifiers() != null) {
                    identifiers.addAll(def.getIdentifiers());
                }

                //TODO set matching rule instead fo null
                for (ResourceAttributeDefinition attrDef : identifiers) {
                    conditions.add(EqualsFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES), attrDef, null, dto.getSearchText()));
                }
            }

            if (dto.isName()) {
                List<ResourceAttributeDefinition> secondaryIdentifiers = new ArrayList<ResourceAttributeDefinition>();
                if (def.getSecondaryIdentifiers() != null) {
                    secondaryIdentifiers.addAll(def.getSecondaryIdentifiers());
                }
                for (ResourceAttributeDefinition attrDef : secondaryIdentifiers) {
                    conditions.add(SubstringFilter.createSubstring(new ItemPath(ShadowType.F_ATTRIBUTES),
                            attrDef, dto.getSearchText()));
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
        parameters.add(PageAccount.PARAM_ACCOUNT_ID, accountOid);
        setResponsePage(PageAccount.class, parameters);
    }

    private List<AccountContentDto> isAnythingSelected(AjaxRequestTarget target, AccountContentDto dto) {
        List<AccountContentDto> accounts;
        if (dto != null) {
            accounts = new ArrayList<AccountContentDto>();
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
}
