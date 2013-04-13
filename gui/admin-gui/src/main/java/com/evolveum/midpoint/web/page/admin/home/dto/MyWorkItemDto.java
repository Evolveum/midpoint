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

package com.evolveum.midpoint.web.page.admin.home.dto;

import com.evolveum.midpoint.wf.api.WorkItem;

import java.io.Serializable;
import java.util.Date;

/**
 * @author lazyman
 */
public class MyWorkItemDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_CREATED = "created";

    private String name;
    private Date createdDate;

    public MyWorkItemDto(WorkItem workItem) {
        this.name = workItem.getName();
        this.createdDate = workItem.getCreateTime();
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public String getName() {
        return name;
    }

    public String getCreated() {
        //todo use date format
        return createdDate != null ? createdDate.toString() : null;
    }
}
