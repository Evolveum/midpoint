/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
