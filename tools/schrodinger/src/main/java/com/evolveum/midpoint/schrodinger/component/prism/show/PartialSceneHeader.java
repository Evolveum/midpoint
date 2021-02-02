/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.prism.show;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.page.org.OrgPage;
import com.evolveum.midpoint.schrodinger.page.role.RolePage;
import com.evolveum.midpoint.schrodinger.page.service.ServicePage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.apache.commons.lang3.ObjectUtils;
import org.testng.Assert;

import static com.codeborne.selenide.Selenide.$;

public class PartialSceneHeader extends Component<ScenePanel> {

    public PartialSceneHeader(ScenePanel parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public PartialSceneHeader assertChangeTypeEquals(String expectedValue) {
        SelenideElement element = $(Schrodinger.byDataId("changeType"));
        Assert.assertEquals(expectedValue, element.getText(), "Unexpected change type");
        return this;
    }

    public PartialSceneHeader assertChangedObjectNameEquals(String expectedValue) {
        SelenideElement element;
        if (isLink()) {
            element = getNameLink();
        } else {
            element = getNameLabel();
        }
        Assert.assertEquals(expectedValue, element.getText(), "Unexpected object name.");
        return this;
    }

    public PartialSceneHeader assertChangedObjectTypeEquals(String expectedValue) {
        SelenideElement element = $(Schrodinger.byDataId("objectType"));
        Assert.assertEquals(expectedValue, element.getText(), "Unexpected change object type");
        return this;
    }

    private boolean changedObjectTypeEquals(String expectedValue) {
        SelenideElement element = $(Schrodinger.byDataId("objectType"));
        return element != null && expectedValue != null
                && element.getText() != null && expectedValue.toLowerCase().equals(element.getText().toLowerCase());
    }

    public AssignmentHolderDetailsPage clickNameLink() {
        if (changedObjectTypeEquals("user")) {
            return new UserPage();
        }
        if (changedObjectTypeEquals("role")) {
            return new RolePage();
        }
        if (changedObjectTypeEquals("service")) {
            return new ServicePage();
        }
        if (changedObjectTypeEquals("org")) {
            return new OrgPage();
        }

        return null;
    }

    public boolean isLink() {
        SelenideElement element = getNameLink();
        return element.exists();
    }

    public PartialSceneHeader assertIsLink() {
        Assert.assertTrue(isLink(), "Link is expected.");
        return this;
    }

    public PartialSceneHeader assertIsNotLink() {
        Assert.assertFalse(isLink(), "Link is not expected.");
        return this;
    }

    private SelenideElement getNameLabel() {
        return $(Schrodinger.byDataId("nameLabel"));
    }

    private SelenideElement getNameLink() {
        return $(Schrodinger.byDataId("nameLink"));
    }
}
