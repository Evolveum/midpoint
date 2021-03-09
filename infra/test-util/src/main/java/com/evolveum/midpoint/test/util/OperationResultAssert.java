/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Strings;

import com.evolveum.midpoint.schema.result.OperationResult;

@SuppressWarnings("UnusedReturnValue")
public class OperationResultAssert extends AbstractAssert<OperationResultAssert, OperationResult> {

    public OperationResultAssert(OperationResult operationResult) {
        super(operationResult, OperationResultAssert.class);
    }

    public OperationResultAssert isSuccess() {
        isNotNull();
        actual.computeStatusIfUnknown();
        if (!actual.isSuccess()) {
            failWithMessage("Expected operation result to be success: %s", actual);
        }
        return this;
    }

    public OperationResultAssert isFatalError() {
        isNotNull();
        actual.computeStatusIfUnknown();
        if (!actual.isFatalError()) {
            failWithMessage("Expected operation result to be fatal error: %s", actual);
        }
        return this;
    }

    public OperationResultAssert isTracedSomewhere() {
        if (findTracedSubresult(actual) == null) {
            failWithMessage("No traced subresult in %s", actual);
        }
        return this;
    }

    private OperationResult findTracedSubresult(OperationResult root) {
        if (root.isTraced()) {
            return root;
        }

        for (OperationResult subresult : root.getSubresults()) {
            OperationResult traced = findTracedSubresult(subresult);
            if (traced != null) {
                return traced;
            }
        }
        return null;
    }

    public OperationResultAssert noneLogEntryMatches(Predicate<String> entryPredicate) {
        actual.getResultStream().forEach(subresult -> {
            List<String> matchingEntries = subresult.getLogSegments().stream()
                    .flatMap(seg -> seg.getEntry().stream())
                    .filter(entryPredicate)
                    .collect(Collectors.toList());
            if (!matchingEntries.isEmpty()) {
                failWithMessage("Found " + matchingEntries.size()
                        + " log entries matching the predicate in: " + subresult + "\n"
                        + String.join("\n", matchingEntries));
            }
        });
        return this;
    }

    public OperationResultAssert anyLogEntryMatches(Predicate<String> entryPredicate) {
        if (actual.getResultStream()
                .flatMap(res -> res.getLogSegments().stream())
                .flatMap(seg -> seg.getEntry().stream())
                .noneMatch(entryPredicate)) {
            failWithMessage("Found no log entries matching specified predicate");
        }
        return this;
    }

    public OperationResultAssert hasMessageContaining(String message) {
        isNotNull();
        Strings.instance().assertContains(info, actual.getMessage(), message);
        return this;
    }

    public OperationResultAssert hasMessageMatching(String regex) {
        isNotNull();
        Strings.instance().assertMatches(info, actual.getMessage(), regex);
        return this;
    }
}
