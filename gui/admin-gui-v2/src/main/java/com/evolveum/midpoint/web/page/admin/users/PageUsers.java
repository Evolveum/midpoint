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

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.dto.UsersDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author lazyman
 */
public class PageUsers extends PageAdminUsers {

    private IModel<UsersDto> model;

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

        OptionPanel option = new OptionPanel("option", createStringResource("pageUserList.optionsTitle"));
        mainForm.add(option);

        OptionItem item = new OptionItem("search", createStringResource("pageUserList.search"));
        option.getBodyContainer().add(item);
        initSearch(item);

        item = new OptionItem("action", createStringResource("pageUserList.action"));
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
        TablePanel table = new TablePanel<UserType>("table", new ObjectDataProvider(UserType.class), columns);
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

        AjaxLinkButton searchButton = new AjaxLinkButton("searchButton",
                createStringResource("pageUserList.searchButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                searchPerformed(target);
            }
        };
        item.add(searchButton);
    }

    private IModel<List<ObjectTypes>> createChoiceModel(final IChoiceRenderer<ObjectTypes> renderer) {
        return new LoadableModel<List<ObjectTypes>>(false) {

            @Override
            protected List<ObjectTypes> load() {
                //todo fix....
                List<ObjectTypes> choices = new ArrayList<ObjectTypes>();
                Collections.addAll(choices, ObjectTypes.values());
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

    private void initAction(OptionItem item) {
        //todo fix
        IChoiceRenderer<ObjectTypes> renderer = new IChoiceRenderer<ObjectTypes>() {

            @Override
            public Object getDisplayValue(ObjectTypes object) {
                return new StringResourceModel(object.getLocalizationKey(),
                        (PageBase) PageUsers.this, null).getString();
            }

            @Override
            public String getIdValue(ObjectTypes object, int index) {
                return object.getClassDefinition().getSimpleName();
            }
        };

        final IModel<ObjectTypes> choice = new Model<ObjectTypes>();
        ListChoice listChoice = new ListChoice("choice", choice, createChoiceModel(renderer), renderer, 5) {

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

    private void searchPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void actionPerformed(AjaxRequestTarget target) {
        //todo implement
    }
}
