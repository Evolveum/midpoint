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

import javax.xml.bind.JAXBElement;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.sync.action.Action;
import com.evolveum.midpoint.model.sync.action.ActionManager;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeDeletionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationType;

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

		ResourceObjectShadowType objectShadow = change.getShadow();
		if (objectShadow == null && (change.getObjectChange() instanceof ObjectChangeAdditionType)) {
			// There may not be a previous shadow in addition. But in that case
			// we have (almost) everything in the ObjectChangeType - almost
			// everything except OID. But we can live with that.
			objectShadow = (ResourceObjectShadowType) ((ObjectChangeAdditionType) change.getObjectChange())
					.getObject();
		} else {
			throw new IllegalArgumentException("Change doesn't contain ResourceObjectShadow.");
		}

		ResourceObjectShadowType objectShadowAfterChange = getObjectAfterChange(objectShadow,
				change.getObjectChange());
		SynchronizationSituation situation = checkSituation(change);

		notifyChange(change, situation, resource, objectShadowAfterChange, parentResult);
	}

	/**
	 * Apply the changes to the provided shadow.
	 * 
	 * @param objectShadow
	 *            shadow with some data
	 * @param change
	 *            changes to be applied
	 */
	@SuppressWarnings("unchecked")
	private ResourceObjectShadowType getObjectAfterChange(ResourceObjectShadowType objectShadow,
			ObjectChangeType change) {
		if (change instanceof ObjectChangeAdditionType) {
			ObjectChangeAdditionType objectAddition = (ObjectChangeAdditionType) change;
			ObjectType object = objectAddition.getObject();
			if (object instanceof ResourceObjectShadowType) {
				return (ResourceObjectShadowType) object;
			} else {
				throw new IllegalArgumentException("The changed object is not a shadow, it is "
						+ object.getClass().getName());
			}
		} else if (change instanceof ObjectChangeModificationType) {
			try {
				ObjectChangeModificationType objectModification = (ObjectChangeModificationType) change;
				ObjectModificationType modification = objectModification.getObjectModification();
				PatchXml patchXml = new PatchXml();

				String patchedXml = patchXml.applyDifferences(modification, objectShadow);
				ResourceObjectShadowType changedResourceShadow = ((JAXBElement<ResourceObjectShadowType>) JAXBUtil
						.unmarshal(patchedXml)).getValue();
				return changedResourceShadow;
			} catch (Exception ex) {
				throw new SystemException(ex.getMessage(), ex);
			}
		} else if (change instanceof ObjectChangeDeletionType) {
			// in case of deletion the object has already all that it can have
			return objectShadow;
		} else {
			throw new IllegalArgumentException("Unknown change type " + change.getClass().getName());
		}
	}

	private boolean isSynchronizationEnabled(SynchronizationType synchronization) {
		if (synchronization == null || synchronization.isEnabled() == null) {
			return false;
		}

		return synchronization.isEnabled();
	}

	private SynchronizationSituation checkSituation(ResourceObjectShadowChangeDescriptionType change) {
		return null;
	}

	private void notifyChange(ResourceObjectShadowChangeDescriptionType change,
			SynchronizationSituation situation, ResourceType resource,
			ResourceObjectShadowType objectShadowAfterChange, OperationResult parentResult) {

	}
}
