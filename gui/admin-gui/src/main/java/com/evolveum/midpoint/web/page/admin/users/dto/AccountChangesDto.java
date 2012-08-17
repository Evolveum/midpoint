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

package com.evolveum.midpoint.web.page.admin.users.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.RestartResponseException;

import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.users.PageSubmit;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;

/**
 * @author mserbak
 */
public class AccountChangesDto extends PageAdmin implements Serializable {
	private List<PrismObject> accountsList = new ArrayList<PrismObject>();
	private List<SubmitAccountDto> accountChangesList = new ArrayList<SubmitAccountDto>();
	private PrismObject oldAccountObject;
	private ArrayList<PrismObject> prismAccountsInSession;

	public AccountChangesDto(Collection<? extends ModelProjectionContext> accounts,
			ArrayList<PrismObject> prismAccountsInSession) {
		if (accounts == null) {
			return;
		}
		this.prismAccountsInSession = prismAccountsInSession;
		for (ModelProjectionContext account : accounts) {
			accountsList.add(account.getObjectNew());
			this.oldAccountObject = account.getObjectOld();
			SubmitResourceDto resource = new SubmitResourceDto(account.getObjectNew(), false);
			if (!getChanges(resource, account.getPrimaryDelta(), false)) {
				getChanges(resource, account.getSecondaryDelta(), true);
			}
		}
	}

	private boolean getChanges(SubmitResourceDto resource, ObjectDelta delta, boolean secondaryValue) {
		// Return true if modification is delete

		if (delta == null) {
			return false;
		} else if (delta.getChangeType().equals(ChangeType.DELETE)) {
			if (prismAccountsInSession == null) {
				addAccountFromResourceForDelete(new SubmitResourceDto(oldAccountObject, false));
			} else {
				for (PrismObject account : prismAccountsInSession) {
					addAccountFromResourceForDelete(new SubmitResourceDto(account, false));
				}
			}
			return true;
		} else if (delta.getChangeType().equals(ChangeType.ADD)) {
			PrismObject object = delta.getObjectToAdd();

			ObjectWrapper wrapper = new ObjectWrapper(null, null, object, ContainerStatus.ADDING);
			for (ContainerWrapper containerWrapper : wrapper.getContainers()) {
				for (Object propertyValueObject : containerWrapper.getProperties()) {
					List<SubmitAccountChangesDto> values = new ArrayList<SubmitAccountChangesDto>();
					PropertyWrapper propertyValue = (PropertyWrapper) propertyValueObject;

					for (Object valueObject : propertyValue.getValues()) {
						ValueWrapper value = (ValueWrapper) valueObject;
						if (value.getValue().getValue() != null) {
							values.add(new SubmitAccountChangesDto(value.getValue(), SubmitStatus.ADDING));
						}
					}
					PropertyDelta propertyDelta = new PropertyDelta(containerWrapper.getPath(), propertyValue
							.getItem().getDefinition());
					if (!values.isEmpty()) {
						getDeltasFromAccount(resource, values, propertyDelta, secondaryValue);
					}
				}
			}

			return false;
		}

		for (Object modification : delta.getModifications()) {
			ItemDelta modifyDelta = (ItemDelta) modification;

			List<SubmitAccountChangesDto> values = new ArrayList<SubmitAccountChangesDto>();

			if (modifyDelta.getValuesToDelete() != null) {
				for (Object valueToDelete : modifyDelta.getValuesToDelete()) {
					PrismPropertyValue value = (PrismPropertyValue) valueToDelete;
					values.add(new SubmitAccountChangesDto(value, SubmitStatus.DELETING));
				}
			}

			if (modifyDelta.getValuesToAdd() != null) {
				for (Object valueToAdd : modifyDelta.getValuesToAdd()) {
					PrismPropertyValue value = (PrismPropertyValue) valueToAdd;
					values.add(new SubmitAccountChangesDto(value, SubmitStatus.ADDING));
				}
			}

			if (modifyDelta.getValuesToReplace() != null) {
				for (Object valueToReplace : modifyDelta.getValuesToReplace()) {
					PrismPropertyValue value = (PrismPropertyValue) valueToReplace;
					values.add(new SubmitAccountChangesDto(value, SubmitStatus.REPLACEING));
				}
			}

			if (!values.isEmpty()) {
				getDeltasFromAccount(resource, values, modifyDelta, secondaryValue);
			}
		}
		return false;
	}

