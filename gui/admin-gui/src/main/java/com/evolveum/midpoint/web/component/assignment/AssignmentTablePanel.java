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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.AuthorizationConstants;

import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
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
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.TypedAssignablePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
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
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreeAssignablePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.w3c.dom.Attr;

/**
 * @author shood
 */
public class AssignmentTablePanel<T extends ObjectType> extends BasePanel<List<AssignmentEditorDto>> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(AssignmentTablePanel.class);

	private static final String DOT_CLASS = AssignmentTablePanel.class.getName() + ".";

	private static final String ID_ASSIGNMENTS = "assignments";
	private static final String ID_CHECK_ALL = "assignmentsCheckAll";
	private static final String ID_HEADER = "assignmentsHeader";
	private static final String ID_MENU = "assignmentsMenu";
	private static final String ID_LIST = "assignmentList";
	protected static final String ID_ROW = "assignmentEditor";
	private PageBase pageBase = null;
    private boolean isModelChanged = false;

	public AssignmentTablePanel(String id, IModel<String> label,
			IModel<List<AssignmentEditorDto>> assignmentModel) {
		this(id, label, assignmentModel, null);
	}

	public AssignmentTablePanel(String id, IModel<String> label,
			IModel<List<AssignmentEditorDto>> assignmentModel, PageBase pageBase) {
		super(id, assignmentModel);
		this.pageBase = pageBase;
		initLayout(label);
	}

	public List<AssignmentType> getAssignmentTypeList() {
		return null;
	}

	public String getExcludeOid() {
		return null;
	}

	protected IModel<List<AssignmentEditorDto>> getAssignmentModel() {
		return getModel();
	}

	private void initLayout(IModel<String> labelText) {
		final WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
		assignments.setOutputMarkupId(true);
		add(assignments);

		Label label = new Label(ID_HEADER, labelText);
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
				List<AssignmentEditorDto> assignmentEditors = getAssignmentModel().getObject();

				for (AssignmentEditorDto dto : assignmentEditors) {
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
		AssignmentEditorPanel editor = new AssignmentEditorPanel(ID_ROW, item.getModel(), pageBase){
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
									getPageBase().getMainPopupBodyId(), RoleType.class, true, getPageBase()) {
								private static final long serialVersionUID = 1L;

								@Override
								protected void addPerformed(AjaxRequestTarget target, List selected) {
									super.addPerformed(target, selected);
									addSelectedAssignablePerformed(target, selected,
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
										addSelectedAssignablePerformed(target, (List) selectedOrgs,
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
							AssignmentTablePanel.this.deleteAssignmentPerformed(target);
                        }
					});
			items.add(item);
		}
		item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.showAllAssignments"),
				new InlineMenuItemAction() {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						showAllAssignments(target);
					}
				});
		items.add(item);

		return items;
	}

	protected void showAllAssignments(AjaxRequestTarget target) {

	}

	private List<AssignmentEditorDto> getSelectedAssignments() {
		List<AssignmentEditorDto> selected = new ArrayList<>();

		List<AssignmentEditorDto> all = getAssignmentModel().getObject();

		for (AssignmentEditorDto dto : all) {
			if (dto.isSelected()) {
				selected.add(dto);
			}
		}

		return selected;
	}

	protected void deleteAssignmentPerformed(AjaxRequestTarget target) {
		List<AssignmentEditorDto> selected = getSelectedAssignments();

		if (selected.isEmpty()) {
			warn(getNoAssignmentsSelectedMessage());
			target.add(getPageBase().getFeedbackPanel());
			return;
		}

		getPageBase().showMainPopup(getDeleteAssignmentPopupContent(), target);
	}

	protected String getNoAssignmentsSelectedMessage(){
		return getString("AssignmentTablePanel.message.noAssignmentSelected");
	}

	protected String getAssignmentsDeleteMessage(int size){
		return createStringResource("AssignmentTablePanel.modal.message.delete",
				size).getString();
	}

	private Popupable getDeleteAssignmentPopupContent() {
		return new ConfirmationPanel(getPageBase().getMainPopupBodyId(), new AbstractReadOnlyModel<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				return getAssignmentsDeleteMessage(getSelectedAssignments().size());
			}
		}) {

			private static final long serialVersionUID = 1L;

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				ModalWindow modalWindow = findParent(ModalWindow.class);
				if (modalWindow != null) {
					modalWindow.close(target);
					deleteAssignmentConfirmedPerformed(target, getSelectedAssignments());
                    reloadMainFormButtons(target);
                }
			}
		};
	}

	private void deleteAssignmentConfirmedPerformed(AjaxRequestTarget target,
			List<AssignmentEditorDto> toDelete) {
		List<AssignmentEditorDto> assignments = getAssignmentModel().getObject();

		for (AssignmentEditorDto assignment : toDelete) {
			if (UserDtoStatus.ADD.equals(assignment.getStatus())) {
				assignments.remove(assignment);
			} else {
				assignment.setStatus(UserDtoStatus.DELETE);
				assignment.setSelected(false);
			}
		}
		target.add(getPageBase().getFeedbackPanel(), get(ID_ASSIGNMENTS));
	}

	protected void addSelectedAssignablePerformed(AjaxRequestTarget target, List<ObjectType> newAssignments,
			String popupId) {
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
			try {

				if (object instanceof ResourceType) {
					addSelectedResourceAssignPerformed((ResourceType) object);
					continue;
				}
				if (object instanceof UserType) {
					AssignmentEditorDto dto = AssignmentEditorDto.createDtoAddFromSelectedObject(object,
							SchemaConstants.ORG_DEPUTY, getPageBase());
					assignments.add(dto);
				} else {
					AssignmentEditorDto dto = AssignmentEditorDto.createDtoAddFromSelectedObject(object,
							getPageBase());
					assignments.add(dto);
				}
			} catch (Exception e) {
				error(getString("AssignmentTablePanel.message.couldntAssignObject", object.getName(),
						e.getMessage()));
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't assign object", e);
			}
		}
		reloadAssignmentsPanel(target);
	}

	protected void reloadAssignmentsPanel(AjaxRequestTarget target){
		target.add(getPageBase().getFeedbackPanel(), get(ID_ASSIGNMENTS));
	}

	protected void addSelectedResourceAssignPerformed(ResourceType resource) {
		AssignmentType assignment = new AssignmentType();
		ConstructionType construction = new ConstructionType();
		assignment.setConstruction(construction);

		try {
			getPageBase().getPrismContext().adopt(assignment, UserType.class,
					new ItemPath(UserType.F_ASSIGNMENT));
		} catch (SchemaException e) {
			error(getString("Could not create assignment", resource.getName(), e.getMessage()));
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create assignment", e);
			return;
		}

		construction.setResource(resource);

		List<AssignmentEditorDto> assignments = getAssignmentModel().getObject();
		AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, getPageBase());
		assignments.add(dto);

		dto.setMinimized(true);
		dto.setShowEmpty(true);
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
		ContainerDelta assDelta = new ContainerDelta(new ItemPath(), assignmentPath, def,
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

	protected void reloadMainFormButtons(AjaxRequestTarget target){
        isModelChanged = true;
        AbstractObjectMainPanel panel = AssignmentTablePanel.this.findParent(AbstractObjectMainPanel.class);
        if (panel != null){
            panel.reloadSavePreviewButtons(target);
        }
    }

    public boolean isModelChanged() {
        return isModelChanged;
    }
}
