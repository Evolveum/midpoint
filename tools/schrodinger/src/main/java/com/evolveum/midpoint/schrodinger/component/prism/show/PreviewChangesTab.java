/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.prism.show;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.PreviewPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

public class PreviewChangesTab extends Component<PreviewPage> {

    public PreviewChangesTab(PreviewPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ScenePanel<PreviewChangesTab> primaryDeltas() {
        SelenideElement primaryDeltas = $(Schrodinger.byDataId("primaryDeltas"));
        return new ScenePanel<>(this, primaryDeltas);
    }

    public ScenePanel<PreviewChangesTab> secondaryDeltas() {
        SelenideElement secondaryDeltas = $(Schrodinger.byDataId("secondaryDeltas"));
        return new ScenePanel<>(this, secondaryDeltas);
    }
}
