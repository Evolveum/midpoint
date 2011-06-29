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
package com.evolveum.midpoint.model.sync;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.sync.action.Action;
import com.evolveum.midpoint.model.sync.action.ActionManager;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.SystemFaultType;

/**
 * 
 * @author lazyman
 * 
 */
@Service
public class SynchronizationService implements ResourceObjectChangeListener {

	private static final Trace LOGGER = TraceManager.getTrace(SynchronizationService.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	private ActionManager<Action> actionManager;

	@Override
	public void notifyChange(ResourceObjectShadowChangeDescriptionType change, OperationResult parentResult) {
		Validate.notNull(change, "Resource object shadow change description must not be null.");
		Validate.notNull(change.getObjectChange(), "Object change in change description must not be null.");
		Validate.notNull(change.getResource(), "Resource in change must not be null.");
		Validate.notNull(parentResult, "Parent operation result must not be null.");

		ResourceType resource = change.getResource();
		if (!isSynchronizationEnabled(resource.getSynchronization())) {
			return;
		}

		ResourceObjectShadowType resourceObjectShadow = change.getShadow();
		if (resourceObjectShadow == null && (change.getObjectChange() instanceof ObjectChangeAdditionType)) {
			// There may not be a previous shadow in addition. But in that case
			// we have (almost) everything in the ObjectChangeType - almost
			// everything except OID. But we can live with that.
			resourceObjectShadow = (ResourceObjectShadowType) ((ObjectChangeAdditionType) change
					.getObjectChange()).getObject();
		} else {
			throw new IllegalArgumentException("Change doesn't contain ResourceObjectShadow.");
		}
		
	}

	private boolean isSynchronizationEnabled(SynchronizationType synchronization) {
		if (synchronization == null || synchronization.isEnabled() == null) {
			return false;
		}

		return synchronization.isEnabled();
	}
}
