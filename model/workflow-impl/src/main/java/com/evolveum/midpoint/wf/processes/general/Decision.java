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

package com.evolveum.midpoint.wf.processes.general;

import java.io.Serializable;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 7.8.2012
 * Time: 17:42
 * To change this template use File | Settings | File Templates.
 */
public class Decision<I extends Serializable> implements Serializable {

    private static final long serialVersionUID = -542549699933865819L;

    private String user;
    private ApprovalRequest<I> approvalRequest;
    private boolean approved;
    private String comment;
    private Date date;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public ApprovalRequest<I> getApprovalRequest() {
        return approvalRequest;
    }

    public void setApprovalRequest(ApprovalRequest<I> approvalRequest) {
        this.approvalRequest = approvalRequest;
    }

    @Override
    public String toString() {
        return "Decision: approvalRequest=" + approvalRequest + ", approved=" + isApproved() + ", comment=" + getComment() + ", user=" + getUser() + ", date=" + getDate();
    }
}
