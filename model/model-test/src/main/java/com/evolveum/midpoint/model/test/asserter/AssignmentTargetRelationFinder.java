/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class AssignmentTargetRelationFinder<RA> {

    private final AssignmentObjectRelationsAsserter<RA> collectionAsserter;
    private List<QName> targetTypes;
    private List<QName> relations;

    public AssignmentTargetRelationFinder(AssignmentObjectRelationsAsserter<RA> collectionAsserter) {
        this.collectionAsserter = collectionAsserter;
    }

    public AssignmentTargetRelationFinder<RA> targetType(QName qname) {
        targetTypes = new ArrayList<>();
        targetTypes.add(qname);
        return this;
    }

    public AssignmentTargetRelationFinder<RA> relation(QName qname) {
        relations = new ArrayList<>();
        relations.add(qname);
        return this;
    }

    public AssignmentTargetRelationFinder<RA> relations(QName... qnames) {
        relations = Arrays.asList(qnames);
        return this;
    }

    public AssignmentTargetRelationAsserter<AssignmentObjectRelationsAsserter<RA>> find() throws ObjectNotFoundException, SchemaException {
        AssignmentObjectRelation found = null;
        for (AssignmentObjectRelation item: collectionAsserter.getAssignmentTargetRelations()) {
            if (matches(item)) {
                if (found == null) {
                    found = item;
                } else {
                    fail("Found more than one assignment target relations that matches search criteria");
                }
            }
        }
        if (found == null) {
            fail("Found no assignment target relation that matches search criteria");
        }
        return collectionAsserter.forAssignmentTargetRelation(found);
    }

    public AssignmentObjectRelationsAsserter<RA> assertCount(int expectedCount) throws ObjectNotFoundException, SchemaException {
        int foundCount = 0;
        for (AssignmentObjectRelation item: collectionAsserter.getAssignmentTargetRelations()) {
            if (matches(item)) {
                foundCount++;
            }
        }
        assertEquals("Wrong number of assignment target relations for specified criteria in "+collectionAsserter.desc(), expectedCount, foundCount);
        return collectionAsserter;
    }

    private boolean matches(AssignmentObjectRelation item) throws ObjectNotFoundException, SchemaException {

        if (targetTypes != null) {
            if (!QNameUtil.unorderedCollectionMatch(targetTypes, item.getObjectTypes())) {
                return false;
            }
        }

        if (relations != null) {
            if (!QNameUtil.unorderedCollectionMatch(relations, item.getRelations())) {
                return false;
            }
        }

        // TODO: archetypes criterium

        return true;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

}
