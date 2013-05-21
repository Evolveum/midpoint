/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.web.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
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
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.users.dto.AccountChangesDto;
import com.evolveum.midpoint.web.page.admin.users.dto.AccountDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitAccountDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitAssignmentDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitDeltaObjectDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitPropertiesDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitResourceDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitStatus;
import com.evolveum.midpoint.web.page.admin.users.dto.SubmitUserDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserChangesDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.wicket.util.string.StringValue;
/**
 * @author mserbak
 */
public class PageUserPreview extends PageAdmin {
	private static final String DOT_CLASS = PageUserPreview.class.getName() + ".";
	private static final String OPERATION_SAVE_USER = DOT_CLASS + "saveUser";
	private static final String OPERATION_MODIFY_ACCOUNT = DOT_CLASS + "modifyAccount";
	private static final Trace LOGGER = TraceManager.getTrace(PageUserPreview.class);

	private UserChangesDto userChangesDto;
	private AccountChangesDto accountChangesDto;
	private ModelContext previewChanges;
	private Collection<ObjectDelta<? extends ObjectType>> deltasChanges;
	private ObjectDelta<UserType> delta;

	private List<SubmitAccountDto> accountsChangesList;
	private List<SubmitAssignmentDto> assignmentsChangesList;
	private List<SubmitUserDto> userChangesList;

    private ExecuteChangeOptionsDto executeOptions;

	public PageUserPreview(ModelContext previewChanges, Collection<ObjectDelta<? extends ObjectType>> allDeltas,
                           ObjectDelta<UserType> userDelta, ArrayList<PrismObject> accountsBeforeModify,
                           ExecuteChangeOptionsDto options) {
		if (previewChanges == null || allDeltas == null || userDelta == null) {
			getSession().error(getString("pageUserPreview.message.cantLoadData"));
			throw new RestartResponseException(PageUsers.class);
		}
        this.executeOptions = options;
		this.deltasChanges = allDeltas;
		this.previewChanges = previewChanges;
		this.delta = userDelta;
		userChangesDto = new UserChangesDto(previewChanges.getFocusContext());
		accountChangesDto = new AccountChangesDto(previewChanges.getProjectionContexts(),
				accountsBeforeModify);
		getSession().setAttribute("prismAccounts", null);   //todo delete, what is this for? [lazyman]
		initLayout();
	}

