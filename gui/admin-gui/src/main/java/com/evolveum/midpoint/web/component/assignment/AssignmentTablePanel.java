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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.AuthorizationConstants;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.TypedAssignablePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreeAssignablePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;

/**
 * @author shood
 */
public class AssignmentTablePanel<T extends ObjectType> extends AbstractAssignmentListPanel {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(AssignmentTablePanel.class);

	private static final String DOT_CLASS = AssignmentTablePanel.class.getName() + ".";

	private static final String ID_ASSIGNMENTS = "assignments";
	private static final String ID_CHECK_ALL = "assignmentsCheckAll";
	private static final String ID_HEADER = "assignmentsHeader";
	private static final String ID_MENU = "assignmentsMenu";
	private static final String ID_LIST = "assignmentList";
	protected static final String ID_ROW = "assignmentEditor";

	
	public AssignmentTablePanel(String id, IModel<List<AssignmentEditorDto>> assignmentModel) {
		super(id, assignmentModel);
	}

	public List<AssignmentType> getAssignmentTypeList() {
		return null;
	}

	public String getExcludeOid() {
		return null;
	}
	
	public IModel<String> getLabel() {
		return new Model<>("label");
	}



	@Override
	protected void onInitialize() {
		super.onInitialize();
	
		final WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
		assignments.setOutputMarkupId(true);
		add(assignments);

		Label label = new Label(ID_HEADER, getLabel());
		assignments.add(label);

		InlineMenu assignmentMenu = new InlineMenu(ID_MENU, new Model((Serializable) createAssignmentMenu()));
        assignmentMenu.setVisible(getAssignmentMenuVisibility());
        assignments.add(assignmentMenu);

		ListView<AssignmentEditorDto> list = new ListView<AssignmentEditorDto>(ID_LIST, getModel()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<AssignmentEditorDto> item) {
				AssignmentTablePanel.this.populateAssignmentDetailsPanel(item);
			}
		};
		list.setOutputMarkupId(true);
		assignments.add(list);

