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

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchSpecification;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeValidityPolicyConstraintType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Execution of a single focus validity scanner task part.
 */
public final class FocusValidityScanPartialRun
        extends ScanActivityRun
        <FocusType,
                FocusValidityScanWorkDefinition,
                FocusValidityScanActivityHandler> {

    /** Determines whether we want to search for objects, assignments, or both at once. */
    @NotNull private final ScanScope scanScope;

    FocusValidityScanPartialRun(
            @NotNull ActivityRunInstantiationContext<FocusValidityScanWorkDefinition, FocusValidityScanActivityHandler> context,
            @NotNull ScanScope scanScope) {
        super(context, String.format("Validity scan (%s)", scanScope));
        this.scanScope = scanScope;
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .actionsExecutedStatisticsSupported(true);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!super.beforeRun(result)) {
            return false;
        }
        ensureNoDryRun();
        return true;
    }

    public void customizeQuery(SearchSpecification<FocusType> searchSpecification, OperationResult result) {
        TimeValidityPolicyConstraintType validityConstraint = getWorkDefinition().getValidityConstraint();
        searchSpecification.addFilter(
                validityConstraint != null ?
                        createFilterForValidityChecking(searchSpecification, validityConstraint) :
                        createStandardFilter());
    }

    private ObjectFilter createFilterForValidityChecking(
            SearchSpecification<FocusType> searchSpecification, TimeValidityPolicyConstraintType validityConstraint) {
        ItemPathType itemPathType = validityConstraint.getItem();
        ItemPath path = java.util.Objects.requireNonNull(itemPathType.getItemPath(),
                "No path defined in the validity constraint");
        XMLGregorianCalendar lowerBound = CloneUtil.clone(lastScanTimestamp);
        XMLGregorianCalendar upperBound = CloneUtil.clone(thisScanTimestamp);
        Duration negativeOffset = getNegativeActivationOffset(validityConstraint);
        if (lowerBound != null) {
            lowerBound.add(negativeOffset);
        }
        upperBound.add(negativeOffset);
        return createFilterForItemTimestamp(searchSpecification, path, lowerBound, upperBound);
    }

    private Duration getNegativeActivationOffset(@NotNull TimeValidityPolicyConstraintType validityConstraint) {
        if (validityConstraint.getActivateOn() != null) {
            return validityConstraint.getActivateOn().negate();
        } else {
            return XmlTypeConverter.createDuration(0);
        }
    }

    private ObjectFilter createStandardFilter() {
        S_FilterExit i = PrismContext.get().queryFor(FocusType.class).none();
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
        return scanScope == ScanScope.OBJECTS ||
                scanScope == ScanScope.COMBINED;
    }

    private boolean checkAssignmentValidity() {
        return scanScope == ScanScope.ASSIGNMENTS ||
                scanScope == ScanScope.COMBINED;
    }

    private ObjectFilter createFilterForItemTimestamp(
            SearchSpecification<FocusType> searchSpecification, ItemPath path,
            XMLGregorianCalendar lowerBound, XMLGregorianCalendar upperBound) {
        if (lowerBound == null) {
            return PrismContext.get().queryFor(searchSpecification.getType())
                    .item(path).le(upperBound)
                    .buildFilter();
        } else {
            return PrismContext.get().queryFor(searchSpecification.getType())
                    .item(path).gt(lowerBound)
                    .and().item(path).le(upperBound)
                    .buildFilter();
        }
    }

    @Override
    public boolean processItem(@NotNull FocusType object,
            @NotNull ItemProcessingRequest<FocusType> request, RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityRunException {
        // We want the reconcile option here. There may be accounts that are in wrong activation state.
        // We will not notice that unless we go with reconcile.
        ModelExecuteOptions options = ModelExecuteOptions.create().reconcile();
        getModelBeans().modelController.executeRecompute(object.asPrismObject(), options, workerTask, result);
        return true;
    }

    public enum ScanScope {

        /** The activity will search for objects with validity changes. */
        OBJECTS,

        /** The activity will search for assignments with validity changes. */
        ASSIGNMENTS,

        /** The activity will search for both objects and assignments with validity changes. (In a single query.) */
        COMBINED
    }
}
