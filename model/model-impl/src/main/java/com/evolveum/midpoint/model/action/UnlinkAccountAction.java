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

import com.evolveum.midpoint.model.*;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.diff.CalculateXmlDiff;
import com.evolveum.midpoint.util.diff.DiffException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import java.util.List;

/**
 *
 * @author Vilo Repan
 */
public class UnlinkAccountAction extends BaseAction {

    private static Trace trace = TraceManager.getTrace(UnlinkAccountAction.class);

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
            SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange) throws SynchronizationException {
        trace.trace("executeChanges::start");

        UserType userType = getUser(userOid);
        UserType oldUserType = getUser(userOid);
        ResourceObjectShadowType resourceShadow = change.getShadow();

        if (userType == null) {
            throw new SynchronizationException("Can't unlink account. User with oid '" + userOid +
                    "' doesn't exits. Try insert create action before this action.");
        }

        List<ObjectReferenceType> references = userType.getAccountRef();
        ObjectReferenceType accountRef = null;
        for (ObjectReferenceType reference : references) {
            if (!SchemaConstants.I_ACCOUNT_REF.equals(reference.getType())) {
                continue;
            }

            if (reference.getOid().equals(resourceShadow.getOid())) {
                accountRef = reference;
                break;
            }
        }
        if (accountRef != null) {
            trace.debug("Removing account ref {} from user {}.", new Object[]{accountRef.getOid(), userOid});
            references.remove(accountRef);

            try {
                ObjectModificationType changes = CalculateXmlDiff.calculateChanges(oldUserType, userType);
                getModel().modifyObject(changes);
            } catch (com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage ex) {
                throw new SynchronizationException("Can't unlink account. Can't save user",
                        ex, ex.getFaultInfo());
            } catch (DiffException ex) {
                trace.error("Couldn't create user diff for '{}', reason: {}.", userOid, ex.getMessage());
                throw new SynchronizationException("Couldn't create user diff for '" + userOid + "'.", ex);
            }
        }

        trace.trace("executeChanges::end");
        return userOid;
    }
}
