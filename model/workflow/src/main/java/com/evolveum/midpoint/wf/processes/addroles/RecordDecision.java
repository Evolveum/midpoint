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

package com.evolveum.midpoint.wf.processes.addroles;

import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.RoleType;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 7.8.2012
 * Time: 17:56
 * To change this template use File | Settings | File Templates.
 */
public class RecordDecision implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(RecordDecision.class);

    public void execute(DelegateExecution execution) {
        RoleType role = (RoleType) execution.getVariableLocal("role");
        Boolean yesOrNo = (Boolean) execution.getVariable("decision#C");
        String comment = (String) execution.getVariable("comment#C");

        if (comment != null && comment.startsWith("+")) {
            comment = comment.substring(1);
            yesOrNo = true;
        }

        Decision decision = new Decision();

        PrincipalUser user = getPrincipalUser();
        if (user != null) {
            decision.setUser(user.getName().getOrig());  //TODO: probably not correct setting
        } else {
            decision.setUser("?");    // todo
        }

        decision.setApproved(yesOrNo == null ? false : yesOrNo);
        decision.setComment(comment);
        decision.setRole(role);
        decision.setDate(new Date());
        DecisionList decisionList = (DecisionList) execution.getVariable("decisionList");
        decisionList.addDecision(decision);
        execution.setVariable("decisionList", decisionList);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Logged decision '" + yesOrNo + "' for role " + role);
            LOGGER.trace("Resulting decision list = " + decisionList);
        }
    }

    // todo fixme: copied from web SecurityUtils
    public com.evolveum.midpoint.model.security.api.PrincipalUser getPrincipalUser() {
        SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx != null && ctx.getAuthentication() != null && ctx.getAuthentication().getPrincipal() != null) {
            Object principal = ctx.getAuthentication().getPrincipal();
            if (!(principal instanceof com.evolveum.midpoint.model.security.api.PrincipalUser)) {
                LOGGER.warn("Principal user in security context holder is {} but not type of {}",
                        new Object[]{principal, com.evolveum.midpoint.model.security.api.PrincipalUser.class.getName()});
                return null;
            }
            return (com.evolveum.midpoint.model.security.api.PrincipalUser) principal;
        } else {
            LOGGER.warn("No spring security context or authentication or principal.");
            return null;
        }
    }

}
