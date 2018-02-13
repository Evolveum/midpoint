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

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreeAssignablePanel;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class TypedAssignablePanel<T extends ObjectType> extends BasePanel<T> implements Popupable{

	private static final long serialVersionUID = 1L;

	private static final String ID_TYPE = "type";
	private static final String ID_RELATION = "relation";
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
		addPerformed(target, selected, getSelectedRelation());
	}


	protected void initAssignmentParametersPanel(){
		DropDownChoicePanel<ObjectTypes> typeSelect = new DropDownChoicePanel<>(
				ID_TYPE, typeModel, Model.ofList(WebComponentUtil.createAssignableTypesList()), new EnumChoiceRenderer<>());
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


		DropDownChoicePanel<RelationTypes> relationSelector = WebComponentUtil.createEnumPanel(RelationTypes.class, ID_RELATION,
                WebComponentUtil.createReadonlyModelFromEnum(RelationTypes.class), Model.of(RelationTypes.MEMBER), TypedAssignablePanel.this, false);
        relationSelector.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        relationSelector.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return !ResourceType.COMPLEX_TYPE.equals(typeModel.getObject().getTypeQName());
            }

            @Override
            public boolean isVisible() {
            	return TypedAssignablePanel.this.isRelationPanelVisible();
            }

        });
        relationSelector.setOutputMarkupId(true);
        relationSelector.setOutputMarkupPlaceholderTag(true);
        add(relationSelector);

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
		DropDownChoicePanel<RelationTypes> relationPanel = (DropDownChoicePanel<RelationTypes>) get(ID_RELATION);
		RelationTypes relation = relationPanel.getModel().getObject();
		if (relation == null) {
			return SchemaConstants.ORG_DEFAULT;
		}
		return relation.getRelation();
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
				refreshCounts(target);
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

	protected void addPerformed(AjaxRequestTarget target, List<T> selected, QName relation) {
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

}
