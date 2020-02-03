/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.prism.ObjectFilterAsserter;
import com.evolveum.midpoint.util.PrettyPrinter;
import org.testng.AssertJUnit;

import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author semancik
 */
public class RoleSelectionSpecificationAsserter<RA> extends AbstractAsserter<RA> {

    private final RoleSelectionSpecification roleSelectionSpec;

    public RoleSelectionSpecificationAsserter(RoleSelectionSpecification roleSelectionSpec, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.roleSelectionSpec = roleSelectionSpec;
    }

    RoleSelectionSpecification getRoleSelectionSpecification() {
        assertNotNull("Null " + desc(), roleSelectionSpec);
        return roleSelectionSpec;
    }

    public RoleSelectionSpecificationAsserter<RA> assertNull() {
        AssertJUnit.assertNull("Unexpected " + desc(), roleSelectionSpec);
        return this;
    }

    public RoleSelectionSpecificationAsserter<RA> assertSize(int expectedSize) {
        AssertJUnit.assertEquals("Unexpected size of " + desc(), expectedSize, roleSelectionSpec.size());
        return this;
    }

    public RoleSelectionSpecificationAsserter<RA> assertNoAccess() {
        assertNotNull("Null " + desc(), roleSelectionSpec);
        globalFilter().assertNone();
        return this;
    }

    public ObjectFilterAsserter<RoleSelectionSpecificationAsserter<RA>> globalFilter() {
        ObjectFilterAsserter<RoleSelectionSpecificationAsserter<RA>> asserter = new ObjectFilterAsserter<>(roleSelectionSpec.getGlobalFilter(), this, "global filter in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public RoleSelectionSpecificationRelationAsserter<RoleSelectionSpecificationAsserter<RA>> relation(QName relation) {
        ObjectFilter filter = getRoleSelectionSpecification().getRelationFilter(relation);
        RoleSelectionSpecificationRelationAsserter<RoleSelectionSpecificationAsserter<RA>> asserter = new RoleSelectionSpecificationRelationAsserter<>(filter, this, "relation "+ PrettyPrinter.prettyPrint(relation) + " in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public RoleSelectionSpecificationRelationAsserter<RoleSelectionSpecificationAsserter<RA>> relationDefault() {
        return relation(SchemaConstants.ORG_DEFAULT);
    }

    public RoleSelectionSpecificationAsserter<RA> display() {
        display(desc());
        return this;
    }

    public RoleSelectionSpecificationAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, roleSelectionSpec);
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("role selection specification");
    }

}
