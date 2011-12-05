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

import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author Vilo Repan
 */
public class AddUserAction extends BaseAction {

    private static final Trace LOGGER = TraceManager.getTrace(AddUserAction.class);

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
                                 SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
                                 OperationResult result) throws SynchronizationException {
        super.executeChanges(userOid, change, situation, shadowAfterChange, result);

        OperationResult subResult = new OperationResult("Add User Action");
        result.addSubresult(subResult);

        try {
            UserType user = getUser(userOid, subResult);
            if (user == null) {
                user = new ObjectFactory().createUserType();

//				user = getSchemaHandler().processInboundHandling(user, shadowAfterChange, subResult);

                if (user.getName() == null) {
                    LOGGER.warn("Inbound expressions haven't generated 'name' property for user created from " + ObjectTypeUtil.toShortString(shadowAfterChange));
                }

                UserTemplateType userTemplate = null;
                String userTemplateOid = getUserTemplateOid();
                if (StringUtils.isNotEmpty(userTemplateOid)) {
                    userTemplate = getModel().getObject(UserTemplateType.class, userTemplateOid,
                            new PropertyReferenceListType(), subResult);
                }

//				userOid = getModel().addUser(user, userTemplate, MiscSchemaUtil.toCollection(ResourceObjectShadowUtil.getResourceOid(shadowAfterChange)), subResult);
            } else {
                LOGGER.debug("User with oid {} already exists, skipping create.",
                        new Object[]{user.getOid()});
            }
            subResult.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't perform Add User Action for shadow '{}', oid '{}'.",
                    ex, shadowAfterChange.getName(), shadowAfterChange.getOid());
            subResult.recordFatalError(
                    "Couldn't perform Add User Action for shadow '" + shadowAfterChange.getName()
                            + "', oid '" + shadowAfterChange.getOid() + "'.", ex);
            throw new SynchronizationException(ex.getMessage(), ex);
        }

        return userOid;
    }

    private String getUserTemplateOid() {
        List<Object> parameters = getParameters();
        Element userTemplateRef = getParameterElement(new QName(SchemaConstants.NS_C, "userTemplateRef"));

        if (userTemplateRef != null) {
            return userTemplateRef.getAttribute("oid");
        }

        return null;
    }
}
