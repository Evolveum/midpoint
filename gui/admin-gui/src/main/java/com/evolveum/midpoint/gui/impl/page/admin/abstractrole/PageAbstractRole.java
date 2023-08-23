/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.abstractrole;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.focus.PageFocusDetails;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction.ConstructionWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.BusinessRoleDto;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

public abstract class PageAbstractRole<AR extends AbstractRoleType, ARDM extends FocusDetailsModels<AR>> extends PageFocusDetails<AR, ARDM> {

    public PageAbstractRole() {
        super();
    }

    public PageAbstractRole(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageAbstractRole(PrismObject<AR> focus) {
        super(focus);
    }

    public PageAbstractRole(PrismObject<AR> focus, List<BusinessRoleDto> patternDeltas) {
        super(focus, patternDeltas);
    }

    public void showConstructionWizard(AjaxRequestTarget target) {
        showWizard(target, null, ConstructionWizardPanel.class);
    }

}
