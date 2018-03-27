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
package com.evolveum.midpoint.gui.api.component;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreeAssignablePanel;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.assignment.RelationTypes;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

public class TypedAssignablePanel<T extends ObjectType> extends BasePanel<T> implements Popupable{

	private static final long serialVersionUID = 1L;

	private static final String ID_TYPE = "type";
	private static final String ID_RELATION = "relation";
	private static final String ID_KIND = "kind";
	private static final String ID_INTENT = "intent";
	private static final String ID_RELATION_CONTAINER = "relationContainer";
	private static final String ID_KIND_CONTAINER = "kindContainer";
	private static final String ID_INTENT_CONTAINER = "intentContainer";
	private static final String ID_ROLE_TABLE = "roleTable";
	private static final String ID_RESOURCE_TABLE = "resourceTable";
	private static final String ID_ORG_TABLE = "orgTable";
	private static final String ID_ORG_TREE_VIEW = "orgTreeView";
	private static final String ID_ORG_TREE_VIEW_CONTAINER = "orgTreeViewContainer";
	private static final String ID_ORG_TREE_VIEW_PANEL = "orgTreeViewPanel";

	private static final String ID_SELECTED_ROLES = "rolesSelected";
	private static final String ID_SELECTED_RESOURCES = "resourcesSelected";
	private static final String ID_SELECTED_ORGS = "orgSelected";

	private static final String ID_TABLES_CONTAINER = "tablesContainer";
	private static final String ID_COUNT_CONTAINER = "countContainer";
	private static final String ID_SERVICE_TABLE = "serviceTable";
	private static final String ID_SELECTED_SERVICES = "servicesSelected";

	private static final String ID_BUTTON_ASSIGN = "assignButton";

    private static final String DOT_CLASS = TypedAssignablePanel.class.getName();
    private static final Trace LOGGER = TraceManager.getTrace(TypedAssignablePanel.class);
    private static final String OPERATION_LOAD_ASSIGNABLE_ROLES = DOT_CLASS + "loadAssignableRoles";

    protected IModel<ObjectTypes> typeModel;
    private IModel<Boolean> orgTreeViewModel;
	private IModel<String> intentValueModel;
	private LoadableModel<List<String>> intentValues;
	String intent;

	public TypedAssignablePanel(String id, final Class<T> type) {
		super(id);
    	typeModel = new LoadableModel<ObjectTypes>(false) {

			private static final long serialVersionUID = 1L;

			@Override
			protected ObjectTypes load() {
				if (type == null) {
					return null;
				}

				return ObjectTypes.getObjectType(type);
			}
		};
    	orgTreeViewModel = Model.of(false);
    	intentValues =  getIntentAvailableValuesModel();
    	intentValueModel = new IModel<String>() {
			@Override
			public String getObject() {
				return intent != null ? intent :
						(intentValues.getObject().size() > 0 ?
						intentValues.getObject().get(0) : "default");
			}

			@Override
			public void setObject(String s) {
				intent = s;
			}

			@Override
			public void detach() {

			}
		};
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		initAssignmentParametersPanel();

		WebMarkupContainer tablesContainer = new WebMarkupContainer(ID_TABLES_CONTAINER);
		tablesContainer.setOutputMarkupId(true);
		add(tablesContainer);

		PopupObjectListPanel<T> listRolePanel = createObjectListPanel(ID_ROLE_TABLE, ID_SELECTED_ROLES, ObjectTypes.ROLE);
		tablesContainer.add(listRolePanel);
		PopupObjectListPanel<T> listResourcePanel = createObjectListPanel(ID_RESOURCE_TABLE, ID_SELECTED_RESOURCES, ObjectTypes.RESOURCE);
		tablesContainer.add(listResourcePanel);
		PopupObjectListPanel<T> listOrgPanel = createObjectListPanel(ID_ORG_TABLE, ID_SELECTED_ORGS, ObjectTypes.ORG);
		tablesContainer.add(listOrgPanel);
		PopupObjectListPanel<T> listServicePanel = createObjectListPanel(ID_SERVICE_TABLE, ID_SELECTED_SERVICES, ObjectTypes.SERVICE);
		tablesContainer.add(listServicePanel);

		OrgTreeAssignablePanel orgTreePanel = new OrgTreeAssignablePanel(
				ID_ORG_TREE_VIEW_PANEL, true, getPageBase()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void assignSelectedOrgPerformed(List<OrgType> selectedOrgs, AjaxRequestTarget target) {
				TypedAssignablePanel.this.assignButtonClicked(target, (List<T>)selectedOrgs);
			}
		};
		orgTreePanel.setOutputMarkupId(true);
		orgTreePanel.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return OrgType.COMPLEX_TYPE.equals(typeModel.getObject().getTypeQName()) && isOrgTreeViewSelected();
			}
		});
		tablesContainer.add(orgTreePanel);

		//todo now it's usually hiden by object list panel - bad layout; need to discuss: if count panel should be visible
		//after org tree panel is added
