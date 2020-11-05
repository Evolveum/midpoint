/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.resource;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.TabPanel;
/**
 * Created by honchar.
 */
public class SchemaWizardStep extends Component<ResourceWizardPage> {
    public SchemaWizardStep(ResourceWizardPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public TabPanel<SchemaWizardStep> getTabPanel() {
        return new TabPanel<>(this, getParentElement());
    }

    public SchemaStepSchemaTab selectSchemaTab() {
        return new SchemaStepSchemaTab(this, getTabPanel().clickTab("Schema"));
    }

    public SchemaStepXmlTab selectXmlTab() {
        return new SchemaStepXmlTab(this, getTabPanel().clickTab("XML"));
    }
}
