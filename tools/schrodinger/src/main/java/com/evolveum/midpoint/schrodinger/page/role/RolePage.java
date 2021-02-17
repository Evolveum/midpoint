/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.role;

import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.ProjectionsTab;
import com.evolveum.midpoint.schrodinger.page.AbstractRolePage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RolePage extends AbstractRolePage {

    @Override
    public ProjectionsTab<RolePage> selectTabProjections() {
        return super.selectTabProjections();
    }

    @Override
    public AssignmentHolderBasicTab<RolePage> selectTabBasic() {
        return super.selectTabBasic();
    }

    @Override
    public AssignmentsTab<RolePage> selectTabAssignments() {
        return super.selectTabAssignments();
    }

    public RolePage assertName(String expectedValue) {
        selectTabBasic().form().assertPropertyInputValue("name", expectedValue);
        return this;
    }

    public RolePage assertDisplayName(String expectedValue) {
        selectTabBasic().form().assertPropertyInputValue("displayName", expectedValue);
        return this;
    }

    public RolePage assertIdentifier(String expectedValue) {
        selectTabBasic().form().assertPropertyInputValue("identifier", expectedValue);
        return this;
    }
}
