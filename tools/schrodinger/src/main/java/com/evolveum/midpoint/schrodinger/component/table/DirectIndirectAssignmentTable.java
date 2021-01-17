/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.table;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.testng.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author skublik
 */

public class DirectIndirectAssignmentTable<T> extends Component<T> {

    public DirectIndirectAssignmentTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public boolean containsIndirectAssignments(String... expectedAssignments) {
        return containsAssignments("Indirect", expectedAssignments);
    }

    public boolean containsDirectAssignments(String... expectedAssignments) {
        return containsAssignments("Direct", expectedAssignments);
    }

    private boolean containsAssignments(String status, String... expectedAssignments) {
        ElementsCollection labels = getParentElement()
                .$$(Schrodinger.byAncestorFollowingSiblingDescendantOrSelfElementEnclosedValue("span", "data-s-id", "label", "data-s-id", "3", status));
        List<String> indirectAssignments = new ArrayList<String>();
        for (SelenideElement label : labels) {
            if (!label.getText().isEmpty()) {
                indirectAssignments.add(label.getText());
            }
        }
        return indirectAssignments.containsAll(Arrays.asList(expectedAssignments));
    }

    public DirectIndirectAssignmentTable<T> assertIndirectAssignmentsExist(String... expectedAssignments) {
        Assert.assertTrue(containsIndirectAssignments(expectedAssignments));
        return this;
    }

    public DirectIndirectAssignmentTable<T> assertDirectAssignmentsExist(String... expectedAssignments) {
        Assert.assertTrue(containsDirectAssignments(expectedAssignments));
        return this;
    }

}
