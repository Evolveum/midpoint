/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.component.wf;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCaseOrDecisionDto;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItem;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDtoProvider;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.dispatchToObjectDetailsPage;

/**
 * @author lazyman
 * @author mederly
 */
public class WorkItemsTablePanel extends BasePanel {

    private static final String ID_WORK_ITEMS_TABLE = "workItemsTable";

    private ISortableDataProvider<WorkItemDto, String> provider;

    public WorkItemsTablePanel(String id, ISortableDataProvider<WorkItemDto, String> provider,
            UserProfileStorage.TableId tableId, int pageSize, boolean showAssigned) {
        super(id);
        this.provider = provider;
        initLayout(tableId, pageSize, showAssigned);
    }

    // this is called locally in order to take showAssigned into account
    private void initLayout(UserProfileStorage.TableId tableId, int pageSize, boolean showAssigned) {
        List<IColumn<WorkItemDto, String>> columns = new ArrayList<>();

        // TODO configurable
        columns.add(new CheckBoxHeaderColumn<WorkItemDto>());

		columns.add(createTypeIconColumn(true));
		columns.add(createObjectNameColumn("WorkItemsPanel.object"));
		columns.add(createTypeIconColumn(false));
		columns.add(createTargetNameColumn("WorkItemsPanel.target"));

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_WORK_ITEM_URL)) {
            columns.add(new LinkColumn<WorkItemDto>(createStringResource("WorkItemsPanel.name"), WorkItemDto.F_NAME, WorkItemDto.F_NAME) {
                @Override
                public void onClick(AjaxRequestTarget target, IModel<WorkItemDto> rowModel) {
                    PageParameters parameters = new PageParameters();
                    parameters.add(OnePageParameterEncoder.PARAMETER, rowModel.getObject().getWorkItemId());
                    setResponsePage(new PageWorkItem(parameters, (PageBase) WorkItemsTablePanel.this.getPage()));
                }
            });
        } else {
            columns.add(new AbstractColumn<WorkItemDto, String>(createStringResource("WorkItemsPanel.name")) {
                @Override
                public void populateItem(Item<ICellPopulator<WorkItemDto>> item, String componentId,
                                         final IModel<WorkItemDto> rowModel) {
                    item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {
                        @Override
                        public Object getObject() {
                            return rowModel.getObject().getName();
                        }
                    }));
                }
            });
        }

        columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.started"), WorkItemDto.F_PROCESS_STARTED));
        columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.created"), WorkItemDto.F_CREATED));
        if (showAssigned) {
            columns.add(new PropertyColumn(createStringResource("WorkItemsPanel.assigned"), WorkItemDto.F_ASSIGNEE_OR_CANDIDATES));
        }

        BoxedTablePanel<WorkItemDto> workItemsTable = new BoxedTablePanel<>(ID_WORK_ITEMS_TABLE, provider, columns, tableId, pageSize);
        add(workItemsTable);
    }

    private BoxedTablePanel getWorkItemTable() {
        return (BoxedTablePanel) get(ID_WORK_ITEMS_TABLE);
    }

    public List<WorkItemDto> getSelectedWorkItems() {
        DataTable table = getWorkItemTable().getDataTable();
        WorkItemDtoProvider provider = (WorkItemDtoProvider) table.getDataProvider();

        List<WorkItemDto> selected = new ArrayList<>();
        for (WorkItemDto row : provider.getAvailableData()) {
            if (row.isSelected()) {
                selected.add(row);
            }
        }

        return selected;
    }

	IColumn<WorkItemDto, String> createObjectNameColumn(final String headerKey) {
		return new LinkColumn<WorkItemDto>(createStringResource(headerKey), WorkItemDto.F_OBJECT_NAME) {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<WorkItemDto> rowModel) {
				WorkItemDto dto = rowModel.getObject();
				dispatchToObjectDetailsPage(dto.getObjectRef(), getPageBase());
			}
		};
	}

	IColumn<WorkItemDto, String> createTargetNameColumn(final String headerKey) {
		return new LinkColumn<WorkItemDto>(createStringResource(headerKey), WorkItemDto.F_TARGET_NAME) {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<WorkItemDto> rowModel) {
				WorkItemDto dto = rowModel.getObject();
				dispatchToObjectDetailsPage(dto.getTargetRef(), getPageBase());
			}
		};
	}

	public IColumn<WorkItemDto, String> createTypeIconColumn(final boolean object) {		// true = object, false = target
		return new IconColumn<WorkItemDto>(createStringResource("")) {
			@Override
			protected IModel<String> createIconModel(IModel<WorkItemDto> rowModel) {
				ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(rowModel);
				String icon = guiDescriptor != null ? guiDescriptor.getIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;
				return new Model<>(icon);
			}

			private ObjectTypeGuiDescriptor getObjectTypeDescriptor(IModel<WorkItemDto> rowModel) {
				QName type = object ? rowModel.getObject().getObjectType() : rowModel.getObject().getTargetType();
				return ObjectTypeGuiDescriptor.getDescriptor(ObjectTypes.getObjectTypeFromTypeQName(type));
			}

			@Override
			public void populateItem(Item<ICellPopulator<WorkItemDto>> item, String componentId, IModel<WorkItemDto> rowModel) {
				super.populateItem(item, componentId, rowModel);
				ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(rowModel);
				if (guiDescriptor != null) {
					item.add(AttributeModifier.replace("title", createStringResource(guiDescriptor.getLocalizationKey())));
					item.add(new TooltipBehavior());
				}
			}
		};
	}

}
