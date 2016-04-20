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

package com.evolveum.midpoint.web.page.admin.home.component;

import com.evolveum.midpoint.web.component.assignment.AssignmentHeaderPanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class MyAssignmentsPanel extends SimplePanel<List<AssignmentItemDto>> {

    private static final String ID_ASSIGNMETNS_TABLE = "assignmentsTable";

    public MyAssignmentsPanel(String id, IModel<List<AssignmentItemDto>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        List<IColumn<AssignmentItemDto, String>> columns = new ArrayList<IColumn<AssignmentItemDto, String>>();
        columns.add(new IconColumn<AssignmentItemDto>(createStringResource("MyAssignmentsPanel.assignment.type")) {

            @Override
            protected IModel<String> createIconModel(final IModel<AssignmentItemDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        AssignmentItemDto item = rowModel.getObject();
                        if (item.getType() == null) {
                            return "silk-error";
                        }

                        switch (item.getType()) {
                            case CONSTRUCTION:
                                return "silk-drive";
                            case ORG_UNIT:
                                return "silk-building";
                            case ROLE:
                                return "silk-user_suit";
                            default:
                                return "silk-error";
                        }
                    }
                };
            }

            @Override
            protected IModel<String> createTitleModel(final IModel<AssignmentItemDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        AssignmentItemDto item = rowModel.getObject();
                        if (item.getType() == null) {
                            return MyAssignmentsPanel.this.getString("MyAssignmentsPanel.type.error");
                        }

                        switch (item.getType()) {
                            case CONSTRUCTION:
                                return MyAssignmentsPanel.this.getString("MyAssignmentsPanel.type.accountConstruction");
                            case ORG_UNIT:
                                return MyAssignmentsPanel.this.getString("MyAssignmentsPanel.type.orgUnit");
                            case ROLE:
                                return MyAssignmentsPanel.this.getString("MyAssignmentsPanel.type.role");
                            default:
                                return MyAssignmentsPanel.this.getString("MyAssignmentsPanel.type.error");
                        }
                    }
                };
            }
        });

        columns.add(new AbstractColumn<AssignmentItemDto, String>(
                createStringResource("MyAssignmentsPanel.assignment.displayName")) {

            @Override
            public void populateItem(Item<ICellPopulator<AssignmentItemDto>> cellItem, String componentId,
                                     final IModel<AssignmentItemDto> rowModel) {

                AssignmentHeaderPanel panel = new AssignmentHeaderPanel(componentId, rowModel);
                panel.add(new AttributeModifier("class", "dashAssignmentHeader"));
                cellItem.add(panel);
            }
        });


        ISortableDataProvider provider = new ListDataProvider(this, getModel());
        TablePanel accountsTable = new TablePanel<AssignmentItemDto>(ID_ASSIGNMETNS_TABLE, provider, columns);
        accountsTable.setShowPaging(false);

        add(accountsTable);
    }
}
