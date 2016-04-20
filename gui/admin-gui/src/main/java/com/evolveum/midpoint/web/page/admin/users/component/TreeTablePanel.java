/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.web.page.admin.users.component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.FocusBrowserPanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.dialog.ChooseFocusTypeDialogPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreeAssignablePanel;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreePanel;
import com.evolveum.midpoint.web.page.admin.resources.PageResource;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.services.PageService;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTableDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.StringResourceChoiceRenderer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Used as a main component of the Org tree page.
 * 
 * todo create function computeHeight() in midpoint.js, update height properly
 * when in "mobile" mode... [lazyman] todo implement midpoint theme for tree
 * [lazyman]
 *
 * @author lazyman
 */
public class TreeTablePanel extends BasePanel<String> {

	private PageBase parentPage;

	@Override
	public PageBase getPageBase() {
		return parentPage;
	}

	private static final String ID_MANAGER_SUMMARY = "managerSummary";
	private static final String ID_REMOVE_MANAGER = "removeManager";
	private static final String ID_EDIT_MANAGER = "editManager";

	protected static final String DOT_CLASS = TreeTablePanel.class.getName() + ".";
	protected static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";
	protected static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
	protected static final String OPERATION_MOVE_OBJECTS = DOT_CLASS + "moveObjects";
	protected static final String OPERATION_MOVE_OBJECT = DOT_CLASS + "moveObject";
	protected static final String OPERATION_UPDATE_OBJECTS = DOT_CLASS + "updateObjects";
	protected static final String OPERATION_UPDATE_OBJECT = DOT_CLASS + "updateObject";
	protected static final String OPERATION_RECOMPUTE = DOT_CLASS + "recompute";
	protected static final String OPERATION_SEARCH_MANAGERS = DOT_CLASS + "searchManagers";

	private static final String ID_TREE_PANEL = "treePanel";
	private static final String ID_MEMBER_PANEL = "memberPanel";

	private static final Trace LOGGER = TraceManager.getTrace(TreeTablePanel.class);

	public TreeTablePanel(String id, IModel<String> rootOid, PageBase parentPage) {
		super(id, rootOid);
		this.parentPage = parentPage;
		setParent(parentPage);
		initLayout();
	}

	protected void initLayout() {

		OrgTreePanel treePanel = new OrgTreePanel(ID_TREE_PANEL, getModel(), false) {

			@Override
			protected void selectTreeItemPerformed(SelectableBean<OrgType> selected,
					AjaxRequestTarget target) {
				TreeTablePanel.this.selectTreeItemPerformed(selected.getValue(), target);
			}
			

			protected List<InlineMenuItem> createTreeMenu() {
				return TreeTablePanel.this.createTreeMenu();
			}

			@Override
			protected List<InlineMenuItem> createTreeChildrenMenu() {
				return TreeTablePanel.this.createTreeChildrenMenu();
			}

		};
		add(treePanel);
		add(createMemberPanel(treePanel.getSelected().getValue()));
		setOutputMarkupId(true);
	}

	private OrgMemberPanel createMemberPanel(OrgType org){
		OrgMemberPanel memberPanel = new OrgMemberPanel(ID_MEMBER_PANEL, new Model<OrgType>(org), parentPage);
		memberPanel.setOutputMarkupId(true);
		return memberPanel;
	}
	private OrgTreePanel getTreePanel() {
		return (OrgTreePanel) get(ID_TREE_PANEL);
	}


