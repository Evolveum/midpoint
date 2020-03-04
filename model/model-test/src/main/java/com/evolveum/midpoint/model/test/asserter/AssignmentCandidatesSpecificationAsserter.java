/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertNotNull;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

/**
 * @author semancik
 *
 */
public class AssignmentCandidatesSpecificationAsserter<RA> extends AbstractAsserter<RA> {

    private final AssignmentCandidatesSpecification candidateSpec;

    public AssignmentCandidatesSpecificationAsserter(AssignmentCandidatesSpecification candidateSpec, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.candidateSpec = candidateSpec;
    }

    AssignmentCandidatesSpecification getAssignmentCandidateSpecification() {
        assertNotNull("Null " + desc(), candidateSpec);
        return candidateSpec;
    }

    public AssignmentCandidatesSpecificationAsserter<RA> assertNull() {
        AssertJUnit.assertNull("Unexpected " + desc(), candidateSpec);
        return this;
    }

    public AssignmentObjectRelationsAsserter<AssignmentCandidatesSpecificationAsserter<RA>> assignmentObjectRelations() {
        AssignmentObjectRelationsAsserter<AssignmentCandidatesSpecificationAsserter<RA>> displayAsserter = new AssignmentObjectRelationsAsserter<>(getAssignmentCandidateSpecification().getAssignmentObjectRelations(), this, "in " + desc());
        copySetupTo(displayAsserter);
        return displayAsserter;
    }

    public AssignmentCandidatesSpecificationAsserter<RA> display() {
        display(desc());
        return this;
    }

    public AssignmentCandidatesSpecificationAsserter<RA> display(String message) {
        PrismTestUtil.display(message, candidateSpec);
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("archetype candidate specification");
    }

}
