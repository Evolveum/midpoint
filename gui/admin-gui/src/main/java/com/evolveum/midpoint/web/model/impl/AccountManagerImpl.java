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
 */
package com.evolveum.midpoint.web.model.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.web.model.AccountManager;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public class AccountManagerImpl extends ObjectManagerImpl<AccountShadowType, AccountShadowDto> implements
		AccountManager {

	private static final long serialVersionUID = 3793939681394774533L;
	private static final Trace LOGGER = TraceManager.getTrace(AccountManagerImpl.class);

	@Override
	public Collection<AccountShadowDto> list(PagingType paging) {
		return list(paging, ObjectTypes.ACCOUNT);
	}

	@Override
	protected Class<? extends ObjectType> getSupportedObjectClass() {
		return AccountShadowType.class;
	}

	@Override
	protected AccountShadowDto createObject(AccountShadowType objectType) {
		return new AccountShadowDto(objectType);
	}

	@Override
	public Set<PropertyChange> submit(AccountShadowDto changedObject) {
		Validate.notNull(changedObject, "Changed account must not be null.");

		AccountShadowDto oldObject = get(changedObject.getOid(), Utils.getResolveResourceList());
		OperationResult result = new OperationResult(AccountManager.SUBMIT);
		try {
			ObjectModificationType changes = CalculateXmlDiff.calculateChanges(oldObject.getXmlObject(),
					changedObject.getXmlObject());
			if (changes != null && changes.getOid() != null) {
				getModel().modifyObject(changes, result);
			}
		} catch (DiffException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't update account {}, error while diffing", ex,
					changedObject.getName());
			result.recordFatalError("Couldn't update account '" + changedObject.getName()
					+ "', error while diffing.", ex);
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't update account {}, because it doesn't exists", ex,
					changedObject.getName());
			result.recordFatalError("Couldn't update account '" + changedObject.getName()
					+ "', because it doesn't exists.", ex);
		} catch (Exception ex) {

		}

		return new HashSet<PropertyChange>();
	}

	@Override
	public UserType listOwner(String oid) {
		Validate.notNull(oid, "Account oid must not be null.");

		UserType user = null;
		OperationResult result = new OperationResult(AccountManager.SUBMIT);
		try {
			user = getModel().listAccountShadowOwner(oid, result);
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list owner of account oid {}", ex, oid);
			result.recordFatalError("Couldn't list owner of account oid '" + oid + "'.", ex);
		}

		return user;
	}
}
