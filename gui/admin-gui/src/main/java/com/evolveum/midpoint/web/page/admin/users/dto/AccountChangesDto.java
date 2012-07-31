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

import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.web.page.admin.users.PageSubmit;

/**
 * @author mserbak
 */
public class AccountChangesDto implements Serializable {
	List<PrismObject> accountsList = new ArrayList<PrismObject>();
	List<SubmitAccountDto> accountChangesList = new ArrayList<SubmitAccountDto>();

	public AccountChangesDto(Collection<? extends ModelProjectionContext> accounts) {
		if (accounts == null) {
			return;
		}

		for (ModelProjectionContext account : accounts) {
			accountsList.add(account.getObjectNew());
			SubmitResourceDto resource = new SubmitResourceDto(account.getObjectNew(), false);
			getChanges(resource, account.getPrimaryDelta(), false);
			getChanges(resource, account.getSecondaryDelta(), true);

		}

	}

	private void getChanges(SubmitResourceDto resource, ObjectDelta account, boolean secondaryValue) {
		if (account == null || !account.getChangeType().equals(ChangeType.MODIFY)) {
			return;
		}
		for (Object modification : account.getModifications()) {
			ItemDelta modifyDelta = (ItemDelta) modification;
			List<String> oldValue = new ArrayList<String>();
			List<String> newValue = new ArrayList<String>();

			ItemDefinition def = modifyDelta.getDefinition();
			String attribute = def.getDisplayName() != null ? def.getDisplayName() : def.getName()
					.getLocalPart();

			if (modifyDelta.getValuesToDelete() != null) {
				for (Object valueToDelete : modifyDelta.getValuesToDelete()) {
					PrismPropertyValue value = (PrismPropertyValue) valueToDelete;
					oldValue.add(value == null ? "" : value.getValue().toString());
				}
			}

			if (modifyDelta.getValuesToAdd() != null) {
				for (Object valueToAdd : modifyDelta.getValuesToAdd()) {
					PrismPropertyValue value = (PrismPropertyValue) valueToAdd;
					newValue.add(value == null ? "" : value.getValue().toString());
				}
			}
			accountChangesList.add(new SubmitAccountDto(resource.getResourceName(), attribute, PageSubmit
					.listToString(oldValue), PageSubmit.listToString(newValue), secondaryValue));
		}
	}

	public List<PrismObject> getAccountsList() {
		return accountsList;
	}

	public List<SubmitAccountDto> getAccountChangesList() {
		return accountChangesList;
	}

}
