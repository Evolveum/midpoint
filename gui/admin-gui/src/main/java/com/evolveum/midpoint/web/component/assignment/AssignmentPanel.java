/*
 * Copyright (c) 2018 Evolveum
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

import java.util.*;

import com.evolveum.midpoint.gui.api.component.AssignmentPopup;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.users.component.AllAssignmentsPreviewDialog;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

public class AssignmentPanel extends BasePanel<ContainerWrapper<AssignmentType>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(AssignmentPanel.class);

	private static final String ID_ASSIGNMENTS = "assignments";
	protected static final String ID_SEARCH_FRAGMENT = "searchFragment";
	protected static final String ID_SPECIFIC_CONTAINERS_FRAGMENT = "specificContainersFragment";
	private final static String ID_ACTIVATION_PANEL = "activationPanel";
	protected static final String ID_SPECIFIC_CONTAINER = "specificContainers";
	protected static final String ID_TYPE_SWITCH_PANEL = "typeSwitchPanel";
	protected static final String ID_TYPE_LINK = "typeLink";
	private static final String ID_BUTTON_TOOLBAR_FRAGMENT = "buttonToolbarFragment";
	private static final String ID_NEW_ITEM_BUTTON = "newItemButton";
	private static final String ID_SHOW_ALL_ASSIGNMENTS_BUTTON = "showAllAssignmentsButton";

//	private LoadableModel<QName> assignmentTypeModel;

	private static final String DOT_CLASS = AssignmentPanel.class.getName() + ".";
	protected static final String OPERATION_LOAD_ASSIGNMENTS_LIMIT = DOT_CLASS + "loadAssignmentsLimit";

	protected int assignmentsRequestsLimit = -1;
	private List<ContainerValueWrapper<AssignmentType>> detailsPanelAssignmentsList = new ArrayList<>();

	public AssignmentPanel(String id, IModel<ContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
		super(id, assignmentContainerWrapperModel);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		assignmentsRequestsLimit = AssignmentsUtil.loadAssignmentsLimit(new OperationResult(OPERATION_LOAD_ASSIGNMENTS_LIMIT), getPageBase());
//		assignmentTypeModel = new LoadableModel<QName>(true) {
//			@Override
//			protected QName load() {
//				return getAssignmentType();
//			}
//		};
		initLayout();
	}
	
	private void initLayout() {
		List<QName> typesList = getAvailableTypesList();
		ListView<QName> typeSwitchPanel = new ListView<QName>(ID_TYPE_SWITCH_PANEL, Model.ofList(typesList)){
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<QName> item) {
				item.add(createTypeLinkPanel(ID_TYPE_LINK, item.getModel()));
			}
		};
		typeSwitchPanel.setOutputMarkupId(true);
		add(typeSwitchPanel);

		addOrReplaceAssignmentsTablePanel();
		setOutputMarkupId(true);
	}

	private List<QName> getAvailableTypesList(){
		List<QName> typesList = new ArrayList<>();
		typesList.add(null);
		typesList.add(RoleType.COMPLEX_TYPE);
		typesList.add(OrgType.COMPLEX_TYPE);
		typesList.add(ServiceType.COMPLEX_TYPE);
		typesList.add(ResourceType.COMPLEX_TYPE);
		typesList.add(PolicyRuleType.COMPLEX_TYPE);
		return typesList;
	}
	private Component createTypeLinkPanel(String id, IModel<QName> model) {
		AjaxLink<QName> button = new AjaxLink<QName>(id, model) {

			@Override
			public IModel<String> getBody() {
				QName type = model.getObject();
				if (type == null){
					return createStringResource("AssignmentPanel.allLabel");
				}
				return Model.of(model.getObject().getLocalPart());
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				getAssignmentsTabStorage().setType(model.getObject());
				addOrReplaceAssignmentsTablePanel();
				target.add(AssignmentPanel.this);
			}

//			@Override
//			protected void onComponentTag(ComponentTag tag) {
//				super.onComponentTag(tag);
//				QName type = getAssignmentType();
//				if (type == null && model.getObject() == null
//						|| type != null && type.equals(model.getObject())){
//					tag.put("class", "btn active");
//				} else {
//					tag.put("class", "btn btn-default");
//				}
//			}
		};
		button.setOutputMarkupId(true);
		return button;
	}

	private void addOrReplaceAssignmentsTablePanel(){
		MultivalueContainerListPanelWithDetailsPanel<AssignmentType> multivalueContainerListPanel = new MultivalueContainerListPanelWithDetailsPanel<AssignmentType>(ID_ASSIGNMENTS, getModel(), getTableId(),
				getAssignmentsTabStorage()) {

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
			protected void newItemPerformed(AjaxRequestTarget target) {
				newAssignmentClickPerformed(target);
			}

			@Override
			protected boolean isNewObjectButtonEnabled(){
				return !isAssignmentsLimitReached();
			}

			@Override
			protected IModel<String> getNewObjectButtonTitleModel(){
				return getAssignmentsLimitReachedTitleModel("MainObjectListPanel.newObject");
			}

			@Override
			protected void deleteItemPerformed(AjaxRequestTarget target, List<ContainerValueWrapper<AssignmentType>> toDeleteList) {
				int countAddedAssignments = 0;
				for (ContainerValueWrapper<AssignmentType> assignment : toDeleteList) {
					if (ValueStatus.ADDED.equals(assignment.getStatus())){
						countAddedAssignments++;
					}
				}
				boolean isLimitReached = isAssignmentsLimitReached(toDeleteList.size() - countAddedAssignments, true);
				if (isLimitReached) {
					warn(getParentPage().getString("AssignmentPanel.assignmentsLimitReachedWarning", assignmentsRequestsLimit));
					target.add(getPageBase().getFeedbackPanel());
					return;
				}
				super.deleteItemPerformed(target, toDeleteList);
			}

			@Override
			protected List<ContainerValueWrapper<AssignmentType>> postSearch(
					List<ContainerValueWrapper<AssignmentType>> assignments) {
				return customPostSearch(assignments);
			}

			@Override
			protected MultivalueContainerDetailsPanel<AssignmentType> getMultivalueContainerDetailsPanel(
					ListItem<ContainerValueWrapper<AssignmentType>> item) {
				return createMultivalueContainerDetailsPanel(item);
			}

			@Override
			protected WebMarkupContainer getSearchPanel(String contentAreaId) {
				return getCustomSearchPanel(contentAreaId);
			}

			@Override
			protected List<SearchItemDefinition> initSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
				return createSearchableItems(containerDef);
			}

			@Override
			protected WebMarkupContainer initButtonToolbar(String id) {
				WebMarkupContainer buttonToolbar = initCustomButtonToolbar(id);
				if(buttonToolbar == null) {
					return super.initButtonToolbar(id);
				}
				return buttonToolbar;
			}

		};
		multivalueContainerListPanel.setOutputMarkupId(true);
		//we need to initialize type specific columns after each panel reload
		//because columns list depends on the assignment type selected
		addOrReplace(multivalueContainerListPanel);
	}

	protected Fragment initCustomButtonToolbar(String contentAreaId){
		Fragment searchContainer = new Fragment(contentAreaId, ID_BUTTON_TOOLBAR_FRAGMENT, this);

		AjaxIconButton newObjectIcon = getMultivalueContainerListPanel().getNewItemButton(ID_NEW_ITEM_BUTTON);
		searchContainer.add(newObjectIcon);

		AjaxIconButton showAllAssignmentsButton = new AjaxIconButton(ID_SHOW_ALL_ASSIGNMENTS_BUTTON, new Model<>("fa fa-address-card"),
				createStringResource("AssignmentTablePanel.menu.showAllAssignments")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget ajaxRequestTarget) {
				showAllAssignments(ajaxRequestTarget);
			}
		};
		searchContainer.addOrReplace(showAllAssignmentsButton);
		showAllAssignmentsButton.setOutputMarkupId(true);
		showAllAssignmentsButton.add(new VisibleEnableBehaviour(){

			private static final long serialVersionUID = 1L;

			public boolean isVisible(){
				return showAllAssignmentsVisible();
			}
		});
		return searchContainer;
	}

	protected boolean showAllAssignmentsVisible(){
		return true;
	}

	protected void showAllAssignments(AjaxRequestTarget target) {
		PageBase pageBase = getPageBase();
		List<AssignmentInfoDto> previewAssignmentsList;
		if (pageBase instanceof PageAdminFocus) {
			previewAssignmentsList = ((PageAdminFocus<?>) pageBase).showAllAssignmentsPerformed(target);
		} else {
			previewAssignmentsList = Collections.emptyList();
		}
		AllAssignmentsPreviewDialog assignmentsDialog = new AllAssignmentsPreviewDialog(pageBase.getMainPopupBodyId(), previewAssignmentsList,
				pageBase);
		pageBase.showMainPopup(assignmentsDialog, target);
	}

	protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
		List<SearchItemDefinition> defs = new ArrayList<>();

		if (getAssignmentType() == null) {
			SearchFactory.addSearchRefDef(containerDef, new ItemPath(AssignmentType.F_TARGET_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
		}
		SearchFactory.addSearchRefDef(containerDef, new ItemPath(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
		SearchFactory.addSearchPropertyDef(containerDef, new ItemPath(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), defs);
		SearchFactory.addSearchPropertyDef(containerDef, new ItemPath(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), defs);

		defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));

		return defs;
	}

	protected void initCustomPaging(){
		getAssignmentsTabStorage().setPaging(ObjectPaging.createPaging(0, (int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE)));
	}

	private QName getAssignmentType(){
		return getAssignmentsTabStorage().getType();
	}

	protected ObjectTabStorage getAssignmentsTabStorage(){
        return getParentPage().getSessionStorage().getAssignmentsTabStorage();
    }
	
	protected List<ContainerValueWrapper<AssignmentType>> customPostSearch(List<ContainerValueWrapper<AssignmentType>> assignments) {
		return assignments;
	}

	protected ObjectQuery createObjectQuery() {
		Collection<QName> delegationRelations = getParentPage().getRelationRegistry()
				.getAllRelationsFor(RelationKindType.DELEGATION);
		ObjectFilter deputyFilter = QueryBuilder.queryFor(AssignmentType.class, getParentPage().getPrismContext())
				.item(new ItemPath(AssignmentType.F_TARGET_REF))
				.ref(delegationRelations.toArray(new QName[0]))
				.buildFilter();

		QName targetType = getAssignmentType();
		RefFilter targetRefFilter = null;
		if (targetType != null){
			ObjectReferenceType ort = new ObjectReferenceType();
			ort.setType(getAssignmentType());
			ort.setRelation(new QName("http://prism.evolveum.com/xml/ns/public/query-3", "any"));
			targetRefFilter = (RefFilter) QueryBuilder.queryFor(AssignmentType.class, getParentPage().getPrismContext())
					.item(new ItemPath(AssignmentType.F_TARGET_REF))
					.ref(ort.asReferenceValue())
					.buildFilter();
			targetRefFilter.setOidNullAsAny(true);
		}
		ObjectQuery query = QueryBuilder.queryFor(AssignmentType.class, getParentPage().getPrismContext())
				.not()
				.exists(AssignmentType.F_POLICY_RULE)
				.build();
		query.addFilter(NotFilter.createNot(deputyFilter));
		query.addFilter(targetRefFilter);
		return query;
	}

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
            	String name = AssignmentsUtil.getName(rowModel.getObject(), getParentPage());
           		if (StringUtils.isBlank(name)) {
            		return createStringResource("AssignmentPanel.noName");
            	}
            	return Model.of(name);
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
            	getMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
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
        columns.addAll(ColumnUtils.getTypeSpecificAssignmentColumns(getAssignmentType(), getPageBase()));
        List<InlineMenuItem> menuActionsList = getAssignmentMenuActions();
		columns.add(new InlineMenuButtonColumn<ContainerValueWrapper<AssignmentType>>(menuActionsList, getPageBase()){
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean isButtonMenuItemEnabled(IModel<ContainerValueWrapper<AssignmentType>> rowModel){
				if (rowModel != null
						&& ValueStatus.ADDED.equals(rowModel.getObject().getStatus())) {
					return true;
				}
				return !isAssignmentsLimitReached();
			}
		});
        return columns;
	}

	protected List<IColumn<ContainerValueWrapper<AssignmentType>, String>> initColumns(){
		return new ArrayList<>();
	}

	protected void newAssignmentClickPerformed(AjaxRequestTarget target) {
		AssignmentPopup popupPanel = new AssignmentPopup(getPageBase().getMainPopupBodyId()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void addPerformed(AjaxRequestTarget target, List newAssignmentsList) {
				super.addPerformed(target, newAssignmentsList);
				addSelectedAssignmentsPerformed(target, newAssignmentsList);
			}

			@Override
			protected List<ObjectTypes> getAvailableObjectTypesList(){
				return null;//getObjectTypesList();
			}

			@Override
			protected <F extends FocusType> PrismObject<F> getTargetedObject() {
				ObjectWrapper<F> w = AssignmentPanel.this.getModelObject().getObjectWrapper();
				if (w == null) {
					return null;
				}
				return w.getObject();
			}

		};
		popupPanel.setOutputMarkupId(true);
		getPageBase().showMainPopup(popupPanel, target);
	}

	protected void addSelectedAssignmentsPerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList){
		if (newAssignmentsList == null || newAssignmentsList.isEmpty()) {
			warn(getParentPage().getString("AssignmentTablePanel.message.noAssignmentSelected"));
			target.add(getPageBase().getFeedbackPanel());
			return;
		}
		boolean isAssignmentsLimitReached = isAssignmentsLimitReached(newAssignmentsList != null ? newAssignmentsList.size() : 0, true);
		if (isAssignmentsLimitReached) {
			warn(getParentPage().getString("AssignmentPanel.assignmentsLimitReachedWarning", assignmentsRequestsLimit));
			target.add(getPageBase().getFeedbackPanel());
			return;
		}

		newAssignmentsList.forEach(assignment -> {
			PrismContainerDefinition<AssignmentType> definition = getModelObject().getItem().getDefinition();
			PrismContainerValue<AssignmentType> newAssignment;
			try {
				newAssignment = definition.instantiate().createNewValue();
				AssignmentType assignmentType = newAssignment.asContainerable();

				if (assignment.getConstruction() != null && assignment.getConstruction().getResourceRef() != null) {
					assignmentType.setConstruction(assignment.getConstruction());
				} else {
					assignmentType.setTargetRef(assignment.getTargetRef());
				}
				getMultivalueContainerListPanel().createNewItemContainerValueWrapper(newAssignment, getModel());
				getMultivalueContainerListPanel().refreshTable(target);
				getMultivalueContainerListPanel().reloadSavePreviewButtons(target);
			} catch (SchemaException e) {
				getSession().error("Cannot create new assignment " + e.getMessage());
				target.add(getPageBase().getFeedbackPanel());
				target.add(this);
			}

		});


	}

	protected WebMarkupContainer getCustomSearchPanel(String contentAreaId) {
		return new WebMarkupContainer(contentAreaId);
	}
	
	private MultivalueContainerDetailsPanel<AssignmentType> createMultivalueContainerDetailsPanel(
			ListItem<ContainerValueWrapper<AssignmentType>> item) {
		if (isAssignmentsLimitReached()){
			item.getModelObject().setReadonly(true, true);
		} else if (item.getModelObject().isReadonly()){
			item.getModelObject().setReadonly(false, true);
		}
		MultivalueContainerDetailsPanel<AssignmentType> detailsPanel = new  MultivalueContainerDetailsPanel<AssignmentType>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected ItemVisibility getBasicTabVisibity(ItemWrapper itemWrapper, ItemPath parentAssignmentPath) {
				return AssignmentPanel.this.getBasicTabVisibity(itemWrapper, parentAssignmentPath, item.getModel());
			}
			
			@Override
			protected void addBasicContainerValuePanel(String idPanel) {
				add(getBasicContainerPanel(idPanel, item.getModel()));
			}

			@Override
			protected  Fragment getSpecificContainers(String contentAreaId) {
				Fragment specificContainers = getCustomSpecificContainers(contentAreaId, item.getModelObject());
				Form form = this.findParent(Form.class);
				
				ItemPath assignmentPath = item.getModelObject().getContainerValue().getValue().asPrismContainerValue().getPath();
				ContainerWrapperFromObjectWrapperModel<ActivationType, FocusType> activationModel = new ContainerWrapperFromObjectWrapperModel<ActivationType, FocusType>(((PageAdminObjectDetails<FocusType>)getPageBase()).getObjectModel(), assignmentPath.append(AssignmentType.F_ACTIVATION));
				PrismContainerPanel<ActivationType> acitvationContainer = new PrismContainerPanel<ActivationType>(ID_ACTIVATION_PANEL, Model.of(activationModel), true, form, itemWrapper -> getActivationVisibileItems(itemWrapper.getPath(), assignmentPath), getPageBase());
				specificContainers.add(acitvationContainer);
				
				return specificContainers;
			}

			@Override
			protected DisplayNamePanel<AssignmentType> createDisplayNamePanel(String displayNamePanelId) {
				IModel<AssignmentType> displayNameModel = getDisplayModel(item.getModelObject().getContainerValue().getValue());
				return new DisplayNamePanel<AssignmentType>(displayNamePanelId, displayNameModel) {
		    		
					private static final long serialVersionUID = 1L;

					@Override
					protected QName getRelation() {
			    		return getRelationForDisplayNamePanel(item.getModelObject());
					}

					@Override
					protected IModel<String> getKindIntentLabelModel() {
						return getKindIntentLabelModelForDisplayNamePanel(item.getModelObject());
					}

				};
			}
		
		};
		return detailsPanel;
	}
	
	private ItemVisibility getBasicTabVisibity(ItemWrapper itemWrapper, ItemPath parentAssignmentPath, IModel<ContainerValueWrapper<AssignmentType>> model) {
		PrismContainerValue<AssignmentType> prismContainerValue = model.getObject().getContainerValue();
		ItemPath assignmentPath = model.getObject().getContainerValue().getValue().asPrismContainerValue().getPath();
		return getAssignmentBasicTabVisibity(itemWrapper, parentAssignmentPath, assignmentPath, prismContainerValue);
	}

	protected ContainerValuePanel getBasicContainerPanel(String idPanel, IModel<ContainerValueWrapper<AssignmentType>>  model) {
		Form form = new Form<>("form");
    	ItemPath itemPath = getModelObject().getPath();
    	model.getObject().getContainer().setShowOnTopLevel(true);
		return new ContainerValuePanel<AssignmentType>(idPanel, model, true, form,
				itemWrapper -> getBasicTabVisibity(itemWrapper, itemPath, model), getPageBase());
	}

	private QName getRelationForDisplayNamePanel(ContainerValueWrapper<AssignmentType> modelObject) {
		AssignmentType assignment = modelObject.getContainerValue().getValue();
		if (assignment.getTargetRef() != null) {
			return assignment.getTargetRef().getRelation();
		} else {
			return null;
		}
	}
	
	private IModel<String> getKindIntentLabelModelForDisplayNamePanel(ContainerValueWrapper<AssignmentType> modelObject) {
		AssignmentType assignment = modelObject.getContainerValue().getValue();
		if (assignment.getConstruction() != null){
			return createStringResource("DisplayNamePanel.kindIntentLabel", assignment.getConstruction().getKind(),
					assignment.getConstruction().getIntent());
		}
		return Model.of();
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

	protected Fragment getCustomSpecificContainers(String contentAreaId, ContainerValueWrapper<AssignmentType> modelObject) {
		Fragment specificContainers = new Fragment(contentAreaId, AssignmentPanel.ID_SPECIFIC_CONTAINERS_FRAGMENT, this);
		specificContainers.add(getSpecificContainerPanel(modelObject));
		return specificContainers;
	}

	protected PrismContainerPanel getSpecificContainerPanel(ContainerValueWrapper<AssignmentType> modelObject) {
		Form form = new Form<>("form");
		ItemPath assignmentPath = modelObject.getPath();
		PrismContainerPanel constraintsContainerPanel = new PrismContainerPanel(ID_SPECIFIC_CONTAINER,
				getSpecificContainerModel(modelObject), false, form,
				itemWrapper -> getSpecificContainersItemsVisibility(itemWrapper, assignmentPath), getPageBase());
		constraintsContainerPanel.setOutputMarkupId(true);
		return constraintsContainerPanel;
	}
	
	protected ItemVisibility getSpecificContainersItemsVisibility(ItemWrapper itemWrapper, ItemPath parentAssignmentPath) {
		if(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_ATTRIBUTE).append(ResourceAttributeDefinitionType.F_INBOUND).removeIdentifiers().isSubPathOrEquivalent(itemWrapper.getPath().removeIdentifiers())
				|| parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_ASSOCIATION).append(ResourceAttributeDefinitionType.F_INBOUND).removeIdentifiers().isSubPathOrEquivalent(itemWrapper.getPath().removeIdentifiers())) {
			return ItemVisibility.HIDDEN;
		}
		if (ContainerWrapper.class.isAssignableFrom(itemWrapper.getClass())){
			return ItemVisibility.AUTO;
		}
		List<ItemPath> pathsToHide = new ArrayList<>();
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_RESOURCE_REF).removeIdentifiers());
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_AUXILIARY_OBJECT_CLASS).removeIdentifiers());
		if (PropertyOrReferenceWrapper.class.isAssignableFrom(itemWrapper.getClass()) && !WebComponentUtil.isItemVisible(pathsToHide, itemWrapper.getPath().removeIdentifiers())) {
			return ItemVisibility.AUTO;
		} else {
			return ItemVisibility.HIDDEN;
		}
	}

	protected IModel<ContainerWrapper> getSpecificContainerModel(ContainerValueWrapper<AssignmentType> modelObject) {
		if (ConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(modelObject.getContainerValue().getValue()))) {
			ContainerWrapper<ConstructionType> constructionWrapper = modelObject.findContainerWrapper(new ItemPath(modelObject.getPath(),
					AssignmentType.F_CONSTRUCTION));

			return Model.of(constructionWrapper);
		}

		if (PersonaConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(modelObject.getContainerValue().getValue()))) {
			ContainerWrapper<PolicyRuleType> personasWrapper = modelObject.findContainerWrapper(new ItemPath(modelObject.getPath(),
					AssignmentType.F_PERSONA_CONSTRUCTION));

			return Model.of(personasWrapper);
		}
		return Model.of();
	}

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
	
	private <C extends Containerable> IModel<C> getDisplayModel(AssignmentType assignment){
		final AbstractReadOnlyModel<C> displayNameModel = new AbstractReadOnlyModel<C>() {

    		private static final long serialVersionUID = 1L;

			@Override
    		public C getObject() {
    			if (assignment.getTargetRef() != null) {
    				Task task = getPageBase().createSimpleTask("Load target");
    				com.evolveum.midpoint.schema.result.OperationResult result = task.getResult();
    				PrismObject<ObjectType> targetObject = WebModelServiceUtils.loadObject(assignment.getTargetRef(), getPageBase(), task, result);
    				return targetObject != null ? (C) targetObject.asObjectable() : null;
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

	private List<InlineMenuItem> getAssignmentMenuActions() {
		List<InlineMenuItem> menuItems = new ArrayList<>();
		PrismObject obj = getMultivalueContainerListPanel().getFocusObject();
		try {
			boolean isUnassignAuthorized = getParentPage().isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI,
					AuthorizationPhaseType.REQUEST, obj,
					null, null, null);
			if (isUnassignAuthorized) {
				menuItems.add(new ButtonInlineMenuItem(getAssignmentsLimitReachedTitleModel("pageAdminFocus.menu.unassign")) {
					private static final long serialVersionUID = 1L;

					@Override
                    public String getButtonIconCssClass() {
						return GuiStyleConstants.CLASS_DELETE_MENU_ITEM;
					}

					@Override
                    public InlineMenuItemAction initAction() {
						return getMultivalueContainerListPanel().createDeleteColumnAction();
					}
				});
			}

		} catch (Exception ex){
			LOGGER.error("Couldn't check unassign authorization for the object: {}, {}", obj.getName(), ex.getLocalizedMessage());
			if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI)){
				menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.unassign")) {
					private static final long serialVersionUID = 1L;

					@Override
                    public String getButtonIconCssClass() {
						return GuiStyleConstants.CLASS_DELETE_MENU_ITEM;
					}

					@Override
                    public InlineMenuItemAction initAction() {
						return getMultivalueContainerListPanel().createDeleteColumnAction();
					}
				});
			}
		}
		menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
			private static final long serialVersionUID = 1L;

			@Override
            public String getButtonIconCssClass() {
				return GuiStyleConstants.CLASS_EDIT_MENU_ITEM;
			}

			@Override
            public InlineMenuItemAction initAction() {
				return getMultivalueContainerListPanel().createEditColumnAction();
			}
		});
		return menuItems;
	}

	protected MultivalueContainerListPanelWithDetailsPanel<AssignmentType> getMultivalueContainerListPanel() {
		return ((MultivalueContainerListPanelWithDetailsPanel<AssignmentType>)get(ID_ASSIGNMENTS));
	}
	
	protected MultivalueContainerDetailsPanel<AssignmentType> getMultivalueContainerDetailsPanel() {
		return ((MultivalueContainerDetailsPanel<AssignmentType>)get(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS));
	}

	protected TableId getTableId() {
		return UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE;
	}

	protected WebMarkupContainer getAssignmentContainer() {
		return getMultivalueContainerListPanel().getItemContainer();
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
					validFrom, validTo, getMultivalueContainerListPanel());
		}

	}

	private IModel<String> getAssignmentsLimitReachedTitleModel(String defaultTitleKey) {
		return new LoadableModel<String>(true) {
			@Override
			protected String load() {
				return isAssignmentsLimitReached() ?
						AssignmentPanel.this.getPageBase().createStringResource("RoleCatalogItemButton.assignmentsLimitReachedTitle",
								assignmentsRequestsLimit).getString() : createStringResource(defaultTitleKey).getString();
			}
		};
	}

	protected boolean isAssignmentsLimitReached() {
		return isAssignmentsLimitReached(0, false);
	}

	protected boolean isAssignmentsLimitReached(int selectedAssignmentsCount, boolean actionPerformed) {
		if (assignmentsRequestsLimit < 0){
			return false;
		}
		int changedItems = 0;
		List<ContainerValueWrapper<AssignmentType>> assignmentsList = getModelObject().getValues();
		for (ContainerValueWrapper assignment : assignmentsList){
			if (assignment.hasChanged()){
				changedItems++;
			}
		}
		return actionPerformed ? (changedItems + selectedAssignmentsCount) > assignmentsRequestsLimit :
				(changedItems + selectedAssignmentsCount)  >= assignmentsRequestsLimit;
	}

}
