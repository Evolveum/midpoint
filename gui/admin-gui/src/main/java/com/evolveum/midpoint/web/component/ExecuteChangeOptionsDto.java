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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ExecuteChangeOptionsDto implements Serializable {

    public static final String F_FORCE = "force";
    public static final String F_RECONCILE = "reconcile";
    public static final String F_EXECUTE_AFTER_ALL_APPROVALS = "executeAfterAllApprovals";

    private boolean force;
    private boolean reconcile;
    private boolean executeAfterAllApprovals = true;

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public boolean isReconcile() {
        return reconcile;
    }

    public void setReconcile(boolean reconcile) {
        this.reconcile = reconcile;
    }

    public boolean isExecuteAfterAllApprovals() {
        return executeAfterAllApprovals;
    }

    public void setExecuteAfterAllApprovals(boolean executeAfterAllApprovals) {
        this.executeAfterAllApprovals = executeAfterAllApprovals;
    }

    public ModelExecuteOptions createOptions() {
        ModelExecuteOptions options = new ModelExecuteOptions();
        options.setForce(isForce());
        options.setReconcile(isReconcile());
        options.setExecuteImmediatelyAfterApproval(!isExecuteAfterAllApprovals());

        return options;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Options{force=").append(isForce());
        builder.append(",reconcile=").append(isReconcile());
        builder.append('}');

        return builder.toString();
    }
}
