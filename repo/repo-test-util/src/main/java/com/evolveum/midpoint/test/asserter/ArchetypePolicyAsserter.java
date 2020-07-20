/*
 * Copyright (c) 2018-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertNotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemConstraintType;

import org.assertj.core.api.Assertions;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypePolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;

/**
 * @author semancik
 *
 */
public class ArchetypePolicyAsserter<RA> extends AbstractAsserter<RA> {

    private final ArchetypePolicyType archetypePolicy;

    public ArchetypePolicyAsserter(ArchetypePolicyType archetypePolicy, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.archetypePolicy = archetypePolicy;
    }

    ArchetypePolicyType getArchetypePolicy() {
        assertNotNull("Null " + desc(), archetypePolicy);
        return archetypePolicy;
    }

    public ArchetypePolicyAsserter<RA> assertNull() {
        AssertJUnit.assertNull("Unexpected " + desc() + ": "+archetypePolicy, archetypePolicy);
        return this;
    }

    public DisplayTypeAsserter<ArchetypePolicyAsserter<RA>> displayType() {
        DisplayTypeAsserter<ArchetypePolicyAsserter<RA>> displayAsserter = new DisplayTypeAsserter<>(getArchetypePolicy().getDisplay(), this, "in " + desc());
        copySetupTo(displayAsserter);
        return displayAsserter;
    }

    public ArchetypePolicyAsserter<RA> assertItemConstraints(int expectedCount) {
        List<ItemConstraintType> itemConstraintTypeList = getArchetypePolicy().getItemConstraint();
        Assertions.assertThat(itemConstraintTypeList).hasSize(expectedCount);
        return this;
    }

    public ItemConstraintsAsserter<ArchetypePolicyAsserter<RA>> itemConstraints() {
        return new ItemConstraintsAsserter<>(getArchetypePolicy().getItemConstraint(), this, "for archetypePolicy " + archetypePolicy);
    }

    public ArchetypePolicyAsserter<RA> assertNoDisplay() {
        AssertJUnit.assertNull("Unexpected display specification in " + desc() + ": "+archetypePolicy.getDisplay(), archetypePolicy.getDisplay());
        return this;
    }

    public ArchetypeAdminGuiConfigurationAsserter<ArchetypePolicyAsserter<RA>> adminGuiConfig() {
        return new ArchetypeAdminGuiConfigurationAsserter<>(getArchetypePolicy().getAdminGuiConfiguration(), this, "for archetypePolicy " + archetypePolicy);
    }

    public ArchetypePolicyAsserter<RA> assertObjectTemplate(String expectedOid) {
        ObjectReferenceType objectTemplateRef = archetypePolicy.getObjectTemplateRef();
        AssertJUnit.assertNotNull("Missing objectTemplateRef in " + desc(), objectTemplateRef);
        AssertJUnit.assertEquals("Wrong OID in objectTemplateRef in " + desc(), expectedOid, objectTemplateRef.getOid());
        return this;
    }

    public ArchetypePolicyAsserter<RA> display() {
        display(desc());
        return this;
    }

    public ArchetypePolicyAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, archetypePolicy);
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("archetype policy");
    }

}
