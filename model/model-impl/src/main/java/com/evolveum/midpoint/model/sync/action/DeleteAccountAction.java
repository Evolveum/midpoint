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

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;

/**
 * 
 * @author Vilo Repan
 */
public class DeleteAccountAction extends BaseAction {

	private static Trace trace = TraceManager.getTrace(DeleteAccountAction.class);

	@Override
	public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
			SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
			OperationResult result) throws SynchronizationException {
		try {
			ScriptsType scripts = getScripts(change.getResource());
			getProvisioning().deleteObject(change.getShadow().getOid(), scripts,
					new OperationResult("Delete Object"));
		} catch (Exception ex) {
			ResourceType resource = change.getResource();
			String resourceName = resource == null ? "Undefined" : resource.getName();
			trace.error("Couldn't delete resource object with oid '{}' on resource '{}'.", new Object[] {
					change.getShadow().getOid(), resourceName });
			throw new SynchronizationException("Couldn't delete resource object with oid '"
					+ change.getShadow().getOid() + "' on resource '" + resourceName + "'.", ex);
		}

		return userOid;
	}
}
