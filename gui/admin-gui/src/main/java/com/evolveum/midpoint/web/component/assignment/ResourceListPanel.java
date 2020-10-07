/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.util.BaseDeprecatedPanel;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class ResourceListPanel extends BaseDeprecatedPanel {

    public ResourceListPanel(String id) {
        super(id, null);
    }

    protected void initLayout() {
        TablePanel resources = new TablePanel("table", new ObjectDataProvider((PageBase) getPage(),
                ResourceType.class), initColumns());
        resources.setOutputMarkupId(true);
        add(resources);
    }

    private List<IColumn> initColumns() {
        List<IColumn> columns = new ArrayList<>();

        IColumn column = new AjaxLinkColumn<SelectableBeanImpl<ResourceType>>(createStringResource("ObjectType.name"), "name",
                "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBeanImpl<ResourceType>> rowModel) {
                ResourceType resource = rowModel.getObject().getValue();
                resourceSelectedPerformed(target, resource);
            }
        };
        columns.add(column);


        return columns;
    }

    public void resourceSelectedPerformed(AjaxRequestTarget target, ResourceType resource) {

    }
}
