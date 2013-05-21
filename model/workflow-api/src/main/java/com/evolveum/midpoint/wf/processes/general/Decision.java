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
