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

package com.evolveum.midpoint.wf.activiti;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 22.9.2012
 * Time: 14:25
 * To change this template use File | Settings | File Templates.
 */
public class ActivitiUtil implements Serializable {

    private static final long serialVersionUID = 5183098710717369392L;

    private static final Trace LOGGER = TraceManager.getTrace(ActivitiUtil.class);

    public static String DEFAULT_APPROVER = "00000000-0000-0000-0000-000000000002";

    public String getApprover(RoleType r) {
        String approver;
        if (r.getApproverRef().isEmpty()) {
            LOGGER.warn("No approvers defined for role " + r + ", using default one instead: " + DEFAULT_APPROVER);
            return DEFAULT_APPROVER;
        }
        approver = r.getApproverRef().get(0).getOid();
        if (r.getApproverRef().size() > 1) {
            LOGGER.warn("More than one approver defined for role " + r + ", using the first one: " + approver);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Approver for role " + r + " determined to be " + approver);
        }
        return approver;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " object.";
    }
}
