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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import org.apache.commons.lang.Validate;

import java.util.Date;

/**
 * @author lazyman
 */
public class TaskDto  extends Selectable {

    private String oid;
    private String name;
    private String category;
    private ObjectReferenceType objectRef;
    private TaskExecutionStatus execution;
    private Long currentRuntime;
    private Date scheduledToStartAgain;
    private OperationResultStatus status;

    public TaskDto(Task task) {
        Validate.notNull(task, "Task must not be null.");
        oid = task.getOid();
        name = task.getName();
        category = task.getCategory();
//        objectRef = ;
        execution = task.getExecutionStatus();
//        currentRuntime =;
//        scheduledToStartAgain=task.;

        OperationResult result = task.getResult();
        if (result != null) {
            status = result.getStatus();
        }
    }

    public String getCategory() {
        return category;
    }

    public Long getCurrentRuntime() {
        return currentRuntime;
    }

    public TaskExecutionStatus getExecution() {
        return execution;
    }

    public String getName() {
        return name;
    }

    public ObjectReferenceType getObjectRef() {
        return objectRef;
    }

    public String getOid() {
        return oid;
    }

    public Date getScheduledToStartAgain() {
        return scheduledToStartAgain;
    }

    public OperationResultStatus getStatus() {
        return status;
    }
}
