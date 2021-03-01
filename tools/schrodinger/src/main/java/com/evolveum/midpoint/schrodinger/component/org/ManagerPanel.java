/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.org;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.testng.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author skublik
 */

public class ManagerPanel<T> extends Component<T> {

    public ManagerPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public boolean containsManager(String... expectedManagers) {
        ElementsCollection managersElements = getParentElement().$$x(".//span[@" + Schrodinger.DATA_S_ID + "='summaryDisplayName']");
        List<String> managers = new ArrayList<String>();
        for (SelenideElement managerElement : managersElements) {
            managers.add(managerElement.getText());
        }

        return managers.containsAll(Arrays.asList(expectedManagers));
    }

    public ManagerPanel<T> assertContainsManager(String... expectedManagers) {
        assertion.assertTrue(containsManager(expectedManagers));
        return this;
    }
}
