/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractVerifiableWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.Containerable;

public abstract class AbstractResourceNavigationWizardBasicPanel<C extends Containerable> extends AbstractVerifiableWizardBasicPanel<C, ResourceDetailsModel> {

    public AbstractResourceNavigationWizardBasicPanel(
            String id,
            WizardPanelHelper<C, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected String getExitButtonCssClass() {
        return "btn-link";
    }
}
