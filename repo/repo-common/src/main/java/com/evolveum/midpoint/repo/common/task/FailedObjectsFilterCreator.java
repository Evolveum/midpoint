package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_MatchingRuleEntry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedObjectsSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;

import static java.util.Objects.requireNonNull;

/**
 * Creates query filter to match failed objects.
 */
class FailedObjectsFilterCreator {

    @NotNull private final FailedObjectsSelectorType selector;
    @NotNull private final AbstractSearchIterativeTaskPartExecution<?, ?, ?, ?, ?> taskPartExecution;
    @NotNull private final PrismContext prismContext;

    public FailedObjectsFilterCreator(@NotNull FailedObjectsSelectorType selector,
            @NotNull AbstractSearchIterativeTaskPartExecution<?, ?, ?, ?, ?> taskPartExecution, @NotNull PrismContext prismContext) {
        this.selector = selector;
        this.taskPartExecution = taskPartExecution;
        this.prismContext = prismContext;
    }

    public ObjectFilter createFilter() {

        S_MatchingRuleEntry entry = prismContext.queryFor(ObjectType.class)
                .exists(ObjectType.F_OPERATION_EXECUTION).block()
                    .item(OperationExecutionType.F_TASK_REF).ref(getTaskOids())
                    .and().item(OperationExecutionType.F_STATUS).eq(getStatusList());

        XMLGregorianCalendar startTime = getTimeFrom();
        if (startTime != null) {
            entry = entry.and().item(OperationExecutionType.F_TIMESTAMP).ge(startTime);
        }

        XMLGregorianCalendar endTime = getTimeTo();
        if (endTime != null) {
            entry = entry.and().item(OperationExecutionType.F_TIMESTAMP).le(endTime);
        }

        return entry.endBlock().buildFilter();
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
        return requireNonNull(taskPartExecution.getRootTaskOid(), "no root task OID");
    }

    private Object[] getStatusList() {
        if (selector.getStatus().isEmpty()) {
            return new OperationResultStatusType[] {
                    OperationResultStatusType.FATAL_ERROR,
                    OperationResultStatusType.HANDLED_ERROR
            };
        } else {
            return selector.getStatus().toArray(OperationResultStatusType[]::new);
        }
    }

    private XMLGregorianCalendar getTimeFrom() {
        return selector.getTimeFrom();
    }

    private XMLGregorianCalendar getTimeTo() {
        if (selector.getTimeTo() != null) {
            return selector.getTimeTo();
        } else if (selector.getTaskRef().isEmpty()) {
            return XmlTypeConverter.createXMLGregorianCalendar(
                    requireNonNull(taskPartExecution.localCoordinatorTask.getLastRunStartTimestamp(),
                            "no start time for the current task"));
        } else {
            return null;
        }
    }
}
