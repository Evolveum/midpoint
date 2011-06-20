/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.model.impl;

import java.util.Collection;
import java.util.Set;

import javax.xml.ws.Holder;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.web.model.AccountShadowManager;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;

public class AccountShadowTypeManager extends AccountShadowManager {

	private static final long serialVersionUID = 4540270042561861862L;
	private static final Trace TRACE = TraceManager.getTrace(AccountShadowTypeManager.class);
	private Class<? extends AccountShadowDto> constructAccountShadowType;

	public AccountShadowTypeManager(Class<? extends AccountShadowDto> constructAccountShadowType) {
		this.constructAccountShadowType = constructAccountShadowType;
	}

	@Override
	public String add(AccountShadowDto accountShadowDto) throws WebModelException {
		Validate.notNull(accountShadowDto);

		try { // Call Web Service Operation
			String result = getModel().addObject(accountShadowDto.getXmlObject(),
					new Holder<OperationResultType>(new OperationResultType()));
			return result;
		} catch (FaultMessage ex) {
			throw new WebModelException(ex.getMessage(), "[Web Service Error] Add account failed");
		}
	}

	@Override
	public UserType listOwner(String oid) throws WebModelException {
		Validate.notNull(oid);

		try {
			UserType userType = getModel().listAccountShadowOwner(oid,
					new Holder<OperationResultType>(new OperationResultType()));
			return userType;
		} catch (FaultMessage ex) {
			throw new WebModelException(ex.getMessage(), "[Web Service Error] List owner failed.");
		}
	}

	@Override
	public AccountShadowDto create() {
		try {
			AccountShadowDto account = constructAccountShadowType.newInstance();
			// resource.setXmlObject(new ResourceType());
			return account;
		} catch (Exception ex) {
			throw new IllegalStateException("Couldn't create instance of '" + constructAccountShadowType
					+ "'.");
		}
	}

	@Override
	public Set<PropertyChange> submit(AccountShadowDto changedObject) throws WebModelException {
		AccountShadowDto oldObject = get(changedObject.getOid(), Utils.getResolveResourceList());
		try {
			ObjectModificationType changes = CalculateXmlDiff.calculateChanges(oldObject.getXmlObject(),
					changedObject.getXmlObject());
			if (changes != null && changes.getOid() != null) {
				getModel().modifyObject(changes, new Holder<OperationResultType>(new OperationResultType()));
			}
		} catch (FaultMessage ex) {
			throw new WebModelException(ex.getMessage(),
					"[Web Service Error] Submit account failed (Model service call failed)");
		} catch (DiffException ex) {
			throw new WebModelException(ex.getMessage(), "Submit account failed (XML Diff failed)");
		}

		// TODO: convert changes to GUI changes
		return null;
	}

	@Override
	public Collection<AccountShadowDto> list(PagingType paging) {
		return list(paging, ObjectTypes.ACCOUNT);
	}
}