		AjaxCheckBox checkAll = new AjaxCheckBox(ID_CHECK_ALL, new Model()) {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				List<AssignmentEditorDto> assignmentsList = getAssignmentModel().getObject();

				for (AssignmentEditorDto dto : assignmentsList) {
					dto.setSelected(this.getModelObject());
				}

				target.add(assignments);
			}
		};
		checkAll.add(new VisibleEnableBehaviour(){
			@Override
			public boolean isVisible(){
				int count = 0;
				for (AssignmentEditorDto dto : getModelObject()){
					if (dto.isSimpleView()){
						count++;
					}
				}
				if (count == getModelObject().size()){
					return false;
				} else {
					return true;
				}
			}
		});
		assignments.add(checkAll);

	}

	protected void populateAssignmentDetailsPanel(ListItem<AssignmentEditorDto> item){
		AssignmentEditorPanel editor = new AssignmentEditorPanel(ID_ROW, item.getModel()){
			@Override
			protected boolean ignoreMandatoryAttributes(){
				return AssignmentTablePanel.this.ignoreMandatoryAttributes();
			}
		};
		item.add(editor);

		editor.add(getClassModifier(item));
	}

	protected AttributeModifier getClassModifier(ListItem<AssignmentEditorDto> item){
		return AttributeModifier.append("class", new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				AssignmentEditorDto dto = item.getModel().getObject();
				ObjectReferenceType targetRef = dto.getTargetRef();
				if (targetRef != null && targetRef.getType() != null) {
					return WebComponentUtil.getBoxThinCssClasses(targetRef.getType());
				} else {
					return GuiStyleConstants.CLASS_OBJECT_RESOURCE_BOX_THIN_CSS_CLASSES;
				}
			}
		});
	}

	protected List<InlineMenuItem> createAssignmentMenu() {
		List<InlineMenuItem> items = new ArrayList<>();

		InlineMenuItem item;
		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI)) {
			item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.assign"),
					new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							TypedAssignablePanel panel = new TypedAssignablePanel(
									getPageBase().getMainPopupBodyId(), RoleType.class) {
								private static final long serialVersionUID = 1L;

								@Override
								protected void addPerformed(AjaxRequestTarget target, List selected, QName relation, ShadowKindType kind, String intent) {
									super.addPerformed(target, selected, relation, kind, intent);
									addSelectedAssignablePerformed(target, selected, relation,
											getPageBase().getMainPopup().getId());
                                    reloadMainFormButtons(target);
								}

							};
							panel.setOutputMarkupId(true);
							getPageBase().showMainPopup(panel, target);
						}
					});
			items.add(item);

			item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.assignOrg"),
					new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							int count = WebModelServiceUtils.countObjects(OrgType.class, null, getPageBase());
							if (count > 0) {
								OrgTreeAssignablePanel orgTreePanel = new OrgTreeAssignablePanel(
										getPageBase().getMainPopupBodyId(), true, getPageBase()) {
									private static final long serialVersionUID = 1L;

									@Override
									protected void assignSelectedOrgPerformed(List<OrgType> selectedOrgs,
											AjaxRequestTarget target) {
										// TODO Auto-generated method stub
										addSelectedAssignablePerformed(target, (List) selectedOrgs, SchemaConstants.ORG_DEFAULT,
												getPageBase().getMainPopup().getId());
                                        reloadMainFormButtons(target);
                                    }
								};
								orgTreePanel.setOutputMarkupId(true);
								getPageBase().showMainPopup(orgTreePanel, target);
							} else {
								warn(createStringResource("AssignmentTablePanel.menu.assignOrg.noorgs").getString());
								target.add(getPageBase().getFeedbackPanel());
							}

						}
					});
			items.add(item);
			items.add(new InlineMenuItem());
		}
		if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI)) {
			item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.unassign"),
					new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							AssignmentTablePanel.this.deleteAssignmentPerformed(target, null);
                        }
					});
			items.add(item);
		}
		if (isShowAllAssignmentsVisible()) {
			item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.showAllAssignments"),
					new InlineMenuItemAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(AjaxRequestTarget target) {
							showAllAssignments(target);
						}
					});
			items.add(item);
		}
		return items;
	}

	protected void showAllAssignments(AjaxRequestTarget target) {

	}

	protected void reloadMainAssignmentsComponent(AjaxRequestTarget target){
		target.add(get(ID_ASSIGNMENTS));
	}

	protected boolean isShowAllAssignmentsVisible(){
		return false;
	}

	protected void addSelectedAssignablePerformed(AjaxRequestTarget target, List<ObjectType> newAssignments,
			QName relation, String popupId) {
		ModalWindow window = (ModalWindow) get(popupId);
		if (window != null) {
			window.close(target);
		}
		getPageBase().hideMainPopup(target);
		if (newAssignments.isEmpty()) {
			warn(getNoAssignmentsSelectedMessage());
			target.add(getPageBase().getFeedbackPanel());
			return;
		}

		List<AssignmentEditorDto> assignments = getAssignmentModel().getObject();
		for (ObjectType object : newAssignments) {
			assignments.add(createAssignmentFromSelectedObjects(object, relation));
		}
		reloadAssignmentsPanel(target);
	}

	protected void reloadAssignmentsPanel(AjaxRequestTarget target){
		target.add(getPageBase().getFeedbackPanel(), get(ID_ASSIGNMENTS));
	}

	public void handleAssignmentsWhenAdd(PrismObject<T> object, PrismContainerDefinition assignmentDef,
			List<AssignmentType> objectAssignments) throws SchemaException {

		List<AssignmentEditorDto> assignments = getAssignmentModel().getObject();
		for (AssignmentEditorDto assDto : assignments) {
			if (!UserDtoStatus.ADD.equals(assDto.getStatus())) {
				warn(getString("AssignmentTablePanel.message.illegalAssignmentState", assDto.getStatus()));
				continue;
			}

			AssignmentType assignment = new AssignmentType();
			PrismContainerValue value = assDto.getNewValue(getPageBase().getPrismContext());
			assignment.setupContainerValue(value);
			value.applyDefinition(assignmentDef, false);
			objectAssignments.add(assignment.clone());

			// todo remove this block [lazyman] after model is updated - it has
			// to remove resource from accountConstruction
			removeResourceFromAccConstruction(assignment);
		}
	}

	public ContainerDelta handleAssignmentDeltas(ObjectDelta<T> userDelta, PrismContainerDefinition def,
			QName assignmentPath) throws SchemaException {
		ContainerDelta assDelta = new ContainerDelta(ItemPath.EMPTY_PATH, assignmentPath, def,
				def.getPrismContext()); // hoping that def contains a prism
										// context!

		// PrismObject<OrgType> org =
		// (PrismObject<OrgType>)getModel().getObject().getAssignmentParent();
		// PrismObjectDefinition orgDef = org.getDefinition();
		// PrismContainerDefinition assignmentDef =
		// def.findContainerDefinition(assignmentPath);

		List<AssignmentEditorDto> assignments = getAssignmentModel().getObject();
		for (AssignmentEditorDto assDto : assignments) {
			PrismContainerValue newValue = assDto.getNewValue(getPageBase().getPrismContext());
			switch (assDto.getStatus()) {
				case ADD:
					newValue.applyDefinition(def, false);
					assDelta.addValueToAdd(newValue.clone());
					break;
				case DELETE:
					PrismContainerValue oldValue = assDto.getOldValue();
					oldValue.applyDefinition(def);
					assDelta.addValueToDelete(oldValue.clone());
					break;
				case MODIFY:
					if (!assDto.isModified(getPageBase().getPrismContext())) {
						LOGGER.trace("Assignment '{}' not modified.", new Object[] { assDto.getName() });
						continue;
					}

					handleModifyAssignmentDelta(assDto, def, newValue, userDelta);
					break;
				default:
					warn(getString("pageUser.message.illegalAssignmentState", assDto.getStatus()));
			}
		}

		if (!assDelta.isEmpty()) {
			assDelta = userDelta.addModification(assDelta);
		}

		// todo remove this block [lazyman] after model is updated - it has to
		// remove resource from accountConstruction
		Collection<PrismContainerValue> values = assDelta.getValues(PrismContainerValue.class);
		for (PrismContainerValue value : values) {
			AssignmentType ass = new AssignmentType();
			ass.setupContainerValue(value);
			removeResourceFromAccConstruction(ass);
		}

		return assDelta;
	}

	private void handleModifyAssignmentDelta(AssignmentEditorDto assDto,
			PrismContainerDefinition assignmentDef, PrismContainerValue newValue, ObjectDelta<T> userDelta)
					throws SchemaException {
		LOGGER.debug("Handling modified assignment '{}', computing delta.",
				new Object[] { assDto.getName() });

		PrismValue oldValue = assDto.getOldValue();
		Collection<? extends ItemDelta> deltas = oldValue.diff(newValue);

		for (ItemDelta delta : deltas) {
			ItemPath deltaPath = delta.getPath().rest();
			ItemDefinition deltaDef = assignmentDef.findItemDefinition(deltaPath);

			delta.setParentPath(
					WebComponentUtil.joinPath(oldValue.getPath(), delta.getPath().allExceptLast()));
			delta.applyDefinition(deltaDef);

			userDelta.addModification(delta);
		}
	}

	/**
	 * remove this method after model is updated - it has to remove resource
	 * from accountConstruction
	 */
	@Deprecated
	private void removeResourceFromAccConstruction(AssignmentType assignment) {
		ConstructionType accConstruction = assignment.getConstruction();
		if (accConstruction == null || accConstruction.getResource() == null) {
			return;
		}

		ObjectReferenceType ref = new ObjectReferenceType();
		ref.setOid(assignment.getConstruction().getResource().getOid());
		ref.setType(ResourceType.COMPLEX_TYPE);
		assignment.getConstruction().setResourceRef(ref);
		assignment.getConstruction().setResource(null);
	}

	/**
	 * Override to provide handle operation for partial error during provider
	 * iterator operation.
	 */
	protected void handlePartialError(OperationResult result) {
	}

    protected boolean getAssignmentMenuVisibility(){
        return true;
    }

    protected boolean ignoreMandatoryAttributes(){
		return false;
	}

}
