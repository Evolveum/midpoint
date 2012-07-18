/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.delta.ObjectDeltaComponent;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitAccountProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitAssignmentProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitChangesProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitObjectStatus;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author mserbak
 */
public class PageSubmit extends PageAdmin {
	private ObjectDeltaComponent userDelta;
	List<ObjectDeltaComponent> accountsDeltas;
	private List<ReferenceDelta> accounts = new ArrayList<ReferenceDelta>();
	private List<ContainerDelta> assignments = new ArrayList<ContainerDelta>();
	private List<PropertyDelta> userPropertiesDeltas = new ArrayList<PropertyDelta>();

	public PageSubmit(ObjectDeltaComponent userDelta, List<ObjectDeltaComponent> accountsDeltas) {

		if (userDelta == null || accountsDeltas == null) {
			getSession().error(getString("pageSubmit.message.cantLoadData"));
			throw new RestartResponseException(PageUsers.class);
		}
		this.userDelta = userDelta;
		this.accountsDeltas = accountsDeltas;

		PropertyPath account = new PropertyPath(SchemaConstants.I_ACCOUNT_REF);
		PropertyPath assignment = new PropertyPath(SchemaConstantsGenerated.C_ASSIGNMENT);
		ObjectDelta newObject = userDelta.getNewDelta();

		if (newObject.getChangeType().equals(ChangeType.ADD)) {

			for (Object item : newObject.getObjectToAdd().getValues()) {
				PrismContainerValue itemDelta = (PrismContainerValue) item;
				// itemDelta.f
				// if(itemDelta.getPath(new PropertyPath()))
			}
		} else {
			for (Object item : newObject.getModifications()) {
				ItemDelta itemDelta = (ItemDelta) item;

				if (itemDelta.getPath().equals(account)) {
					continue;
				} else if (itemDelta.getPath().equals(assignment)) {
					assignments.add((ContainerDelta) itemDelta);
				} else {
					userPropertiesDeltas.add((PropertyDelta) itemDelta);
				}
			}
		}

		initLayout();
	}

