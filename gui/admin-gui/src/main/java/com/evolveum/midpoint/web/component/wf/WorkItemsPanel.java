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

package com.evolveum.midpoint.web.component.wf;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.GenericColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItem;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItems;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDtoProvider;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.dispatchToObjectDetailsPage;
import static com.evolveum.midpoint.web.component.wf.WorkItemsPanel.View.FULL_LIST;
import static com.evolveum.midpoint.web.component.wf.WorkItemsPanel.View.ITEMS_FOR_PROCESS;
import static com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto.F_STAGE_INFO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author lazyman
 * @author mederly
 */
public class WorkItemsPanel extends BasePanel {

    private static final String ID_WORK_ITEMS_TABLE = "workItemsTable";

	public enum View {
		FULL_LIST,				// selectable, full information
		DASHBOARD, 				// not selectable, reduced info (on dashboard)
		ITEMS_FOR_PROCESS		// work items for a process
	}

    private ISortableDataProvider<WorkItemDto, String> provider;

    public WorkItemsPanel(String id, ISortableDataProvider<WorkItemDto, String> provider,
            UserProfileStorage.TableId tableId, int pageSize, View view) {
        super(id);
        this.provider = provider;
        initLayout(tableId, pageSize, view);
    }

    // this is called locally in order to take showAssigned into account
    private void initLayout(UserProfileStorage.TableId tableId, int pageSize, View view) {
        List<IColumn<WorkItemDto, String>> columns = new ArrayList<>();

		if (view != ITEMS_FOR_PROCESS) {
			if (view == FULL_LIST) {
				columns.add(new CheckBoxHeaderColumn<>());
			}
			columns.add(createNameColumn());
			columns.add(createStageColumn());
			columns.add(createTypeIconColumn(true));
			columns.add(createObjectNameColumn("WorkItemsPanel.object"));
			columns.add(createTypeIconColumn(false));
			columns.add(createTargetNameColumn("WorkItemsPanel.target"));
			if (view == FULL_LIST) {
				columns.add(new AbstractColumn<WorkItemDto, String>(createStringResource("WorkItemsPanel.started")) {
					@Override
					public void populateItem(Item<ICellPopulator<WorkItemDto>> cellItem, String componentId, final IModel<WorkItemDto> rowModel) {
						cellItem.add(new DateLabelComponent(componentId, new AbstractReadOnlyModel<Date>() {

							@Override
							public Date getObject() {
								return rowModel.getObject().getStartedDate();
							}
						}, DateLabelComponent.LONG_MEDIUM_STYLE));
					}
				});
			}
			columns.add(new AbstractColumn<WorkItemDto, String>(createStringResource("WorkItemsPanel.created")){
                @Override
                public void populateItem(Item<ICellPopulator<WorkItemDto>> cellItem, String componentId, final IModel<WorkItemDto> rowModel) {
                        cellItem.add(new DateLabelComponent(componentId, new AbstractReadOnlyModel<Date>() {

                            @Override
                            public Date getObject() {
                                return rowModel.getObject().getCreatedDate();
                            }
                        }, DateLabelComponent.LONG_MEDIUM_STYLE));
                    }
            });
			columns.add(new AbstractColumn<WorkItemDto, String>(createStringResource("WorkItemsPanel.deadline")){
                @Override
                public void populateItem(Item<ICellPopulator<WorkItemDto>> cellItem, String componentId, final IModel<WorkItemDto> rowModel) {
                        cellItem.add(new DateLabelComponent(componentId, new AbstractReadOnlyModel<Date>() {
                            @Override
                            public Date getObject() {
                                return rowModel.getObject().getDeadlineDate();
                            }
                        }, DateLabelComponent.LONG_MEDIUM_STYLE));
                    }
            });
			columns.add(new PropertyColumn<>(createStringResource("WorkItemsPanel.escalationLevel"),
					WorkItemDto.F_ESCALATION_LEVEL_NUMBER));
			if (view == FULL_LIST) {
				columns.add(new PropertyColumn<>(createStringResource("WorkItemsPanel.actors"), WorkItemDto.F_ASSIGNEE_OR_CANDIDATES));
			}
		} else {
			columns.add(createNameColumn());
			columns.add(createStageColumn());
			columns.add(new PropertyColumn<>(createStringResource("WorkItemsPanel.actors"), WorkItemDto.F_ASSIGNEE_OR_CANDIDATES));
            columns.add(new PropertyColumn<>(createStringResource("WorkItemsPanel.created"), WorkItemDto.F_CREATED_FORMATTED));
            columns.add(new PropertyColumn<>(createStringResource("WorkItemsPanel.deadline"), WorkItemDto.F_DEADLINE_FORMATTED));
			columns.add(new PropertyColumn<>(createStringResource("WorkItemsPanel.escalationLevel"),
					WorkItemDto.F_ESCALATION_LEVEL_NUMBER));
		}

        BoxedTablePanel<WorkItemDto> workItemsTable = new BoxedTablePanel<>(ID_WORK_ITEMS_TABLE, provider, columns, tableId, pageSize);
		workItemsTable.setAdditionalBoxCssClasses("without-box-header-top-border");
        add(workItemsTable);
    }