	private void initLayout() {

		Form mainForm = new Form("mainForm");
		add(mainForm);

		mainForm.add(new Label("confirmText", createStringResource("pageUserPreview.confirmText",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						ModelElementContext userDelta = previewChanges.getFocusContext();
						if (userDelta.getPrimaryDelta() != null && userDelta.getPrimaryDelta().getChangeType().equals(ChangeType.ADD)) {
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
				return getString("pageUserPreview.changesList");
			}
		});
		changesList.setOutputMarkupId(true);
		accordion.getBodyContainer().add(changesList);

		Accordion changeType = new Accordion("changeType");
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
						return getString("pageUserPreview.resourceList");
					}
				});
		accountsList.setOutputMarkupId(true);
		accordion.getBodyContainer().add(accountsList);

		List<IColumn<SubmitResourceDto, String>> columns = new ArrayList<IColumn<SubmitResourceDto, String>>();
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.resourceList.name"), "name"));
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.resourceList.resourceName"),
				"resourceName"));
		PropertyColumn status = new PropertyColumn(createStringResource("pageUserPreview.resourceList.status"), "syncPolicy"){

			@Override
			public void populateItem(final Item item, String componentId, final IModel rowModel) {
				
				Label label = new Label(componentId, getDataModel(rowModel));
				item.add(label);	
				
				SubmitResourceDto resourceDto = (SubmitResourceDto) rowModel.getObject();
				if(resourceDto.getSyncPolicy() == null) {
					return;
				}
				if(resourceDto.getSyncPolicy().equals("Delete")) {
					label.add(new AttributeModifier("class", "deletedValue"));
				} else if (resourceDto.getSyncPolicy().equals("Add")) {
					label.add(new AttributeModifier("class", "addedValue"));
				}
			}
		};
		columns.add(status);

		ListDataProvider<SubmitResourceDto> provider = new ListDataProvider<SubmitResourceDto>(this,
				new AbstractReadOnlyModel<List<SubmitResourceDto>>() {

					@Override
					public List<SubmitResourceDto> getObject() {
						List<SubmitResourceDto> list = new ArrayList<SubmitResourceDto>();
						for (AccountDto accountDto : accountChangesDto.getAccountsList()) {
							if (accountDto != null) {
								list.add(new SubmitResourceDto(accountDto, true));
							}
						}
						return list;
					}
				});
		TablePanel resourcesTable = new TablePanel<SubmitResourceDto>("resourcesTable", provider, columns);
		resourcesTable.setStyle("margin-top: 0px;");
		resourcesTable.setShowPaging(false);
		resourcesTable.setOutputMarkupId(true);
		accountsList.getBodyContainer().add(resourcesTable);
	}

	private void initUserChanges(Accordion changeType) {
		AccordionItem userInfoAccordion = new AccordionItem("userInfoAccordion",
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageUserPreview.userInfoAccordion", userChangesList.size())
								.getString();
					}
				});
		userInfoAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(userInfoAccordion);

		List<IColumn<SubmitUserDto, String>> columns = new ArrayList<IColumn<SubmitUserDto, String>>();
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.attribute"), "attribute"));
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.oldValue"), "oldValue"));
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.newValue"), "newValue"));
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.originType"), "originType"));
		columns.add(new AdditionalDataIconColumn(createStringResource("pageUserPreview.typeOfAddData")));

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
						return createStringResource("pageUserPreview.accountsAccordion",
								accountChangesDto.getAccountChangesList().size()).getString();
					}
				});
		accountsAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(accountsAccordion);

		List<IColumn<SubmitAccountDto, String>> columns = new ArrayList<IColumn<SubmitAccountDto, String>>();
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.resource"), "resourceName"));
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.attribute"), "attribute"));
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.oldValue"), "oldValue"));
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.newValue"), "newValue"));
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.originType"), "originType"));
		columns.add(new AdditionalDataIconColumn(createStringResource("pageUserPreview.typeOfAddData")));

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
						return createStringResource("pageUserPreview.assignmentsAccordion",
								assignmentsChangesList.size()).getString();
					}
				});
		assignmentsAccordion.setOutputMarkupId(true);
		changeType.getBodyContainer().add(assignmentsAccordion);

		List<IColumn<SubmitAssignmentDto, String>> columns = new ArrayList<IColumn<SubmitAssignmentDto, String>>();
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.assignmentsList.assignment"),
				"assignment"));
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.assignmentsList.operation"), "status"));
		columns.add(new PropertyColumn(createStringResource("pageUserPreview.originType"), "originType"));
		columns.add(new AdditionalDataIconColumn(createStringResource("pageUserPreview.typeOfAddData")));

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
				createStringResource("pageUserPreview.button.save")) {

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

		AjaxLinkButton cancelButton = new AjaxLinkButton("cancelButton",
				createStringResource("pageUserPreview.button.cancel")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(PageUsers.class);
			}
		};
		mainForm.add(cancelButton);
		
		AjaxLinkButton returnButton = new AjaxLinkButton("returnButton", createStringResource("pageUserPreview.button.return")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
            	String oid = null;
            	if (previewChanges.getFocusContext().getObjectOld() != null){
            		oid = previewChanges.getFocusContext().getObjectOld().getOid();
            	} else if (previewChanges.getFocusContext().getObjectNew() != null){
            		oid = previewChanges.getFocusContext().getObjectNew().getOid();
            	}
				setResponsePage(new PageUser(deltasChanges, oid));
            }
        };
        mainForm.add(returnButton);

	}

	private List<SubmitAssignmentDto> loadAssignmentsChanges() {

		List<SubmitAssignmentDto> list = new ArrayList<SubmitAssignmentDto>();
		PrismValue prismValue;
		OriginType originType;
		for (SubmitDeltaObjectDto assignmentDto : userChangesDto.getAssignmentsList()) {
			originType = null;
			ItemDelta assignment = null;
			if(assignmentDto.getItemDelta() instanceof ContainerDelta) {
				assignment = (ContainerDelta) assignmentDto.getItemDelta(); 
			} else {
				assignment = (ReferenceDelta) assignmentDto.getItemDelta(); 
			}
			 
			if (assignment.getValuesToAdd() != null) {
				for (Object item : assignment.getValuesToAdd()) {
					prismValue = (PrismValue) item;
					originType = prismValue.getOriginType();
					list.add(new SubmitAssignmentDto(getReferenceFromAssignment(prismValue),
							getString("pageUserPreview.status." + SubmitStatus.ADDING), getString("OriginType."
									+ originType), assignmentDto.isSecondaryValue(), false));
				}
			}

			if (assignment.getValuesToDelete() != null) {
				for (Object item : assignment.getValuesToDelete()) {
					prismValue = (PrismValue) item;
					originType = prismValue.getOriginType();
					list.add(new SubmitAssignmentDto(getReferenceFromAssignment(prismValue),
							getString("pageUserPreview.status." + SubmitStatus.DELETING),getString("OriginType."
									+ originType), assignmentDto.isSecondaryValue(), true));
				}
			}
		}
		return list;
	}

	private List<SubmitUserDto> loadUserChanges() {
		List<SubmitUserDto> list = new ArrayList<SubmitUserDto>();
		List<SubmitDeltaObjectDto> userPropertiesDelta = userChangesDto.getUserPropertiesList();
		if (userPropertiesDelta != null && !userChangesDto.getUserPropertiesList().isEmpty()) {
			PrismObject oldUser = previewChanges.getFocusContext().getObjectOld();
			PrismObject newUser = previewChanges.getFocusContext().getObjectNew();

			for (SubmitDeltaObjectDto itemDeltaDto : userPropertiesDelta) {
				PropertyDelta propertyDelta = (PropertyDelta) itemDeltaDto.getItemDelta();
				List<SubmitPropertiesDto> values = new ArrayList<SubmitPropertiesDto>();

				if (propertyDelta.getValuesToAdd() != null) {
					for (Object value : propertyDelta.getValuesToAdd()) {
						if (value instanceof PrismContainerValue) {
							PrismContainerValue containerValues = (PrismContainerValue) value;

							for (Object propertyValueObject : containerValues.getItems()) {
								if (propertyValueObject instanceof PrismContainer) {
									continue;
								}
								PrismProperty propertyValue = (PrismProperty) propertyValueObject;
								values.add(new SubmitPropertiesDto(propertyValue.getValue(), SubmitStatus.ADDING));
							}
							continue;
						}
						values.add(new SubmitPropertiesDto((PrismPropertyValue) value, SubmitStatus.ADDING));
					}
				}

				if (propertyDelta.getValuesToDelete() != null) {
					for (Object value : propertyDelta.getValuesToDelete()) {
						values.add(new SubmitPropertiesDto((PrismPropertyValue) value, SubmitStatus.DELETING));
					}
				}

				if (propertyDelta.getValuesToReplace() != null) {
					for (Object value : propertyDelta.getValuesToReplace()) {
						values.add(new SubmitPropertiesDto((PrismPropertyValue) value,
								SubmitStatus.REPLACEING));
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

	private SubmitUserDto getDeltasFromUserProperties(List<SubmitPropertiesDto> values,
			PropertyDelta propertyDelta, boolean secondaryValue) {

		ItemDefinition def = propertyDelta.getDefinition();
		String attribute = def.getDisplayName() != null ? def.getDisplayName() : def.getName().getLocalPart();
		List<String> oldValues = new ArrayList<String>();
		List<String> newValues = new ArrayList<String>();

		PrismObject oldUserObject = previewChanges.getFocusContext().getObjectOld();

		if (oldUserObject != null) {
			boolean exist = true;
			PrismProperty oldPropertyValue = oldUserObject.findProperty(propertyDelta.getPath());
			if (oldPropertyValue != null && oldPropertyValue.getValues() != null) {
				for (Object valueObject : oldPropertyValue.getValues()) {
					PrismPropertyValue oldValue = (PrismPropertyValue) valueObject;

					// add old value to list oldValues
					oldValues.add(readableValue(oldValue));

					// test if imported values contains current old value. If
					// not exist, will add
					for (SubmitPropertiesDto newValue : values) {
						if (newValue.getStatus().equals(SubmitStatus.REPLACEING)) {
							continue;
						}
						exist = false;
						String newValueObjectString = readableValue(newValue.getSubmitedProperties());
						if (newValueObjectString.equals(readableValue(oldValue))) {
							exist = true;
							break;
						}
					}
					if (!exist) {
						newValues.add(readableValue(oldValue));
					}
				}
			}
		}
		OriginType originType = null;
		// add imported values to newValues
		for (SubmitPropertiesDto newValue : values) {
			originType = newValue.getSubmitedProperties().getOriginType();
			if (newValue.getStatus().equals(SubmitStatus.DELETING)) {
				continue;
			}
			String stringValue = readableValue(newValue.getSubmitedProperties());
			newValues.add(stringValue);
		}

		return new SubmitUserDto(attribute, StringUtils.join(oldValues, ", "),
				StringUtils.join(newValues, ", "), getString("OriginType." + originType), secondaryValue);
	}

    private String readableValue(PrismPropertyValue prismValue) {
        if (prismValue == null || prismValue.getValue() == null) {
            return getString("PageUserPreview.nullValue");
        }

        Object value = prismValue.getValue();
        if (value instanceof ProtectedStringType) {
            ProtectedStringType pString = (ProtectedStringType) value;
            if (pString.getClearValue() == null && pString.getEncryptedData() == null) {
                return getString("PageUserPreview.nullValue");
            }
            return getString("PageUserPreview.passwordValue");
        } else if (value instanceof PolyString) {
            PolyString poly = (PolyString) value;
            return poly.getOrig() != null ? poly.getOrig() : getString("PageUserPreview.nullValue");
        } else if ((value instanceof String) && StringUtils.isEmpty((String) value)) {
            return getString("PageUserPreview.emptyValue");
        }

        return value.toString();
    }

	private String getReferenceFromAssignment(PrismValue assignment) {
		Task task = createSimpleTask("getRefFromAssignment: Load role");
		OperationResult result = new OperationResult("getRefFromAssignment: Load role");
		PrismReference accountConstrRef = null;
		PrismReferenceValue prismRefValue = null;
		
		if(assignment instanceof PrismReferenceValue) {
			prismRefValue = (PrismReferenceValue) assignment;
		} else {
			PrismContainerValue prismContVal = (PrismContainerValue) assignment;
			accountConstrRef = prismContVal.findReference(AssignmentType.F_CONSTRUCTION);
			if(accountConstrRef == null) {
				accountConstrRef = prismContVal.findReference(AssignmentType.F_TARGET_REF);
			}
		}

		if (prismRefValue != null) {
			if(prismRefValue.getTargetType().equals(OrgType.COMPLEX_TYPE)) {
				PrismObject<OrgType> org = null;
				try {
					org = getModelService().getObject(OrgType.class, prismRefValue.getOid(), null, task,
							result);
				} catch (Exception ex) {
					result.recordFatalError("Unable to get orgUnit object", ex);
					showResultInSession(result);
					throw new RestartResponseException(PageUsers.class);
				}
				return WebMiscUtil.getName(org);
			} else {
				PrismObject<RoleType> role = null;
				try {
					role = getModelService().getObject(RoleType.class, prismRefValue.getOid(), null, task,
							result);
				} catch (Exception ex) {
					result.recordFatalError("Unable to get role object", ex);
					showResultInSession(result);
					throw new RestartResponseException(PageUsers.class);
				}
				return WebMiscUtil.getName(role);
			}
		}

		if (accountConstrRef != null) {
			if(accountConstrRef.getValue().getTargetType().equals(OrgType.COMPLEX_TYPE)) {
				PrismObject<OrgType> org = null;
				try {
					org = getModelService().getObject(OrgType.class, accountConstrRef.getValue().getOid(),
							null, task, result);
				} catch (Exception ex) {
					result.recordFatalError("Unable to get orgUnit object", ex);
					showResultInSession(result);
					throw new RestartResponseException(PageUsers.class);
				}
				return WebMiscUtil.getName(org);
			} else {
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
		}

		return "";
	}

	private PrismObject<ShadowType> getAccountFromDelta(ObjectDelta delta) {
		if (delta.getChangeType().equals(ChangeType.ADD)) {
			return delta.getObjectToAdd();
		} else {
			Task task = createSimpleTask("loadResourceList: Load account");
			OperationResult result = new OperationResult("loadResourceList: Load account");
			PrismObject<ShadowType> accountObject = null;
			try {
				accountObject = getModelService().getObject(ShadowType.class, delta.getOid(), null,
						task, result);
			} catch (Exception ex) {
				result.recordFatalError("Unable to get account object", ex);
				showResultInSession(result);
				throw new RestartResponseException(PageUsers.class);
			}
			return accountObject;
		}
	}

	private void savePerformed(AjaxRequestTarget target) {
		LOGGER.debug("Saving user changes.");
		OperationResult result = new OperationResult(OPERATION_SAVE_USER);

		try {
			Task task = createSimpleTask(OPERATION_SAVE_USER);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Delta before save user:\n{}", new Object[] { delta.debugDump(3) });
			}

            ModelExecuteOptions options = executeOptions.createOptions();
            LOGGER.debug("Using options {}.", new Object[]{executeOptions});
			getModelService().executeChanges(deltasChanges, options, task, result);

			result.recomputeStatus();
		} catch (Exception ex) {
			result.recordFatalError(getString("pageUserPreview.message.cantCreateUser"), ex);
			LoggingUtils.logException(LOGGER, getString("pageUserPreview.message.cantCreateUser"), ex);
		}

        boolean userAdded = delta != null && delta.isAdd() && StringUtils.isNotEmpty(delta.getOid());
        if (userAdded || result.isSuccess() || result.isHandledError() || result.isInProgress()) {
            showResultInSession(result);
            setResponsePage(PageUsers.class);
		} else {
            showResult(result);
            target.add(getFeedbackPanel());
		}
	}

	private class AdditionalDataIconColumn<T> extends IconColumn<T> {

		public AdditionalDataIconColumn(IModel<String> displayModel) {
			super(displayModel);
		}

		@Override
		protected IModel<String> createTitleModel(final IModel<T> rowModel) {
			return new AbstractReadOnlyModel<String>() {

				@Override
				public String getObject() {
					T dto = rowModel.getObject();

					if (dto instanceof SubmitUserDto) {
						SubmitUserDto submitDto = (SubmitUserDto) dto;
						return getTitle(submitDto.isSecondaryValue(), submitDto.isDeletedValue());
					} else if (dto instanceof SubmitAccountDto) {
						SubmitAccountDto submitDto = (SubmitAccountDto) dto;
						return getTitle(submitDto.isSecondaryValue(), submitDto.isDeletedValue());
					} else {
						SubmitAssignmentDto submitDto = (SubmitAssignmentDto) dto;
						return getTitle(submitDto.isSecondaryValue(), submitDto.isDeletedValue());
					}
				}
			};
		}

		@Override
		protected IModel<ResourceReference> createIconModel(final IModel<T> rowModel) {
			return new AbstractReadOnlyModel<ResourceReference>() {

				@Override
				public ResourceReference getObject() {
					T dto = rowModel.getObject();

					if (dto instanceof SubmitUserDto) {
						SubmitUserDto submitDto = (SubmitUserDto) dto;
						return getIcon(submitDto.isSecondaryValue(), submitDto.isDeletedValue());
					} else if (dto instanceof SubmitAccountDto) {
						SubmitAccountDto submitDto = (SubmitAccountDto) dto;
						return getIcon(submitDto.isSecondaryValue(), submitDto.isDeletedValue());
					} else {
						SubmitAssignmentDto submitDto = (SubmitAssignmentDto) dto;
						return getIcon(submitDto.isSecondaryValue(), submitDto.isDeletedValue());
					}
				}
			};
		}

		@Override
		protected IModel<AttributeModifier> createAttribute(final IModel<T> rowModel) {
			return new AbstractReadOnlyModel<AttributeModifier>() {

				@Override
				public AttributeModifier getObject() {
					T dto = rowModel.getObject();

					if (dto instanceof SubmitUserDto) {
						SubmitUserDto submitDto = (SubmitUserDto) dto;
						return getAttribute(submitDto.isSecondaryValue(), submitDto.isDeletedValue());

					} else if (dto instanceof SubmitAccountDto) {
						SubmitAccountDto submitDto = (SubmitAccountDto) dto;
						return getAttribute(submitDto.isSecondaryValue(), submitDto.isDeletedValue());
					} else {
						SubmitAssignmentDto submitDto = (SubmitAssignmentDto) dto;
						return getAttribute(submitDto.isSecondaryValue(), submitDto.isDeletedValue());
					}
				}
			};
		}

		private AttributeModifier getAttribute(boolean secondaryValue, boolean deletedValue) {
			if (deletedValue) {
				return new AttributeModifier("class", "deletedValue");
			} else if (secondaryValue) {
				return new AttributeModifier("class", "secondaryValue");
			} else {
				return new AttributeModifier("class", "primaryValue");
			}
		}

		private String getTitle(boolean secondaryValue, boolean deletedValue) {
			if (deletedValue) {
				return PageUserPreview.this.getString("pageUserPreview.deletedValue");
			} else if (secondaryValue) {
				return PageUserPreview.this.getString("pageUserPreview.secondaryValue");
			} else {
				return PageUserPreview.this.getString("pageUserPreview.primaryValue");
			}
		}

		private ResourceReference getIcon(boolean secondaryValue, boolean deletedValue) {
			if (deletedValue) {
                return ImgResources.createReference(ImgResources.DELETED_VALUE);
			} else if (secondaryValue) {
                return ImgResources.createReference(ImgResources.SECONDARY_VALUE);
			} else {
                return ImgResources.createReference(ImgResources.PRIMARY_VALUE);
			}
		}
	}
}
