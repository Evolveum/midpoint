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
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.ButtonColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.UserBrowserDialog;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.resources.PageAdminResources;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountContentDataProvider;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountContentDto;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountContentSearchDto;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountOwnerChangeDto;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
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
    private static final String OPERATION_CREATE_USER_FROM_ACCOUNT = DOT_CLASS + "createUserFromAccount";
    private static final String MODAL_ID_OWNER_CHANGE = "ownerChangePopup";

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
        Form mainForm = new Form("mainForm");
        add(mainForm);

        OptionPanel option = new OptionPanel("option", createStringResource("pageContentAccounts.optionsTitle"), false);
        option.setOutputMarkupId(true);
        mainForm.add(option);

        OptionItem item = new OptionItem("search", createStringResource("pageContentAccounts.search"));
        option.getBodyContainer().add(item);
        initSearch(item);

        OptionContent content = new OptionContent("optionContent");
        mainForm.add(content);
        initTable(content);

        initDialog();
        initButtons(mainForm);
    }

    private void initButtons(Form mainForm) {
        AjaxLinkButton removeOwner = new AjaxLinkButton("removeOwner", ButtonType.NEGATIVE,
                createStringResource("pageContentAccounts.button.removeOwner")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeOwnerPerformed(target);
            }
        };
        mainForm.add(removeOwner);
    }

    private void initSearch(OptionItem item) {
        TextField<String> search = new TextField<String>("searchText", new PropertyModel<String>(model, "searchText"));
        item.add(search);

        CheckBox nameCheck = new CheckBox("nameCheck", new PropertyModel<Boolean>(model, "name"));
        item.add(nameCheck);
        CheckBox fullNameCheck = new CheckBox("identifiersCheck", new PropertyModel<Boolean>(model, "identifiers"));
        item.add(fullNameCheck);

        AjaxSubmitLinkButton clearButton = new AjaxSubmitLinkButton("clearButton",
                createStringResource("pageContentAccounts.button.clearButton")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                clearButtonPerformed(target);
            }
        };
        item.add(clearButton);

        AjaxSubmitLinkButton searchButton = new AjaxSubmitLinkButton("searchButton",
                createStringResource("pageContentAccounts.button.searchButton")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                searchPerformed(target);
            }
        };
        item.add(searchButton);
    }

    private void initTable(OptionContent content) {
        List<IColumn> columns = initColumns();
        TablePanel table = new TablePanel("table", new AccountContentDataProvider(this,
                new PropertyModel<String>(resourceModel, "oid"), createObjectClassModel()), columns);
        table.setOutputMarkupId(true);
        content.getBodyContainer().add(table);
    }

    private List<IColumn> initColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();

        IColumn column = new CheckBoxColumn(new Model<String>(), "selected");
        columns.add(column);

        column = new LinkColumn<SelectableBean<AccountContentDto>>(
                createStringResource("pageContentAccounts.name"), "value.accountName") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<AccountContentDto>> rowModel) {
                AccountContentDto dto = rowModel.getObject().getValue();
                accountDetailsPerformed(target, dto.getAccountName(), dto.getAccountOid());
            }
        };
        columns.add(column);

        column = new AbstractColumn<SelectableBean<AccountContentDto>, String>(
                createStringResource("pageContentAccounts.identifiers")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AccountContentDto>>> cellItem,
                                     String componentId, IModel<SelectableBean<AccountContentDto>> rowModel) {

                AccountContentDto dto = rowModel.getObject().getValue();
                List values = new ArrayList();
                for (ResourceAttribute<?> attr : dto.getIdentifiers()) {
                    values.add(attr.getName().getLocalPart() + ": " + attr.getRealValue());
                }
                cellItem.add(new Label(componentId, new Model<String>(StringUtils.join(values, ", "))));
            }
        };
        columns.add(column);

        column = new EnumPropertyColumn(createStringResource("pageContentAccounts.situation"), "value.situation") {

            @Override
            protected String translate(Enum en) {
                return createStringResource(en).getString();
            }
        };
        columns.add(column);

        column = new LinkColumn<SelectableBean<AccountContentDto>>(createStringResource("pageContentAccounts.owner")) {

            @Override
            protected IModel<String> createLinkModel(final IModel<SelectableBean<AccountContentDto>> rowModel) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        AccountContentDto dto = rowModel.getObject().getValue();
                        if (StringUtils.isNotBlank(dto.getOwnerName())) {
                            return dto.getOwnerName();
                        }

                        return dto.getOwnerOid();
                    }
                };
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<AccountContentDto>> rowModel) {
                AccountContentDto dto = rowModel.getObject().getValue();

                ownerDetailsPerformed(target, dto.getOwnerName(), dto.getOwnerOid());
            }
        };
        columns.add(column);

        column = new ButtonColumn<SelectableBean<AccountContentDto>>(new Model<String>(),
                createStringResource("pageContentAccounts.button.changeOwner")) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<AccountContentDto>> rowModel) {
                changeOwnerPerformed(target, rowModel);
            }
        };
        columns.add(column);

        column = new ButtonColumn<SelectableBean<AccountContentDto>>(new Model<String>(),
                createStringResource("pageContentAccounts.button.importAccount")){

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<AccountContentDto>> rowModel){
                importSingleAccount(target, rowModel);
            }
        };

        columns.add(column);

        return columns;
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                String name = WebMiscUtil.getName(resourceModel.getObject());
                return new StringResourceModel("page.title", PageContentAccounts.this, null, null, name).getString();
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

    private void changeOwnerPerformed(AjaxRequestTarget target, IModel<SelectableBean<AccountContentDto>> rowModel) {
        AccountContentDto contentDto = rowModel.getObject().getValue();
        reloadOwnerChangeModel(contentDto.getAccountOid(), contentDto.getOwnerOid());

        showModalWindow(MODAL_ID_OWNER_CHANGE, target);
    }

    private void reloadOwnerChangeModel(String accountOid, String ownerOid) {
        ownerChangeModel.reset();

        AccountOwnerChangeDto changeDto = ownerChangeModel.getObject();

        changeDto.setAccountOid(accountOid);
        changeDto.setAccountType(ShadowType.COMPLEX_TYPE);

        changeDto.setOldOwnerOid(ownerOid);
    }

    private void importSingleAccount(AjaxRequestTarget target, IModel<SelectableBean<AccountContentDto>> rowModel){
        AccountContentDto contentDto = rowModel.getObject().getValue();

        Task task = createSimpleTask(OPERATION_CREATE_USER_FROM_ACCOUNT);
        OperationResult result = new OperationResult(OPERATION_CREATE_USER_FROM_ACCOUNT);

        try {
            getModelService().importFromResource(contentDto.getAccountOid(), task, result);
        } catch (Exception ex) {
            result.computeStatus(getString("pageContentAccounts.message.cantImportAccount", contentDto.getAccountOid()));
            LoggingUtils.logException(LOGGER, getString("pageContentAccounts.message.cantImportAccount", contentDto.getAccountName()), ex);
        }

        result.computeStatus();
        showResult(result);
        target.add(getFeedbackPanel());
        target.add(getTable());
    }

    private void clearButtonPerformed(AjaxRequestTarget target) {
        model.reset();
        target.appendJavaScript("init()");
        target.add(get("mainForm:option"));
        searchPerformed(target);
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
            ObjectQuery query = null;//new ObjectQuery();
//            Document document = DOMUtil.getDocument();

            List<ObjectFilter> conditions = new ArrayList<ObjectFilter>();
            ObjectClassComplexTypeDefinition def = getAccountDefinition();
            PrismObject<ResourceType> resource = resourceModel.getObject();
//            query = ObjectQueryUtil.createResourceAndAccountQuery(resource.getOid(), def.getTypeName(), getPrismContext());
            if (dto.isIdentifiers()) {

                List<ResourceAttributeDefinition> identifiers = new ArrayList<ResourceAttributeDefinition>();
                if (def.getIdentifiers() != null) {
                    identifiers.addAll(def.getIdentifiers());
                }
//                if (def.getSecondaryIdentifiers() != null) {
//                    identifiers.addAll(def.getIdentifiers());
//                }
//                XPathHolder attributes = new XPathHolder(Arrays.asList(new XPathSegment(SchemaConstants.I_ATTRIBUTES)));
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

    private void removeOwnerPerformed(AjaxRequestTarget target) {
        List<SelectableBean<AccountContentDto>> selected = WebMiscUtil.getSelectedData(getTable());
        target.add(getFeedbackPanel());
        if (selected.isEmpty()) {
            warn(getString("pageContentAccounts.message.noAccountSelected"));
            return;
        }

        for (SelectableBean<AccountContentDto> bean : selected) {
            AccountContentDto dto = bean.getValue();
            reloadOwnerChangeModel(dto.getAccountOid(), dto.getOwnerOid());
            ownerChangePerformed(target, null);
        }

        target.add(getTable());
    }

    private void ownerChangePerformed(AjaxRequestTarget target, UserType user) {
        AccountOwnerChangeDto dto = ownerChangeModel.getObject();
        OperationResult result = new OperationResult(OPERATION_CHANGE_OWNER);
        try {
            Task task = createSimpleTask(OPERATION_CHANGE_OWNER);
//            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();

            if (StringUtils.isNotEmpty(dto.getOldOwnerOid())) {
                ObjectDelta delta = new ObjectDelta(UserType.class, ChangeType.MODIFY, getPrismContext());
//                deltas.add(delta);
                delta.setOid(dto.getOldOwnerOid());
                PrismReferenceValue refValue = new PrismReferenceValue(dto.getAccountOid());
                refValue.setTargetType(dto.getAccountType());
                delta.addModification(ReferenceDelta.createModificationDelete(UserType.class,
                        UserType.F_LINK_REF, getPrismContext(), refValue));
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            }

            if (user != null) {
                ObjectDelta delta = new ObjectDelta(UserType.class, ChangeType.MODIFY, getPrismContext());
//                deltas.add(delta);
                delta.setOid(user.getOid());
                PrismReferenceValue refValue = new PrismReferenceValue(dto.getAccountOid());
                refValue.setTargetType(dto.getAccountType());
                delta.addModification(ReferenceDelta.createModificationAdd(UserType.class,
                        UserType.F_LINK_REF, getPrismContext(), refValue));

                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);

            }

//            if (!deltas.isEmpty()) {
//                getModelService().executeChanges(deltas, null, task, result);
//            }
            result.recomputeStatus();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't submit user.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't submit user", ex);
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }
}
