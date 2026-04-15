/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

public abstract class AbstractWizardNavigationBasicPanel<AHD extends AssignmentHolderDetailsModel<?>> extends AbstractWizardBasicPanel<AHD> {

    public AbstractWizardNavigationBasicPanel(String id, AHD detailsModel) {
        super(id, detailsModel);
    }

    @Override
    protected String getExitButtonCssClass() {
        return "btn-link";
    }
}
