/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.sync.action;

import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import org.apache.commons.lang.StringUtils;

/**
 * @author lazyman
 */
public class DeleteUserAction extends BaseAction {

    private static final Trace LOGGER = TraceManager.getTrace(DeleteUserAction.class);

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change,
            SynchronizationSituationType situation,
            OperationResult result) throws SynchronizationException {
        super.executeChanges(userOid, change, situation, result);

        OperationResult subResult = result.createSubresult(ACTION_DELETE_USER);

        if (StringUtils.isEmpty(userOid)) {
            String message = "Can't delete user, because user oid is null.";
            subResult.recordFatalError(message);
            LOGGER.warn("User oid is null or empty, that means, user was probably already deleted.");
            return userOid;
        }

        try {
            //todo create sync context, delete user through it


            userOid = null;
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't delete user {}", ex, userOid);
            subResult.recordFatalError("Couldn't delete user '" + userOid + "'.", ex);
            throw new SynchronizationException("Couldn't delete user '" + userOid + "', reason: "
                    + ex.getMessage(), ex);
        } finally {
            subResult.recomputeStatus();
        }

        return userOid;
    }
}
