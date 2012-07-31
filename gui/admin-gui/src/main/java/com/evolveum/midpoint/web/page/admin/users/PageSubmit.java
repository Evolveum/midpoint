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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
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
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.delta.ObjectDeltaComponent;
import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.page.admin.users.dto.AccountChangesDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitAccountDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitDeltaObjectDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitResourceDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitAssignmentDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserChangesDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitUserDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitObjectStatus;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;
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

	private UserChangesDto userChangesDto;
	private AccountChangesDto accountChangesDto;
	private ModelContext previewChanges;

	private List<SubmitAccountDto> accountsChangesList;
	private List<SubmitAssignmentDto> assignmentsChangesList;
	private List<SubmitUserDto> userChangesList;

	public PageSubmit(ModelContext previewChanges) {
		if (previewChanges == null) {
			getSession().error(getString("pageSubmit.message.cantLoadData"));
			throw new RestartResponseException(PageUsers.class);
		}
		this.previewChanges = previewChanges;
		userChangesDto = new UserChangesDto(previewChanges.getFocusContext());
		accountChangesDto = new AccountChangesDto(previewChanges.getProjectionContexts());
		initLayout();
	}

	private void initLayout() {

		Form mainForm = new Form("mainForm");
		add(mainForm);

		mainForm.add(new Label("confirmText", createStringResource("pageSubmit.confirmText",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						ModelElementContext userDelta = previewChanges.getFocusContext();
						if (userDelta.getPrimaryDelta().getChangeType().equals(ChangeType.ADD)) {
							return WebMiscUtil.getName(userDelta.getPrimaryDelta().getObjectToAdd());
						}
						return WebMiscUtil.getName(userDelta.getObjectOld());
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

		initAccounts(accordion);

		initUserChanges(changeType);
		initAccountsChanges(changeType);
		initAssignmentsChanges(changeType);

		initButtons(mainForm);

	}

	private void initAccounts(Accordion accordion) {
		AccordionItem accountsList = new AccordionItem("resourcesDeltas",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return getString("pageSubmit.resourceList");
					}
				});
		accountsList.setOutputMarkupId(true);
		accordion.getBodyContainer().add(accountsList);

		List<IColumn<SubmitResourceDto>> columns = new ArrayList<IColumn<SubmitResourceDto>>();

		IColumn column = new CheckBoxHeaderColumn<SubmitResourceDto>();
		columns.add(column);

		columns.add(new PropertyColumn(createStringResource("pageSubmit.resourceList.name"), "name"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.resourceList.resourceName"),
				"resourceName"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.resourceList.exist"), "exist"));

		ListDataProvider<SubmitResourceDto> provider = new ListDataProvider<SubmitResourceDto>(this,
				new AbstractReadOnlyModel<List<SubmitResourceDto>>() {

					@Override
					public List<SubmitResourceDto> getObject() {
						List<SubmitResourceDto> list = new ArrayList<SubmitResourceDto>();
						for (PrismObject account : accountChangesDto.getAccountsList()) {
							if (account != null) {
								list.add(new SubmitResourceDto(account, true));
							}
						}
						return list;
					}
				});
		TablePanel resourcesTable = new TablePanel<SubmitResourceDto>("resourcesTable", provider, columns);
		resourcesTable.setShowPaging(false);
		resourcesTable.setOutputMarkupId(true);
		accountsList.getBodyContainer().add(resourcesTable);
	}

	private void initUserChanges(Accordion changeType) {
		AccordionItem userInfoAccordion = new AccordionItem("userInfoAccordion",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageSubmit.userInfoAccordion", userChangesList.size())
								.getString();
					}
				});
		userInfoAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(userInfoAccordion);

		List<IColumn<SubmitUserDto>> columns = new ArrayList<IColumn<SubmitUserDto>>();

		columns.add(new PropertyColumn(createStringResource("pageSubmit.attribute"), "attribute"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.oldValue"), "oldValue"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.newValue"), "newValue"));
		IColumn column = new IconColumn<SubmitUserDto>(createStringResource("pageSubmit.typeOfAddData")) {

			@Override
			protected IModel<ResourceReference> createIconModel(final IModel<SubmitUserDto> rowModel) {
				return new AbstractReadOnlyModel<ResourceReference>() {

					@Override
					public ResourceReference getObject() {
						SubmitUserDto dto = rowModel.getObject();
						if (dto.isSecondaryValue()) {
							return new PackageResourceReference(PageSubmit.class, "secondaryValue.png");
						}
						return new PackageResourceReference(PageSubmit.class, "primaryValue.png");
					}
				};
			}

			@Override
			protected IModel<String> createTitleModel(final IModel<SubmitUserDto> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						SubmitUserDto dto = rowModel.getObject();
						if (dto.isSecondaryValue()) {
							return PageSubmit.this.getString("pageSubmit.secondaryValue");
						}
						return PageSubmit.this.getString("pageSubmit.primaryValue");
					}

				};
			}

			@Override
			protected IModel<AttributeModifier> createAttribute(final IModel<SubmitUserDto> rowModel) {
				return new AbstractReadOnlyModel<AttributeModifier>() {

					@Override
					public AttributeModifier getObject() {
						SubmitUserDto dto = rowModel.getObject();
						if (dto.isSecondaryValue()) {
							return new AttributeModifier("class", "secondaryValue");
						}
						return new AttributeModifier("class", "primaryValue");
					}
				};
			}
		};

		columns.add(column);

		ListDataProvider<SubmitUserDto> provider = new ListDataProvider<SubmitUserDto>(this,
				new AbstractReadOnlyModel<List<SubmitUserDto>>() {

					@Override
					public List<SubmitUserDto> getObject() {
						return userChangesList = loadUserChanges();
					}
				});
		TablePanel userTable = new TablePanel<SubmitUserDto>("userTable", provider, columns);
		userTable.setShowPaging(false);
		userTable.setOutputMarkupId(true);
		userInfoAccordion.getBodyContainer().add(userTable);
	}

	private void initAccountsChanges(Accordion changeType) {
		AccordionItem accountsAccordion = new AccordionItem("accountsAccordion",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageSubmit.accountsAccordion",
								accountChangesDto.getAccountChangesList().size()).getString();
					}
				});
		accountsAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(accountsAccordion);

		List<IColumn<SubmitAccountDto>> columns = new ArrayList<IColumn<SubmitAccountDto>>();
		columns.add(new PropertyColumn(createStringResource("pageSubmit.resource"), "resourceName"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.attribute"), "attribute"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.oldValue"), "oldValue"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.newValue"), "newValue"));
		IColumn column = new IconColumn<SubmitAccountDto>(createStringResource("pageSubmit.typeOfAddData")) {

			@Override
			protected IModel<ResourceReference> createIconModel(final IModel<SubmitAccountDto> rowModel) {
				return new AbstractReadOnlyModel<ResourceReference>() {

					@Override
					public ResourceReference getObject() {
						SubmitAccountDto dto = rowModel.getObject();
						if (dto.isSecondaryValue()) {
							return new PackageResourceReference(PageSubmit.class, "secondaryValue.png");
						}
						return new PackageResourceReference(PageSubmit.class, "primaryValue.png");
					}
				};
			}

			@Override
			protected IModel<String> createTitleModel(final IModel<SubmitAccountDto> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						SubmitAccountDto dto = rowModel.getObject();
						if (dto.isSecondaryValue()) {
							return PageSubmit.this.getString("pageSubmit.secondaryValue");
						}
						return PageSubmit.this.getString("pageSubmit.primaryValue");
					}

				};
			}

			@Override
			protected IModel<AttributeModifier> createAttribute(final IModel<SubmitAccountDto> rowModel) {
				return new AbstractReadOnlyModel<AttributeModifier>() {

					@Override
					public AttributeModifier getObject() {
						SubmitAccountDto dto = rowModel.getObject();
						if (dto.isSecondaryValue()) {
							return new AttributeModifier("class", "secondaryValue");
						}
						return new AttributeModifier("class", "primaryValue");
					}
				};
			}
		};

		columns.add(column);

		ListDataProvider<SubmitAccountDto> provider = new ListDataProvider<SubmitAccountDto>(this,
				new AbstractReadOnlyModel<List<SubmitAccountDto>>() {

					@Override
					public List<SubmitAccountDto> getObject() {
						return accountChangesDto.getAccountChangesList();
					}
				});
		TablePanel accountsTable = new TablePanel<SubmitAccountDto>("accountsTable", provider, columns);
		accountsTable.setShowPaging(false);
		accountsTable.setOutputMarkupId(true);
		accountsAccordion.getBodyContainer().add(accountsTable);
	}

	private void initAssignmentsChanges(Accordion changeType) {
		AccordionItem assignmentsAccordion = new AccordionItem("assignmentsAccordion",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageSubmit.assignmentsAccordion",
								assignmentsChangesList.size()).getString();
					}
				});
		assignmentsAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(assignmentsAccordion);

		List<IColumn<SubmitAssignmentDto>> columns = new ArrayList<IColumn<SubmitAssignmentDto>>();
		columns.add(new PropertyColumn(createStringResource("pageSubmit.assignmentsList.assignment"),
				"assignment"));
		columns.add(new PropertyColumn(createStringResource("pageSubmit.assignmentsList.operation"), "status"));
		IColumn column = new IconColumn<SubmitAssignmentDto>(createStringResource("pageSubmit.typeOfAddData")) {

			@Override
			protected IModel<ResourceReference> createIconModel(final IModel<SubmitAssignmentDto> rowModel) {
				return new AbstractReadOnlyModel<ResourceReference>() {

					@Override
					public ResourceReference getObject() {
						SubmitAssignmentDto dto = rowModel.getObject();
						if (dto.isSecondaryValue()) {
							return new PackageResourceReference(PageSubmit.class, "secondaryValue.png");
						}
						return new PackageResourceReference(PageSubmit.class, "primaryValue.png");
					}
				};
			}

			@Override
			protected IModel<String> createTitleModel(final IModel<SubmitAssignmentDto> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						SubmitAssignmentDto dto = rowModel.getObject();
						if (dto.isSecondaryValue()) {
							return PageSubmit.this.getString("pageSubmit.secondaryValue");
						}
						return PageSubmit.this.getString("pageSubmit.primaryValue");
					}

				};
			}

			@Override
			protected IModel<AttributeModifier> createAttribute(final IModel<SubmitAssignmentDto> rowModel) {
				return new AbstractReadOnlyModel<AttributeModifier>() {

					@Override
					public AttributeModifier getObject() {
						SubmitAssignmentDto dto = rowModel.getObject();
						if (dto.isSecondaryValue()) {
							return new AttributeModifier("class", "secondaryValue");
						}
						return new AttributeModifier("class", "primaryValue");
					}
				};
			}
		};
		columns.add(column);

		ListDataProvider<SubmitAssignmentDto> provider = new ListDataProvider<SubmitAssignmentDto>(this,
				new AbstractReadOnlyModel<List<SubmitAssignmentDto>>() {

					@Override
					public List<SubmitAssignmentDto> getObject() {
						return assignmentsChangesList = loadAssignmentsChanges();
					}
				});
		TablePanel assignmentsTable = new TablePanel<SubmitAssignmentDto>("assignmentsTable", provider,
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

		modifyAccounts(result);

		try {
			ModelElementContext userElement = previewChanges.getFocusContext();
			Task task = createSimpleTask(OPERATION_SAVE_USER);
			switch (userElement.getPrimaryDelta().getChangeType()) {
				case ADD:
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before add user:\n{}", new Object[] { userElement
								.getPrimaryDelta().debugDump(3) });
					}

					getModelService().addObject(userElement.getObjectNew(), task, result);
					break;
				case MODIFY:
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before modify user:\n{}", new Object[] { userElement
								.getPrimaryDelta().debugDump(3) });
					}
					if (!userElement.getPrimaryDelta().isEmpty()) {
						getModelService().modifyObject(UserType.class, userElement.getObjectNew().getOid(),
								userElement.getPrimaryDelta().getModifications(), task, result);
					} else {
						result.recordSuccessIfUnknown();
					}
					break;
				default:
					error(getString("pageSubmit.message.unsupportedState", userElement.getPrimaryDelta()
							.getChangeType()));
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

	private void modifyAccounts(OperationResult result) {
		if (accountChangesDto.getAccountsList() != null && !accountChangesDto.getAccountsList().isEmpty()) {
			OperationResult subResult = null;
			for (PrismObject account : accountChangesDto.getAccountsList()) {
				try {
					ObjectDelta newDelta = previewChanges.getFocusContext().getPrimaryDelta();
					if (!(newDelta.getChangeType().equals(ChangeType.MODIFY)) || newDelta.isEmpty()) {
						continue;
					}

					subResult = result.createSubresult(OPERATION_MODIFY_ACCOUNT);
					Task task = createSimpleTask(OPERATION_MODIFY_ACCOUNT);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Modifying account:\n{}", new Object[] { newDelta.debugDump(3) });
					}

					getModelService().modifyObject(newDelta.getObjectTypeClass(), newDelta.getOid(),
							newDelta.getModifications(), task, subResult);
					subResult.recomputeStatus();
				} catch (Exception ex) {
					if (subResult != null) {
						subResult.recomputeStatus();
						subResult.recordFatalError("Modify account failed.", ex);
					}
					LoggingUtils.logException(LOGGER, "Couldn't modify account", ex);
				}
			}
		}
	}

	private List<SubmitAssignmentDto> loadAssignmentsChanges() {

		List<SubmitAssignmentDto> list = new ArrayList<SubmitAssignmentDto>();
		for (SubmitDeltaObjectDto assignmentDto : userChangesDto.getAssignmentsDeltas()) {
			ContainerDelta assignment = (ContainerDelta) assignmentDto.getItemDelta();
			if (assignment.getValuesToAdd() != null) {
				for (Object item : assignment.getValuesToAdd()) {
					list.add(new SubmitAssignmentDto(getReferenceFromAssignment((PrismContainerValue) item),
							getString("pageSubmit.status." + SubmitObjectStatus.ADDING), assignmentDto
									.isSecondaryValue()));
				}
			}

			if (assignment.getValuesToDelete() != null) {
				for (Object item : assignment.getValuesToDelete()) {
					list.add(new SubmitAssignmentDto(getReferenceFromAssignment((PrismContainerValue) item),
							getString("pageSubmit.status." + SubmitObjectStatus.DELETING), assignmentDto
									.isSecondaryValue()));
				}
			}
		}
		return list;
	}

	private List<SubmitUserDto> loadUserChanges() {
		List<SubmitUserDto> list = new ArrayList<SubmitUserDto>();
		List<SubmitDeltaObjectDto> userPropertiesDelta = userChangesDto.getUserPropertiesDeltas();
		if (userPropertiesDelta != null && !userChangesDto.getUserPropertiesDeltas().isEmpty()) {
			PrismObject oldUser = previewChanges.getFocusContext().getObjectOld();
			PrismObject newUser = previewChanges.getFocusContext().getObjectNew();

			for (SubmitDeltaObjectDto itemDeltaDto : userPropertiesDelta) {
				PropertyDelta propertyDelta = (PropertyDelta) itemDeltaDto.getItemDelta();
				if (propertyDelta.getDefinition().getTypeName().equals(ProtectedStringType.COMPLEX_TYPE)) {
					continue;
				}
				List<PrismPropertyValue> values = new ArrayList<PrismPropertyValue>();

				if (propertyDelta.getValuesToAdd() != null) {
					for (Object value : propertyDelta.getValuesToAdd()) {
						if (value instanceof PrismContainerValue) {
							PrismContainerValue containerValues = (PrismContainerValue) value;

							PropertyDelta delta = null;

							for (Object propertyValueObject : containerValues.getItems()) {
								if (propertyValueObject instanceof PrismContainer) {
									continue;
								}
								PrismProperty propertyValue = (PrismProperty) propertyValueObject;
								delta = new PropertyDelta(propertyValue.getDefinition());
								values.add((PrismPropertyValue) propertyValue.getValue());
							}
							values = new ArrayList<PrismPropertyValue>();
							continue;
						}
						values.add((PrismPropertyValue) value);
					}
				}

				if (propertyDelta.getValuesToDelete() != null) {
					for (Object value : propertyDelta.getValuesToDelete()) {
						values.add((PrismPropertyValue) value);
					}
				}

				if (propertyDelta.getValuesToReplace() != null) {
					for (Object value : propertyDelta.getValuesToReplace()) {
						values.add((PrismPropertyValue) value);
					}
				}

				if (!values.isEmpty()) {
					list.add(getDeltasFromUserProperties(values, propertyDelta,
							itemDeltaDto.isSecondaryValue()));
				}
			}
		}
		return list;
	}

	private SubmitUserDto getDeltasFromUserProperties(List<PrismPropertyValue> values,
			PropertyDelta propertyDelta, boolean secondaryValue) {

		ItemDefinition def = propertyDelta.getDefinition();
		String attribute = def.getDisplayName() != null ? def.getDisplayName() : def.getName().getLocalPart();
		List<String> oldValues = new ArrayList<String>();
		List<String> newValues = new ArrayList<String>();

		PrismObject oldUserObject = previewChanges.getFocusContext().getObjectOld();

		if (oldUserObject != null) {
			PrismProperty oldPropertyValue = oldUserObject.findProperty(propertyDelta.getName());

			if (oldPropertyValue != null && oldPropertyValue.getValues() != null) {
				for (Object valueObject : oldPropertyValue.getValues()) {
					PrismPropertyValue oldValue = (PrismPropertyValue) valueObject;
					oldValues.add(oldValue.getValue() != null ? oldValue.getValue().toString() : " ");
					if (!values.contains(oldValue)) {
						newValues.add(oldValue.getValue().toString());
					}
				}
			}
		}
		for (PrismPropertyValue newValue : values) {
			if (oldValues.contains(newValue.getValue().toString())) {
				continue;
			}
			newValues.add(newValue.getValue() != null ? newValue.getValue().toString() : " ");
		}
		return new SubmitUserDto(attribute, listToString(oldValues), listToString(newValues), secondaryValue);
	}

	public static String listToString(List<String> list) {
		StringBuilder sb = new StringBuilder(list.size());
		for (int i = 0; i < list.size(); i++) {
			sb.append(list.get(i));
			if (i < list.size() - 1) {
				sb.append(", ");
			}
		}
		return sb.toString();
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

	private PrismObject<AccountShadowType> getAccountFromDelta(ObjectDelta delta) {
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
