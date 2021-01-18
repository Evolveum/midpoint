/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.scanner;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType.F_VALID_FROM;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType.F_VALID_TO;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.F_ASSIGNMENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType.F_ACTIVATION;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.tasks.scanner.FocusValidityScannerTaskExecution.QueryScope;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.task.DefaultHandledObjectType;
import com.evolveum.midpoint.repo.common.task.ResultHandlerClass;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Execution of a single focus validity scanner task part.
 */
@ResultHandlerClass(FocusValidityScannerResultHandler.class)
@DefaultHandledObjectType(FocusType.class)
public class FocusValidityScannerTaskPartExecution
        extends AbstractScannerTaskPartExecution
        <FocusType,
                FocusValidityScannerTaskHandler,
                FocusValidityScannerTaskExecution,
                FocusValidityScannerTaskPartExecution,
                FocusValidityScannerResultHandler> {

    /** Determines whether we want to search for objects, assignments, or both at once. */
    @NotNull private final QueryScope queryScope;

    FocusValidityScannerTaskPartExecution(@NotNull FocusValidityScannerTaskExecution taskExecution,
            @NotNull QueryScope queryScope) {
        super(taskExecution);
        this.queryScope = queryScope;
    }

    @Override
    protected ObjectQuery createQuery(OperationResult opResult) throws SchemaException {
        ObjectQuery query = getPrismContext().queryFactory().createQuery();
        ObjectFilter filter;

        if (taskExecution.doCustomValidityCheck()) {
            assert taskExecution.validityConstraint != null;
            ItemPathType itemPathType = taskExecution.validityConstraint.getItem();
            ItemPath path = java.util.Objects.requireNonNull(itemPathType.getItemPath(),
                    "No path defined in the validity constraint");
            XMLGregorianCalendar lowerBound = CloneUtil.clone(taskExecution.lastScanTimestamp);
            XMLGregorianCalendar upperBound = CloneUtil.clone(taskExecution.thisScanTimestamp);
            Duration negativeOffset = taskExecution.getNegativeActivationOffset();
            if (lowerBound != null) {
                lowerBound.add(negativeOffset);
            }
            upperBound.add(negativeOffset);
            filter = createFilterForItemTimestamp(path, lowerBound, upperBound);
        } else {
            filter = createStandardFilter();
        }

        query.setFilter(filter);
        return query;
    }

    private ObjectFilter createStandardFilter() {
        XMLGregorianCalendar lastScanTimestamp = taskExecution.lastScanTimestamp;
        XMLGregorianCalendar thisScanTimestamp = taskExecution.thisScanTimestamp;

        S_AtomicFilterExit i = getPrismContext().queryFor(FocusType.class).none();
        if (lastScanTimestamp == null) {
            if (checkFocusValidity()) {
                i = i.or().item(F_ACTIVATION, F_VALID_FROM).le(thisScanTimestamp)
                        .or().item(F_ACTIVATION, F_VALID_TO).le(thisScanTimestamp);
            }
            if (checkAssignmentValidity()) {
                i = i.or().exists(F_ASSIGNMENT)
                        .block()
                        .item(AssignmentType.F_ACTIVATION, F_VALID_FROM).le(thisScanTimestamp)
                        .or().item(AssignmentType.F_ACTIVATION, F_VALID_TO).le(thisScanTimestamp)
                        .endBlock();
            }
        } else {
            if (checkFocusValidity()) {
                i = i.or().item(F_ACTIVATION, F_VALID_FROM).gt(lastScanTimestamp)
                        .and().item(F_ACTIVATION, F_VALID_FROM).le(thisScanTimestamp)
                        .or().item(F_ACTIVATION, F_VALID_TO).gt(lastScanTimestamp)
                        .and().item(F_ACTIVATION, F_VALID_TO).le(thisScanTimestamp);

            }
            if (checkAssignmentValidity()) {
                i = i.or().exists(F_ASSIGNMENT)
                        .block()
                        .item(AssignmentType.F_ACTIVATION, F_VALID_FROM).gt(lastScanTimestamp)
                        .and().item(AssignmentType.F_ACTIVATION, F_VALID_FROM).le(thisScanTimestamp)
                        .or().item(AssignmentType.F_ACTIVATION, F_VALID_TO).gt(lastScanTimestamp)
                        .and().item(AssignmentType.F_ACTIVATION, F_VALID_TO).le(thisScanTimestamp)
                        .endBlock();
            }
        }
        return i.buildFilter();
    }

    private boolean checkFocusValidity() {
        return queryScope == FocusValidityScannerTaskExecution.QueryScope.OBJECTS || queryScope == FocusValidityScannerTaskExecution.QueryScope.COMBINED;
    }

    private boolean checkAssignmentValidity() {
        return queryScope == FocusValidityScannerTaskExecution.QueryScope.ASSIGNMENTS || queryScope == FocusValidityScannerTaskExecution.QueryScope.COMBINED;
    }

    private ObjectFilter createFilterForItemTimestamp(ItemPath path,
            XMLGregorianCalendar lowerBound, XMLGregorianCalendar upperBound) {
        Class<? extends FocusType> type = determineObjectType();
        if (lowerBound == null) {
            return getPrismContext().queryFor(type)
                    .item(path).le(upperBound)
                    .buildFilter();
        } else {
            return getPrismContext().queryFor(type)
                    .item(path).gt(lowerBound)
                    .and().item(path).le(upperBound)
                    .buildFilter();
        }
    }
}
