/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.IntegrationTestTools;
import org.testng.AssertJUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author semancik
 */
public class OperationResultRepoSearchAsserter<RA> extends AbstractAsserter<RA> {

    private final OperationResult result;
    private List<OperationResult> searchResults;

    public OperationResultRepoSearchAsserter(OperationResult result, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.result = result;
    }

    public static OperationResultRepoSearchAsserter<Void> forResult(OperationResult result) {
        return new OperationResultRepoSearchAsserter(result, null, null);
    }

    List<OperationResult> getRepoSearches() {
        if (searchResults == null) {
            searchResults = new ArrayList<>();
            result.accept(subresult -> {
                if (subresult.getOperation().startsWith("com.evolveum.midpoint.repo.api.RepositoryService.search")) {
                    searchResults.add(subresult);
                }
            });
        }
        return searchResults;
    }

    public OperationResultRepoSearchAsserter<RA> assertNull() {
        AssertJUnit.assertNull("Unexpected " + desc(), result);
        return this;
    }

    public OperationResultRepoSearchAsserter<RA> assertSize(int expected) {
        assertEquals("Wrong status in "+desc(), expected, getRepoSearches().size());
        return this;
    }

    public OperationResultRepoSearchAsserter<RA> assertContains(Predicate<OperationResult> predicate) {
        if (!contains(predicate)) {
            fail("Expected that search results will contain "+predicate.toString()+", but they did not; in " + desc());
        }
        return this;
    }

    public OperationResultRepoSearchAsserter<RA> assertNotContains(Predicate<OperationResult> predicate) {
        if (contains(predicate)) {
            fail("Expected that search results will not contain "+predicate.toString()+", but they did; in " + desc());
        }
        return this;
    }

    private boolean contains(Predicate<OperationResult> predicate) {
        for (OperationResult searchResult : getRepoSearches()) {
            if (predicate.test(searchResult)) {
                return true;
            }
        }
        return false;
    }

    public OperationResultRepoSearchAsserter<RA> assertContainsQuerySubstring(String expectedSubstring) {
        assertContains(createQuerySubstringPredicate(expectedSubstring));
        return this;
    }

    public OperationResultRepoSearchAsserter<RA> assertNotContainsQuerySubstring(String expectedSubstring) {
        assertNotContains(createQuerySubstringPredicate(expectedSubstring));
        return this;
    }

    private Predicate<OperationResult> createQuerySubstringPredicate(String expectedSubstring) {
        return new Predicate<OperationResult>() {
            @Override
            public boolean test(OperationResult operationResult) {
                String queryParam = operationResult.getParamSingle(OperationResult.PARAM_QUERY);
                return queryParam.contains(expectedSubstring);
            }

            @Override
            public String toString() {
                return "substring '"+expectedSubstring+"'";
            }
        };
    }

    public OperationResultRepoSearchAsserter<RA> display() {
        IntegrationTestTools.display(desc(), searchResults);
        return this;
    }

    public OperationResultRepoSearchAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, searchResults);
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("repo search results of result "+result.getOperation());
    }

}
