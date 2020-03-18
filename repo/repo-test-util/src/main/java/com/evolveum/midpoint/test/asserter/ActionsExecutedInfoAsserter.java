/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertTrue;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.statistics.ActionsExecutedInformation;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActionsExecutedInformationType;

/**
 * Asserter that checks task "actions executed" information.
 */
public class ActionsExecutedInfoAsserter<RA> extends AbstractAsserter<RA> {

    private final ActionsExecutedInformationType information;

    ActionsExecutedInfoAsserter(ActionsExecutedInformationType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ActionsExecutedInfoAsserter<RA> display() {
        IntegrationTestTools.display(desc(), ActionsExecutedInformation.format(information));
        return this;
    }

    public ActionsExecutedInfoAsserter<RA> assertEmpty() {
        assertTrue("all object actions list is not empty", information.getObjectActionsEntry().isEmpty());
        assertTrue("resulting object actions list is not empty", information.getResultingObjectActionsEntry().isEmpty());
        return this;
    }

    public ActionsExecutedPartInfoAsserter<ActionsExecutedInfoAsserter<RA>> resulting() {
        return part(ActionsExecutedInformation.Part.RESULTING);
    }

    public ActionsExecutedPartInfoAsserter<ActionsExecutedInfoAsserter<RA>> all() {
        return part(ActionsExecutedInformation.Part.ALL);
    }

    public ActionsExecutedPartInfoAsserter<ActionsExecutedInfoAsserter<RA>> part(@NotNull ActionsExecutedInformation.Part part) {
        ActionsExecutedPartInfoAsserter<ActionsExecutedInfoAsserter<RA>> asserter =
                new ActionsExecutedPartInfoAsserter<>(information, this, part, getDetails());
        copySetupTo(asserter);
        return asserter;
    }
}
