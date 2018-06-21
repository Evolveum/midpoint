/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentHeaderPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;

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
            protected IModel<String> createIconModel(final IModel<AssignmentItemDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {
					private static final long serialVersionUID = 1L;

					@Override
                    public String getObject() {
                        AssignmentItemDto item = rowModel.getObject();
                        if (item.getType() == null) {
                            return OperationResultStatusPresentationProperties.FATAL_ERROR.getIcon() + " fa-lg";
                        }

                        return item.getType().getIconCssClass();
                    }
                };
            }

            @Override
            protected IModel<String> createTitleModel(final IModel<AssignmentItemDto> rowModel) {
                return AssignmentsUtil.createAssignmentIconTitleModel(MyAssignmentsPanel.this, rowModel.getObject().getType());
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
