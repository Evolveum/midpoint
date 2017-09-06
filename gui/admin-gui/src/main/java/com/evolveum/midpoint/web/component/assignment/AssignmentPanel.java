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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
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

public abstract class AssignmentPanel extends BasePanel<List<AssignmentDto>> {

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

	private PageBase pageBase;

	public AssignmentPanel(String id, IModel<List<AssignmentDto>> assignmentsModel, PageBase pageBase) {
		super(id, assignmentsModel);
		this.pageBase = pageBase;
		initPaging();
		initLayout();

	}

	protected abstract void initPaging();

	private void initLayout() {

		initListPanel();

		initDetailsPanel();

		setOutputMarkupId(true);

	}

	private void initListPanel() {
		WebMarkupContainer assignmentsContainer = new WebMarkupContainer(ID_ASSIGNMENTS);
		assignmentsContainer.setOutputMarkupId(true);
		add(assignmentsContainer);

		BoxedTablePanel<AssignmentDto> assignmentTable = initAssignmentTable();
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

	private BoxedTablePanel<AssignmentDto> initAssignmentTable() {

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

		List<IColumn<AssignmentDto, String>> columns = initBasicColumns();
		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI)) {
			columns.add(new InlineMenuButtonColumn<AssignmentDto>(getAssignmentMenuActions(), 2, getParentPage()));
		}