//		WebMarkupContainer countContainer = createCountContainer();
//		add(countContainer);


		AjaxButton addButton = new AjaxButton(ID_BUTTON_ASSIGN,
				createStringResource("userBrowserDialog.button.addButton")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				TypedAssignablePanel.this.assignButtonClicked(target, new ArrayList<>());
			}
		};

		addButton.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !isOrgTreeViewSelected();
			}
		});

		add(addButton);
	}

	private void assignButtonClicked(AjaxRequestTarget target, List<T> selectedOrgs){
		List<T> selected = getSelectedData(ID_ROLE_TABLE);
		selected.addAll(getSelectedData(ID_RESOURCE_TABLE));
		selected.addAll(getSelectedData(ID_SERVICE_TABLE));
		if (isOrgTreeViewSelected()){
			selected.addAll(selectedOrgs);
		} else {
			selected.addAll(getSelectedData(ID_ORG_TABLE));
		}
		addPerformed(target, selected, getSelectedRelation(), getKind(), getIntent());
	}


	protected void initAssignmentParametersPanel(){
		DropDownChoicePanel<ObjectTypes> typeSelect = new DropDownChoicePanel<>(
				ID_TYPE, typeModel, Model.ofList(getObjectTypesList()), new EnumChoiceRenderer<>());
		typeSelect.getBaseFormComponent().add(new OnChangeAjaxBehavior() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(get(ID_TABLES_CONTAINER));
				target.add(addOrReplace(createCountContainer()));
			}
		});
		typeSelect.setOutputMarkupId(true);
		add(typeSelect);


		WebMarkupContainer relationContainer = new WebMarkupContainer(ID_RELATION_CONTAINER);
		relationContainer.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return TypedAssignablePanel.this.isRelationPanelVisible() &&
						!ResourceType.COMPLEX_TYPE.equals(typeModel.getObject().getTypeQName());
			}

		});
		relationContainer.setOutputMarkupId(true);
		add(relationContainer);

		DropDownChoicePanel<RelationTypes> relationSelector = WebComponentUtil.createEnumPanel(RelationTypes.class, ID_RELATION,
                WebComponentUtil.createReadonlyModelFromEnum(RelationTypes.class), Model.of(RelationTypes.MEMBER), TypedAssignablePanel.this, false);
        relationSelector.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        relationSelector.setOutputMarkupId(true);
        relationSelector.setOutputMarkupPlaceholderTag(true);
		relationContainer.add(relationSelector);

		WebMarkupContainer kindContainer = new WebMarkupContainer(ID_KIND_CONTAINER);
		kindContainer.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return ResourceType.COMPLEX_TYPE.equals(typeModel.getObject().getTypeQName());
			}
		});
		kindContainer.setOutputMarkupId(true);
		add(kindContainer);

		DropDownChoicePanel kindSelector = WebComponentUtil.createEnumPanel(ShadowKindType.class, ID_KIND,
				WebComponentUtil.createReadonlyModelFromEnum(ShadowKindType.class), Model.of(ShadowKindType.ACCOUNT),TypedAssignablePanel.this, false);
		kindSelector.setOutputMarkupId(true);
		kindSelector.getBaseFormComponent().add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled(){
				return ResourceType.COMPLEX_TYPE.equals(typeModel.getObject().getTypeQName()) && getSelectedResourceCount() > 0;
			}
		});
		kindSelector.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(TypedAssignablePanel.this);
			}
		});
		kindSelector.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		kindSelector.setOutputMarkupPlaceholderTag(true);
		kindContainer.add(kindSelector);

		WebMarkupContainer intentContainer = new WebMarkupContainer(ID_INTENT_CONTAINER);
		intentContainer.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return ResourceType.COMPLEX_TYPE.equals(typeModel.getObject().getTypeQName());
			}
		});
		intentContainer.setOutputMarkupId(true);
		add(intentContainer);

		DropDownChoicePanel intentSelector = new DropDownChoicePanel(ID_INTENT,
				intentValueModel, intentValues);
		intentSelector.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		intentSelector.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(TypedAssignablePanel.this);
			}
		});
		intentSelector.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
		intentSelector.setOutputMarkupId(true);
		intentSelector.setOutputMarkupPlaceholderTag(true);
		intentSelector.getBaseFormComponent().add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
				return ResourceType.COMPLEX_TYPE.equals(typeModel.getObject().getTypeQName()) && getSelectedResourceCount() > 0;
            }
        });
		intentContainer.add(intentSelector);

        WebMarkupContainer orgTreeViewContainer = new WebMarkupContainer(ID_ORG_TREE_VIEW_CONTAINER);
		orgTreeViewContainer.setOutputMarkupId(true);
		orgTreeViewContainer.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				boolean res= OrgType.COMPLEX_TYPE.equals(typeModel.getObject().getTypeQName());
				return res;
			}
		});
		add(orgTreeViewContainer);

		CheckBox orgTreeViewCheckbox = new CheckBox(ID_ORG_TREE_VIEW, orgTreeViewModel);
		orgTreeViewCheckbox.add(new AjaxEventBehavior("change") {
			@Override
			protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
				orgTreeViewModel.setObject(!orgTreeViewModel.getObject());
				ajaxRequestTarget.add(TypedAssignablePanel.this);
			}
		});
		orgTreeViewCheckbox.setOutputMarkupId(true);
		orgTreeViewContainer.add(orgTreeViewCheckbox);

	}

	private boolean isOrgTreeViewSelected(){
		CheckBox checkPanel = (CheckBox) TypedAssignablePanel.this.get(ID_ORG_TREE_VIEW_CONTAINER).get(ID_ORG_TREE_VIEW);
		return checkPanel.getModel().getObject();
	}

	private List<T> getSelectedData(String id){
		return ((ObjectListPanel) get(createComponentPath(ID_TABLES_CONTAINER, id))).getSelectedObjects();
	}

	private QName getSelectedRelation(){
		DropDownChoicePanel<RelationTypes> relationPanel = (DropDownChoicePanel<RelationTypes>) get(ID_RELATION_CONTAINER).get(ID_RELATION);
		RelationTypes relation = relationPanel.getModel().getObject();
		if (relation == null) {
			return SchemaConstants.ORG_DEFAULT;
		}
		return relation.getRelation();
	}

	private ShadowKindType getKind(){
		DropDownChoicePanel<ShadowKindType> kindPanel = getKindDropdownComponent();
		ShadowKindType kind = kindPanel.getModel().getObject();
		if (kind == null) {
			return ShadowKindType.ACCOUNT;
		}
		return kind;
	}

	private String getIntent(){
		DropDownChoicePanel<String> intentPanel = getIntentDropdownComponent();
		String intent = intentPanel.getBaseFormComponent().getModelObject();
		return intent == null  ? "default" : intent;
	}

	private WebMarkupContainer createCountContainer(){
		WebMarkupContainer countContainer = new WebMarkupContainer(ID_COUNT_CONTAINER);
		countContainer.setOutputMarkupId(true);
		countContainer.add(createCountLabel(ID_SELECTED_ORGS, (PopupObjectListPanel<T>)get(createComponentPath(ID_TABLES_CONTAINER, ID_ORG_TABLE))));
		countContainer.add(createCountLabel(ID_SELECTED_RESOURCES, (PopupObjectListPanel<T>)get(createComponentPath(ID_TABLES_CONTAINER, ID_RESOURCE_TABLE))));
		countContainer.add(createCountLabel(ID_SELECTED_ROLES, (PopupObjectListPanel<T>)get(createComponentPath(ID_TABLES_CONTAINER, ID_ROLE_TABLE))));
		countContainer.add(createCountLabel(ID_SELECTED_SERVICES, (PopupObjectListPanel<T>)get(createComponentPath(ID_TABLES_CONTAINER, ID_SERVICE_TABLE))));
		return countContainer;
	}

	private Label  createCountLabel(String id, ObjectListPanel panel){
		Label label = new Label(id, panel.getSelectedObjects().size());
		label.setOutputMarkupId(true);
		return label;
	}

	protected void onClick(AjaxRequestTarget target, T focus) {
		getPageBase().hideMainPopup(target);
	}

	private void refreshCounts(AjaxRequestTarget target) {
//		addOrReplace(createCountContainer());
//		target.add(get(ID_COUNT_CONTAINER));
	}

	private PopupObjectListPanel<T> createObjectListPanel(String id, final String countId, final ObjectTypes type) {
		PopupObjectListPanel<T> listPanel = new PopupObjectListPanel<T>(id, (Class) type.getClassDefinition(), true, getPageBase()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdateCheckbox(AjaxRequestTarget target) {
				if (type.equals(ObjectTypes.RESOURCE)) {
					target.add(TypedAssignablePanel.this);
				}
				refreshCounts(target);
			}

			@Override
			protected IModel<Boolean> getCheckBoxEnableModel(IModel<SelectableBean<T>> rowModel){
				if (type.equals(ObjectTypes.RESOURCE)) {
					return new LoadableModel<Boolean>() {
						private static final long serialVersionUID = 1L;

						@Override
						protected Boolean load() {
							return getSelectedResourceCount() == 0 || (rowModel != null && rowModel.getObject().isSelected());
						}
					};

				} else {
					return Model.of(true);
				}
			}

            @Override
            protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
                if (type.equals(RoleType.COMPLEX_TYPE)) {
                    LOGGER.debug("Loading roles which the current user has right to assign");
                    Task task = TypedAssignablePanel.this.getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNABLE_ROLES);
                    OperationResult result = task.getResult();
                    ObjectFilter filter = null;
                    try {
                        ModelInteractionService mis = TypedAssignablePanel.this.getPageBase().getModelInteractionService();
                        RoleSelectionSpecification roleSpec =
                                mis.getAssignableRoleSpecification(SecurityUtils.getPrincipalUser().getUser().asPrismObject(), task, result);
                        filter = roleSpec.getFilter();
                    } catch (Exception ex) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load available roles", ex);
                        result.recordFatalError("Couldn't load available roles", ex);
                    } finally {
                        result.recomputeStatus();
                    }
                    if (!result.isSuccess() && !result.isHandledError()) {
                    	TypedAssignablePanel.this.getPageBase().showResult(result);
                    }
                    if (query == null){
                        query = new ObjectQuery();
                    }
                    query.addFilter(filter);
                }
                return query;
            }

		};

		listPanel.setOutputMarkupId(true);
		listPanel.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				if (typeModel.getObject().getTypeQName().equals(OrgType.COMPLEX_TYPE)){
					return type.equals(typeModel.getObject()) && !isOrgTreeViewSelected();
				}
				return type.equals(typeModel.getObject());
			}
		});
		return listPanel;
	}

	protected boolean isRelationPanelVisible() {
		return true;
	}

	protected void addPerformed(AjaxRequestTarget target, List<T> selected, QName relation, ShadowKindType kind, String intent) {
		getPageBase().hideMainPopup(target);
	}

	@Override
	public int getWidth() {
		return 900;
	}

	@Override
	public int getHeight() {
		return 500;
	}

	@Override
	public StringResourceModel getTitle() {
		return PageBase.createStringResourceStatic(TypedAssignablePanel.this, "TypedAssignablePanel.selectObjects");
	}

	@Override
	public Component getComponent() {
		return this;
	}

	private LoadableModel<List<String>> getIntentAvailableValuesModel(){
		return new LoadableModel<List<String>>(true){
			private static final long serialVersionUID = 1L;

			@Override
			protected List<String> load(){
				List<String> availableIntentValues = new ArrayList<>();
				if (getResourceTable() != null) {
					List<T> selectedResources = getResourceTable().getSelectedObjects();
					if (selectedResources != null && selectedResources.size() > 0) {
						ResourceType selectedResource = (ResourceType) selectedResources.get(0);

						try {
							RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(selectedResource.asPrismObject());
							if (refinedSchema != null) {
								ShadowKindType kind = (ShadowKindType) TypedAssignablePanel.this.getKindDropdownComponent().getBaseFormComponent().getModelObject();
								List<? extends RefinedObjectClassDefinition> definitions = refinedSchema.getRefinedDefinitions(kind);
								for (RefinedObjectClassDefinition def : definitions) {
									availableIntentValues.add(def.getIntent());
								}
							}
						} catch (SchemaException ex) {
							LOGGER.error("Cannot get refined resource schema for resource {}. {}", selectedResource.getName().getOrig(), ex.getLocalizedMessage());
						}

					}
				}
				return availableIntentValues;
			}
		};
	}

	private DropDownChoicePanel getKindDropdownComponent(){
		return (DropDownChoicePanel)get(ID_KIND_CONTAINER).get(ID_KIND);
	}

	private DropDownChoicePanel getIntentDropdownComponent(){
		return (DropDownChoicePanel)get(ID_INTENT_CONTAINER).get(ID_INTENT);
	}

	private PopupObjectListPanel<T> getResourceTable(){
		return (PopupObjectListPanel<T>)get(createComponentPath(ID_TABLES_CONTAINER, ID_RESOURCE_TABLE));
	}
	private int getSelectedResourceCount(){
		return getResourceTable().getSelectedObjectsCount();

	}

	protected List<ObjectTypes> getObjectTypesList(){
		return WebComponentUtil.createAssignableTypesList();
	}
}