	@NotNull
	private AbstractColumn<WorkItemDto, String> createNameColumn() {
		AbstractColumn<WorkItemDto, String> nameColumn;
		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_APPROVALS_ALL_URL,
				AuthorizationConstants.AUTZ_UI_WORK_ITEM_URL)) {
			nameColumn = new LinkColumn<WorkItemDto>(createStringResource("WorkItemsPanel.name"), WorkItemDto.F_NAME,
					WorkItemDto.F_NAME) {
				@Override
				protected IModel<String> createLinkModel(IModel<WorkItemDto> rowModel) {
					return createWorkItemNameModel(rowModel);
				}

				@Override
				public void onClick(AjaxRequestTarget target, IModel<WorkItemDto> rowModel) {
					PageParameters parameters = new PageParameters();
					parameters.add(OnePageParameterEncoder.PARAMETER, rowModel.getObject().getWorkItemId());
					PageWorkItem page = Session.get().getPageFactory().newPage(PageWorkItem.class, parameters);
					page.setPowerDonor(determinePowerDonor());
					getPageBase().navigateToNext(page);
				}
			};
        } else {
			nameColumn = new GenericColumn<>(createStringResource("WorkItemsPanel.name"), rowModel -> createWorkItemNameModel(rowModel));
        }
		return nameColumn;
	}

	private PrismObject<UserType> determinePowerDonor() {
		PageBase pageBase = getPageBase();
		return pageBase instanceof PageWorkItems ? ((PageWorkItems) pageBase).getPowerDonor() : null;
	}

	private IModel<String> createWorkItemNameModel(IModel<WorkItemDto> workItemDtoModel) {
		return new ReadOnlyModel<>(() -> {
			WorkItemDto workItemDto = workItemDtoModel.getObject();
			return defaultIfNull(
					WfGuiUtil.getLocalizedProcessName(workItemDto.getWorkflowContext(), WorkItemsPanel.this),
					workItemDto.getName());
		});
	}

	@NotNull
	private PropertyColumn<WorkItemDto, String> createStageColumn() {
		return new PropertyColumn<>(createStringResource("WorkItemsPanel.stage"), F_STAGE_INFO);
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

	@SuppressWarnings("SameParameterValue")
	private IColumn<WorkItemDto, String> createObjectNameColumn(final String headerKey) {
		return new LinkColumn<WorkItemDto>(createStringResource(headerKey), WorkItemDto.F_OBJECT_NAME) {
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createLinkModel(IModel<WorkItemDto> rowModel) {
				return Model.of(WebModelServiceUtils.resolveReferenceName(rowModel.getObject().getObjectRef(), getPageBase()));
			}

			@Override
			public void onClick(AjaxRequestTarget target, IModel<WorkItemDto> rowModel) {
				WorkItemDto dto = rowModel.getObject();
				dispatchToObjectDetailsPage(dto.getObjectRef(), getPageBase(), false);
			}
		};
	}

	@SuppressWarnings("SameParameterValue")
	private IColumn<WorkItemDto, String> createTargetNameColumn(final String headerKey) {
		return new LinkColumn<WorkItemDto>(createStringResource(headerKey), WorkItemDto.F_TARGET_NAME) {

			@Override
			protected IModel<String> createLinkModel(IModel<WorkItemDto> rowModel) {
				return Model.of(WebModelServiceUtils.resolveReferenceName(rowModel.getObject().getTargetRef(), getPageBase()));
			}

			@Override
			public void onClick(AjaxRequestTarget target, IModel<WorkItemDto> rowModel) {
				WorkItemDto dto = rowModel.getObject();
				dispatchToObjectDetailsPage(dto.getTargetRef(), getPageBase(), false);
			}

			@Override
			public void populateItem(Item<ICellPopulator<WorkItemDto>> cellItem, String componentId,
									 final IModel<WorkItemDto> rowModel) {
				super.populateItem(cellItem, componentId, rowModel);
				Component c = cellItem.get(componentId);
				c.add(new AttributeAppender("title", getTargetObjectDescription(rowModel)));
			}
		};
	}

	private String getTargetObjectDescription(IModel<WorkItemDto> rowModel){
		if (rowModel == null || rowModel.getObject() == null ||
				rowModel.getObject().getTargetRef() == null) {
			return "";
		}
		PrismReferenceValue refVal = rowModel.getObject().getTargetRef().asReferenceValue();
		return refVal.getObject() != null ?
				refVal.getObject().asObjectable().getDescription() : "";

	}

	private IColumn<WorkItemDto, String> createTypeIconColumn(final boolean object) {		// true = object, false = target
		return new IconColumn<WorkItemDto>(createStringResource("")) {
			@Override
			protected IModel<String> createIconModel(IModel<WorkItemDto> rowModel) {
				if (getObjectType(rowModel) == null) {
					return null;
				}
				ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(rowModel);
				String icon = guiDescriptor != null ? guiDescriptor.getBlackIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;
				return new Model<>(icon);
			}

			private ObjectTypeGuiDescriptor getObjectTypeDescriptor(IModel<WorkItemDto> rowModel) {
				return ObjectTypeGuiDescriptor.getDescriptor(ObjectTypes.getObjectTypeFromTypeQName(getObjectType(rowModel)));
			}

			private QName getObjectType(IModel<WorkItemDto> rowModel) {
				return object ? rowModel.getObject().getObjectType() : rowModel.getObject().getTargetType();
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