	private void addAccountFromResourceForDelete(SubmitResourceDto resourceDto) {
		SubmitAccountDto submitedAccount = new SubmitAccountDto(resourceDto.getResourceName(),
				getString("schema.objectTypes.account"), resourceDto.getName(), "", false);
		if (!accountChangesList.contains(submitedAccount)) {
			accountChangesList.add(submitedAccount);
		}
	}

	private void getDeltasFromAccount(SubmitResourceDto resource, List<SubmitAccountChangesDto> values,
			ItemDelta modifyDelta, boolean secondaryValue) {
		ItemDefinition def = modifyDelta.getDefinition();
		String attribute = def.getDisplayName() != null ? def.getDisplayName() : def.getName().getLocalPart();

		PropertyPath passwordPath = new PropertyPath(SchemaConstantsGenerated.C_CREDENTIALS,
				CredentialsType.F_PASSWORD);

		if (passwordPath.equals(modifyDelta.getParentPath())
				&& PasswordType.F_PROTECTED_STRING.equals(def.getName())) {
			attribute = "Password";
		}

		if (passwordPath.equals(modifyDelta.getParentPath())
				&& PasswordType.F_PROTECTED_STRING.equals(def.getName())) {
			attribute = "Password";
		}
		List<String> oldValues = new ArrayList<String>();
		List<String> newValues = new ArrayList<String>();

		if (!prismAccountsInSession.isEmpty()) {
			for (PrismObject accountInSession : prismAccountsInSession) {
				if (accountInSession.getOid() == null) {
					continue;
				}
				if (accountInSession.getOid().equals(oldAccountObject.getOid())) {
					Item oldAccountValue = accountInSession.findItem(modifyDelta.getPath());
					boolean exist = true;
					if (oldAccountValue != null && oldAccountValue.getValues() != null) {
						for (Object valueObject : oldAccountValue.getValues()) {
							PrismPropertyValue oldValue = (PrismPropertyValue) valueObject;
							oldValues.add(oldValue.getValue() != null ? oldValue.getValue().toString() : " ");

							// Getting remaining values from oldValues​​ ​​for
							// newValues​​
							for (SubmitAccountChangesDto newValue : values) {
								if (newValue.getStatus().equals(SubmitStatus.REPLACEING)) {
									continue;
								}
								exist = false;
								String newValueObjectString = newValue.getSubmitedValue().getValue()
										.toString();
								if (newValueObjectString.equals(oldValue.getValue().toString())) {
									exist = true;
									break;
								}
							}
							if (!exist) {
								newValues.add(oldValue.getValue().toString());
							}
						}
					}
				}
			}
		}

		for (SubmitAccountChangesDto newValue : values) {
			if (newValue.getStatus().equals(SubmitStatus.DELETING)) {
				continue;
			}

			Object newValueObject = newValue.getSubmitedValue().getValue();
			PropertyDelta parent = (PropertyDelta) newValue.getSubmitedValue().getParent();
			if (newValueObject instanceof ProtectedStringType
					|| (parent != null && parent.getParentPath().equals(SchemaConstants.PATH_PASSWORD))) {
				newValues.add("*****");
				continue;
			}
			String stringValue;
			if (newValueObject instanceof PolyString) {
				PolyString polyStringValue = (PolyString) newValueObject;
				stringValue = polyStringValue.getOrig() != null ? newValueObject.toString() : "";
			} else {
				stringValue = newValueObject != null ? newValueObject.toString() : "";
			}
			newValues.add(stringValue);
		}
		accountChangesList.add(new SubmitAccountDto(resource.getResourceName(), attribute, PageSubmit
				.listToString(oldValues), PageSubmit.listToString(newValues), secondaryValue));
	}

	public List<PrismObject> getAccountsList() {
		return accountsList;
	}

	public List<SubmitAccountDto> getAccountChangesList() {
		return accountChangesList;
	}

}
