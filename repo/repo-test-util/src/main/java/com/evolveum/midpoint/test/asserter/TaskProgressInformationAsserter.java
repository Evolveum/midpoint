/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import com.evolveum.midpoint.schema.util.task.TaskPartProgressInformation;
import com.evolveum.midpoint.schema.util.task.TaskProgressInformation;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;

/**
 *  Asserter that checks {@link TaskProgressInformation} objects.
 */
@SuppressWarnings("unused")
public class TaskProgressInformationAsserter<RA> extends AbstractAsserter<RA> {

    private final TaskProgressInformation information;

    public TaskProgressInformationAsserter(TaskProgressInformation information, String details) {
        super(details);
        this.information = information;
    }

    public TaskProgressInformationAsserter<RA> assertAllPartsCount(int expected) {
        assertThat(information.getAllPartsCount()).as("all parts count").isEqualTo(expected);
        return this;
    }

    public TaskProgressInformationAsserter<RA> assertCurrentPartNumber(Integer expected) {
        assertThat(information.getCurrentPartNumber()).as("current part number").isEqualTo(expected);
        return this;
    }

    public TaskProgressInformationAsserter<RA> assertCurrentPartUri(String expected) {
        assertThat(information.getCurrentPartUri()).as("current part URI").isEqualTo(expected);
        return this;
    }

    public TaskProgressInformationAsserter<RA> assertParts(int expected) {
        assertThat(information.getParts().size()).as("parts size").isEqualTo(expected);
        return this;
    }

    public TaskPartProgressInformationAsserter<TaskProgressInformationAsserter<RA>> part(String uri) {
        TaskPartProgressInformation partInformation = information.getParts().get(uri);
        assertThat(partInformation).as("part information for: " + uri).isNotNull();
        TaskPartProgressInformationAsserter<TaskProgressInformationAsserter<RA>> asserter =
                new TaskPartProgressInformationAsserter<>(partInformation, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public TaskPartProgressInformationAsserter<TaskProgressInformationAsserter<RA>> currentPart() {
        return part(information.getCurrentPartUri());
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public TaskProgressInformationAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(information));
        return this;
    }
}
