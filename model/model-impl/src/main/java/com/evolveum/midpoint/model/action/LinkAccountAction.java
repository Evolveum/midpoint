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

package com.evolveum.midpoint.model.action;

import javax.xml.bind.JAXBException;
import javax.xml.ws.Holder;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.SynchronizationException;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author Vilo Repan
 */
public class LinkAccountAction extends BaseAction {

	private static Trace trace = TraceManager.getTrace(LinkAccountAction.class);

	@Override
	public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
			SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
			OperationResultType resultType) throws SynchronizationException {
		UserType userType = getUser(userOid, resultType);
		UserType oldUserType = null;
		try {
			oldUserType = (UserType) JAXBUtil.clone(userType);
		} catch (JAXBException ex) {
			// TODO: logging
			throw new SynchronizationException("Couldn't clone user.", ex);
		}
		ResourceObjectShadowType resourceShadow = change.getShadow();

		if (userType != null) {
			if (!(resourceShadow instanceof AccountShadowType)) {
				throw new SynchronizationException("Can't link resource object of type '"
						+ resourceShadow.getClass() + "', only '" + AccountShadowType.class
						+ "' can be linked.");
			}

			ObjectReferenceType accountRef = new ObjectReferenceType();
			accountRef.setOid(resourceShadow.getOid());
			accountRef.setType(ObjectTypes.ACCOUNT.getQName());
			userType.getAccountRef().add(accountRef);

			try {
				ObjectModificationType changes = CalculateXmlDiff.calculateChanges(oldUserType, userType);
				getModel().modifyObject(changes, new Holder<OperationResultType>(resultType));
			} catch (com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage ex) {
				trace.error("Error while saving user {} (modifyObject on model).", new Object[] { userOid });
				throw new SynchronizationException("Can't link account. Can't save user", ex,
						ex.getFaultInfo());
			} catch (DiffException ex) {
				trace.error("Couldn't create user diff for '{}', reason: {}.",
						new Object[] { userOid, ex.getMessage() });
				throw new SynchronizationException("Couldn't create user diff for '" + userOid
						+ "', reason: " + ex.getMessage(), ex);
			}
		} else {
			throw new SynchronizationException("User with oid '" + userOid
					+ "' doesn't exits. Try insert create action before this action.");
		}

		return userOid;
	}
}
