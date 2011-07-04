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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author Vilo Repan
 */
public class AddUserAction extends BaseAction {

	private static Trace LOGGER = TraceManager.getTrace(AddUserAction.class);

	@Override
	public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
			SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
			OperationResult result) throws SynchronizationException {
		Validate.notNull(change, "Resource object change description must not be null.");
		Validate.notNull(situation, "Synchronization situation must not be null.");
		Validate.notNull(shadowAfterChange, "Resource object shadow after change must not be null.");
		Validate.notNull(result, "Operation result must not be null.");

		OperationResult subResult = new OperationResult("Add User Action");
		result.addSubresult(subResult);

		try {
			UserType user = getUser(userOid, subResult);
			if (user == null) {
				user = new ObjectFactory().createUserType();

				UserTemplateType userTemplate = null;
				String userTemplateOid = getUserTemplateOid();
				if (StringUtils.isNotEmpty(userTemplateOid)) {
					userTemplate = getModel().getObject(userTemplateOid, new PropertyReferenceListType(),
							subResult, UserTemplateType.class);
				}

				userOid = getModel().addUser(user, userTemplate, subResult);
			} else {
				LOGGER.debug("User with oid {} already exists, skipping create.",
						new Object[] { user.getOid() });
			}
			subResult.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't perform Add User Action for shadow '', oid ''.", ex,
					shadowAfterChange.getName(), shadowAfterChange.getOid());
			subResult.recordFatalError(
					"Couldn't perform Add User Action for shadow '" + shadowAfterChange.getName()
							+ "', oid '" + shadowAfterChange.getOid() + "'.", ex);
			throw new SynchronizationException(ex.getMessage(), ex);
		}

		return userOid;
	}

	private String getUserTemplateOid() {
		List<Object> parameters = getParameters();
		Element userTemplateRef = null;
		for (Object object : parameters) {
			if (!(object instanceof Element)) {
				continue;
			}
			Element element = (Element) object;
			if ("userTemplateRef".equals(element.getLocalName())
					&& SchemaConstants.NS_C.equals(element.getNamespaceURI())) {
				userTemplateRef = element;
				break;
			}
		}

		if (userTemplateRef != null) {
			return userTemplateRef.getAttribute("oid");
		}

		return null;
	}
}
