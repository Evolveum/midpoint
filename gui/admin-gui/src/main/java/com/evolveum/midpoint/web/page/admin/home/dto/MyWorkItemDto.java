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
