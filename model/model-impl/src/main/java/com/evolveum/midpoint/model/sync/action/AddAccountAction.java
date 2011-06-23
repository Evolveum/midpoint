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

import javax.xml.ws.Holder;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.SynchronizationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;

/**
 * 
 * @author Vilo Repan
 */
public class AddAccountAction extends BaseAction {

	private static Trace trace = TraceManager.getTrace(AddAccountAction.class);

	@Override
	public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
			SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
			OperationResultType resultType) throws SynchronizationException {
		if (!(change.getShadow() instanceof AccountShadowType)) {
			throw new SynchronizationException("Resource object is not account (class '"
					+ AccountShadowType.class + "'), but it's '" + change.getShadow().getClass() + "'.");
		}

		AccountShadowType account = (AccountShadowType) change.getShadow();

		// account password generator
		// int randomPasswordLength = getRandomPasswordLength(account);
		// if (randomPasswordLength != -1) {
		// generatePassword(account, randomPasswordLength);
		// }
		// account password generator end

		// UserType userType = getUser(userOid);
		Utils.unresolveResource(account);
		try {
			// trace.debug("Applying outbound schema handling on account '{}'.",
			// account.getOid());
			// SchemaHandling util = new SchemaHandling();
			// util.setModel(getModel());
			// account = (AccountShadowType)
			// util.applyOutboundSchemaHandlingOnAccount(userType, account);
			// ScriptsType scripts = getScripts(change.getResource());
			//
			// trace.debug("Adding account '{}' to provisioning.",
			// account.getOid());
			// provisioning.addObject(container, scripts, new
			// Holder<OperationalResultType>());
			getModel().addObject(account, new Holder<OperationResultType>(resultType));
			// } catch (SchemaHandlingException ex) {
			// trace.error("Couldn't add account to provisioning: Couldn't apply resource outbound schema handling "
			// +
			// "(resource '{}') on account '{}', reason: {}", new
			// Object[]{change.getResource().getOid(),
			// account.getOid(), ex.getMessage()});
			// throw new
			// SynchronizationException("Couldn't add account to provisioning: Couldn't apply resource "
			// +
			// "outbound schema handling (resource '" +
			// change.getResource().getOid() + "') on account '" +
			// account.getOid() + "', reason: " + ex.getMessage() + ".",
			// ex.getFaultType());
		} catch (FaultMessage ex) {
			trace.error("Couldn't add account to provisioning, reason: " + getMessage(ex));
			throw new SynchronizationException("Can't add account to provisioning.", ex, ex.getFaultInfo());
		}

		return userOid;
	}

	private String getMessage(FaultMessage ex) {
		String message = null;
		if (ex.getFaultInfo() != null) {
			message = ex.getFaultInfo().getMessage();
		} else {
			message = ex.getMessage();
		}

		return message;
	}
}
