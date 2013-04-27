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

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Possible values that can be used to filter tasks by their execution status.
 *
 * @see com.evolveum.midpoint.task.api.TaskExecutionStatus
 * @author Pavol Mederly
 */
public enum TaskDtoExecutionStatusFilter {

    ALL,
    RUNNING_OR_RUNNABLE,
    WAITING,
    SUSPENDED,
    CLOSED,
    NOT_CLOSED;

    public ObjectFilter createFilter(Class clazz, PrismContext prismContext) throws SchemaException {
        switch(this) {
            case ALL: return null;
            case RUNNING_OR_RUNNABLE: return EqualsFilter.createEqual(clazz, prismContext, TaskType.F_EXECUTION_STATUS, TaskExecutionStatusType.RUNNABLE);
            case WAITING: return EqualsFilter.createEqual(clazz, prismContext, TaskType.F_EXECUTION_STATUS, TaskExecutionStatusType.WAITING);
            case SUSPENDED: return EqualsFilter.createEqual(clazz, prismContext, TaskType.F_EXECUTION_STATUS, TaskExecutionStatusType.SUSPENDED);
            case CLOSED: return EqualsFilter.createEqual(clazz, prismContext, TaskType.F_EXECUTION_STATUS, TaskExecutionStatusType.CLOSED);
            case NOT_CLOSED: return NotFilter.createNot(EqualsFilter.createEqual(clazz, prismContext, TaskType.F_EXECUTION_STATUS, TaskExecutionStatusType.CLOSED));
            default: throw new SystemException("Unknown value for TaskDtoExecutionStatusFilter: " + this);
        }
    }


}
