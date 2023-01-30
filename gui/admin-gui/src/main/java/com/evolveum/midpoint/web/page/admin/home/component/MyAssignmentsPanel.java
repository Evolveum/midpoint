/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.home.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.web.component.assignment.AssignmentHeaderPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class MyAssignmentsPanel extends BasePanel<List<AssignmentItemDto>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ASSIGNMETNS_TABLE = "assignmentsTable";

    public MyAssignmentsPanel(String id, IModel<List<AssignmentItemDto>> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        List<IColumn<AssignmentItemDto, String>> columns = new ArrayList<>();
        columns.add(new IconColumn<AssignmentItemDto>(null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<AssignmentItemDto> rowModel) {
                AssignmentItemDto item = rowModel.getObject();
                String iconClass = "";
                if (item.getType() == null) {
                    iconClass = OperationResultStatusPresentationProperties.FATAL_ERROR.getIcon() + " fa-lg";
                } else {
                    iconClass = item.getType().getIconCssClass();
                }
                return GuiDisplayTypeUtil.createDisplayType(iconClass, "",
                        AssignmentsUtil.createAssignmentIconTitleModel(MyAssignmentsPanel.this, rowModel.getObject().getType()).getObject());

            }
        });

        columns.add(new AbstractColumn<AssignmentItemDto, String>(
                createStringResource("MyAssignmentsPanel.assignment.displayName")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<AssignmentItemDto>> cellItem, String componentId,
                                     final IModel<AssignmentItemDto> rowModel) {

        AssignmentHeaderPanel panel = new AssignmentHeaderPanel(componentId, rowModel);
        panel.add(new AttributeModifier("class", "dash-assignment-header"));
        cellItem.add(panel);
        }
        });


        ISortableDataProvider provider = new ListDataProvider(this, getModel());
        TablePanel accountsTable = new TablePanel<>(ID_ASSIGNMETNS_TABLE, provider, columns);
        accountsTable.setShowPaging(false);

        add(accountsTable);
        }
        }
