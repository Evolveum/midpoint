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
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
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
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitResourceProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitAssignmentProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitUserProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitObjectStatus;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author mserbak
 */
public class PageSubmit extends PageAdmin {
	private static final String DOT_CLASS = PageSubmit.class.getName() + ".";
	private static final String OPERATION_SAVE_USER = DOT_CLASS + "saveUser";
	private static final String OPERATION_MODIFY_ACCOUNT = DOT_CLASS + "saveUser - modifyAccount";
	private static final Trace LOGGER = TraceManager.getTrace(PageSubmit.class);

	private ObjectDeltaComponent userDelta;
	List<ObjectDeltaComponent> accountsDeltas;
	private List<ContainerDelta> assignmentsDeltas = new ArrayList<ContainerDelta>();
	private List<PropertyDelta> userPropertiesDeltas = new ArrayList<PropertyDelta>();

	private List<SubmitAccountProvider> accountsList;
	private List<SubmitAssignmentProvider> assignmentsList;
	private List<SubmitUserProvider> userList;

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

			for (Object value : newObject.getObjectToAdd().getValues()) {
				PrismContainerValue prismValue = (PrismContainerValue) value;
				for (Object itemObject : prismValue.getItems()) {
					Item item = (Item) itemObject;
					if (item instanceof PrismProperty) {
						PrismProperty property = (PrismProperty) item;
						PropertyDelta propertyDelta = new PropertyDelta(property.getDefinition());
						propertyDelta.addValuesToAdd(property.getValues());
						userPropertiesDeltas.add(propertyDelta);
					} else if (item instanceof PrismContainer) {
						if (!(item.getDefinition().getTypeName().equals(AssignmentType.COMPLEX_TYPE))) {
							PrismContainer property = (PrismContainer) item;
							PrismContainerDefinition def = property.getDefinition();
							PrismPropertyDefinition propertyDef = new PrismPropertyDefinition(def.getName(),
									def.getDefaultName(), def.getTypeName(), def.getPrismContext());
							PropertyDelta propertyDelta = new PropertyDelta(propertyDef);
							propertyDelta.addValuesToAdd(property.getValues());
							userPropertiesDeltas.add(propertyDelta);
							continue;
						}
						PrismContainer assign = (PrismContainer) item;
						ContainerDelta assignDelta = new ContainerDelta(assign.getDefinition());
						assignDelta.addValuesToAdd(assign.getValues());
						assignmentsDeltas.add(assignDelta);
					}
				}
			}
		} else {
			for (Object item : newObject.getModifications()) {
				ItemDelta itemDelta = (ItemDelta) item;

				if (itemDelta.getPath().equals(account)) {
					continue;
				} else if (itemDelta.getPath().equals(assignment)) {
					assignmentsDeltas.add((ContainerDelta) itemDelta);
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
		accordion.setExpanded(true);
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
		// changeType.setExpanded(true);
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

		List<IColumn<SubmitResourceProvider>> columns = new ArrayList<IColumn<SubmitResourceProvider>>();

		IColumn column = new CheckBoxHeaderColumn<SubmitResourceProvider>();
		columns.add(column);

		columns.add(new PropertyColumn(createStringResource("pageSubmit.resourceList.resourceName"),
				"resourceName"));

		ListDataProvider<SubmitResourceProvider> provider = new ListDataProvider<SubmitResourceProvider>(
				this, new AbstractReadOnlyModel<List<SubmitResourceProvider>>() {

					@Override
					public List<SubmitResourceProvider> getObject() {
						List<SubmitResourceProvider> list = new ArrayList<SubmitResourceProvider>();
						for (PrismObject item : loadResourceList()) {
							if (item != null) {
								list.add(new SubmitResourceProvider(item, true));
							}
						}
						return list;
					}
				});
		TablePanel resourcesTable = new TablePanel<SubmitResourceProvider>("resourcesTable", provider,
				columns);
		resourcesTable.setShowPaging(false);
		resourcesTable.setOutputMarkupId(true);
		accountsList.getBodyContainer().add(resourcesTable);
	}

	private void initUserInfo(Accordion changeType) {
		AccordionItem userInfoAccordion = new AccordionItem("userInfoAccordion",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageSubmit.userInfoAccordion", userList.size())
								.getString();
					}
				});
		userInfoAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(userInfoAccordion);

		List<IColumn<SubmitUserProvider>> columns = new ArrayList<IColumn<SubmitUserProvider>>();

		columns.add(new PropertyColumn(createStringResource("pageSubmit.attribute"), "attribute"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.oldValue"), "oldValue"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.newValue"), "newValue"));

		ListDataProvider<SubmitUserProvider> provider = new ListDataProvider<SubmitUserProvider>(this,
				new AbstractReadOnlyModel<List<SubmitUserProvider>>() {

					@Override
					public List<SubmitUserProvider> getObject() {
						return userList = loadUserProperties();
					}
				});
		TablePanel userTable = new TablePanel<SubmitUserProvider>("userTable", provider, columns);
		userTable.setShowPaging(false);
		userTable.setOutputMarkupId(true);
		userInfoAccordion.getBodyContainer().add(userTable);
	}

	private void initAccounts(Accordion changeType) {
		AccordionItem accountsAccordion = new AccordionItem("accountsAccordion",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageSubmit.accountsAccordion", accountsList.size())
								.getString();
					}
				});
		accountsAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(accountsAccordion);

		List<IColumn<SubmitAccountProvider>> columns = new ArrayList<IColumn<SubmitAccountProvider>>();
		columns.add(new PropertyColumn(createStringResource("pageSubmit.resource"), "resourceName"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.attribute"), "attribute"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.oldValue"), "oldValue"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.newValue"), "newValue"));

		ListDataProvider<SubmitAccountProvider> provider = new ListDataProvider<SubmitAccountProvider>(this,
				new AbstractReadOnlyModel<List<SubmitAccountProvider>>() {

					@Override
					public List<SubmitAccountProvider> getObject() {
						return accountsList = loadAccountsList();
					}
				});
		TablePanel accountsTable = new TablePanel<SubmitAccountProvider>("accountsTable", provider, columns);
		accountsTable.setShowPaging(false);
		accountsTable.setOutputMarkupId(true);
		accountsAccordion.getBodyContainer().add(accountsTable);
	}

	private void initAssignments(Accordion changeType) {
		AccordionItem assignmentsAccordion = new AccordionItem("assignmentsAccordion",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageSubmit.assignmentsAccordion", assignmentsList.size())
								.getString();
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
						return assignmentsList = loadAssignmentsList();
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

		/*
		 * AjaxLinkButton returnButton = new AjaxLinkButton("returnButton",
		 * createStringResource("pageSubmit.button.return")) {
		 * 
		 * @Override public void onClick(AjaxRequestTarget target) { // TODO
		 * setResponsePage(PageUser.class); } }; mainForm.add(returnButton);
		 */

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
		LOGGER.debug("Saving user changes.");
		OperationResult result = new OperationResult(OPERATION_SAVE_USER);
		OperationResult subResult = null;
		try {
			for (ObjectDeltaComponent account : accountsDeltas) {
				if (account.getStatus().equals(SubmitObjectStatus.MODIFYING)) {
					subResult = result.createSubresult(OPERATION_MODIFY_ACCOUNT);
					Task task = createSimpleTask(OPERATION_MODIFY_ACCOUNT);

					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Modifying account:\n{}", new Object[] { account.getNewDelta()
								.debugDump(3) });
					}
					getModelService().modifyObject(account.getNewDelta().getObjectTypeClass(),
							account.getNewDelta().getOid(), account.getNewDelta().getModifications(), task,
							subResult);
				}
				continue;
			}
			subResult.recomputeStatus();
		} catch (Exception ex) {
			if (subResult != null) {
				subResult.recomputeStatus();
				subResult.recordFatalError("Modify account failed.", ex);
			}
			LoggingUtils.logException(LOGGER, "Couldn't modify account", ex);
		}

		try {
			ObjectDelta userDeltaObject = userDelta.getNewDelta();
			Task task = createSimpleTask(OPERATION_SAVE_USER);
			switch (userDeltaObject.getChangeType()) {
				case ADD:
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before add user:\n{}", new Object[] { userDelta.getNewDelta()
								.debugDump(3) });
					}
					getModelService().addObject(userDelta.getNewUser(), task, result);
					break;
				case MODIFY:
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before modify user:\n{}", new Object[] { userDeltaObject.debugDump(3) });
					}
					if (!userDeltaObject.isEmpty()) {
						getModelService().modifyObject(UserType.class, userDeltaObject.getOid(),
								userDeltaObject.getModifications(), task, result);
					} else {
						result.recordSuccessIfUnknown();
					}
					break;
				default:
					error(getString("pageSubmit.message.unsupportedState", userDeltaObject.getChangeType()));
			}
			result.recomputeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't save user.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't save user", ex);
		}

		if (!result.isSuccess()) {
			showResult(result);
			target.add(getFeedbackPanel());
		} else {
			showResultInSession(result);
			setResponsePage(PageUsers.class);
		}
	}

	private List<SubmitAccountProvider> loadAccountsList() {
		List<SubmitAccountProvider> list = new ArrayList<SubmitAccountProvider>();
		if (accountsDeltas != null) {
			for (ObjectDeltaComponent account : accountsDeltas) {
				ObjectDelta delta = account.getNewDelta();
				if (delta.getChangeType().equals(ChangeType.MODIFY)) {
					for (Object modification : account.getNewDelta().getModifications()) {
						ItemDelta modifyDelta = (ItemDelta) modification;
						String oldValue = "";
						String newValue = "";

						ItemDefinition def = modifyDelta.getDefinition();
						String attribute = def.getDisplayName() != null ? def.getDisplayName() : def
								.getName().getLocalPart();

						if (modifyDelta.getValuesToDelete() != null) {
							List<Object> valuesToDelete = new ArrayList<Object>(
									modifyDelta.getValuesToDelete());
							Integer listSize = valuesToDelete.size();
							for (int i = 0; i < listSize; i++) {
								PrismPropertyValue value = (PrismPropertyValue) valuesToDelete.get(i);
								String valueToDelete = value == null ? "" : value.getValue().toString();
								oldValue += valueToDelete;
								if (i != listSize - 1) {
									oldValue += ", ";
								}
							}
						}

						if (modifyDelta.getValuesToAdd() != null) {
							List<Object> valuesToAdd = new ArrayList<Object>(modifyDelta.getValuesToAdd());
							Integer listSize = valuesToAdd.size();
							for (int i = 0; i < listSize; i++) {
								PrismPropertyValue value = (PrismPropertyValue) valuesToAdd.get(i);
								String valueToAdd = value == null ? "" : value.getValue().toString();
								newValue += valueToAdd;
								if (i != listSize - 1) {
									newValue += ", ";
								}
							}
						}
						SubmitResourceProvider resourceProvider = new SubmitResourceProvider(
								getResource(account.getNewDelta()), false);
						list.add(new SubmitAccountProvider(resourceProvider.getResourceName(), attribute,
								oldValue, newValue));
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
				list.add(getResource(delta));
			}
		}
		return list;
	}

	private List<SubmitAssignmentProvider> loadAssignmentsList() {

		List<SubmitAssignmentProvider> list = new ArrayList<SubmitAssignmentProvider>();
		for (ContainerDelta assignment : assignmentsDeltas) {
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

	private List<SubmitUserProvider> loadUserProperties() {
		List<SubmitUserProvider> list = new ArrayList<SubmitUserProvider>();
		if (userPropertiesDeltas != null && !userPropertiesDeltas.isEmpty()) {
			PrismObject oldUser = userDelta.getOldObject();
			ObjectDelta newUserDelta = userDelta.getNewDelta();
			for (PropertyDelta propertyDelta : userPropertiesDeltas) {
				if (propertyDelta.getValuesToAdd() != null) {
					for (Object value : propertyDelta.getValuesToAdd()) {
						if (value instanceof PrismContainerValue) {
							PrismContainerValue containerValues = (PrismContainerValue) value;
							for (Object containerValue : containerValues.getItems()) {
								PrismProperty propertyValue = (PrismProperty) containerValue;
								PropertyDelta delta = new PropertyDelta(propertyValue.getDefinition());
								list.add(getDeltasFromUserProperties(oldUser, newUserDelta,
										(PrismPropertyValue) propertyValue.getValue(), delta));
							}
							continue;
						}
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

	private SubmitUserProvider getDeltasFromUserProperties(PrismObject oldUser, ObjectDelta newUserDelta,
			PrismPropertyValue prismValue, PropertyDelta propertyDelta) {
		ItemDefinition def = propertyDelta.getDefinition();
		String attribute = def.getDisplayName() != null ? def.getDisplayName() : def.getName().getLocalPart();
		String oldValue = "";
		String newValue = "";

		PrismProperty oldPropertyValue = oldUser.findProperty(propertyDelta.getName());
		if (oldPropertyValue != null) {
			if (oldPropertyValue.getValues() != null) {
				for (Object valueObject : oldPropertyValue.getValues()) {
					PrismPropertyValue value = (PrismPropertyValue) valueObject;
					oldValue = value.getValue().toString();
				}

			}
		}
		newValue = prismValue.getValue().toString();
		return new SubmitUserProvider(attribute, oldValue, newValue);
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

	private PrismObject<AccountShadowType> getResource(ObjectDelta delta) {
		if (delta.getChangeType().equals(ChangeType.ADD)) {
			return delta.getObjectToAdd();
		} else {
			Task task = createSimpleTask("loadResourceList: Load account");
			OperationResult result = new OperationResult("loadResourceList: Load account");
			PrismObject<AccountShadowType> accountObject = null;
			try {
				accountObject = getModelService().getObject(AccountShadowType.class, delta.getOid(), null,
						task, result);
			} catch (Exception ex) {
				result.recordFatalError("Unable to get account object", ex);
				showResultInSession(result);
				throw new RestartResponseException(PageUsers.class);
			}
			return accountObject;
		}
	}
}
