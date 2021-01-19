/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.prism.show;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

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
