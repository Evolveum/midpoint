/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicInitializer;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

public abstract class AbstractWizardBasicWithNavigationPanel<AHD extends AssignmentHolderDetailsModel> extends AbstractWizardBasicInitializer {

    public AbstractWizardBasicWithNavigationPanel(String id) {
        super(id);
    }
}
