package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.GenericColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.wf.WfGuiUtil;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.dto.ApprovalOutcomeIcon;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.dispatchToObjectDetailsPage;
import static com.evolveum.midpoint.web.page.admin.workflow.ProcessInstancesPanel.View.FULL_LIST;
import static com.evolveum.midpoint.web.page.admin.workflow.ProcessInstancesPanel.View.TASKS_FOR_PROCESS;
import static com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto.*;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author Kate
 * @author mederly
 */

public class ProcessInstancesPanel extends BasePanel {

    private static final String ID_REQUESTS_TABLE = "requestsTable";

	private ISortableDataProvider<ProcessInstanceDto, String> provider;

	public enum View {
		FULL_LIST,				// selectable, full information
		DASHBOARD, 				// view for dashboard (not selectable, maybe reduced view)
		TASKS_FOR_PROCESS		// tasks for a process
	}

	public ProcessInstancesPanel(String id, ISortableDataProvider<ProcessInstanceDto, String> provider,
			UserProfileStorage.TableId tableId, int pageSize, View view, @Nullable IModel<String> currentInstanceIdModel) {
		super(id);
		this.provider = provider;
		initLayout(tableId, pageSize, view, currentInstanceIdModel);
	}

    private void initLayout(UserProfileStorage.TableId tableId, int pageSize, View view, final IModel<String> currentInstanceIdModel) {
		BoxedTablePanel<ProcessInstanceDto> table = new BoxedTablePanel<ProcessInstanceDto>(
				ID_REQUESTS_TABLE, provider, initColumns(view), tableId, pageSize) {

			@Override
			protected Item<ProcessInstanceDto> customizeNewRowItem(Item<ProcessInstanceDto> item,
																   final IModel<ProcessInstanceDto> rowModel) {

				item.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						if (currentInstanceIdModel == null || currentInstanceIdModel.getObject() == null) {
							return "";
						}
						ProcessInstanceDto rowDto = rowModel.getObject();
						if (currentInstanceIdModel.getObject().equals(rowDto.getProcessInstanceId())) {
							return "info";
						} else {
							return "";
						}
					}
				}));
				return item;
			}
		};
		table.setOutputMarkupId(true);
		table.setAdditionalBoxCssClasses("without-box-header-top-border");
		add(table);
	}

	public BoxedTablePanel<ProcessInstanceDto> getTablePanel() {
		return (BoxedTablePanel<ProcessInstanceDto>) get(ID_REQUESTS_TABLE);
	}

	private List<IColumn<ProcessInstanceDto, String>> initColumns(View view) {

		List<IColumn<ProcessInstanceDto, String>> columns = new ArrayList<>();

		if (view != TASKS_FOR_PROCESS) {
			if (view == FULL_LIST) {
				columns.add(new CheckBoxHeaderColumn<>());
			}
			columns.add(createNameColumn());
			columns.add(createTypeIconColumn(true));
			columns.add(createObjectNameColumn("pageProcessInstances.item.object"));
			columns.add(createTypeIconColumn(false));
			columns.add(createTargetNameColumn("pageProcessInstances.item.target"));
			columns.add(createStageColumn());
			//columns.add(createStateColumn());
			columns.add(new PropertyColumn<ProcessInstanceDto, String>(createStringResource("pageProcessInstances.item.started"), F_START_FORMATTED));
			columns.add(createOutcomeColumn());
			columns.add(createFinishedColumn());
		} else {
			columns.add(createNameColumn());
			columns.add(createStageColumn());
			//columns.add(createStateColumn());
			columns.add(createOutcomeColumn());
			columns.add(createFinishedColumn());
		}
		return columns;
    }

	@NotNull
	private PropertyColumn<ProcessInstanceDto, String> createFinishedColumn() {
		return new PropertyColumn<>(createStringResource("pageProcessInstances.item.finished"), F_END_FORMATTED);
	}

