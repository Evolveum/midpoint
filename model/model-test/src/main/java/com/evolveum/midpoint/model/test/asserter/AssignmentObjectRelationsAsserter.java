/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

/**
 * @author semancik
 *
 */
public class AssignmentObjectRelationsAsserter<RA> extends AbstractAsserter<RA> {

    private final List<AssignmentObjectRelation> assignmentObjectRelations;

    public AssignmentObjectRelationsAsserter(List<AssignmentObjectRelation> assignmentObjectRelations, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.assignmentObjectRelations = assignmentObjectRelations;
    }

    AssignmentTargetRelationAsserter<AssignmentObjectRelationsAsserter<RA>> forAssignmentTargetRelation(AssignmentObjectRelation view) {
        AssignmentTargetRelationAsserter<AssignmentObjectRelationsAsserter<RA>> asserter = new AssignmentTargetRelationAsserter<>(view, this, "assignment target relation in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public  AssignmentTargetRelationFinder<RA> by() {
        return new  AssignmentTargetRelationFinder<>(this);
    }

    public List<AssignmentObjectRelation> getAssignmentTargetRelations() {
        return assignmentObjectRelations;
    }

    public AssignmentObjectRelationsAsserter<RA> assertItems(int expected) {
        assertEquals("Wrong number of assignment object relation in "+desc(), expected, getAssignmentTargetRelations().size());
        return this;
    }

    public AssignmentTargetRelationAsserter<AssignmentObjectRelationsAsserter<RA>> single() {
        assertItems(1);
        return forAssignmentTargetRelation(getAssignmentTargetRelations().get(0));
    }

    public AssignmentObjectRelationsAsserter<RA> display() {
        display(desc());
        return this;
    }

    public AssignmentObjectRelationsAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, assignmentObjectRelations);
        return this;
    }

    @Override
    protected String desc() {
        return "assignment object relations of " + getDetails();
    }


}
