/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.prism.show;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.Selenide.$;

public class ScenePanel<T> extends Component<T> {

    public ScenePanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public boolean isExpanded() {
        SelenideElement minimizeButton = $(Schrodinger.byDataId("minimizeButton"));
        SelenideElement icon = minimizeButton.$(By.tagName("i"));
        return icon.has(Condition.cssClass("fa-chevron-down"));
    }

    public List<ScenePanel> objectDeltas() {
        ElementsCollection collection = $(Schrodinger.byDataId("body")).$$(Schrodinger.byDataId("partialScene"));
        List<ScenePanel> partialScenes = new ArrayList<>(collection.size());
        collection.forEach(element -> partialScenes.add(new ScenePanel<>(this, element)));
        return partialScenes;
    }

    public PartialSceneHeader header() {
        SelenideElement element = $(Schrodinger.byDataId("headerPanel"));
        return new PartialSceneHeader(this, element);
    }



}
