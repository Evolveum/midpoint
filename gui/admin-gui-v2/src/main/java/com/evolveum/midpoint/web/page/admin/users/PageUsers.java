/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.UsersAction;
import com.evolveum.midpoint.web.page.admin.users.dto.UsersDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
public class PageUsers extends PageAdminUsers {

    private static final Trace LOGGER = TraceManager.getTrace(PageUsers.class);
    private LoadableModel<UsersDto> model;

    public PageUsers() {
        model = new LoadableModel<UsersDto>(false) {

            @Override
            protected UsersDto load() {
                return new UsersDto();
            }
        };
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        OptionPanel option = new OptionPanel("option", createStringResource("pageUsers.optionsTitle"));
        mainForm.add(option);

        OptionItem item = new OptionItem("search", createStringResource("pageUsers.search"));
        option.getBodyContainer().add(item);
        initSearch(item);

        item = new OptionItem("action", createStringResource("pageUsers.action"));
        option.getBodyContainer().add(item);
        initAction(item);

        OptionContent content = new OptionContent("optionContent");
        mainForm.add(content);
        initTable(content);
    }

    private List<IColumn<UserType>> initColumns() {
        List<IColumn<UserType>> columns = new ArrayList<IColumn<UserType>>();

        IColumn column = new CheckBoxHeaderColumn<UserType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<UserType>>(createStringResource("pageUsers.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<UserType>> rowModel) {
                UserType user = rowModel.getObject().getValue();
                userDetailsPerformed(target, user.getOid());
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageUsers.givenName"), "givenName", "value.givenName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageUsers.familyName"), "familyName", "value.familyName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageUsers.fullName"), "fullName", "value.fullName");
        columns.add(column);

        column = new AbstractColumn<SelectableBean<UserType>>(createStringResource("pageUsers.email")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem, String componentId,
                    IModel<SelectableBean<UserType>> rowModel) {

                List<String> emails = rowModel.getObject().getValue().getEmailAddress();
                String email = "";
                if (emails != null && !emails.isEmpty()) {
                    email = emails.get(0);
                }

                cellItem.add(new Label(componentId, new Model<String>(email)));
            }
        };
        columns.add(column);

        return columns;
    }

    private void initTable(OptionContent content) {
        List<IColumn<UserType>> columns = initColumns();
        TablePanel table = new TablePanel<UserType>("table", new ObjectDataProvider(PageUsers.this, UserType.class), columns);
        table.setOutputMarkupId(true);
        content.getBodyContainer().add(table);
    }

    private void initSearch(OptionItem item) {
        TextField<String> search = new TextField<String>("searchText", new PropertyModel<String>(model, "searchText"));
        item.add(search);

        CheckBox nameCheck = new CheckBox("nameCheck", new PropertyModel<Boolean>(model, "name"));
        item.add(nameCheck);
        CheckBox fullNameCheck = new CheckBox("fullNameCheck", new PropertyModel<Boolean>(model, "fullName"));
        item.add(fullNameCheck);
        CheckBox givenNameCheck = new CheckBox("givenNameCheck", new PropertyModel<Boolean>(model, "givenName"));
        item.add(givenNameCheck);
        CheckBox familyNameCheck = new CheckBox("familyNameCheck", new PropertyModel<Boolean>(model, "familyName"));
        item.add(familyNameCheck);

        AjaxSubmitLinkButton clearButton = new AjaxSubmitLinkButton("clearButton",
                createStringResource("pageUsers.button.clearButton")) {

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
                createStringResource("pageUsers.button.searchButton")) {

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

    private IModel<List<UsersAction>> createChoiceModel() {
        return new LoadableModel<List<UsersAction>>(false) {

            @Override
            protected List<UsersAction> load() {
                List<UsersAction> choices = new ArrayList<UsersAction>();
                Collections.addAll(choices, UsersAction.values());

                return choices;
            }
        };
    }

    private void initAction(OptionItem item) {
        final IModel<ObjectTypes> choice = new Model<ObjectTypes>();
        ListChoice listChoice = new ListChoice("choice", choice, createChoiceModel(),
                new EnumChoiceRenderer(PageUsers.this), 5) {

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                return "";
            }
        };
        listChoice.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                actionPerformed(target);
            }
        });
        item.getBodyContainer().add(listChoice);
    }

    private void userDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageUser.PARAM_USER_ID, oid);
        setResponsePage(PageUser.class, parameters);
    }

    private TablePanel getTable() {
        OptionContent content = (OptionContent) get("mainForm:optionContent");
        return (TablePanel) content.getBodyContainer().get("table");
    }

    private void searchPerformed(AjaxRequestTarget target) {
        QueryType query = createQuery();
        target.add(getFeedbackPanel());

        TablePanel panel = getTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(query);

        table.setCurrentPage(0);

        target.add(panel);
    }

    private QueryType createQuery() {
        UsersDto dto = model.getObject();
        QueryType query = null;
        if (StringUtils.isEmpty(dto.getSearchText())) {
            return null;
        }

        try {
            Document document = DOMUtil.getDocument();
            List<Element> elements = new ArrayList<Element>();
            if (dto.isName()) {
                elements.add(QueryUtil.createEqualFilter(document, null, ObjectType.F_NAME, dto.getSearchText()));
            }
            if (dto.isFamilyName()) {
                elements.add(QueryUtil.createEqualFilter(document, null, UserType.F_FAMILY_NAME, dto.getSearchText()));
            }
            if (dto.isFullName()) {
                elements.add(QueryUtil.createEqualFilter(document, null, UserType.F_FULL_NAME, dto.getSearchText()));
            }
            if (dto.isGivenName()) {
                elements.add(QueryUtil.createEqualFilter(document, null, UserType.F_GIVEN_NAME, dto.getSearchText()));
            }

            if (!elements.isEmpty()) {
                query = new QueryType();
                query.setFilter(QueryUtil.createOrFilter(document, elements.toArray(new Element[elements.size()])));
            }
        } catch (Exception ex) {
            error(getString("pageUsers.message.queryError") + " " + ex.getMessage());
            LoggingUtils.logException(LOGGER, "Couldn't create query filter.", ex);
        }

        return query;
    }

    private void clearButtonPerformed(AjaxRequestTarget target) {
        model.reset();
        searchPerformed(target);
    }

    private void actionPerformed(AjaxRequestTarget target) {
        //todo implement
    }
}
