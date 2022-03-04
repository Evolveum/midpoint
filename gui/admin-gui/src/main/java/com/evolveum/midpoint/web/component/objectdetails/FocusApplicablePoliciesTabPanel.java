/*
 * Copyright (c) 2010-2018 Evolveum and contributors

 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.objectdetails;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.web.component.assignment.ApplicablePolicyConfigPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Created by honchar.
 */
public class FocusApplicablePoliciesTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F> {
    private static final long serialVersionUID = 1L;

    private static final String ID_APPLICABLE_POLICIES_CONTAINER = "applicablePoliciesContainer";
    private static final String ID_APPLICABLE_POLICIES_PANEL = "applicablePolicyPanel";

    public FocusApplicablePoliciesTabPanel(String id, LoadableModel<PrismObjectWrapper<F>> focusWrapperModel) {
        super(id, focusWrapperModel);
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer applicablePoliciesContainer = new WebMarkupContainer(ID_APPLICABLE_POLICIES_CONTAINER);
        applicablePoliciesContainer.setOutputMarkupId(true);
        add(applicablePoliciesContainer);

        ApplicablePolicyConfigPanel applicablePolicyPanel = new ApplicablePolicyConfigPanel<F>(ID_APPLICABLE_POLICIES_PANEL,
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), FocusType.F_ASSIGNMENT),
                getObjectWrapperModel());

        applicablePoliciesContainer.add(applicablePolicyPanel);
    }
}
