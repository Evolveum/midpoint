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

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.SynchronizationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;

/**
 *
 * @author Vilo Repan
 */
public class DeleteUserAction extends BaseAction {

    private static Trace trace = TraceManager.getTrace(DeleteUserAction.class);

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
            SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange) throws SynchronizationException {
        if (userOid == null) {
            throw new SynchronizationException("Can't delete user, because user oid is null.");
        }

        try {
            getModel().deleteObject(userOid);
        } catch (FaultMessage ex) {
            String message = ex.getFaultInfo() == null ? ex.getMessage() : ex.getFaultInfo().getMessage();
            trace.error("Couldn't delete user {}, reason: {}", userOid, message);
            throw new SynchronizationException("Couldn't delete user '" + userOid + "'.", ex, ex.getFaultInfo());
        }

        return null;
    }
}
