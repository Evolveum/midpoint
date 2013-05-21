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
