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

import static com.codeborne.selenide.Selenide.$;

public class PartialSceneHeader extends Component<ScenePanel> {

    public PartialSceneHeader(ScenePanel parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public String getChangeType() {
        SelenideElement element = $(Schrodinger.byDataId("changeType"));
        return element.getText();
    }

    public String getChangedObjectName() {
        SelenideElement element;
        if (isLink()) {
            element = getNameLink();
        } else {
            element = getNameLabel();
        }
        return element.getText();
    }

    public boolean isLink() {
        SelenideElement element = getNameLink();
        return element.exists();
    }

    private SelenideElement getNameLabel() {
        return $(Schrodinger.byDataId("nameLabel"));
    }

    private SelenideElement getNameLink() {
        return $(Schrodinger.byDataId("nameLink"));
    }
}
