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
import javax.xml.ws.Holder;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.model.xpath.SchemaHandlingException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeDeletionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author Vilo Repan
 */
public class ModifyUserAction extends BaseAction {

	private static transient Trace logger = TraceManager.getTrace(ModifyUserAction.class);

	// @Autowired
	// private SchemaHandling schemaHandling;

	@Override
	public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
			SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
			OperationResultType resultType) throws SynchronizationException {
		UserType userType = getUser(userOid, resultType);
		if (userType == null) {
			throw new SynchronizationException("Can't find user with oid '" + userOid + "'.");
		}

		// As this implementation is in fact diffing user before change and
		// after change,
		// it can easily be applied to modification and addintion.
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

			userType = getSchemaHandling().applyInboundSchemaHandlingOnUser(userType, shadowAfterChange);
			ObjectFactory of = new ObjectFactory();
			ObjectContainerType userContainer = of.createObjectContainerType();
			userContainer.setObject(userType);

			ObjectModificationType modification = CalculateXmlDiff.calculateChanges(oldUserType, userType);
			if (modification != null && modification.getOid() != null) {
				getModel().modifyObject(modification, new Holder<OperationResultType>(resultType));
			} else {
				logger.warn("Diff returned null for changes of user {}, caused by shadow {}",
						userType.getOid(), shadowAfterChange.getOid());
			}
		} catch (SchemaHandlingException ex) {
			throw new SynchronizationException("Can't handle inbound section in schema handling", ex);
		} catch (com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage ex) {
			throw new SynchronizationException("Can't save user", ex);
		} catch (DiffException ex) {
			throw new SynchronizationException("Can't save user. Unexpected error: "
					+ "Couldn't create create diff.", ex);
		} catch (JAXBException ex) {
			throw new SynchronizationException("Couldn't clone user object '" + userOid + "', reason: "
					+ ex.getMessage(), ex);
		}

		return userOid;
	}

	private ResourceObjectShadowType resolveResource(ResourceObjectShadowType shadowAfterChange)
			throws SynchronizationException {
		try {
			ResourceType resourceType = (ResourceType) getRepository().getObject(
					shadowAfterChange.getResourceRef().getOid(), new PropertyReferenceListType(),
					new OperationResult("Get Object"));

			shadowAfterChange.setResource(resourceType);
			shadowAfterChange.setResourceRef(null);
		} catch (Exception ex) {
			logger.error("Failed to resolve resource with oid {}", shadowAfterChange.getResourceRef()
					.getOid(), ex);
			throw new SynchronizationException("Resource can't be resolved.", ex);
		}
		return shadowAfterChange;
	}
}
