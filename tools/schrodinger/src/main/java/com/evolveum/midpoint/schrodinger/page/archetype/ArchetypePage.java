/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.archetype;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.ProjectionsTab;
import com.evolveum.midpoint.schrodinger.component.archetype.ArchetypePolicyTab;
import com.evolveum.midpoint.schrodinger.page.AbstractRolePage;
import com.evolveum.midpoint.schrodinger.page.service.ServicePage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ArchetypePage extends AbstractRolePage {

    @Override
    public ProjectionsTab<ArchetypePage> selectTabProjections() {
        return super.selectTabProjections();
    }

    @Override
    public AssignmentHolderBasicTab<ArchetypePage> selectTabBasic() {
        return super.selectTabBasic();
    }

    @Override
    public AssignmentsTab<ArchetypePage> selectTabAssignments() {
        return super.selectTabAssignments();
    }

    public ArchetypePolicyTab selectTabArchetypePolicy() {
        SelenideElement element = getTabPanel().clickTab("PageArchetype.archetypePolicy")
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ArchetypePolicyTab(this, element);
    }

    public ArchetypePage assertName(String expectedValue) {
        selectTabBasic().form().assertPropertyInputValue("name", expectedValue);
        return this;
    }

    public ArchetypePage assertDisplayName(String expectedValue) {
        selectTabBasic().form().assertPropertyInputValue("displayName", expectedValue);
        return this;
    }

    public ArchetypePage assertIdentifier(String expectedValue) {
        selectTabBasic().form().assertPropertyInputValue("identifier", expectedValue);
        return this;
    }

}
