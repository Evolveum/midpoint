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

package com.evolveum.midpoint.model.sync.action;

import com.evolveum.midpoint.model.ActivationDecision;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;

/**
 * @author Vilo Repan
 */
public class DisableAccountAction extends ModifyUserAction {

    private static final Trace trace = TraceManager.getTrace(DisableAccountAction.class);

    public DisableAccountAction() {
        super(null, ACTION_DISABLE_ACCOUNT, ActivationDecision.DISABLE);
    }

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
                                 SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
                                 OperationResult result) throws SynchronizationException {
        if (!(change.getShadow() instanceof AccountShadowType)) {
            throw new SynchronizationException("Resource object is not account (class '"
                    + AccountShadowType.class + "'), but it's '" + change.getShadow().getClass() + "'.");
        }

        OperationResult subResult = result.createSubresult(ACTION_DISABLE_ACCOUNT);


        return userOid;
    }
}
