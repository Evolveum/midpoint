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

package com.evolveum.midpoint.wf.messages;

/**
 * Command to query a process instance.
 */
public class QueryProcessCommand extends MidPointToActivitiMessage {

    private String pid;
    private String taskOid;

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getTaskOid() {
        return taskOid;
    }

    public void setTaskOid(String taskOid) {
        this.taskOid = taskOid;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[pid=" + pid + ", task=" + taskOid + "]";
    }
}
