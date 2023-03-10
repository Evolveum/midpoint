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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 */
public class AssignmentAsserter<R> extends AbstractAsserter<R> {

    private final AssignmentType assignment;

    public AssignmentAsserter(AssignmentType assignment) {
        super();
        this.assignment = assignment;
    }

    public AssignmentAsserter(AssignmentType assignment, String detail) {
        super(detail);
        this.assignment = assignment;
    }

    public AssignmentAsserter(AssignmentType assignment, R returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.assignment = assignment;
    }

    protected AssignmentType getAssignment() {
        return assignment;
    }

    public String getTargetOid() {
        return getAssignment().getTargetRef().getOid();
    }

    public AssignmentAsserter<R> assertTargetOid() {
        assertNotNull("No target OID in " + desc(), getTargetOid());
        return this;
    }

    public AssignmentAsserter<R> assertTargetOid(String expected) {
        assertEquals("Wrong target OID in " + desc(), expected, getTargetOid());
        return this;
    }

    public AssignmentAsserter<R> assertTargetType(QName expected) {
        assertEquals("Wrong target type in " + desc(), expected, getAssignment().getTargetRef().getType());
        return this;
    }

    public AssignmentAsserter<R> assertRole(String expectedOid) {
        assertTargetOid(expectedOid);
        assertTargetType(RoleType.COMPLEX_TYPE);
        return this;
    }

    public AssignmentAsserter<R> assertResource(String expectedOid) {
        ObjectReferenceType resourceRef = getConstructionRequired().getResourceRef();
        assertThat(resourceRef).as("resourceRef in construction in " + desc()).isNotNull();
        assertThat(resourceRef.getOid()).as("resource OID in construction in " + desc()).isEqualTo(expectedOid);
        return this;
    }

    private @NotNull ConstructionType getConstructionRequired() {
        ConstructionType construction = assignment.getConstruction();
        assertThat(construction).as("construction in " + desc()).isNotNull();
        return construction;
    }

    /** Checks the kind exactly (no defaults are assumed). */
    public AssignmentAsserter<R> assertKind(ShadowKindType expected) {
        assertThat(getConstructionRequired().getKind())
                .as("kind in construction in " + desc())
                .isEqualTo(expected);
        return this;
    }

    public AssignmentAsserter<R> assertIntent(String expected) {
        assertThat(getConstructionRequired().getIntent())
                .as("intent in construction in " + desc())
                .isEqualTo(expected);
        return this;
    }

    public AssignmentAsserter<R> assertSubtype(String expected) {
        List<String> subtypes = assignment.getSubtype();
        if (subtypes.isEmpty()) {
            fail("No subtypes in " + desc() + ", expected " + expected);
        }
        if (subtypes.size() > 1) {
            fail("Too many subtypes in " + desc() + ", expected " + expected + ", was " + subtypes);
        }
        assertEquals("Wrong subtype in " + desc(), expected, subtypes.get(0));
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
        PrismContainer<ValueMetadataType> valueMetadata = assignment.asPrismContainerValue().getValueMetadataAsContainer();
        ValueMetadataAsserter<AssignmentAsserter<R>> asserter =
                new ValueMetadataAsserter<>(valueMetadata, this, "."); // TODO details
        copySetupTo(asserter);
        return asserter;
    }

    public ValueMetadataValueAsserter<AssignmentAsserter<R>> valueMetadataSingle() {
        PrismContainer<ValueMetadataType> valueMetadata = assignment.asPrismContainerValue().getValueMetadataAsContainer();
        if (valueMetadata.size() != 1) {
            fail("Value metadata container has none or multiple values: " + valueMetadata);
        }
        ValueMetadataValueAsserter<AssignmentAsserter<R>> asserter =
                new ValueMetadataValueAsserter<>(valueMetadata.getValue(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public AssignmentAsserter<R> assertExclusionViolationSituation() {
        return assertPolicySituation(SchemaConstants.MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION);
    }

    public AssignmentAsserter<R> assertPolicySituation(String... expected) {
        assertThat(assignment.getPolicySituation())
                .as("policy situation in " + assignment)
                .containsExactlyInAnyOrder(expected);
        return this;
    }
}
