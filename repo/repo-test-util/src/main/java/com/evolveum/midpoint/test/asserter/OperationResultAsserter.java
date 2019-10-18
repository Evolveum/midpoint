/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.test.IntegrationTestTools;
import org.jetbrains.annotations.NotNull;
import org.testng.AssertJUnit;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 *
 */
public class OperationResultAsserter<RA> extends AbstractAsserter<RA> {

    private final OperationResult result;

    public OperationResultAsserter(OperationResult result, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.result = result;
    }

    public static final OperationResultAsserter<Void> forResult(OperationResult result) {
        return new OperationResultAsserter(result, null, null);
    }

    OperationResult getResult() {
        assertNotNull("Null " + desc(), result);
        return result;
    }

    public OperationResultAsserter<RA> assertNull() {
        AssertJUnit.assertNull("Unexpected " + desc(), result);
        return this;
    }

    public OperationResultAsserter<RA> assertStatus(OperationResultStatus expected) {
        assertEquals("Wrong status in "+desc(), expected, result.getStatus());
        return this;
    }

    public OperationResultAsserter<RA> assertSuccess() {
        assertEquals("Wrong status in "+desc(), OperationResultStatus.SUCCESS, result.getStatus());
        return this;
    }

    public OperationResultRepoSearchAsserter<OperationResultAsserter<RA>> repoSearches() {
        OperationResultRepoSearchAsserter<OperationResultAsserter<RA>> asserter = new OperationResultRepoSearchAsserter(result, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public OperationResultAsserter<RA> display() {
        IntegrationTestTools.display(desc(), result);
        return this;
    }

    public OperationResultAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, result);
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("operation result " + result.getOperation());
    }

    public OperationResultAsserter<RA> assertTracedSomewhere() {
        OperationResult traced = findTracedSubresult(result);
        assertNotNull("No traced subresult in " + result, traced);
        return this;
    }

    private OperationResult findTracedSubresult(OperationResult root) {
        if (root.isTraced()) {
            return root;
        } else {
            for (OperationResult subresult : root.getSubresults()) {
                OperationResult traced = findTracedSubresult(subresult);
                if (traced != null) {
                    return traced;
                }
            }
            return null;
        }
    }

    public OperationResultAsserter<RA> assertNoLogEntry(Predicate<String> entryPredicate) {
        result.getResultStream().forEach(subresult -> {
            List<String> matchingEntries = getMatchingStream(entryPredicate, subresult)
                    .collect(Collectors.toList());
            if (!matchingEntries.isEmpty()) {
                fail("Found " + matchingEntries.size() + " log entries matching the predicate in: " + subresult + "\n" +
                        String.join("\n", matchingEntries));
            }
        });
        return this;
    }

    public OperationResultAsserter<RA> assertLogEntry(Predicate<String> entryPredicate) {
        List<String> allMatchingEntries = result.getResultStream()
                .flatMap(subresult -> getMatchingStream(entryPredicate, subresult))
                .collect(Collectors.toList());
        assertFalse("Found no log entries matching specified predicate", allMatchingEntries.isEmpty());
        return this;
    }

    @NotNull
    private Stream<String> getMatchingStream(Predicate<String> entryPredicate, OperationResult subresult) {
        return subresult.getLogSegments().stream()
                .flatMap(segment -> segment.getEntry().stream().filter(entryPredicate));
    }
}
