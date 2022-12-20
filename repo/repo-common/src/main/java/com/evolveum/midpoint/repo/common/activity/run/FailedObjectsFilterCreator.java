/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedObjectsSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;

import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Creates query filter to match failed objects.
 */
class FailedObjectsFilterCreator {

    @NotNull private final FailedObjectsSelectorType selector;
    @NotNull private final RunningTask task;
    @NotNull private final PrismContext prismContext = PrismContext.get();

    FailedObjectsFilterCreator(@NotNull FailedObjectsSelectorType selector, @NotNull RunningTask task) {
        this.selector = selector;
        this.task = task;
    }

    public ObjectFilter createFilter() {

        S_FilterExit builder = prismContext.queryFor(ObjectType.class)
                .exists(ObjectType.F_OPERATION_EXECUTION).block()
                    .item(OperationExecutionType.F_TASK_REF).ref(getTaskOids());

        builder = addStatusClause(builder, getStatusList());

        XMLGregorianCalendar startTime = getTimeFrom();
        if (startTime != null) {
            builder = builder.and().item(OperationExecutionType.F_TIMESTAMP).ge(startTime);
        }

        XMLGregorianCalendar endTime = getTimeTo();
        if (endTime != null) {
            builder = builder.and().item(OperationExecutionType.F_TIMESTAMP).le(endTime);
        }

        return builder.endBlock().buildFilter();
    }

    private S_FilterExit addStatusClause(S_FilterExit builder, List<OperationResultStatusType> statusList) {
        assert !statusList.isEmpty();
        if (statusList.size() <= 1) {
            return builder.and().item(OperationExecutionType.F_STATUS).eq(statusList.get(0));
        } else {
            builder = builder.and().block().item(OperationExecutionType.F_STATUS).eq(statusList.get(0));
            for (int i = 1; i < statusList.size(); i++) {
                builder = builder.or().item(OperationExecutionType.F_STATUS).eq(statusList.get(i));
            }
            return builder.endBlock();
        }
    }

    // Returned list must be non empty and should contain no nulls!
    private String[] getTaskOids() {
        if (selector.getTaskRef().isEmpty()) {
            return new String[] { getRootTaskOid() };
        } else {
            return selector.getTaskRef().stream()
                    .map(ref -> requireNonNull(ref.getOid(), "no OID in task ref"))
                    .toArray(String[]::new);
        }
    }

    @NotNull
    private String getRootTaskOid() {
        return requireNonNull(task.getRootTaskOid(), "no root task OID");
    }

    private List<OperationResultStatusType> getStatusList() {
        if (selector.getStatus().isEmpty()) {
            return Arrays.asList(OperationResultStatusType.FATAL_ERROR, OperationResultStatusType.PARTIAL_ERROR);
        } else {
            return selector.getStatus();
        }
    }

    private XMLGregorianCalendar getTimeFrom() {
        return selector.getTimeFrom();
    }

    private XMLGregorianCalendar getTimeTo() {
        if (selector.getTimeTo() != null) {
            return selector.getTimeTo();
        } else if (selector.getTaskRef().isEmpty()) {
            // TODO What if the task was suspended and resumed?
            // TODO What about multinode or partitioned tasks?
            return XmlTypeConverter.createXMLGregorianCalendar(
                    requireNonNull(task.getLastRunStartTimestamp(),
                            "no start time for the current task"));
        } else {
            return null;
        }
    }
}
