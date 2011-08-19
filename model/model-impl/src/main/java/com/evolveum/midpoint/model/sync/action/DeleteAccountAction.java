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

package com.evolveum.midpoint.model.sync.action;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;

/**
 * 
 * @author lazyman
 * 
 */
public class DeleteAccountAction extends BaseAction {

	private static final Trace LOGGER = TraceManager.getTrace(DeleteAccountAction.class);

	@Override
	public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
			SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
			OperationResult result) throws SynchronizationException {
		super.executeChanges(userOid, change, situation, shadowAfterChange, result);

		OperationResult subResult = new OperationResult("Delete Account Action");
		result.addSubresult(subResult);

		if (!(shadowAfterChange instanceof AccountShadowType)) {
			subResult.recordWarning("Resource object is not account (class '" + AccountShadowType.class
					+ "'), but it's '" + shadowAfterChange.getClass() + "'.");
			return userOid;
		}

		try {
			getModel().deleteObject(shadowAfterChange.getClass(), shadowAfterChange.getOid(), subResult);
			subResult.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't delete resource object shadow {}, oid {}.", ex,
					shadowAfterChange.getName(), shadowAfterChange.getOid());
			String message = "Couldn't delete resource object shadow '" + shadowAfterChange.getName()
					+ "', oid '" + shadowAfterChange.getOid() + "'.";
			subResult.recordFatalError(message, ex);
			throw new SynchronizationException(message, ex);
		}

		return userOid;
	}
}
