package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.dispatchToObjectDetailsPage;
import static com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusIcon.FATAL_ERROR;
import static com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusIcon.IN_PROGRESS;
import static com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusIcon.SUCCESS;
import static com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto.*;

/**
 * @author Kate
 * @author mederly
 */

public class WorkflowRequestsPanel extends BasePanel {

    private static final String ID_REQUESTS_TABLE = "requestsTable";

	private ISortableDataProvider<ProcessInstanceDto, String> provider;

	public WorkflowRequestsPanel(String id, ISortableDataProvider<ProcessInstanceDto, String> provider,
			UserProfileStorage.TableId tableId, int pageSize) {
		super(id);
		this.provider = provider;
		initLayout(tableId, pageSize);
	}

    private void initLayout(UserProfileStorage.TableId tableId, int pageSize) {
		BoxedTablePanel<ProcessInstanceDto> table = new BoxedTablePanel<>(ID_REQUESTS_TABLE, provider, initColumns(), tableId, pageSize);
		table.setOutputMarkupId(true);
		add(table);
	}

	private List<IColumn<ProcessInstanceDto, String>> initColumns() {

		List<IColumn<ProcessInstanceDto, String>> columns = new ArrayList<IColumn<ProcessInstanceDto, String>>();

		IColumn column = new CheckBoxHeaderColumn<>();
		columns.add(column);

		columns.add(createTypeIconColumn(true));
		columns.add(createObjectNameColumn("pageProcessInstances.item.object"));
		columns.add(createTypeIconColumn(false));
		columns.add(createTargetNameColumn("pageProcessInstances.item.target"));

		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
				AuthorizationConstants.AUTZ_UI_WORK_ITEMS_PROCESS_INSTANCE_URL)) {
			columns.add(new LinkColumn<ProcessInstanceDto>(createStringResource("MyRequestsPanel.name"), "name") {
				@Override
				public void onClick(AjaxRequestTarget target, IModel<ProcessInstanceDto> rowModel) {
					ProcessInstanceDto piDto = rowModel.getObject();
					itemDetailsPerformed(target, piDto.getTaskOid());
				}
			});
		} else {
			columns.add(new PropertyColumn<ProcessInstanceDto, String>(createStringResource("MyRequestsPanel.name"), F_NAME));
		}

		columns.add(new PropertyColumn<ProcessInstanceDto, String>(createStringResource("pageProcessInstances.item.state"), F_STATE));

		columns.add(new PropertyColumn<ProcessInstanceDto, String>(createStringResource("pageProcessInstances.item.started"), F_START_FORMATTED));

        columns.add(new IconColumn<ProcessInstanceDto>(createStringResource("pageProcessInstances.item.result")) {
            @Override
            protected IModel<String> createIconModel(final IModel<ProcessInstanceDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
						return choose(rowModel, IN_PROGRESS.getIcon(), SUCCESS.getIcon(), FATAL_ERROR.getIcon());
                    }
                };
            }

			@Override
            protected IModel<String> createTitleModel(final IModel<ProcessInstanceDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
						return choose(rowModel,
								createStringResource("MyRequestsPanel.inProgress").getString(),
								createStringResource("MyRequestsPanel.approved").getString(),
								createStringResource("MyRequestsPanel.rejected").getString());
                    }
                };
            }

			private String choose(IModel<ProcessInstanceDto> rowModel, String inProgress, String approved, String rejected) {
				ProcessInstanceDto dto = rowModel.getObject();
				Boolean result = ApprovalUtils.approvalBooleanValue(dto.getAnswer());
				if (result == null) {
					return inProgress;
				} else {
					return result ? approved : rejected;
				}
			}
		});

		columns.add(new PropertyColumn<ProcessInstanceDto, String>(createStringResource("pageProcessInstances.item.finished"), F_END_FORMATTED));

		return columns;
    }

	private void itemDetailsPerformed(AjaxRequestTarget target, String pid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, pid);
        setResponsePage(new PageTaskEdit(parameters, this.getPageBase()));
    }

	// copied and adapted from WorkItemsTablePanel - TODO deduplicate
	
	IColumn<ProcessInstanceDto, String> createObjectNameColumn(final String headerKey) {
		return new LinkColumn<ProcessInstanceDto>(createStringResource(headerKey), ProcessInstanceDto.F_OBJECT_NAME) {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<ProcessInstanceDto> rowModel) {
				ProcessInstanceDto dto = rowModel.getObject();
				dispatchToObjectDetailsPage(dto.getObjectRef(), getPageBase());
			}
		};
	}

	IColumn<ProcessInstanceDto, String> createTargetNameColumn(final String headerKey) {
		return new LinkColumn<ProcessInstanceDto>(createStringResource(headerKey), ProcessInstanceDto.F_TARGET_NAME) {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<ProcessInstanceDto> rowModel) {
				ProcessInstanceDto dto = rowModel.getObject();
				dispatchToObjectDetailsPage(dto.getTargetRef(), getPageBase());
			}
		};
	}

	public IColumn<ProcessInstanceDto, String> createTypeIconColumn(final boolean object) {		// true = object, false = target
		return new IconColumn<ProcessInstanceDto>(createStringResource("")) {
			@Override
			protected IModel<String> createIconModel(IModel<ProcessInstanceDto> rowModel) {
				ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(rowModel);
				String icon = guiDescriptor != null ? guiDescriptor.getIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;
				return new Model<>(icon);
			}

			private ObjectTypeGuiDescriptor getObjectTypeDescriptor(IModel<ProcessInstanceDto> rowModel) {
				QName type = object ? rowModel.getObject().getObjectType() : rowModel.getObject().getTargetType();
				return ObjectTypeGuiDescriptor.getDescriptor(ObjectTypes.getObjectTypeFromTypeQName(type));
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
