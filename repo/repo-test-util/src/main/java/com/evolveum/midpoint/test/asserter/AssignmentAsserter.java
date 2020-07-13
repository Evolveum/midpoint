/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

/**
 * @author semancik
 *
 */
public class AssignmentAsserter<R> extends AbstractAsserter<R> {

    final private AssignmentType assignment;
    private PrismObject<?> resolvedTarget = null;

    public AssignmentAsserter(AssignmentType assignment) {
        super();
        this.assignment = assignment;
    }

    public AssignmentAsserter(AssignmentType assignment, String detail) {
        super(detail);
        this.assignment = assignment;
    }

    public AssignmentAsserter(AssignmentType assignment, PrismObject<?> resolvedTarget, R returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.assignment = assignment;
        this.resolvedTarget = resolvedTarget;
    }

    protected AssignmentType getAssignment() {
        return assignment;
    }

    public String getTargetOid() {
        return getAssignment().getTargetRef().getOid();
    }

    public AssignmentAsserter<R> assertTargetOid() {
        assertNotNull("No target OID in "+desc(), getTargetOid());
        return this;
    }

    public AssignmentAsserter<R> assertTargetOid(String expected) {
        assertEquals("Wrong target OID in "+desc(), expected, getTargetOid());
        return this;
    }

    public AssignmentAsserter<R> assertTargetType(QName expected) {
        assertEquals("Wrong target type in "+desc(), expected, getAssignment().getTargetRef().getType());
        return this;
    }

    public AssignmentAsserter<R> assertRole(String expectedOid) {
        assertTargetOid(expectedOid);
        assertTargetType(RoleType.COMPLEX_TYPE);
        return this;
    }

    public AssignmentAsserter<R> assertResource(String expectedOid) {
        ConstructionType construction = assignment.getConstruction();
        assertThat(construction).as("construction in " + desc()).isNotNull();
        assertThat(construction.getResourceRef()).as("resourceRef in construction in " + desc()).isNotNull();
        assertThat(construction.getResourceRef().getOid()).as("resource OID in construction in " + desc())
                .isEqualTo(expectedOid);
        return this;
    }

    public AssignmentAsserter<R> assertSubtype(String expected) {
        List<String> subtypes = assignment.getSubtype();
        if (subtypes.isEmpty()) {
            fail("No subtypes in "+desc()+", expected "+expected);
        }
        if (subtypes.size() > 1) {
            fail("Too many subtypes in "+desc()+", expected "+expected+", was "+subtypes);
        }
        assertEquals("Wrong subtype in "+desc(), expected, subtypes.get(0));
        return this;
    }

    public AssignmentAsserter<R> assertOriginMappingName(String expected) {
        assertEquals("Wrong origin mapping name", expected, getOriginMappingName());
        return this;
    }

    private String getOriginMappingName() {
        return assignment.getMetadata() != null ? assignment.getMetadata().getOriginMappingName() : null;
    }

    public ActivationAsserter<AssignmentAsserter<R>> activation() {
        ActivationAsserter<AssignmentAsserter<R>> asserter = new ActivationAsserter<>(assignment.getActivation(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public MetadataAsserter<AssignmentAsserter<R>> metadata() {
        MetadataAsserter<AssignmentAsserter<R>> asserter = new MetadataAsserter<>(assignment.getMetadata(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    protected String desc() {
        // TODO: better desc
        return descWithDetails(assignment);
    }

    public AssignmentAsserter<R> display() {
        display(desc());
        return this;
    }

    public AssignmentAsserter<R> display(String message) {
        IntegrationTestTools.display(message, assignment);
        return this;
    }

    public ValueMetadataAsserter<AssignmentAsserter<R>> valueMetadata() {
        //noinspection unchecked
        PrismContainerValue<ValueMetadataType> valueMetadata = (PrismContainerValue<ValueMetadataType>)
                (PrismContainerValue<?>) assignment.asPrismContainerValue().getValueMetadata();
        ValueMetadataAsserter<AssignmentAsserter<R>> asserter =
                new ValueMetadataAsserter<>(valueMetadata, this, "."); // TODO details
        copySetupTo(asserter);
        return asserter;
    }
}
