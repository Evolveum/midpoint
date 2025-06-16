/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.processing;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemProcessingOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;

@Experimental
public class ItemProcessingResult {

    /** Outcome of the processing */
    @NotNull private final QualifiedItemProcessingOutcomeType outcome;

    /**
     * Exception (either native or constructed) related to the error in processing.
     * Always non-null if there is an error and null if there is no error! We can rely on this.
     */
    private final Throwable exception;

    /**
     * The operation result for item processing. Should be closed.
     * Must not be used for further recording. (Only for analysis.)
     */
    @NotNull private final OperationResult operationResult;

    private ItemProcessingResult(
            @NotNull ItemProcessingOutcomeType outcome,
            @Nullable Throwable exception,
            @NotNull OperationResult operationResult) {
        this.operationResult = operationResult;
        this.outcome = new QualifiedItemProcessingOutcomeType()
                .outcome(outcome);
        this.exception = exception;
        argCheck(outcome != ItemProcessingOutcomeType.FAILURE || exception != null,
                "Error without exception");
    }

    public @NotNull QualifiedItemProcessingOutcomeType outcome() {
        return outcome;
    }

    public Throwable exception() {
        return exception;
    }

    public @NotNull OperationResult operationResult() {
        return operationResult;
    }

    static ItemProcessingResult fromOperationResult(OperationResult result) {
        if (result.isError()) {
            // This is an error without visible top-level exception, so we have to find one.
            Throwable exception = RepoCommonUtils.getResultException(result);
            return new ItemProcessingResult(ItemProcessingOutcomeType.FAILURE, exception, result);
        } else if (result.isNotApplicable()) {
            return new ItemProcessingResult(ItemProcessingOutcomeType.SKIP, null, result);
        } else {
            return new ItemProcessingResult(ItemProcessingOutcomeType.SUCCESS, null, result);
        }
    }

    static ItemProcessingResult fromException(OperationResult result, Throwable e) {
        return new ItemProcessingResult(ItemProcessingOutcomeType.FAILURE, e, result);
    }

    boolean isError() {
        return outcome.getOutcome() == ItemProcessingOutcomeType.FAILURE;
    }

    boolean isSuccess() {
        return outcome.getOutcome() == ItemProcessingOutcomeType.SUCCESS;
    }

    boolean isSkip() {
        return outcome.getOutcome() == ItemProcessingOutcomeType.SKIP;
    }

    String getMessage() {
        return exception != null ? exception.getMessage() : null;
    }

    Throwable getExceptionRequired() {
        return requireNonNull(exception, "Error without exception");
    }

    @Override
    public String toString() {
        return "ProcessingResult{" +
                "outcome=" + outcome +
                ", exception=" + exception +
                '}';
    }
}
