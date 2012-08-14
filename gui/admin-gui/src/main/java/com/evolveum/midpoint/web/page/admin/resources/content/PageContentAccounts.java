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

import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.resources.PageAdminResources;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageContentAccounts extends PageAdminResources {

    private IModel model;

    public PageContentAccounts() {
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
        //todo
    }

    private void initTable(OptionContent content) {
        List<IColumn> columns = initColumns();
        TablePanel table = new TablePanel("table", new ObjectDataProvider(this, UserType.class), columns);
        table.setOutputMarkupId(true);
        content.getBodyContainer().add(table);
    }

    private List<IColumn> initColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();

        IColumn column = new LinkColumn<SelectableBean<UserType>>(createStringResource("pageContentAccounts.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<UserType>> rowModel) {
                //todo action
            }
        };
        columns.add(column);

        //name, identifiers, situation, owner

        //todo list

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
}