	private void initLayout() {

		Form mainForm = new Form("mainForm");
		add(mainForm);

		mainForm.add(new Label("confirmText", createStringResource("pageSubmit.confirmText",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						if (userDelta.getNewDelta().getChangeType().equals(ChangeType.ADD)) {
							return WebMiscUtil.getName(userDelta.getNewDelta().getObjectToAdd());
						}
						return WebMiscUtil.getName(userDelta.getOldObject());
					}

				})));

		Accordion accordion = new Accordion("accordion");
		accordion.setMultipleSelect(true);
		accordion.setOpenedPanel(0);
		mainForm.add(accordion);

		AccordionItem changesList = new AccordionItem("changesList", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString("pageSubmit.changesList");
			}
		});
		changesList.setOutputMarkupId(true);
		accordion.getBodyContainer().add(changesList);

		Accordion changeType = new Accordion("changeType");
		changeType.setExpanded(true);
		changeType.setMultipleSelect(true);
		changesList.getBodyContainer().add(changeType);

		initResources(accordion);

		initUserInfo(changeType);
		initAccounts(changeType);
		initAssignments(changeType);

		initButtons(mainForm);

	}

	private void initResources(Accordion accordion) {
		AccordionItem accountsList = new AccordionItem("resourcesDeltas",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return getString("pageSubmit.resourceList");
					}
				});
		accountsList.setOutputMarkupId(true);
		accordion.getBodyContainer().add(accountsList);

		List<IColumn<SubmitAccountProvider>> columns = new ArrayList<IColumn<SubmitAccountProvider>>();

		IColumn column = new CheckBoxHeaderColumn<SubmitAccountProvider>();
		columns.add(column);

		columns.add(new PropertyColumn(createStringResource("pageSubmit.resourceList.resourceName"),
				"resourceName"));

		ListDataProvider<SubmitAccountProvider> provider = new ListDataProvider<SubmitAccountProvider>(this,
				new AbstractReadOnlyModel<List<SubmitAccountProvider>>() {

					@Override
					public List<SubmitAccountProvider> getObject() {
						List<SubmitAccountProvider> list = new ArrayList<SubmitAccountProvider>();
						for (PrismObject item : loadResourceList()) {
							if (item != null) {
								list.add(new SubmitAccountProvider(item, true));
							}
						}
						return list;
					}
				});
		TablePanel resourcesTable = new TablePanel<SubmitAccountProvider>("resourcesTable", provider, columns);
		resourcesTable.setShowPaging(false);
		resourcesTable.setOutputMarkupId(true);
		accountsList.getBodyContainer().add(resourcesTable);
	}

	private void initUserInfo(Accordion changeType) {
		AccordionItem userInfoAccordion = new AccordionItem("userInfoAccordion",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return getString("pageSubmit.userInfoAccordion");
					}
				});
		userInfoAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(userInfoAccordion);

		List<IColumn<SubmitChangesProvider>> columns = new ArrayList<IColumn<SubmitChangesProvider>>();

		columns.add(new PropertyColumn(createStringResource("pageSubmit.attribute"), "attribute"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.oldValue"), "oldValue"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.newValue"), "newValue"));

		ListDataProvider<SubmitChangesProvider> provider = new ListDataProvider<SubmitChangesProvider>(this,
				new AbstractReadOnlyModel<List<SubmitChangesProvider>>() {

					@Override
					public List<SubmitChangesProvider> getObject() {
						return loadUserProperties();
					}
				});
		TablePanel userTable = new TablePanel<SubmitChangesProvider>("userTable", provider, columns);
		userTable.setShowPaging(false);
		userTable.setOutputMarkupId(true);
		userInfoAccordion.getBodyContainer().add(userTable);
	}

	private void initAccounts(Accordion changeType) {
		AccordionItem accountsAccordion = new AccordionItem("accountsAccordion",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return getString("pageSubmit.accountsAccordion");
					}
				});
		accountsAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(accountsAccordion);

		List<IColumn<SubmitChangesProvider>> columns = new ArrayList<IColumn<SubmitChangesProvider>>();
		columns.add(new PropertyColumn(createStringResource("pageSubmit.attribute"), "attribute"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.oldValue"), "oldValue"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.newValue"), "newValue"));

		ListDataProvider<SubmitChangesProvider> provider = new ListDataProvider<SubmitChangesProvider>(this,
				new AbstractReadOnlyModel<List<SubmitChangesProvider>>() {

					@Override
					public List<SubmitChangesProvider> getObject() {
						return loadAccountsList();
					}
				});
		TablePanel accountsTable = new TablePanel<SubmitChangesProvider>("accountsTable", provider, columns);
		accountsTable.setShowPaging(false);
		accountsTable.setOutputMarkupId(true);
		accountsAccordion.getBodyContainer().add(accountsTable);
	}

	private void initAssignments(Accordion changeType) {
		AccordionItem assignmentsAccordion = new AccordionItem("assignmentsAccordion",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return getString("pageSubmit.assignmentsAccordion");
					}
				});
		assignmentsAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(assignmentsAccordion);

		List<IColumn<SubmitAssignmentProvider>> columns = new ArrayList<IColumn<SubmitAssignmentProvider>>();
		columns.add(new PropertyColumn(createStringResource("pageSubmit.assignmentsList.assignment"),
				"assignment"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.assignmentsList.operation"), "status"));

		ListDataProvider<SubmitAssignmentProvider> provider = new ListDataProvider<SubmitAssignmentProvider>(
				this, new AbstractReadOnlyModel<List<SubmitAssignmentProvider>>() {

					@Override
					public List<SubmitAssignmentProvider> getObject() {
						return loadAssignmentsList();
					}
				});
		TablePanel assignmentsTable = new TablePanel<SubmitAssignmentProvider>("assignmentsTable", provider,
				columns);
		assignmentsTable.setShowPaging(false);
		assignmentsTable.setOutputMarkupId(true);
		assignmentsAccordion.getBodyContainer().add(assignmentsTable);
	}

	private void initButtons(Form mainForm) {
		AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("saveButton", ButtonType.POSITIVE,
				createStringResource("pageSubmit.button.save")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		mainForm.add(saveButton);

		AjaxLinkButton returnButton = new AjaxLinkButton("returnButton",
				createStringResource("pageSubmit.button.return")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				// TODO setResponsePage(PageUser.class);
			}
		};
		mainForm.add(returnButton);

		AjaxLinkButton cancelButton = new AjaxLinkButton("cancelButton",
				createStringResource("pageSubmit.button.cancel")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(PageUsers.class);
			}
		};
		mainForm.add(cancelButton);
	}

	private void savePerformed(AjaxRequestTarget target) {
		// TODO
	}

	private List<SubmitChangesProvider> loadAccountsList() {
		List<SubmitChangesProvider> list = new ArrayList<SubmitChangesProvider>();
		if (accountsDeltas != null) {
			for (ObjectDeltaComponent account : accountsDeltas) {
				ObjectDelta delta = account.getNewDelta();
				if (delta.getChangeType().equals(ChangeType.MODIFY)) {
					for (Object modification : account.getNewDelta().getModifications()) {
						ItemDelta modifyDelta = (ItemDelta) modification;
						String oldValue = "";
						String newValue = "";
						
						ItemDefinition def = modifyDelta.getDefinition();
						String attribute = def.getDisplayName() != null ? def.getDisplayName() : def.getName()
								.getLocalPart();

						if (modifyDelta.getValuesToDelete() != null) {
							for (Object valueObject : modifyDelta.getValuesToDelete()) {
								PrismPropertyValue value = (PrismPropertyValue) valueObject;
								oldValue = value == null ? "" : value.getValue().toString();
								// TODO multivalue
							}
						}

						if (modifyDelta.getValuesToAdd() != null) {
							for (Object valueObject : modifyDelta.getValuesToAdd()) {
								PrismPropertyValue value = (PrismPropertyValue) valueObject;
								newValue = value == null ? "" : value.getValue().toString();
								// TODO multivalue
							}
						}
						list.add(new SubmitChangesProvider(attribute, oldValue, newValue));
					}
				}
			}
		}

		return list;

	}

	private List<PrismObject> loadResourceList() {
		List<PrismObject> list = new ArrayList<PrismObject>();
		if (accountsDeltas != null) {
			for (ObjectDeltaComponent account : accountsDeltas) {
				ObjectDelta delta = account.getNewDelta();
				if (delta.getChangeType().equals(ChangeType.ADD)) {
					list.add(delta.getObjectToAdd());
				} else {
					Task task = createSimpleTask("loadResourceList: Load account");
					OperationResult result = new OperationResult("loadResourceList: Load account");
					PrismObject<AccountShadowType> accountObject = null;
					try {
						accountObject = getModelService().getObject(AccountShadowType.class, delta.getOid(),
								null, task, result);
					} catch (Exception ex) {
						result.recordFatalError("Unable to get account object", ex);
						showResultInSession(result);
						throw new RestartResponseException(PageUsers.class);
					}
					list.add(accountObject);
				}
			}
		}
		return list;
	}

	private List<SubmitAssignmentProvider> loadAssignmentsList() {

		List<SubmitAssignmentProvider> list = new ArrayList<SubmitAssignmentProvider>();
		for (ContainerDelta assignment : assignments) {
			if (assignment.getValuesToAdd() != null) {
				for (Object item : assignment.getValuesToAdd()) {
					list.add(new SubmitAssignmentProvider(
							getReferenceFromAssignment((PrismContainerValue) item),
							getString("pageSubmit.status." + SubmitObjectStatus.ADDING)));
				}
			}

			if (assignment.getValuesToDelete() != null) {
				for (Object item : assignment.getValuesToDelete()) {
					list.add(new SubmitAssignmentProvider(
							getReferenceFromAssignment((PrismContainerValue) item),
							getString("pageSubmit.status." + SubmitObjectStatus.DELETING)));
				}
			}
		}
		return list;
	}

	private List<SubmitChangesProvider> loadUserProperties() {
		List<SubmitChangesProvider> list = new ArrayList<SubmitChangesProvider>();
		if (userPropertiesDeltas != null && !userPropertiesDeltas.isEmpty()) {
			PrismObject oldUser = userDelta.getOldObject();
			ObjectDelta newUserDelta = userDelta.getNewDelta();
			for (PropertyDelta propertyDelta : userPropertiesDeltas) {
				if (propertyDelta.getValuesToAdd() != null) {
					for (Object value : propertyDelta.getValuesToAdd()) {
						list.add(getDeltasFromUserProperties(oldUser, newUserDelta,
								(PrismPropertyValue) value, propertyDelta));
					}
				}

				if (propertyDelta.getValuesToDelete() != null) {
					for (Object value : propertyDelta.getValuesToDelete()) {
						list.add(getDeltasFromUserProperties(oldUser, newUserDelta,
								(PrismPropertyValue) value, propertyDelta));
					}
				}

				if (propertyDelta.getValuesToReplace() != null) {
					for (Object value : propertyDelta.getValuesToReplace()) {
						list.add(getDeltasFromUserProperties(oldUser, newUserDelta,
								(PrismPropertyValue) value, propertyDelta));
					}
				}

			}
		}
		return list;
	}

	private SubmitChangesProvider getDeltasFromUserProperties(PrismObject oldUser, ObjectDelta newUserDelta,
			PrismPropertyValue prismValue, PropertyDelta propertyDelta) {
		String attribute = "";
		String oldValue = "";
		String newValue = "";

		PrismProperty attributeProperty = oldUser.findProperty(propertyDelta.getName());
		if (attributeProperty != null) {
			attribute = attributeProperty.getDefinition().getDisplayName();
			if (attributeProperty.getValue() != null) {
				oldValue = attributeProperty.getValue().getValue().toString();
			}
		}
		newValue = prismValue.getValue().toString();
		if (propertyDelta.getParentPath().equals(new PropertyPath(UserType.F_ACTIVATION))) {
			return new SubmitChangesProvider(getString("pageSubmit.activation"), oldValue, newValue);
		}

		return new SubmitChangesProvider(attribute, oldValue, newValue);
	}

	private String getReferenceFromAssignment(PrismContainerValue assignment) {
		Task task = createSimpleTask("getRefFromAssignment: Load role");
		OperationResult result = new OperationResult("getRefFromAssignment: Load role");

		PrismReference targetRef = assignment.findReference(AssignmentType.F_TARGET_REF);
		if (targetRef != null) {
			PrismObject<RoleType> role = null;
			try {
				role = getModelService().getObject(RoleType.class, targetRef.getValue().getOid(), null, task,
						result);
			} catch (Exception ex) {
				result.recordFatalError("Unable to get role object", ex);
				showResultInSession(result);
				throw new RestartResponseException(PageUsers.class);
			}
			return WebMiscUtil.getName(role);
		}

		PrismReference accountConstrRef = assignment.findReference(AssignmentType.F_ACCOUNT_CONSTRUCTION);
		if (accountConstrRef != null) {
			PrismObject<RoleType> role = null;
			try {
				role = getModelService().getObject(RoleType.class, accountConstrRef.getValue().getOid(),
						null, task, result);
			} catch (Exception ex) {
				result.recordFatalError("Unable to get role object", ex);
				showResultInSession(result);
				throw new RestartResponseException(PageUsers.class);
			}
			return WebMiscUtil.getName(role);
		}

		return "";
	}
}
