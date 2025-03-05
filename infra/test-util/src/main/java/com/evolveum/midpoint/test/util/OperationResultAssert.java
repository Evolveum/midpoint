/*
 * Copyright (C) 2010-2022 Evolveum and contributors
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

    public OperationResultAssert isWarning() {
        isNotNull();
        actual.computeStatusIfUnknown();
        if (!actual.isWarning()) {
            failWithMessage("Expected operation result to be warning: %s", actual);
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

    public OperationResultAssert isPartialError() {
        isNotNull();
        actual.computeStatusIfUnknown();
        if (!actual.isPartialError()) {
            failWithMessage("Expected operation result to be partial error: %s", actual);
        }
        return this;
    }

    public OperationResultAssert isHandledError() {
        isNotNull();
        actual.computeStatusIfUnknown();
        if (!actual.isHandledError()) {
            failWithMessage("Expected operation result to be handled error: %s", actual);
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

    public OperationResultAssert anySubResultMatches(Predicate<OperationResult> resultPredicate) {
        if (actual.getResultStream().noneMatch(resultPredicate)) {
            failWithMessage("Found no subresult matching specified predicate");
        }
        return this;
    }

    public OperationResultAssert firstSubResultMatching(Predicate<OperationResult> resultPredicate) {
        var matching = actual.getResultStream()
                .filter(resultPredicate)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Found no subresult matching specified predicate"));
        return new OperationResultAssert(matching);
    }

    public List<OperationResult> getAllSubResultsMatching(Predicate<OperationResult> resultPredicate) {
        return actual.getResultStream()
                .filter(resultPredicate)
                .toList();
    }

    /** Use after asserting success or failure to propagate the message from subresult(s). */
    public OperationResultAssert hasMessage(String message) {
        objects.assertEqual(info, actual.getMessage(), message);
        return this;
    }

    /** Use after asserting success or failure to propagate the message from subresult(s). */
    public OperationResultAssert hasMessageContaining(String message) {
        isNotNull();
        Strings.instance().assertContains(info, actual.getMessage(), message);
        return this;
    }

    /** Use after asserting success or failure to propagate the message from subresult(s). */
    public OperationResultAssert hasMessageMatching(String regex) {
        isNotNull();
        Strings.instance().assertMatches(info, actual.getMessage(), regex);
        return this;
    }
}
