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

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author lazyman
 * 
 */
public class LinkAccountAction extends BaseAction {

	private static final Trace LOGGER = TraceManager.getTrace(LinkAccountAction.class);

	@Override
	public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
			SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
			OperationResult result) throws SynchronizationException {
		LOGGER.trace("Executing link account action.");
		super.executeChanges(userOid, change, situation, shadowAfterChange, result);

		OperationResult subResult = new OperationResult("Link Account Action");
		result.addSubresult(subResult);

		UserType user = getUser(userOid, result);
		if (user == null) {
			LOGGER.trace("User with oid {} doesn't exits. Try insert create action before this action.",
					new Object[] { userOid });

			String message = "User with oid '" + userOid
					+ "' doesn't exits. Try insert create action before this action.";
			subResult.recordFatalError(message);
			throw new SynchronizationException(message);
		}
		LOGGER.trace("Found user {} for link account.", new Object[] { user.getName() });
		try {
			if (shadowAfterChange instanceof AccountShadowType) {
				LOGGER.trace("Creating change for account shadow (accountRef).");
				
				ObjectReferenceType accountRef = new ObjectReferenceType();
				accountRef.setOid(shadowAfterChange.getOid());
				accountRef.setType(ObjectTypes.ACCOUNT.getTypeQName());

				ObjectModificationType changes = new ObjectModificationType();
				changes.setOid(user.getOid());
				changes.getPropertyModification().add(
						ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.add, null,
								SchemaConstants.I_ACCOUNT_REF, accountRef));

//				getModel().modifyObject(changes, subResult);
				getModel().modifyObjectWithExclusion(changes, accountRef.getOid(), subResult);
			} else {
				LOGGER.debug("Skipping link account to user, shadow in change is not AccountShadowType.");
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't link account {} to user {}.", ex,
					shadowAfterChange.getName(), user.getName());
			subResult.recordFatalError("Couldn't link account '" + shadowAfterChange.getName()
					+ "' to user '" + user.getName() + "'.", ex);
			throw new SynchronizationException(ex.getMessage(), ex);
		}

		return userOid;
	}
}
