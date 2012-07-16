package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.springframework.web.util.WebUtils;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.delta.ObjectDeltaComponent;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugList;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitAccountProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitAssignmentProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitObjectStatus;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitPersonalProvider;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.RoleType;

public class PageSubmit extends PageAdmin {
	private ObjectDeltaComponent userDelta;
	List<ObjectDeltaComponent> accountsDeltas;
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

		initLayout();
	}

	private void initLayout() {

		Form mainForm = new Form("mainForm");
		add(mainForm);

		mainForm.add(new Label("confirmText", createStringResource("pageSubmit.confirmText",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return WebMiscUtil.getName(userDelta.getOldObject());
					}

				})));

		Accordion accordion = new Accordion("accordion");
		accordion.setMultipleSelect(true);
		accordion.setOpenedPanel(0);
		mainForm.add(accordion);

		AccordionItem accountsList = new AccordionItem("accountsDeltas", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString("pageSubmit.accountsList");
			}
		});
		accountsList.setOutputMarkupId(true);
		accordion.getBodyContainer().add(accountsList);

		List<IColumn<SubmitAccountProvider>> columns = new ArrayList<IColumn<SubmitAccountProvider>>();

		IColumn column = new CheckBoxHeaderColumn<SubmitAccountProvider>();
		columns.add(column);

		columns.add(new PropertyColumn(createStringResource("pageSubmit.accountList.resourceName"),
				"resourceName"));
		// columns.add(new
		// PropertyColumn(createStringResource("pageSubmit.accountList.name"),
		// "name"));

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
		TablePanel accountsTable = new TablePanel<SubmitAccountProvider>("accountsTable", provider, columns);
		accountsTable.setShowPaging(false);
		accountsTable.setOutputMarkupId(true);
		accountsList.getBodyContainer().add(accountsTable);

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
		// changeType.setOpenedPanel(-1);
		changesList.getBodyContainer().add(changeType);

		initUserInfo(changeType);
		initAccounts(changeType);
		initAssignments(changeType);

		initButtons(mainForm);

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

		List<IColumn<SubmitPersonalProvider>> columns = new ArrayList<IColumn<SubmitPersonalProvider>>();

		columns.add(new PropertyColumn(createStringResource("pageSubmit.userList.attribute"), "attribute"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.userList.oldValue"), "oldValue"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.userList.newValue"), "newValue"));

		ListDataProvider<SubmitPersonalProvider> provider = new ListDataProvider<SubmitPersonalProvider>(
				this, new AbstractReadOnlyModel<List<SubmitPersonalProvider>>() {

					@Override
					public List<SubmitPersonalProvider> getObject() {
						return loadUserProperties();
					}
				});
		TablePanel userTable = new TablePanel<SubmitPersonalProvider>("userTable", provider, columns);
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

		loadAssignmentsList();
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

	private List<PrismObject> loadResourceList() {
		List<PrismObject> list = new ArrayList<PrismObject>();
		if (accountsDeltas != null) {
			for (ObjectDeltaComponent account : accountsDeltas) {
				/*
				 * Collection modification =
				 * account.getNewDelta().getModifications(); if(modification !=
				 * null){ for (Object item : modification) { ReferenceDelta
				 * itemDelta = (ReferenceDelta) item;
				 * 
				 * for (Object item : newObject.getModifications()) { ItemDelta
				 * itemDelta = (ItemDelta) item; PrismReferenceValue accountInfo
				 * = (PrismReferenceValue) values;
				 * WebMiscUtil.getName(accountInfo.getObject()); Object aa =
				 * accountInfo
				 * .getObject().getPropertyRealValue(SchemaConstants.
				 * I_RESOURCE_REF , Object.class); accountInfo.getObject().get
				 * list.add(accountInfo); } }
				 */
				list.add(account.getNewDelta().getObjectToAdd());
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

	private List<SubmitPersonalProvider> loadUserProperties() {
		List<SubmitPersonalProvider> list = new ArrayList<SubmitPersonalProvider>();
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

	private SubmitPersonalProvider getDeltasFromUserProperties(PrismObject oldUser, ObjectDelta newUserDelta,
			PrismPropertyValue prismValue, PropertyDelta propertyDelta) {
		String attribute = "";
		String oldValue = "";
		String newValue = "";

		PrismProperty attributeProperty = oldUser.findProperty(propertyDelta.getName());
		if (attributeProperty != null) {
			attribute = attributeProperty.getDefinition().getDisplayName();
			oldValue = attributeProperty.getValue().getValue().toString();
		}
		newValue = prismValue.getValue().toString();

		return new SubmitPersonalProvider(attribute, oldValue, newValue);
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