//	@NotNull
//	private PropertyColumn<ProcessInstanceDto, String> createStateColumn() {
//		return new PropertyColumn<>(createStringResource("pageProcessInstances.item.state"), F_STATE);
//	}

	@NotNull
	private PropertyColumn<ProcessInstanceDto, String> createStageColumn() {
		return new PropertyColumn<>(createStringResource("pageProcessInstances.item.stage"), F_STAGE);
	}

	@NotNull
	private IColumn<ProcessInstanceDto,String> createNameColumn() {
		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
				AuthorizationConstants.AUTZ_UI_TASK_URL)) {
			return new LinkColumn<ProcessInstanceDto>(createStringResource("MyRequestsPanel.name"), "name") {

				@Override
				protected IModel createLinkModel(IModel<ProcessInstanceDto> rowModel) {
					return createProcessNameModel(rowModel);
				}

				@Override
				public void onClick(AjaxRequestTarget target, IModel<ProcessInstanceDto> rowModel) {
					ProcessInstanceDto piDto = rowModel.getObject();
					itemDetailsPerformed(target, piDto.getTaskOid());
				}
			};
		} else {
			return new GenericColumn<>(createStringResource("MyRequestsPanel.name"), rowModel -> createProcessNameModel(rowModel));
		}
	}

	private IModel<String> createProcessNameModel(IModel<ProcessInstanceDto> processInstanceDtoModel) {
		return new ReadOnlyModel<>(() -> {
			ProcessInstanceDto processInstanceDto = processInstanceDtoModel.getObject();
			return defaultIfNull(
					WfGuiUtil.getLocalizedProcessName(processInstanceDto.getWorkflowContext(), ProcessInstancesPanel.this),
					processInstanceDto.getName());
		});
	}

	@NotNull
	private IconColumn<ProcessInstanceDto> createOutcomeColumn() {
		return new IconColumn<ProcessInstanceDto>(createStringResource("pageProcessInstances.item.result")) {
			@Override
			protected IModel<String> createIconModel(final IModel<ProcessInstanceDto> rowModel) {
				return new AbstractReadOnlyModel<String>() {
					@Override
					public String getObject() {
						return choose(rowModel, null, ApprovalOutcomeIcon.IN_PROGRESS.getIcon(), ApprovalOutcomeIcon.APPROVED.getIcon(), ApprovalOutcomeIcon.REJECTED.getIcon());
					}
				};
			}

			@Override
			protected IModel<String> createTitleModel(final IModel<ProcessInstanceDto> rowModel) {
				return new AbstractReadOnlyModel<String>() {
					@Override
					public String getObject() {
						return choose(rowModel,
								null,
								createStringResource("MyRequestsPanel.inProgress").getString(),
								createStringResource("MyRequestsPanel.approved").getString(),
								createStringResource("MyRequestsPanel.rejected").getString());
					}
				};
			}
			
			// Cannot have the default "icon" class here. This column has text label in the header.
			// Having class "icon" would shrink the column to 25px and the text will overflow.
			@Override
		    public String getCssClass() {
		        return "shrink";
		    }

			private String choose(IModel<ProcessInstanceDto> rowModel, String noReply, String inProgress, String approved, String rejected) {
				ProcessInstanceDto dto = rowModel.getObject();
				Boolean result = ApprovalUtils.approvalBooleanValueFromUri(dto.getOutcome());
				if (result == null) {
					if (dto.getEndTimestamp() != null) {
						return noReply;
					} else {
						return inProgress;
					}
				} else {
					return result ? approved : rejected;
				}
			}
		};
	}

	private void itemDetailsPerformed(AjaxRequestTarget target, String pid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, pid);
        getPageBase().navigateToNext(PageTaskEdit.class, parameters);
    }

	// copied and adapted from WorkItemsPanel - TODO deduplicate

	IColumn<ProcessInstanceDto, String> createObjectNameColumn(final String headerKey) {
		return new LinkColumn<ProcessInstanceDto>(createStringResource(headerKey), ProcessInstanceDto.F_OBJECT_NAME) {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<ProcessInstanceDto> rowModel) {
				ProcessInstanceDto dto = rowModel.getObject();
				dispatchToObjectDetailsPage(dto.getObjectRef(), getPageBase(), false);
			}
		};
	}

	IColumn<ProcessInstanceDto, String> createTargetNameColumn(final String headerKey) {
		return new LinkColumn<ProcessInstanceDto>(createStringResource(headerKey), ProcessInstanceDto.F_TARGET_NAME) {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<ProcessInstanceDto> rowModel) {
				ProcessInstanceDto dto = rowModel.getObject();
				dispatchToObjectDetailsPage(dto.getTargetRef(), getPageBase(), false);
			}
		};
	}

	public IColumn<ProcessInstanceDto, String> createTypeIconColumn(final boolean object) {		// true = object, false = target
		return new IconColumn<ProcessInstanceDto>(createStringResource("")) {
			@Override
			protected IModel<String> createIconModel(IModel<ProcessInstanceDto> rowModel) {
				if (getObjectType(rowModel) == null) {
					return null;
				}
				ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(rowModel);
				String icon = guiDescriptor != null ? guiDescriptor.getBlackIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;
				return new Model<>(icon);
			}

			private ObjectTypeGuiDescriptor getObjectTypeDescriptor(IModel<ProcessInstanceDto> rowModel) {
				QName type = getObjectType(rowModel);
				return ObjectTypeGuiDescriptor.getDescriptor(ObjectTypes.getObjectTypeFromTypeQName(type));
			}

			private QName getObjectType(IModel<ProcessInstanceDto> rowModel) {
				return object ? rowModel.getObject().getObjectType() : rowModel.getObject().getTargetType();
			}

			@Override
			public void populateItem(Item<ICellPopulator<ProcessInstanceDto>> item, String componentId, IModel<ProcessInstanceDto> rowModel) {
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
