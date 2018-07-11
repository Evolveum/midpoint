/*
 * Copyright (c) 2010-2018 Evolveum
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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.session.AssignmentsTabStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

public abstract class AssignmentPanel extends BasePanel<ContainerWrapper<AssignmentType>> {

	private static final long serialVersionUID = 1L;

	private static final String ID_ASSIGNMENTS = "assignments";
	protected static final String ID_SEARCH_FRAGMENT = "searchFragment";
	protected static final String ID_SPECIFIC_CONTAINERS_FRAGMENT = "specificContainersFragment";
	private final static String ID_ACTIVATION_PANEL = "activationPanel";
	protected static final String ID_SPECIFIC_CONTAINER = "specificContainers";
//	private static final String ID_NEW_ASSIGNMENT_BUTTON = "newAssignmentButton";
//	private static final String ID_ASSIGNMENTS_TABLE = "assignmentsTable";
//	private static final String ID_ASSIGNMENTS_DETAILS = "assignmentsDetails";
//	private static final String ID_ASSIGNMENT_DETAILS = "assignmentDetails";
//
//	private static final String ID_DETAILS = "details";
//
//	private final static String ID_DONE_BUTTON = "doneButton";
//	private final static String ID_CANCEL_BUTTON = "cancelButton";

	private static final Trace LOGGER = TraceManager.getTrace(AssignmentPanel.class);

	private MultivalueContainerListPanel<AssignmentType> multivalueContainerListPanel;
	private List<ContainerValueWrapper<AssignmentType>> detailsPanelAssignmentsList = new ArrayList<>();

	public AssignmentPanel(String id, IModel<ContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
		super(id, assignmentContainerWrapperModel);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}
	
	private void initLayout() {
		
		this.multivalueContainerListPanel = new MultivalueContainerListPanel<AssignmentType>(ID_ASSIGNMENTS, getModel()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void initPaging() {
				initCustomPaging();
			}

			@Override
			protected boolean enableActionNewObject() {
				try {
					return getParentPage().isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI,
							AuthorizationPhaseType.REQUEST, getFocusObject(),
							null, null, null);
				} catch (Exception ex){
					return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI);
				}
			}

			@Override
			protected ObjectQuery createQuery() {
				return createObjectQuery();
			}

			@Override
			protected List<IColumn<ContainerValueWrapper<AssignmentType>, String>> createColumns() {
				return initBasicColumns();
			}

			@Override
			protected void newAssignmentPerformed(AjaxRequestTarget target) {
				newAssignmentClickPerformed(target);				
			}

			@Override
			protected void createDetailsPanel(WebMarkupContainer assignmentsContainer) {
				initDetailsPanel(assignmentsContainer);
				
			}

			@Override
			protected TableId getTableId() {
				return getCustomTableId();
			}

			@Override
			protected int getItemsPerPage() {
				return getCustomItemsPerPage();
			}

			@Override
			protected List<ContainerValueWrapper<AssignmentType>> postSearch(
					List<ContainerValueWrapper<AssignmentType>> assignments) {
				return customPostSearch(assignments);
			}

			@Override
			protected Fragment getSearchPanel(String contentAreaId) {
				return getCustomSearchPanel(contentAreaId);
			}

			@Override
			protected PageStorage getPageStorage() {
				return getAssignmentsTabStorage();
			}
		};
		
		add(multivalueContainerListPanel);
		
		
//		initListPanel();
//
//		initDetailsPanel();
//
		setOutputMarkupId(true);

	}
	
	protected abstract void initCustomPaging();
	
	protected AssignmentsTabStorage getAssignmentsTabStorage(){
        return getParentPage().getSessionStorage().getAssignmentsTabStorage();
    }
	
//	private void initListPanel() {
//		WebMarkupContainer assignmentsContainer = new WebMarkupContainer(ID_ASSIGNMENTS);
//		assignmentsContainer.setOutputMarkupId(true);
//		add(assignmentsContainer);
//
//		BoxedTablePanel<ContainerValueWrapper<AssignmentType>> assignmentTable = initAssignmentTable();
//		assignmentsContainer.add(assignmentTable);
//
//		AjaxIconButton newObjectIcon = new AjaxIconButton(ID_NEW_ASSIGNMENT_BUTTON, new Model<>("fa fa-plus"),
//				createStringResource("MainObjectListPanel.newObject")) {
//
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				newAssignmentClickPerformed(target);
//			}
//		};
//
//		newObjectIcon.add(new VisibleEnableBehaviour() {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public boolean isVisible() {
//				try {
//					return getParentPage().isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI,
//							AuthorizationPhaseType.REQUEST, getFocusObject(),
//							null, null, null);
//				} catch (Exception ex){
//					return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI);
//				}
//			}
//		});
//		assignmentsContainer.add(newObjectIcon);
//
//		initCustomLayout(assignmentsContainer);
//
//		assignmentsContainer.add(new VisibleEnableBehaviour() {
//
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public boolean isVisible() {
//				return !assignmentDetailsVisible;
//			}
//		});
//
//	}

//	private BoxedTablePanel<ContainerValueWrapper<AssignmentType>> initAssignmentTable() {
//
//		ContainerListDataProvider assignmentsProvider = new ContainerListDataProvider(this, new PropertyModel<>(getModel(), "values")) {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
//				getAssignmentsStorage().setPaging(paging);
//			}
//
//			@Override
//			public ObjectQuery getQuery() {
//				return createObjectQuery();
//			}
//			
//			@Override
//			protected List<ContainerValueWrapper<AssignmentType>> searchThroughList() {
//				List<ContainerValueWrapper<AssignmentType>> resultList = super.searchThroughList();
//				return postSearch(resultList);
//			}
//
//		};
//
//		List<IColumn<ContainerValueWrapper<AssignmentType>, String>> columns = initBasicColumns();
//		List<InlineMenuItem> menuActionsList = getAssignmentMenuActions();
//		columns.add(new InlineMenuButtonColumn<>(menuActionsList, menuActionsList.size(), getPageBase()));
//
//		BoxedTablePanel<ContainerValueWrapper<AssignmentType>> assignmentTable = new BoxedTablePanel<ContainerValueWrapper<AssignmentType>>(ID_ASSIGNMENTS_TABLE,
//				assignmentsProvider, columns, getCustomTableId(), getCustomItemsPerPage()) {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public int getItemsPerPage() {
//				return getPageBase().getSessionStorage().getUserProfile().getTables()
//						.get(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE);
//			}
//
//			@Override
//			protected Item<ContainerValueWrapper<AssignmentType>> customizeNewRowItem(Item<ContainerValueWrapper<AssignmentType>> item,
//																					  IModel<ContainerValueWrapper<AssignmentType>> model) {
//				item.add(AttributeModifier.append("class", new AbstractReadOnlyModel<String>() {
//							@Override
//							public String getObject() {
//								return AssignmentsUtil.createAssignmentStatusClassModel(model.getObject());
//							}
//						}));
//				return item;
//			}
//
//		};
//		assignmentTable.setOutputMarkupId(true);
//		assignmentTable.setCurrentPage(getAssignmentsStorage().getPaging());
//		return assignmentTable;
//
//	}
	
	protected List<ContainerValueWrapper<AssignmentType>> customPostSearch(List<ContainerValueWrapper<AssignmentType>> assignments) {
		return assignments;
	}

	protected AssignmentsTabStorage getAssignmentsStorage() {
		return getPageBase().getSessionStorage().getAssignmentsTabStorage();
	}

	protected abstract ObjectQuery createObjectQuery();

	private List<IColumn<ContainerValueWrapper<AssignmentType>, String>> initBasicColumns() {
		List<IColumn<ContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

		columns.add(new CheckBoxHeaderColumn<>());

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
				item.add(new Label(componentId, getActivationLabelModel(rowModel.getObject())));
			}
        });
        columns.addAll(initColumns());
        List<InlineMenuItem> menuActionsList = getAssignmentMenuActions();
		columns.add(new InlineMenuButtonColumn<>(menuActionsList, menuActionsList.size(), getPageBase()));
        return columns;
	}

	protected abstract List<IColumn<ContainerValueWrapper<AssignmentType>, String>> initColumns();

	protected abstract void newAssignmentClickPerformed(AjaxRequestTarget target);

	protected abstract Fragment getCustomSearchPanel(String contentAreaId);

	private void initDetailsPanel(WebMarkupContainer details) {

		ListView<ContainerValueWrapper<AssignmentType>> assignmentDetailsView = new ListView<ContainerValueWrapper<AssignmentType>>(MultivalueContainerListPanel.ID_ITEMS_DETAILS,
				new AbstractReadOnlyModel<List<ContainerValueWrapper<AssignmentType>>>() {
					private static final long serialVersionUID = 1L;

					@Override
					public List<ContainerValueWrapper<AssignmentType>> getObject() {
						return detailsPanelAssignmentsList;
					}
				}) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<ContainerValueWrapper<AssignmentType>> item) {
				Form form = this.findParent(Form.class);
				MultivalueContainerDetailsPanel<AssignmentType> detailsPanel = new  MultivalueContainerDetailsPanel<AssignmentType>(MultivalueContainerListPanel.ID_ITEM_DETAILS, item.getModel()) {

					@Override
					protected IModel createDisplayNameModel() {
						return getDisplayModel(item.getModelObject().getContainerValue().getValue());
					}

					@Override
					protected QName getRelationForDisplayNamePanel() {
						AssignmentType assignment = getModelObject().getContainerValue().getValue();
						if (assignment.getTargetRef() != null) {
							return assignment.getTargetRef().getRelation();
						} else {
							return null;
						}
					}
					
					@Override
					protected IModel<String> getKindIntentLabelModelForDisplayNamePanel() {
						AssignmentType assignment = getModelObject().getContainerValue().getValue();
						if (assignment.getConstruction() != null){
							return createStringResource("DisplayNamePanel.kindIntentLabel", assignment.getConstruction().getKind(),
									assignment.getConstruction().getIntent());
						}
						return Model.of();
					}
					
					@Override
					protected ItemVisibility getBasicTabVisibity(ItemWrapper itemWrapper, ItemPath parentAssignmentPath) {
						PrismContainerValue<AssignmentType> prismContainerValue = getModelObject().getContainerValue();
						ItemPath assignmentPath = getModelObject().getContainerValue().getValue().asPrismContainerValue().getPath();
						return getAssignmentBasicTabVisibity(itemWrapper, parentAssignmentPath, assignmentPath, prismContainerValue);
					}

					@Override
					protected  Fragment getSpecificContainers(String contentAreaId) {
						Fragment specificContainers = getCustomSpecificContainers(contentAreaId, getModelObject());
						
						ItemPath assignmentPath = getModelObject().getContainerValue().getValue().asPrismContainerValue().getPath();
						ContainerWrapperFromObjectWrapperModel<ActivationType, FocusType> activationModel = new ContainerWrapperFromObjectWrapperModel<>(((PageAdminObjectDetails<FocusType>)getPageBase()).getObjectModel(), assignmentPath.append(AssignmentType.F_ACTIVATION));
						PrismContainerPanel<ActivationType> acitvationContainer = new PrismContainerPanel(ID_ACTIVATION_PANEL, Model.of(activationModel), true, form, itemWrapper -> getActivationVisibileItems(itemWrapper.getPath(), assignmentPath), getPageBase());
						specificContainers.add(acitvationContainer);
						
						return specificContainers;
					}
				
				};
				item.add(detailsPanel);
				detailsPanel.setOutputMarkupId(true);

			}
			

		};

		assignmentDetailsView.setOutputMarkupId(true);
		details.add(assignmentDetailsView);

	}
	
	private ItemVisibility getActivationVisibileItems(ItemPath pathToCheck, ItemPath assignmentPath) {
    	if (assignmentPath.append(new ItemPath(AssignmentType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP)).equivalent(pathToCheck)) {
    		return ItemVisibility.HIDDEN;
    	}
    	
    	if (assignmentPath.append(new ItemPath(AssignmentType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS)).equivalent(pathToCheck)) {
    		return ItemVisibility.HIDDEN;
    	}
    	
    	return ItemVisibility.AUTO;
    }
	
	protected abstract Fragment getCustomSpecificContainers(String contentAreaId, ContainerValueWrapper<AssignmentType> modelObject);
	
	protected PrismContainerPanel getPolicyRuleContainerPanel(ContainerValueWrapper<AssignmentType> modelObject) {
		Form form = new Form<>("form");
		ItemPath assignmentPath = modelObject.getPath();
		PrismContainerPanel<PolicyRuleType> constraintsContainerPanel = new PrismContainerPanel(ID_SPECIFIC_CONTAINER,
				getPolicyRuleContainerModel(modelObject), false, form,
				itemWrapper -> getPolicyRuleContainersItemsVisibility(itemWrapper, assignmentPath), getPageBase());
		constraintsContainerPanel.setOutputMarkupId(true);
		return constraintsContainerPanel;
	}
	
	protected ItemVisibility getPolicyRuleContainersItemsVisibility(ItemWrapper itemWrapper, ItemPath parentAssignmentPath) {
		if (ContainerWrapper.class.isAssignableFrom(itemWrapper.getClass())){
			return ItemVisibility.AUTO;
		}
		List<ItemPath> pathsToHide = new ArrayList<>();
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_RESOURCE_REF));
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_AUXILIARY_OBJECT_CLASS));
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_STRENGTH));
		if (PropertyOrReferenceWrapper.class.isAssignableFrom(itemWrapper.getClass()) && !WebComponentUtil.isItemVisible(pathsToHide, itemWrapper.getPath())) {
			return ItemVisibility.AUTO;
		} else {
			return ItemVisibility.HIDDEN;
		}
	}
	
	protected abstract IModel<ContainerWrapper> getPolicyRuleContainerModel(ContainerValueWrapper<AssignmentType> modelObject);
	
	private ItemVisibility getAssignmentBasicTabVisibity(ItemWrapper itemWrapper, ItemPath parentAssignmentPath, ItemPath assignmentPath, PrismContainerValue<AssignmentType> prismContainerValue) {
		
    	if (itemWrapper.getPath().equals(assignmentPath.append(AssignmentType.F_METADATA))){
    		return ItemVisibility.AUTO;
		}
    	AssignmentType assignment = prismContainerValue.getValue();
		ObjectReferenceType targetRef = assignment.getTargetRef();
		List<ItemPath> pathsToHide = new ArrayList<>();
		QName targetType = null;
		if (targetRef != null) {
			targetType = targetRef.getType();
		}
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_TARGET_REF));
		
		if (OrgType.COMPLEX_TYPE.equals(targetType) || AssignmentsUtil.isPolicyRuleAssignment(prismContainerValue.asContainerable())) {
			pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_TENANT_REF));
			pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_ORG_REF));
		}
		if (AssignmentsUtil.isPolicyRuleAssignment(prismContainerValue.asContainerable())){
			pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_FOCUS_TYPE));
		}
		
		if (assignment.getConstruction() == null) {
			pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION));
		}
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_PERSONA_CONSTRUCTION));
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_POLICY_RULE));
		
		
    	if (PropertyOrReferenceWrapper.class.isAssignableFrom(itemWrapper.getClass()) && !WebComponentUtil.isItemVisible(pathsToHide, itemWrapper.getPath())) {
    		return ItemVisibility.AUTO;
    	} else {
    		return ItemVisibility.HIDDEN;
    	}
    }
	
	private <C extends Containerable> IModel getDisplayModel(AssignmentType assignment){
		final AbstractReadOnlyModel<C> displayNameModel = new AbstractReadOnlyModel<C>() {

    		private static final long serialVersionUID = 1L;

			@Override
    		public C getObject() {
    			if (assignment.getTargetRef() != null) {
    				Task task = getPageBase().createSimpleTask("Load target");
    				com.evolveum.midpoint.schema.result.OperationResult result = task.getResult();
    				return (C) WebModelServiceUtils.loadObject(assignment.getTargetRef(), getPageBase(), task, result).asObjectable();
    			}
    			if (assignment.getConstruction() != null && assignment.getConstruction().getResourceRef() != null) {
					Task task = getPageBase().createSimpleTask("Load resource");
					com.evolveum.midpoint.schema.result.OperationResult result = task.getResult();
					return (C) WebModelServiceUtils.loadObject(assignment.getConstruction().getResourceRef(), getPageBase(), task, result).asObjectable();
    			} else if (assignment.getPersonaConstruction() != null) {
    				return (C) assignment.getPersonaConstruction();
    			} else if (assignment.getPolicyRule() !=null) {
    				return (C) assignment.getPolicyRule();
    			}

    			return null;
    		}

    	};
		return displayNameModel;
	}

//	protected ContainerListDataProvider getAssignmentListProvider() {
//		return (ContainerListDataProvider) getAssignmentTable().getDataTable().getDataProvider();
//	}

//	protected BoxedTablePanel<ContainerValueWrapper<AssignmentType>> getAssignmentTable() {
//		return (BoxedTablePanel<ContainerValueWrapper<AssignmentType>>) get(createComponentPath(ID_ASSIGNMENTS, ID_ASSIGNMENTS_TABLE));
//	}

	protected void createCustomSpecificContainers(WebMarkupContainer specificContainers) {
		
	}
//	protected abstract AbstractAssignmentDetailsPanel createDetailsPanel(String idAssignmentDetails, Form<?> form, IModel<ContainerValueWrapper<AssignmentType>> model);

	private List<ContainerValueWrapper<AssignmentType>> getSelectedAssignments() {
		BoxedTablePanel<ContainerValueWrapper<AssignmentType>> assignemntTable = this.multivalueContainerListPanel.getItemTable();
		MultivalueContainerListDataProvider<AssignmentType> assignmentProvider = (MultivalueContainerListDataProvider<AssignmentType>) assignemntTable.getDataTable()
				.getDataProvider();
		return assignmentProvider.getAvailableData().stream().filter(a -> a.isSelected()).collect(Collectors.toList());
	}

	private List<InlineMenuItem> getAssignmentMenuActions() {
		List<InlineMenuItem> menuItems = new ArrayList<>();
		PrismObject obj = getFocusObject();
		boolean isUnassignMenuAdded = false;
		try {
			boolean isUnassignAuthorized = getParentPage().isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI,
					AuthorizationPhaseType.REQUEST, obj,
					null, null, null);
			if (isUnassignAuthorized) {
				menuItems.add(new InlineMenuItem(createStringResource("PageBase.button.unassign"), new Model<>(true),
						new Model<>(true), false, createDeleteColumnAction(), 0, GuiStyleConstants.CLASS_DELETE_MENU_ITEM,
						DoubleButtonColumn.BUTTON_COLOR_CLASS.DANGER.toString()));
				isUnassignMenuAdded = true;
			}

		} catch (Exception ex){
			LOGGER.error("Couldn't check unassign authorization for the object: {}, {}", obj.getName(), ex.getLocalizedMessage());
			if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI)){
				menuItems.add(new InlineMenuItem(createStringResource("PageBase.button.unassign"), new Model<>(true),
						new Model<>(true), false, createDeleteColumnAction(), 0, GuiStyleConstants.CLASS_DELETE_MENU_ITEM,
						DoubleButtonColumn.BUTTON_COLOR_CLASS.DANGER.toString()));
				isUnassignMenuAdded = true;
			}
		}
		menuItems.add(new InlineMenuItem(createStringResource("PageBase.button.edit"), new Model<>(true),
            new Model<>(true), false, createEditColumnAction(), isUnassignMenuAdded ? 1 : 0, GuiStyleConstants.CLASS_EDIT_MENU_ITEM,
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
		this.multivalueContainerListPanel.setItemDetailsVisible(true);
		detailsPanelAssignmentsList.clear();
		detailsPanelAssignmentsList.add(rowModel.getObject());
		rowModel.getObject().setSelected(false);
		target.add(multivalueContainerListPanel);
	}

	protected void assignmentDetailsPerformed(AjaxRequestTarget target, List<ContainerValueWrapper<AssignmentType>> rowModel) {
		this.multivalueContainerListPanel.setItemDetailsVisible(true);
		detailsPanelAssignmentsList.clear();
		detailsPanelAssignmentsList.addAll(rowModel);
		rowModel.forEach(assignmentTypeContainerValueWrapper -> {
			assignmentTypeContainerValueWrapper.setSelected(false);
		});
		target.add(multivalueContainerListPanel);
	}

	protected abstract TableId getCustomTableId();

	protected abstract int getCustomItemsPerPage();

	protected void refreshTable(AjaxRequestTarget target) {
		this.multivalueContainerListPanel.refreshTable(target);
	}

	protected void deleteAssignmentPerformed(AjaxRequestTarget target, List<ContainerValueWrapper<AssignmentType>> toDelete) {
		if (toDelete == null){
			return;
		}
		toDelete.forEach(value -> {
			if (value.getStatus() == ValueStatus.ADDED) {
				ContainerWrapper wrapper = multivalueContainerListPanel.getModelObject();
				wrapper.getValues().remove(value);
			} else {
				value.setStatus(ValueStatus.DELETED);
			}
			value.setSelected(false);
		});
		refreshTable(target);
		reloadSavePreviewButtons(target);
	}

	protected ContainerValueWrapper<AssignmentType> createNewAssignmentContainerValueWrapper(PrismContainerValue<AssignmentType> newAssignment) {
		ContainerWrapperFactory factory = new ContainerWrapperFactory(getPageBase());
		Task task = getPageBase().createSimpleTask("Creating new assignment");
		ContainerValueWrapper<AssignmentType> valueWrapper = factory.createContainerValueWrapper(getModelObject(), newAssignment,
                getModelObject().getObjectStatus(), ValueStatus.ADDED, getModelObject().getPath(), task);
		valueWrapper.setShowEmpty(true, false);
		getModelObject().getValues().add(valueWrapper);
		return valueWrapper;
	}

	protected WebMarkupContainer getAssignmentContainer() {
		return this.multivalueContainerListPanel.getItemContainer();
	}

	protected PageBase getParentPage() {
		return getPageBase();
	}

	private IModel<String> getActivationLabelModel(ContainerValueWrapper<AssignmentType> assignmentContainer){
		ContainerWrapper<ActivationType> activationContainer = assignmentContainer.findContainerWrapper(assignmentContainer.getPath().append(new ItemPath(AssignmentType.F_ACTIVATION)));
		ActivationStatusType administrativeStatus = null;
		XMLGregorianCalendar validFrom = null;
		XMLGregorianCalendar validTo = null;
		ActivationType activation = null;
		String lifecycleStatus = "";
		PropertyOrReferenceWrapper lifecycleStatusProperty = assignmentContainer.findPropertyWrapper(AssignmentType.F_LIFECYCLE_STATE);
		if (lifecycleStatusProperty != null && lifecycleStatusProperty.getValues() != null){
			Iterator<ValueWrapper> iter = lifecycleStatusProperty.getValues().iterator();
			if (iter.hasNext()){
				lifecycleStatus = (String) iter.next().getValue().getRealValue();
			}
		}
		if (activationContainer != null){
			activation = new ActivationType();
			PropertyOrReferenceWrapper administrativeStatusProperty = activationContainer.findPropertyWrapper(ActivationType.F_ADMINISTRATIVE_STATUS);
			if (administrativeStatusProperty != null && administrativeStatusProperty.getValues() != null){
				Iterator<ValueWrapper> iter = administrativeStatusProperty.getValues().iterator();
				if (iter.hasNext()){
					administrativeStatus = (ActivationStatusType) iter.next().getValue().getRealValue();
					activation.setAdministrativeStatus(administrativeStatus);
				}
			}
			PropertyOrReferenceWrapper validFromProperty = activationContainer.findPropertyWrapper(ActivationType.F_VALID_FROM);
			if (validFromProperty != null && validFromProperty.getValues() != null){
				Iterator<ValueWrapper> iter = validFromProperty.getValues().iterator();
				if (iter.hasNext()){
					validFrom = (XMLGregorianCalendar) iter.next().getValue().getRealValue();
					activation.setValidFrom(validFrom);
				}
			}
			PropertyOrReferenceWrapper validToProperty = activationContainer.findPropertyWrapper(ActivationType.F_VALID_TO);
			if (validToProperty != null && validToProperty.getValues() != null){
				Iterator<ValueWrapper> iter = validToProperty.getValues().iterator();
				if (iter.hasNext()){
					validTo = (XMLGregorianCalendar) iter.next().getValue().getRealValue();
					activation.setValidTo(validTo);
				}
			}
		}
		if (administrativeStatus != null){
			return Model.of(WebModelServiceUtils
					.getAssignmentEffectiveStatus(lifecycleStatus, activation, getPageBase()).value().toLowerCase());
		} else {
			return AssignmentsUtil.createActivationTitleModel(WebModelServiceUtils
							.getAssignmentEffectiveStatus(lifecycleStatus, activation, getPageBase()),
					validFrom, validTo, multivalueContainerListPanel);
		}

	}


	protected void reloadSavePreviewButtons(AjaxRequestTarget target){
		FocusMainPanel mainPanel = this.multivalueContainerListPanel.findParent(FocusMainPanel.class);
		if (mainPanel != null) {
			mainPanel.reloadSavePreviewButtons(target);
		}
	}

	private PrismObject getFocusObject(){
		FocusMainPanel mainPanel = this.multivalueContainerListPanel.findParent(FocusMainPanel.class);
		if (mainPanel != null) {
			return mainPanel.getObjectWrapper().getObject();
		}
		return null;
	}
}
