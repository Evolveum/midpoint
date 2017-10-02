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
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.tester.WicketTester;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.AssignmentListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.session.AssignmentsTabStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

public abstract class AssignmentPanel extends BasePanel<List<ContainerValueWrapper<AssignmentType>>> {

	private static final long serialVersionUID = 1L;

	public static final String ID_ASSIGNMENTS = "assignments";
	private static final String ID_NEW_ASSIGNMENT_BUTTON = "newAssignmentButton";
	private static final String ID_ASSIGNMENTS_TABLE = "assignmentsTable";
	public static final String ID_ASSIGNMENTS_DETAILS = "assignmentsDetails";
	public static final String ID_ASSIGNMENT_DETAILS = "assignmentDetails";

	public static final String ID_DETAILS = "details";

	private final static String ID_DONE_BUTTON = "doneButton";
	private final static String ID_CANCEL_BUTTON = "cancelButton";

	protected boolean assignmentDetailsVisible;
	protected ContainerWrapper assignmentContainerWrapper;

	public AssignmentPanel(String id, IModel<List<ContainerValueWrapper<AssignmentType>>> assignmentsModel, ContainerWrapper assignmentContainerWrapper) {
		super(id, assignmentsModel);
		this.assignmentContainerWrapper = assignmentContainerWrapper;
	}

	protected abstract void initPaging();

	@Override
	protected void onInitialize() {
		super.onInitialize();
		initPaging();
		initLayout();
	}
	
	private void initLayout() {

		initListPanel();

		initDetailsPanel();

		setOutputMarkupId(true);

	}

	private void initListPanel() {
		WebMarkupContainer assignmentsContainer = new WebMarkupContainer(ID_ASSIGNMENTS);
		assignmentsContainer.setOutputMarkupId(true);
		add(assignmentsContainer);

		BoxedTablePanel<ContainerValueWrapper<AssignmentType>> assignmentTable = initAssignmentTable();
		assignmentsContainer.add(assignmentTable);

		AjaxIconButton newObjectIcon = new AjaxIconButton(ID_NEW_ASSIGNMENT_BUTTON, new Model<>("fa fa-plus"),
				createStringResource("MainObjectListPanel.newObject")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				newAssignmentClickPerformed(target);
			}
		};

		newObjectIcon.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI);
			}
		});
		assignmentsContainer.add(newObjectIcon);

		initCustomLayout(assignmentsContainer);

		assignmentsContainer.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !assignmentDetailsVisible;
			}
		});

	}

	private BoxedTablePanel<ContainerValueWrapper<AssignmentType>> initAssignmentTable() {

		AssignmentListDataProvider assignmentsProvider = new AssignmentListDataProvider(this, getModel()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
				getAssignmentsStorage().setPaging(paging);
			}

			@Override
			public ObjectQuery getQuery() {
				return createObjectQuery();
			}

		};

		List<IColumn<ContainerValueWrapper<AssignmentType>, String>> columns = initBasicColumns();
		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI)) {
			columns.add(new InlineMenuButtonColumn<ContainerValueWrapper<AssignmentType>>(getAssignmentMenuActions(), 2, getPageBase()));
		}

		BoxedTablePanel<ContainerValueWrapper<AssignmentType>> assignmentTable = new BoxedTablePanel<ContainerValueWrapper<AssignmentType>>(ID_ASSIGNMENTS_TABLE,
				assignmentsProvider, columns, getTableId(), getItemsPerPage()) {
			private static final long serialVersionUID = 1L;

			@Override
			public int getItemsPerPage() {
				return getPageBase().getSessionStorage().getUserProfile().getTables()
						.get(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE);
			}

			@Override
			protected Item<ContainerValueWrapper<AssignmentType>> customizeNewRowItem(Item<ContainerValueWrapper<AssignmentType>> item,
																					  IModel<ContainerValueWrapper<AssignmentType>> model) {
				item.add(AttributeModifier.append("class",
						AssignmentsUtil.createAssignmentStatusClassModel(Model.of(model.getObject().getContainerValue().asContainerable()))));
				return item;
			}

		};
		assignmentTable.setOutputMarkupId(true);
		assignmentTable.setCurrentPage(getAssignmentsStorage().getPaging());
		return assignmentTable;

	}

	protected AssignmentsTabStorage getAssignmentsStorage() {
		return getPageBase().getSessionStorage().getAssignmentsTabStorage();
	}

	protected abstract ObjectQuery createObjectQuery();

	protected List<IColumn<ContainerValueWrapper<AssignmentType>, String>> initBasicColumns() {
		List<IColumn<ContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

		columns.add(new CheckBoxHeaderColumn<ContainerValueWrapper<AssignmentType>>(){
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<Boolean> getCheckBoxValueModel(IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
				return Model.of(rowModel.getObject().isSelected());
			}
		});

		columns.add(new IconColumn<ContainerValueWrapper<AssignmentType>>(Model.of("")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return WebComponentUtil.createDefaultBlackIcon(AssignmentsUtil.getTargetType(rowModel.getObject().getContainerValue().asContainerable()));
					}
				};
			}

		});

		columns.add(new LinkColumn<ContainerValueWrapper<AssignmentType>>(createStringResource("PolicyRulesPanel.nameColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
            	String name = AssignmentsUtil.getName(rowModel.getObject().getContainerValue().asContainerable(), getParentPage());
            if (StringUtils.isBlank(name)) {
            	return createStringResource("AssignmentPanel.noName");
            }
            return Model.of(name);

            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
                assignmentDetailsPerformed(target, rowModel);
            }
        });

		columns.add(new LinkColumn<ContainerValueWrapper<AssignmentType>>(createStringResource("AssignmentType.activation")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
//            	return AssignmentsUtil.createActivationTitleModelExperimental(rowModel, AssignmentPanel.this);
return Model.of("");
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
//                updateAssignmnetActivation(target, rowModel);
            }
        });