		BoxedTablePanel<AssignmentDto> assignmentTable = new BoxedTablePanel<AssignmentDto>(ID_ASSIGNMENTS_TABLE,
				assignmentsProvider, columns, getTableId(), getItemsPerPage()) {
			private static final long serialVersionUID = 1L;

			@Override
			public int getItemsPerPage() {
				return pageBase.getSessionStorage().getUserProfile().getTables()
						.get(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE);
			}

			@Override
			protected Item<AssignmentDto> customizeNewRowItem(Item<AssignmentDto> item, IModel<AssignmentDto> model) {
				item.add(AttributeModifier.append("class", AssignmentsUtil.createAssignmentStatusClassModel(model)));
				return item;
			}

		};
		assignmentTable.setOutputMarkupId(true);
		assignmentTable.setCurrentPage(getAssignmentsStorage().getPaging());
		return assignmentTable;

	}

	protected AssignmentsTabStorage getAssignmentsStorage() {
		return pageBase.getSessionStorage().getAssignmentsTabStorage();
	}

	protected abstract ObjectQuery createObjectQuery();

	protected List<IColumn<AssignmentDto, String>> initBasicColumns() {
		List<IColumn<AssignmentDto, String>> columns = new ArrayList<>();

		columns.add(new CheckBoxHeaderColumn<AssignmentDto>());

		columns.add(new IconColumn<AssignmentDto>(Model.of("")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(IModel<AssignmentDto> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return WebComponentUtil.createDefaultBlackIcon(rowModel.getObject().getTargetType());
					}
				};
			}

		});

		columns.add(new LinkColumn<AssignmentDto>(createStringResource("PolicyRulesPanel.nameColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<AssignmentDto> rowModel) {
            	String name = AssignmentsUtil.getName(rowModel.getObject().getAssignment(), getParentPage());
            if (StringUtils.isBlank(name)) {
            	return createStringResource("AssignmentPanel.noName");
            }
            return Model.of(name);

            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<AssignmentDto> rowModel) {
                assignmentDetailsPerformed(target, rowModel);
            }
        });

		columns.add(new LinkColumn<AssignmentDto>(createStringResource("AssignmentType.activation")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<AssignmentDto> rowModel) {
            	return AssignmentsUtil.createActivationTitleModelExperimental(rowModel, AssignmentPanel.this);

            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<AssignmentDto> rowModel) {
                updateAssignmnetActivation(target, rowModel);
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

	protected abstract List<IColumn<AssignmentDto, String>> initColumns();

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

		IModel<List<AssignmentDto>> selectedAssignmnetList = new AbstractReadOnlyModel<List<AssignmentDto>>() {

			private static final long serialVersionUID = 1L;

			@Override
			public List<AssignmentDto> getObject() {
				return getAssignmentListProvider().getSelectedData();
			}
		};

		ListView<AssignmentDto> assignmentDetailsView = new ListView<AssignmentDto>(ID_ASSIGNMENTS_DETAILS,
				selectedAssignmnetList) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<AssignmentDto> item) {
				Form form = this.findParent(Form.class);
				AbstractAssignmentDetailsPanel details = createDetailsPanel(ID_ASSIGNMENT_DETAILS, form, item.getModel(),
						getParentPage());
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
				getSelectedAssignments().stream().forEach(a -> {a.revertChanges(); a.setSelected(false);});
				ajaxRequestTarget.add(AssignmentPanel.this);
			}
		};
		details.add(cancelButton);
	}

	protected AssignmentListDataProvider getAssignmentListProvider() {
		return (AssignmentListDataProvider) getAssignmentTable().getDataTable().getDataProvider();
	}

	protected BoxedTablePanel<AssignmentDto> getAssignmentTable() {
		return (BoxedTablePanel<AssignmentDto>) get(createComponentPath(ID_ASSIGNMENTS, ID_ASSIGNMENTS_TABLE));
	}

	protected abstract AbstractAssignmentDetailsPanel createDetailsPanel(String idAssignmentDetails, Form<?> form, IModel<AssignmentDto> model,
			PageBase parentPage);

	private List<AssignmentDto> getSelectedAssignments() {
		BoxedTablePanel<AssignmentDto> assignemntTable = getAssignmentTable();
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

	private ColumnMenuAction<AssignmentDto> createDeleteColumnAction() {
		return new ColumnMenuAction<AssignmentDto>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				if (getRowModel() == null) {
					deleteAssignmentPerformed(target, getSelectedAssignments());
				} else {
					AssignmentDto rowDto = (AssignmentDto) getRowModel().getObject();
					List<AssignmentDto> toDelete = new ArrayList<>();
					toDelete.add(rowDto);
					deleteAssignmentPerformed(target, toDelete);
				}
			}
		};
	}

	private ColumnMenuAction<AssignmentDto> createEditColumnAction() {
		return new ColumnMenuAction<AssignmentDto>() {
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

	protected void assignmentDetailsPerformed(AjaxRequestTarget target, IModel<AssignmentDto> rowModel) {
		assignmentDetailsVisible = true;
		getModelObject().forEach(a -> a.setSelected(false));
		rowModel.getObject().setSelected(true);
		target.add(AssignmentPanel.this);
	}

	protected void assignmentDetailsPerformed(AjaxRequestTarget target, List<AssignmentDto> rowModel) {
		assignmentDetailsVisible = true;
		rowModel.stream().forEach(a -> a.setSelected(true));
		target.add(AssignmentPanel.this);
	}

	protected void updateAssignmnetActivation(AjaxRequestTarget target, IModel<AssignmentDto> rowModel) {
		AssignmentActivationPopupablePanel activationPanel = new AssignmentActivationPopupablePanel(
				getParentPage().getMainPopupBodyId(),
				new PropertyModel<>(rowModel, AssignmentDto.F_VALUE + "." + AssignmentType.F_ACTIVATION.getLocalPart()));
		activationPanel.setOutputMarkupId(true);

		getParentPage().showMainPopup(activationPanel, target);
	}

	protected abstract TableId getTableId();

	protected abstract int getItemsPerPage();

	protected void refreshTable(AjaxRequestTarget target) {
		target.add(getAssignmentContainer().addOrReplace(initAssignmentTable()));
	}

	protected void deleteAssignmentPerformed(AjaxRequestTarget target, List<AssignmentDto> toDelete) {
		toDelete.forEach(a -> a.setStatus(UserDtoStatus.DELETE));
		refreshTable(target);
	}

	protected WebMarkupContainer getAssignmentContainer() {
		return (WebMarkupContainer) get(ID_ASSIGNMENTS);
	}

	public PageBase getParentPage() {
		return pageBase;
	}
}
