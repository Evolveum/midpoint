/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.activiti;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.util.SerializationSafeContainer;
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

    public void revive(SerializationSafeContainer<?> container) {
        container.setPrismContext(SpringApplicationContextHolder.getPrismContext());
    }

    // todo - better name?
    public MidpointFunctions midpoint() {
        return SpringApplicationContextHolder.getMidpointFunctions();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " object.";
    }
}
