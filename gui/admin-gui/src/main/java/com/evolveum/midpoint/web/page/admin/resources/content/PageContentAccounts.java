/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.resources.content;

import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.resources.PageAdminResources;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountContentDataProvider;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountContentDto;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountContentSearchDto;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageContentAccounts extends PageAdminResources {

    public static final String PARAM_RESOURCE_ID = "resourceOid";
    private IModel<AccountContentSearchDto> model;

    public PageContentAccounts() {
        model = new LoadableModel<AccountContentSearchDto>(false) {

            @Override
            protected AccountContentSearchDto load() {
                return new AccountContentSearchDto();
            }
        };

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        OptionPanel option = new OptionPanel("option", createStringResource("pageContentAccounts.optionsTitle"),
                getPage(), false);
        option.setOutputMarkupId(true);
        mainForm.add(option);

        OptionItem item = new OptionItem("search", createStringResource("pageContentAccounts.search"));
        option.getBodyContainer().add(item);
        initSearch(item);

        OptionContent content = new OptionContent("optionContent");
        mainForm.add(content);
        initTable(content);
    }

    private void initSearch(OptionItem item) {
        TextField<String> search = new TextField<String>("searchText", new PropertyModel<String>(model, "searchText"));
        item.add(search);

        CheckBox nameCheck = new CheckBox("accountNameCheck", new PropertyModel<Boolean>(model, "accountName"));
        item.add(nameCheck);
        CheckBox fullNameCheck = new CheckBox("ownerNameCheck", new PropertyModel<Boolean>(model, "ownerName"));
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
        TablePanel table = new TablePanel("table", new AccountContentDataProvider(this), columns);
        table.setOutputMarkupId(true);
        content.getBodyContainer().add(table);
    }

    private List<IColumn> initColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();

        IColumn column = new LinkColumn<SelectableBean<AccountContentDto>>(createStringResource("pageContentAccounts.name"), "accountName") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<AccountContentDto>> rowModel) {
                accountDetailsPerformed();
            }
        };
        columns.add(column);

        column = new AbstractColumn<SelectableBean<AccountContentDto>>(createStringResource("pageContentAccounts.identifiers")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AccountContentDto>>> cellItem, String componentId,
                                     IModel<SelectableBean<AccountContentDto>> rowModel) {

                AccountContentDto dto = rowModel.getObject().getValue();
                cellItem.add(new Label(componentId, new Model<String>(StringUtils.join(dto.getIdentifiers(), ", "))));
            }
        };
        columns.add(column);

        column = new EnumPropertyColumn(createStringResource("pageContentAccounts.situation"), "situation");
        columns.add(column);

        column = new AbstractColumn<SelectableBean<AccountContentDto>>(createStringResource("pageContentAccounts.owner")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AccountContentDto>>> cellItem, String componentId,
                                     IModel<SelectableBean<AccountContentDto>> rowModel) {

                AccountContentDto dto = rowModel.getObject().getValue();
                StringBuilder owner = new StringBuilder();
                if (StringUtils.isNotEmpty(dto.getOwnerName())) {
                    owner.append(dto.getOwnerName());
                    owner.append(" (");
                    owner.append(dto.getOwnerOid());
                    owner.append(")");
                } else {
                    owner.append(dto.getOwnerOid());
                }
                cellItem.add(new Label(componentId, new Model<String>(owner.toString())));
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
                String name = "some resource name...";
                return new StringResourceModel("page.title", PageContentAccounts.this, null, null, name).getString();
            }
        };
    }

    private void clearButtonPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void searchPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void accountDetailsPerformed() {
        //todo implement
    }
}
