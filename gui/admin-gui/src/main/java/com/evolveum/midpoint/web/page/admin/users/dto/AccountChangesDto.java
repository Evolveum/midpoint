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

package com.evolveum.midpoint.web.page.admin.users.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;

/**
 * @author mserbak
 */
public class AccountChangesDto extends PageAdmin implements Serializable {
	private List<AccountDto> accountsList = new ArrayList<AccountDto>();
	private List<SubmitAccountDto> accountChangesList = new ArrayList<SubmitAccountDto>();
	private PrismObject oldAccountObject;
	private ArrayList<PrismObject> accountsBeforeModify;
	private SynchronizationPolicyDecision syncPolicy;

	public AccountChangesDto(Collection<? extends ModelProjectionContext> accounts,
			ArrayList<PrismObject> accountsBeforeModify) {
		if (accounts == null) {
			return;
		}
		this.accountsBeforeModify = accountsBeforeModify;
		for (ModelProjectionContext account : accounts) {
			this.oldAccountObject = account.getObjectOld();
			syncPolicy = account.getSynchronizationPolicyDecision();
			accountsList.add(new AccountDto(account.getObjectNew() == null ? account.getObjectOld() : account
					.getObjectNew(), syncPolicy));
			
			
			SubmitResourceDto resource = new SubmitResourceDto(account.getObjectNew(), false);
			if (!getChanges(resource, account.getPrimaryDelta(), account, false)) {
				getChanges(resource, account.getSecondaryDelta(), account, true);
			}
		}
	}

	private boolean getChanges(SubmitResourceDto resource, ObjectDelta delta, ModelProjectionContext account, boolean secondaryValue) {
		// Return true if modification is delete or unlink
		if(delta == null && syncPolicy != null && syncPolicy.equals(SynchronizationPolicyDecision.UNLINK)) {
			return true;
		}

		if (delta == null) {
			return false;
		} else if (delta.getChangeType().equals(ChangeType.DELETE)) {
			if (accountsBeforeModify == null) {
				// add deleted resource to changeList
				// addAccountFromResourceForDelete(new SubmitResourceDto(oldAccountObject, false));
			} else {
				for (PrismObject prismAccount : accountsBeforeModify) {
					if(oldAccountObject != null && prismAccount.getOid().equals(oldAccountObject.getOid())) {
						// add deleted resource to changeList
						// addAccountFromResourceForDelete(new SubmitResourceDto(prismAccount, false));
					}
					
				}
			}
			return true;
		} else if (delta.getChangeType().equals(ChangeType.ADD)) {
			PrismObject object = delta.getObjectToAdd();

			ObjectWrapper wrapper = new ObjectWrapper(null, null, object, ContainerStatus.ADDING);
            if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
                showResultInSession(wrapper.getResult());
            }
			for (ContainerWrapper containerWrapper : wrapper.getContainers()) {
				for (Object propertyValueObject : containerWrapper.getProperties()) {
					List<SubmitAccountChangesDto> values = new ArrayList<SubmitAccountChangesDto>();
					PropertyWrapper propertyValue = (PropertyWrapper) propertyValueObject;
					if (propertyValue.getDisplayName().equals("password")) {
						continue;
					}
					for (Object valueObject : propertyValue.getValues()) {
						ValueWrapper value = (ValueWrapper) valueObject;
                        value.normalize();
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
				getString("schema.objectTypes.account"), resourceDto.getName(), "", getString("OriginType.null"), false);
		if (!accountChangesList.contains(submitedAccount)) {
			accountChangesList.add(submitedAccount);
		}
	}

	private void getDeltasFromAccount(SubmitResourceDto resource, List<SubmitAccountChangesDto> values,
			ItemDelta modifyDelta, boolean secondaryValue) {
		ItemDefinition def = modifyDelta.getDefinition();
		String attribute = def.getDisplayName() != null ? def.getDisplayName() : def.getName().getLocalPart();

		ItemPath passwordPath = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
				CredentialsType.F_PASSWORD);

		if (passwordPath.equals(modifyDelta.getParentPath())
				&& PasswordType.F_VALUE.equals(def.getName())) {
			attribute = "Password";
		}
		List<String> oldValues = new ArrayList<String>();
		List<String> newValues = new ArrayList<String>();

		if (!accountsBeforeModify.isEmpty() && oldAccountObject != null) {
			for (PrismObject accountInSession : accountsBeforeModify) {
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
		OriginType originType = null;

		for (SubmitAccountChangesDto newValue : values) {
			originType = newValue.getSubmitedValue().getOriginType();
			if (newValue.getStatus().equals(SubmitStatus.DELETING)) {
				continue;
			}

			Object newValueObject = newValue.getSubmitedValue().getValue();
			PropertyDelta parent = (PropertyDelta) newValue.getSubmitedValue().getParent();
			if (newValueObject instanceof ProtectedStringType
					|| (parent != null && parent.getParentPath().equals(SchemaConstants.PATH_PASSWORD))) {
				newValues.add("*****");
				attribute = "Password";
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
		accountChangesList.add(new SubmitAccountDto(resource.getResourceName(), attribute, StringUtils.join(
				oldValues, ", "), StringUtils.join(newValues, ", "), getString("OriginType." + originType), secondaryValue));
	}

	public List<AccountDto> getAccountsList() {
		return accountsList;
	}

	public List<SubmitAccountDto> getAccountChangesList() {
		return accountChangesList;
	}

}
