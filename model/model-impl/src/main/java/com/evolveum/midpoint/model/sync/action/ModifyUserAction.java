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

import javax.xml.bind.JAXBException;

import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeDeletionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public class ModifyUserAction extends BaseAction {

	private static final Trace LOGGER = TraceManager.getTrace(ModifyUserAction.class);

	@Override
	public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
			SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
			OperationResult result) throws SynchronizationException {
		super.executeChanges(userOid, change, situation, shadowAfterChange, result);

		OperationResult subResult = new OperationResult("Modify User Action");
		result.addSubresult(subResult);

		UserType userType = getUser(userOid, subResult);
		if (userType == null) {
			String message = "Can't find user with oid '" + userOid + "'.";
			subResult.recordFatalError(message);
			throw new SynchronizationException(message);
		}

		// As this implementation is in fact diffing user before change and
		// after change,
		// it can easily be applied to modification and addition.
		// However, this is wrong. This approach may be appropriate for
		// addition.
		// But for modification we should be a bit smarter and process only the
		// list of
		// attributes that were really changed.

		if (change.getObjectChange() instanceof ObjectChangeDeletionType) {
			throw new SynchronizationException("The modifyUser action cannot be applied to deletion.");
		}

		try {
			UserType oldUserType = (UserType) JAXBUtil.clone(userType);

			if (shadowAfterChange.getResource() == null && shadowAfterChange.getResourceRef() != null) {
				resolveResource(shadowAfterChange);
			}

//			userType = getSchemaHandler().processInboundHandling(userType, shadowAfterChange, result);

			ObjectModificationType modification = CalculateXmlDiff.calculateChanges(oldUserType, userType);
			if (modification != null && modification.getOid() != null) {
				getModel().modifyObject(UserType.class, modification, subResult);
			} else {
				LOGGER.warn("Diff returned null for changes of user {}, caused by shadow {}",
						userType.getOid(), shadowAfterChange.getOid());
			}
		} catch (SchemaException ex) {
			throw new SynchronizationException("Can't handle inbound section in schema handling", ex);
		} catch (DiffException ex) {
			throw new SynchronizationException("Can't save user. Unexpected error: "
					+ "Couldn't create create diff.", ex);
		} catch (JAXBException ex) {
			throw new SynchronizationException("Couldn't clone user object '" + userOid + "', reason: "
					+ ex.getMessage(), ex);
		} catch (Exception ex) {
			throw new SynchronizationException("Can't save user", ex);
		}

		return userOid;
	}

	private ResourceObjectShadowType resolveResource(ResourceObjectShadowType shadowAfterChange)
			throws SynchronizationException {
		try {
			ResourceType resourceType = getModel().getObject(ResourceType.class,
					shadowAfterChange.getResourceRef().getOid(), new PropertyReferenceListType(),
					new OperationResult("Get Object"));

			shadowAfterChange.setResource(resourceType);
			shadowAfterChange.setResourceRef(null);
		} catch (Exception ex) {
			LOGGER.error("Failed to resolve resource with oid {}", shadowAfterChange.getResourceRef()
					.getOid(), ex);
			throw new SynchronizationException("Resource can't be resolved.", ex);
		}
		return shadowAfterChange;
	}
}
