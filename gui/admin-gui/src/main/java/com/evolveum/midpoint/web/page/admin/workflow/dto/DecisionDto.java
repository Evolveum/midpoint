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

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.wf.api.WorkItem;
import com.evolveum.midpoint.wf.processes.general.Decision;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class DecisionDto<I extends Serializable> extends Selectable {

    public static final String F_USER = "user";
    public static final String F_RESULT = "result";
    public static final String F_COMMENT = "comment";
    public static final String F_TIME = "time";

    private Decision<I> decision;

    public DecisionDto(Decision<I> decision) {
        this.decision = decision;
    }

    public Decision<I> getDecision() {
        return decision;
    }

    public String getUser() {
        return decision.getUser();
    }

    public String getResult() {
        return decision.isApproved() ? "APPROVED" : "REJECTED";     // todo i18n
    }

    public String getTime() {
        return decision.getDate().toLocaleString();      // todo formatting
    }

    public String getComment() {
        return decision.getComment();
    }
}
