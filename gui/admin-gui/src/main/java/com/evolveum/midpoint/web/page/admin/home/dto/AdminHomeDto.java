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

package com.evolveum.midpoint.web.page.admin.home.dto;

import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.wf.api.WorkItem;

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
            orgAssignments = new ArrayList<SimpleAssignmentDto>();
        }
        return orgAssignments;
    }

    public List<SimpleAccountDto> getAccounts() {
        if (accounts == null) {
            accounts = new ArrayList<SimpleAccountDto>();
        }
        return accounts;
    }

    public List<SimpleAssignmentDto> getAssignments() {
        if (assignments == null) {
            assignments = new ArrayList<SimpleAssignmentDto>();
        }
        return assignments;
    }

    public List<String> getResources() {
        if (resources == null) {
            resources = new ArrayList<String>();
        }
        return resources;
    }

    public List<WorkItemDto> getWorkItems() {
        if (workItems == null) {
            workItems = new ArrayList<WorkItemDto>();
        }
        return workItems;
    }
}