//		columns.add(new IconColumn<AssignmentDto>(Model.of("")){
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected IModel<String> createIconModel(IModel<AssignmentDto> rowModel) {
//                if (AssignmentsUtil.getType(rowModel.getObject().getAssignment()) == null){
//                    return Model.of("");
//                }
//                return Model.of(AssignmentsUtil.getType(rowModel.getObject().getAssignment()).getIconCssClass());
//            }
//
//            @Override
//            protected IModel<String> createTitleModel(IModel<AssignmentDto> rowModel) {
//                return AssignmentsUtil.createAssignmentIconTitleModel(AbstractRoleAssignmentPanel.this, AssignmentsUtil.getType(rowModel.getObject().getAssignment()));
//            }
//
//        });
//
//		 columns.add(new IconColumn<AssignmentDto>(Model.of("")){
//	            private static final long serialVersionUID = 1L;
//
//	            @Override
//	            protected IModel<String> createIconModel(IModel<AssignmentDto> rowModel) {
//	                return Model.of(GuiStyleConstants.CLASS_POLICY_RULES);
//	            }
//
//	            @Override
//	            protected IModel<String> createTitleModel(IModel<AssignmentDto> rowModel) {
//	                return createStringResource("PolicyRulesPanel.imageTitle");
//	            }
//
//	        });



        columns.addAll(initColumns());
        return columns;
	}

	protected abstract List<IColumn<ContainerValueWrapper<AssignmentType>, String>> initColumns();

	protected abstract void newAssignmentClickPerformed(AjaxRequestTarget target);

	protected void initCustomLayout(WebMarkupContainer assignmentsContainer) {

	}

	private void initDetailsPanel() {
		WebMarkupContainer details = new WebMarkupContainer(ID_DETAILS);
		details.setOutputMarkupId(true);
		details.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return assignmentDetailsVisible;
			}
		});

		add(details);

		IModel<List<ContainerValueWrapper<AssignmentType>>> selectedAssignmnetList = new AbstractReadOnlyModel<List<ContainerValueWrapper<AssignmentType>>>() {

			private static final long serialVersionUID = 1L;

			@Override
			public List<ContainerValueWrapper<AssignmentType>> getObject() {
				return getAssignmentListProvider().getSelectedData();
			}
		};

		ListView<ContainerValueWrapper<AssignmentType>> assignmentDetailsView = new ListView<ContainerValueWrapper<AssignmentType>>(ID_ASSIGNMENTS_DETAILS,
				selectedAssignmnetList) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<ContainerValueWrapper<AssignmentType>> item) {
				Form form = this.findParent(Form.class);
				AbstractAssignmentDetailsPanel details = createDetailsPanel(ID_ASSIGNMENT_DETAILS, form, item.getModel());
				item.add(details);
				details.setOutputMarkupId(true);

			}

		};

		assignmentDetailsView.setOutputMarkupId(true);
		details.add(assignmentDetailsView);

		AjaxButton doneButton = new AjaxButton(ID_DONE_BUTTON,
				createStringResource("AssignmentPanel.doneButton")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget ajaxRequestTarget) {
				assignmentDetailsVisible = false;
				getSelectedAssignments().stream().forEach(a -> a.setSelected(false));
				ajaxRequestTarget.add(AssignmentPanel.this);
			}
		};
		details.add(doneButton);

		AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON,
				createStringResource("AssignmentPanel.cancelButton")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget ajaxRequestTarget) {
				assignmentDetailsVisible = false;
//				getSelectedAssignments().stream().forEach(a -> {a.revertChanges(); a.setSelected(false);});
				ajaxRequestTarget.add(AssignmentPanel.this);
			}
		};
		details.add(cancelButton);
	}

	protected AssignmentListDataProvider getAssignmentListProvider() {
		return (AssignmentListDataProvider) getAssignmentTable().getDataTable().getDataProvider();
	}

	protected BoxedTablePanel<ContainerValueWrapper<AssignmentType>> getAssignmentTable() {
		return (BoxedTablePanel<ContainerValueWrapper<AssignmentType>>) get(createComponentPath(ID_ASSIGNMENTS, ID_ASSIGNMENTS_TABLE));
	}

	protected abstract AbstractAssignmentDetailsPanel createDetailsPanel(String idAssignmentDetails, Form<?> form, IModel<ContainerValueWrapper<AssignmentType>> model);

	private List<ContainerValueWrapper<AssignmentType>> getSelectedAssignments() {
		BoxedTablePanel<ContainerValueWrapper<AssignmentType>> assignemntTable = getAssignmentTable();
		AssignmentListDataProvider assignmentProvider = (AssignmentListDataProvider) assignemntTable.getDataTable()
				.getDataProvider();
		return assignmentProvider.getAvailableData().stream().filter(a -> a.isSelected()).collect(Collectors.toList());
	}

	private List<InlineMenuItem> getAssignmentMenuActions() {
		List<InlineMenuItem> menuItems = new ArrayList<>();
		menuItems.add(new InlineMenuItem(createStringResource("PageBase.button.delete"), new Model<Boolean>(true),
				new Model<Boolean>(true), false, createDeleteColumnAction(), 0, GuiStyleConstants.CLASS_DELETE_MENU_ITEM,
				DoubleButtonColumn.BUTTON_COLOR_CLASS.DANGER.toString()));
		menuItems.add(new InlineMenuItem(createStringResource("PageBase.button.edit"), new Model<Boolean>(true),
				new Model<Boolean>(true), false, createEditColumnAction(), 1, GuiStyleConstants.CLASS_EDIT_MENU_ITEM,
				DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString()));
		return menuItems;
	}

	private ColumnMenuAction<ContainerValueWrapper<AssignmentType>> createDeleteColumnAction() {
		return new ColumnMenuAction<ContainerValueWrapper<AssignmentType>>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				if (getRowModel() == null) {
					deleteAssignmentPerformed(target, getSelectedAssignments());
				} else {
					List<ContainerValueWrapper<AssignmentType>> toDelete = new ArrayList<>();
					toDelete.add(getRowModel().getObject());
					deleteAssignmentPerformed(target, toDelete);
				}
			}
		};
	}

	private ColumnMenuAction<ContainerValueWrapper<AssignmentType>> createEditColumnAction() {
		return new ColumnMenuAction<ContainerValueWrapper<AssignmentType>>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				if (getRowModel() == null) {
					assignmentDetailsPerformed(target, getSelectedAssignments());
				} else {
					assignmentDetailsPerformed(target, getRowModel());
				}
			}
		};
	}

	protected void assignmentDetailsPerformed(AjaxRequestTarget target, IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
		assignmentDetailsVisible = true;
		getModelObject().forEach(a -> a.setSelected(false));
		rowModel.getObject().setSelected(true);
		target.add(AssignmentPanel.this);
	}

	protected void assignmentDetailsPerformed(AjaxRequestTarget target, List<ContainerValueWrapper<AssignmentType>> rowModel) {
		assignmentDetailsVisible = true;
		getModelObject().forEach(a -> a.setSelected(false));
		rowModel.stream().forEach(a -> a.setSelected(true));
		target.add(AssignmentPanel.this);
	}

	protected abstract TableId getTableId();

	protected abstract int getItemsPerPage();

	protected void refreshTable(AjaxRequestTarget target) {
		target.add(getAssignmentContainer().addOrReplace(initAssignmentTable()));
	}

	protected void deleteAssignmentPerformed(AjaxRequestTarget target, List<ContainerValueWrapper<AssignmentType>> toDelete) {
		if (toDelete == null){
			return;
		}
		for (ContainerValueWrapper<AssignmentType> assignmentContainerWrapper : getModelObject()){
			if (toDelete.contains(assignmentContainerWrapper)){
				assignmentContainerWrapper.setStatus(ValueStatus.DELETED);
			}
		}
		refreshTable(target);
	}

	protected WebMarkupContainer getAssignmentContainer() {
		return (WebMarkupContainer) get(ID_ASSIGNMENTS);
	}

	protected PageBase getParentPage() {
		return getPageBase();
	}

}
