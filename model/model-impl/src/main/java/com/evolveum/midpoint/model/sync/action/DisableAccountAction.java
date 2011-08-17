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

import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;

/**
 * 
 * @author Vilo Repan
 */
public class DisableAccountAction extends BaseAction {

	private static final Trace trace = TraceManager.getTrace(DisableAccountAction.class);

	@Override
	public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
			SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
			OperationResult result) throws SynchronizationException {
		if (!(change.getShadow() instanceof AccountShadowType)) {
			throw new SynchronizationException("Resource object is not account (class '"
					+ AccountShadowType.class + "'), but it's '" + change.getShadow().getClass() + "'.");
		}

		AccountShadowType account = (AccountShadowType) change.getShadow();
		ActivationType activation = account.getActivation();
		if (activation == null) {
			ObjectFactory of = new ObjectFactory();
			activation = of.createActivationType();
			account.setActivation(activation);
		}

		activation.setEnabled(false);

		try {
			AccountShadowType oldAccount = getModel().getObject(AccountShadowType.class, account.getOid(),
					new PropertyReferenceListType(), new OperationResult("Get Object"),
					true);

			ObjectModificationType changes = CalculateXmlDiff.calculateChanges(oldAccount, account);
			getModel().modifyObject(AccountShadowType.class, changes, new OperationResult("Modify Object"));
		} catch (DiffException ex) {
			trace.error("Couldn't disable account {}, error while creating diff: {}.",
					new Object[] { account.getOid(), ex.getMessage() });
			throw new SynchronizationException("Couldn't disable account " + account.getOid()
					+ ", error while creating diff: " + ex.getMessage() + ".", ex);
		} catch (Exception ex) {
			trace.error("Couldn't update (disable) account '{}' in provisioning, reason: {}.", new Object[] {
					account.getOid(), ex.getMessage() });
			throw new SynchronizationException("Couldn't update (disable) account '" + account.getOid()
					+ "' in provisioning, reason: " + ex.getMessage() + ".", ex);
		}

		return userOid;
	}
}
