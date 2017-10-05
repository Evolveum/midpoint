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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
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

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.AssignmentListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.AssignmentsTabStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import javax.xml.datatype.XMLGregorianCalendar;

public abstract class AssignmentPanel extends BasePanel<ContainerWrapper<AssignmentType>> {

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

	public AssignmentPanel(String id, IModel<ContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
		super(id, assignmentContainerWrapperModel);
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

		AssignmentListDataProvider assignmentsProvider = new AssignmentListDataProvider(this, new PropertyModel<>(getModel(), "values")) {
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
				item.add(AttributeModifier.append("class", new AbstractReadOnlyModel<String>() {
							@Override
							public String getObject() {
								return AssignmentsUtil.createAssignmentStatusClassModel(model.getObject());
							}
						}));
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
			protected boolean isTableRowSelected(ContainerValueWrapper<AssignmentType> object){
				return object.isSelected();
			}

			@Override
			protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
				super.onUpdateRow(target, table, rowModel);
				rowModel.getObject().setSelected(!rowModel.getObject().isSelected());
			};

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

		columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("AssignmentType.activation")){
            private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
									 final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
				List<ItemWrapper> assignmentItems = rowModel.getObject().getItems();//ContainerValue().findContainer(AssignmentType.F_ACTIVATION);
				ItemWrapper activationItem = null;
				for (ItemWrapper wrapper : assignmentItems){
					if (wrapper.getPath().containsName(AssignmentType.F_ACTIVATION)){
						activationItem = wrapper;
						break;
					}
				}
				ActivationStatusType administrativeStatus = null;
				XMLGregorianCalendar validFrom = null;
				XMLGregorianCalendar validTo = null;
				if (activationItem != null){
					List<ContainerValueWrapper<ActivationType>> activationsList = activationItem.getValues();
					if (activationsList != null && activationsList.size() > 0){
						List<ItemWrapper> activation = activationsList.get(0).getItems();
						if (activation != null && activation.size() > 0) {
							for (ItemWrapper activationProperty : activation) {
								if (activationProperty.getValues() != null && activationProperty.getValues().size() > 0) {
									List<ValueWrapper> values = activationProperty.getValues();
									if (values.get(0).getValue() != null && values.get(0).getValue().getRealValue() != null) {
										if (activationProperty.getPath().containsName(ActivationType.F_ADMINISTRATIVE_STATUS)) {
											administrativeStatus = (ActivationStatusType) values.get(0).getValue().getRealValue();
											continue;
										}
										if (activationProperty.getPath().containsName(ActivationType.F_VALID_FROM)) {
											validFrom = (XMLGregorianCalendar) values.get(0).getValue().getRealValue();
											continue;
										}
										if (activationProperty.getPath().containsName(ActivationType.F_VALID_TO)) {
											validTo = (XMLGregorianCalendar) values.get(0).getValue().getRealValue();
											continue;
										}
									}
								}
							}
						}
					}
				}
				item.add(new Label(componentId, AssignmentsUtil.createActivationTitleModel(administrativeStatus, validFrom, validTo, AssignmentPanel.this).getObject()));
			}
        });
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
//		getModelObject().forEach(a -> a.setSelected(false));
		rowModel.getObject().setSelected(true);
		target.add(AssignmentPanel.this);
	}

	protected void assignmentDetailsPerformed(AjaxRequestTarget target, List<ContainerValueWrapper<AssignmentType>> rowModel) {
		assignmentDetailsVisible = true;
//		getModelObject().forEach(a -> a.setSelected(false));
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
		for (ContainerValueWrapper<AssignmentType> assignmentContainerWrapper : getModelObject().getValues()){
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
