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

import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AdminHomeDto implements Serializable {

    private List<SimpleAccountDto> accounts;
    private List<SimpleAssignmentDto> assignments;
    private List<SimpleAssignmentDto> orgAssignments;
    private List<String> resources;
    private List<WorkItemDto> workItems;

    public List<SimpleAssignmentDto> getOrgAssignments() {
        if (orgAssignments == null) {
            orgAssignments = new ArrayList<>();
        }
        return orgAssignments;
    }

    public List<SimpleAccountDto> getAccounts() {
        if (accounts == null) {
            accounts = new ArrayList<>();
        }
        return accounts;
    }

    public List<SimpleAssignmentDto> getAssignments() {
        if (assignments == null) {
            assignments = new ArrayList<>();
        }
        return assignments;
    }

    public List<String> getResources() {
        if (resources == null) {
            resources = new ArrayList<>();
        }
        return resources;
    }

    public List<WorkItemDto> getWorkItems() {
        if (workItems == null) {
            workItems = new ArrayList<>();
        }
        return workItems;
    }
}
