/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.prism.ObjectFilterAsserter;

public class RoleSelectionSpecificationRelationAsserter<RA> extends AbstractAsserter<RA> {

    private ObjectFilter filter;

    public RoleSelectionSpecificationRelationAsserter(ObjectFilter filter, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.filter = filter;
    }

    public ObjectFilterAsserter<RoleSelectionSpecificationRelationAsserter<RA>> filter() {
        ObjectFilterAsserter<RoleSelectionSpecificationRelationAsserter<RA>> asserter = new ObjectFilterAsserter<>(filter, this, "filter in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public RoleSelectionSpecificationRelationAsserter<RA> display() {
        display(desc());
        return this;
    }

    public RoleSelectionSpecificationRelationAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, filter);
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("relation");
    }
}
