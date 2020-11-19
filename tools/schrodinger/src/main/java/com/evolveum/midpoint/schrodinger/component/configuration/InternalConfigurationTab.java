/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.configuration.InternalsConfigurationPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

import static com.evolveum.midpoint.schrodinger.util.Utils.setOptionCheckedById;

/**
 * Created by Viliam Repan (lazyman).
 */
public class InternalConfigurationTab extends Component<InternalsConfigurationPage> {

    public InternalConfigurationTab(InternalsConfigurationPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public InternalConfigurationTab selectCheckConsistency() {
        setOptionCheckedById("consistencyChecks", true);
        return this;
    }

    public InternalConfigurationTab deselectCheckConsistency() {
        setOptionCheckedById("consistencyChecks", false);
        return this;
    }

    public InternalConfigurationTab selectCheckEncryption() {
        setOptionCheckedById("encryptionChecks", true);
        return this;
    }

    public InternalConfigurationTab deselectCheckEncryption() {
        setOptionCheckedById("encryptionChecks", false);
        return this;
    }

    public InternalConfigurationTab selectCheckReadEncryption() {
        setOptionCheckedById("readEncryptionChecks", true);
        return this;
    }

    public InternalConfigurationTab deselectCheckReadEncryption() {
        setOptionCheckedById("readEncryptionChecks", false);
        return this;
    }

    public InternalConfigurationTab selectModelProfiling() {
        setOptionCheckedById("modelProfiling", true);
        return this;
    }

    public InternalConfigurationTab deselectModelProfiling() {
        setOptionCheckedById("modelProfiling", false);
        return this;
    }

    public InternalConfigurationTab selectTolerateUndeclaredPrefixes() {
        setOptionCheckedById("tolerateUndeclaredPrefixes", true);
        return this;
    }

    public InternalConfigurationTab deselectTolerateUndeclaredPrefixes() {
        setOptionCheckedById("tolerateUndeclaredPrefixes", false);
        return this;
    }

    public InternalConfigurationTab clickUpdate() {
        $(Schrodinger.byDataId("updateInternalsConfig"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }
}