	private List<InlineMenuItem> createTreeMenu() {
		List<InlineMenuItem> items = new ArrayList<>();

		items.add(new InlineMenuItem());
		InlineMenuItem item = new InlineMenuItem(createStringResource("TreeTablePanel.moveRoot"),
				new InlineMenuItemAction() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						moveRootPerformed(null, target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(createStringResource("TreeTablePanel.deleteRoot"),
				new InlineMenuItemAction() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						deleteRootPerformed(null, target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(createStringResource("TreeTablePanel.recomputeRoot"),
				new InlineMenuItemAction() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						recomputeRootPerformed(null, target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(createStringResource("TreeTablePanel.editRoot"),
				new InlineMenuItemAction() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						editRootPerformed(null, target);
					}
				});
		items.add(item);

		return items;
	}

	private List<InlineMenuItem> createTreeChildrenMenu() {
		List<InlineMenuItem> items = new ArrayList<>();

		InlineMenuItem item = new InlineMenuItem(createStringResource("TreeTablePanel.move"),
				new ColumnMenuAction<SelectableBean<OrgType>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						moveRootPerformed(getRowModel().getObject(), target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(createStringResource("TreeTablePanel.delete"),
				new ColumnMenuAction<SelectableBean<OrgType>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						deleteRootPerformed(getRowModel().getObject(), target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(createStringResource("TreeTablePanel.recompute"),
				new ColumnMenuAction<SelectableBean<OrgType>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						recomputeRootPerformed(getRowModel().getObject(), target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(createStringResource("TreeTablePanel.edit"),
				new ColumnMenuAction<SelectableBean<OrgType>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						editRootPerformed(getRowModel().getObject(), target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(createStringResource("TreeTablePanel.createChild"),
				new ColumnMenuAction<SelectableBean<OrgType>>() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						initObjectForAdd(
								ObjectTypeUtil.createObjectRef(getRowModel().getObject().getValue()),
								OrgType.COMPLEX_TYPE, null, target);
					}
				});
		items.add(item);

		return items;
	}


	protected static Map<Class, Class> objectDetailsMap;

	static {
		objectDetailsMap = new HashMap<>();
		objectDetailsMap.put(UserType.class, PageUser.class);
		objectDetailsMap.put(OrgType.class, PageOrgUnit.class);
		objectDetailsMap.put(RoleType.class, PageRole.class);
		objectDetailsMap.put(ServiceType.class, PageService.class);
		objectDetailsMap.put(ResourceType.class, PageResource.class);
		objectDetailsMap.put(TaskType.class, PageTaskEdit.class);
	}

	private void initObjectForAdd(ObjectReferenceType parentOrgRef, QName type, QName relation,
			AjaxRequestTarget target) {
		TreeTablePanel.this.getPageBase().hideMainPopup(target);
		PrismContext prismContext = TreeTablePanel.this.getPageBase().getPrismContext();
		PrismObjectDefinition def = prismContext.getSchemaRegistry().findObjectDefinitionByType(type);
		PrismObject obj = def.instantiate();
		if (parentOrgRef == null) {
			ObjectType org = getTreePanel().getSelected().getValue();
			parentOrgRef = ObjectTypeUtil.createObjectRef(org);
			parentOrgRef.setRelation(relation);
		}
		ObjectType objType = (ObjectType) obj.asObjectable();
		objType.getParentOrgRef().add(parentOrgRef);

		if (FocusType.class.isAssignableFrom(obj.getCompileTimeClass())) {
			AssignmentType assignment = new AssignmentType();
			assignment.setTargetRef(parentOrgRef);
			((FocusType) objType).getAssignment().add(assignment);
		}

		Class newObjectPageClass = objectDetailsMap.get(obj.getCompileTimeClass());

		Constructor constructor = null;
		try {
			constructor = newObjectPageClass.getConstructor(PrismObject.class);

		} catch (NoSuchMethodException | SecurityException e) {
			throw new SystemException("Unable to locate constructor (PrismObject) in " + newObjectPageClass
					+ ": " + e.getMessage(), e);
		}

		PageBase page;
		try {
			page = (PageBase) constructor.newInstance(obj);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new SystemException("Error instantiating " + newObjectPageClass + ": " + e.getMessage(), e);
		}

		setResponsePage(page);

	}

	/**
	 * This method check selection in table.
	 */
	private List<OrgTableDto> isAnythingSelected(TablePanel table, AjaxRequestTarget target) {
		List<OrgTableDto> objects = WebComponentUtil.getSelectedData(table);
		if (objects.isEmpty()) {
			warn(getString("TreeTablePanel.message.nothingSelected"));
			target.add(getPageBase().getFeedbackPanel());
		}

		return objects;
	}

	private ObjectDelta createMoveDelta(PrismObject<OrgType> orgUnit, SelectableBean<OrgType> oldParent,
			SelectableBean<OrgType> newParent, OrgUnitBrowser.Operation operation) {
		ObjectDelta delta = orgUnit.createDelta(ChangeType.MODIFY);
		PrismReferenceDefinition refDef = orgUnit.getDefinition()
				.findReferenceDefinition(OrgType.F_PARENT_ORG_REF);
		ReferenceDelta refDelta = delta.createReferenceModification(OrgType.F_PARENT_ORG_REF, refDef);
		PrismReferenceValue value;
		switch (operation) {
			case ADD:
				LOGGER.debug("Adding new parent {}", new Object[] { newParent });
				// adding parentRef newParent to orgUnit
				value = createPrismRefValue(newParent);
				refDelta.addValuesToAdd(value);
				break;
			case REMOVE:
				LOGGER.debug("Removing new parent {}", new Object[] { newParent });
				// removing parentRef newParent from orgUnit
				value = createPrismRefValue(newParent);
				refDelta.addValuesToDelete(value);
				break;
			case MOVE:
				if (oldParent == null && newParent == null) {
					LOGGER.debug("Moving to root");
					// moving orgUnit to root, removing all parentRefs
					PrismReference ref = orgUnit.findReference(OrgType.F_PARENT_ORG_REF);
					if (ref != null) {
						for (PrismReferenceValue val : ref.getValues()) {
							refDelta.addValuesToDelete(val.clone());
						}
					}
				} else {
					LOGGER.debug("Adding new parent {}, removing old parent {}",
							new Object[] { newParent, oldParent });
					// moving from old to new, removing oldParent adding
					// newParent refs
					value = createPrismRefValue(newParent);
					refDelta.addValuesToAdd(value);

					value = createPrismRefValue(oldParent);
					refDelta.addValuesToDelete(value);
				}
				break;
		}

		return delta;
	}

	private PrismReferenceValue createPrismRefValue(SelectableBean<OrgType> org) {
		return ObjectTypeUtil.createObjectRef(org.getValue()).asReferenceValue();
	}

//	private MainObjectListPanel<ObjectType> getMemberTable() {
//		return (MainObjectListPanel<ObjectType>) get(
//				createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
//	}
//
//	private TablePanel getManagerTable() {
//		return (TablePanel) get(createComponentPath(ID_FORM, ID_CONTAINER_MANAGER, ID_MANAGER_TABLE));
//	}
//
	private void selectTreeItemPerformed(OrgType selected, AjaxRequestTarget target) {
	
//		getMemberTable().refreshTable(null, target);
//	
//		Form mainForm = (Form) get(ID_FORM);
//		mainForm.addOrReplace(createManagerContainer());
		target.add(addOrReplace(createMemberPanel(selected)));
	}
//
//	private QName getSearchType() {
//		DropDownChoice<ObjectTypes> searchByTypeChoice = (DropDownChoice) get(
//				createComponentPath(ID_FORM, ID_SEARCH_BY_TYPE));
//		ObjectTypes typeModel = searchByTypeChoice.getModelObject();
//		return typeModel.getTypeQName();
//	}
//
//	private ObjectQuery createManagerQuery() {
//		SelectableBean<OrgType> dto = getTreePanel().getSelected();
//		String oid = dto != null ? dto.getValue().getOid() : getModel().getObject();
//		PrismReferenceValue referenceFilter = new PrismReferenceValue();
//		referenceFilter.setOid(oid);
//		referenceFilter.setRelation(SchemaConstants.ORG_MANAGER);
//		RefFilter referenceOidFilter;
//		try {
//			referenceOidFilter = RefFilter.createReferenceEqual(new ItemPath(FocusType.F_PARENT_ORG_REF),
//					UserType.class, getPageBase().getPrismContext(), referenceFilter);
//			ObjectQuery query = ObjectQuery.createObjectQuery(referenceOidFilter);
//			if (LOGGER.isTraceEnabled()) {
//				LOGGER.trace("Searching members of org {} with query:\n{}", oid, query.debugDump());
//			}
//			return query;
//		} catch (SchemaException e) {
//			LoggingUtils.logException(LOGGER, "Couldn't prepare query for org. managers.", e);
//			return null;
//		}
//
//	}
//
//	private ObjectQuery createMemberQuery() {
//		ObjectQuery query = null;
//		SelectableBean<OrgType> dto = getTreePanel().getSelected();
//
//		String oid = dto != null ? dto.getValue().getOid() : getModel().getObject();
//
//		List<ObjectFilter> filters = new ArrayList<>();
//
//		DropDownChoice<String> searchScopeChoice = (DropDownChoice) get(
//				createComponentPath(ID_FORM, ID_SEARCH_SCOPE));
//		String scope = searchScopeChoice.getModelObject();
//
//		QName searchType = getSearchType();
//
//		try {
//			OrgFilter org;
//			if (OrgType.COMPLEX_TYPE.equals(searchType)) {
//				if (SEARCH_SCOPE_ONE.equals(scope)) {
//					filters.add(OrgFilter.createOrg(oid, OrgFilter.Scope.ONE_LEVEL));
//				} else {
//					filters.add(OrgFilter.createOrg(oid, OrgFilter.Scope.SUBTREE));
//				}
//			}
//			PrismReferenceValue referenceFilter = new PrismReferenceValue();
//			referenceFilter.setOid(oid);
//			RefFilter referenceOidFilter = RefFilter.createReferenceEqual(
//					new ItemPath(FocusType.F_PARENT_ORG_REF), UserType.class, getPageBase().getPrismContext(),
//					referenceFilter);
//			filters.add(referenceOidFilter);
//
//			query = ObjectQuery.createObjectQuery(AndFilter.createAnd(filters));
//
//			if (LOGGER.isTraceEnabled()) {
//				LOGGER.trace("Searching members of org {} with query:\n{}", oid, query.debugDump());
//			}
//
//		} catch (SchemaException e) {
//			LoggingUtils.logException(LOGGER, "Couldn't prepare query for org. members.", e);
//		}
//
//		if (searchType.equals(ObjectType.COMPLEX_TYPE)) {
//			return query;
//		}
//
//		return ObjectQuery.createObjectQuery(TypeFilter.createType(searchType, query.getFilter()));
//
//	}

	private void moveRootPerformed(SelectableBean<OrgType> root, AjaxRequestTarget target) {
		if (root == null) {
			root = getTreePanel().getRootFromProvider();
		}

		final SelectableBean<OrgType> orgToMove = root;

		OrgTreeAssignablePanel orgAssignablePanel = new OrgTreeAssignablePanel(
				parentPage.getMainPopupBodyId(), false, parentPage) {

			@Override
			protected void onItemSelect(SelectableBean<OrgType> selected, AjaxRequestTarget target) {
				moveConfirmPerformed(orgToMove, selected, target);
			}
		};
		
		parentPage.showMainPopup(orgAssignablePanel, new Model<String>("Select new parent"), target, 900, 700);

	}

	private void moveConfirmPerformed(SelectableBean<OrgType> orgToMove, SelectableBean<OrgType> selected,
			AjaxRequestTarget target) {
		getPageBase().hideMainPopup(target);
		
		Task task = getPageBase().createSimpleTask(OPERATION_MOVE_OBJECT);
		OperationResult result = new OperationResult(OPERATION_MOVE_OBJECT);


		OrgType toMove = orgToMove.getValue();
		ObjectDelta<OrgType> moveOrgDelta = ObjectDelta.createEmptyModifyDelta(OrgType.class, toMove.getOid(),
				getPageBase().getPrismContext());

		for (OrgType parentOrg : toMove.getParentOrg()) {
			moveOrgDelta.addModification(ReferenceDelta.createModificationDelete(OrgType.F_PARENT_ORG_REF,
					toMove.asPrismObject().getDefinition(), ObjectTypeUtil.createObjectRef(parentOrg).asReferenceValue()));
		}
		
		moveOrgDelta.addModification(ReferenceDelta.createModificationAdd(OrgType.F_PARENT_ORG_REF,
				toMove.asPrismObject().getDefinition(), ObjectTypeUtil.createObjectRef(selected.getValue()).asReferenceValue()));
		
		try {
			getPageBase().getModelService().executeChanges(WebComponentUtil.createDeltaCollection(moveOrgDelta), null, task, result);
			result.computeStatus();
		} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
				| ExpressionEvaluationException | CommunicationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException e) {
			result.recordFatalError("Failed to move organization unit " + toMove, e);
			LoggingUtils.logException(LOGGER, "Failed to move organization unit" + toMove, e);
		}
		
		parentPage.showResult(result);
		target.add(parentPage.getFeedbackPanel());

	}


//	protected void refreshTable(AjaxRequestTarget target) {
//		DropDownChoice<ObjectTypes> typeChoice = (DropDownChoice) get(
//				createComponentPath(ID_FORM, ID_SEARCH_BY_TYPE));
//		ObjectTypes type = typeChoice.getModelObject();
//		target.add(get(createComponentPath(ID_FORM, ID_SEARCH_SCOPE)));
//		getMemberTable().clearCache();
//		getMemberTable().refreshTable(qnameToClass(type.getTypeQName()), target);
//	}

	private void recomputeRootPerformed(SelectableBean<OrgType> root, AjaxRequestTarget target) {
		if (root == null) {
			root = getTreePanel().getRootFromProvider();
		}

		recomputePerformed(root, target);
	}

	private void recomputePerformed(SelectableBean<OrgType> orgToRecompute, AjaxRequestTarget target) {

		Task task = getPageBase().createSimpleTask(OPERATION_RECOMPUTE);
		OperationResult result = new OperationResult(OPERATION_RECOMPUTE);

		try {
			ObjectDelta emptyDelta = ObjectDelta.createEmptyModifyDelta(OrgType.class,
					orgToRecompute.getValue().getOid(), getPageBase().getPrismContext());
			ModelExecuteOptions options = new ModelExecuteOptions();
			options.setReconcile(true);
			getPageBase().getModelService().executeChanges(WebComponentUtil.createDeltaCollection(emptyDelta),
					options, task, result);

			result.recordSuccess();
		} catch (Exception e) {
			result.recordFatalError(getString("TreeTablePanel.message.recomputeError"), e);
			LoggingUtils.logException(LOGGER, getString("TreeTablePanel.message.recomputeError"), e);
		}

		getPageBase().showResult(result);
		target.add(getPageBase().getFeedbackPanel());
		getTreePanel().refreshTabbedPanel(target);
	}

	

	private void deleteRootPerformed(final SelectableBean<OrgType> orgToDelete, AjaxRequestTarget target) {

		ConfirmationPanel confirmationPanel = new ConfirmationPanel(getPageBase().getMainPopupBodyId()) {
			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				deleteRootConfirmedPerformed(orgToDelete, target);
			}
		};

		confirmationPanel.setOutputMarkupId(true);
		getPageBase().showMainPopup(confirmationPanel, new Model<String>("Delete org?"), target, 150, 100);
	}

	private void deleteRootConfirmedPerformed(SelectableBean<OrgType> orgToDelete, AjaxRequestTarget target) {
		getPageBase().hideMainPopup(target);
		OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);

		PageBase page = getPageBase();

		if (orgToDelete == null) {
			orgToDelete = getTreePanel().getRootFromProvider();
		}
		WebModelServiceUtils.deleteObject(OrgType.class, orgToDelete.getValue().getOid(), result, page);

		result.computeStatusIfUnknown();
		page.showResult(result);

		getTreePanel().refreshTabbedPanel(target);
	}

	

	private void editRootPerformed(SelectableBean<OrgType> root, AjaxRequestTarget target) {
		if (root == null) {
			root = getTreePanel().getRootFromProvider();
		}
		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, root.getValue().getOid());
		setResponsePage(PageOrgUnit.class, parameters);
	}

	

	
}
