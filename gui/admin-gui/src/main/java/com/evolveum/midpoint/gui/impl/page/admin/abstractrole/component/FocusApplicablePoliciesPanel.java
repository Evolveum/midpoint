/*
 * Copyright (c) 2010-2018 Evolveum and contributors

 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.assignment.ApplicablePolicyConfigPanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Created by honchar.
 */
@PanelType(name = "applicablePolicies")
@PanelInstance(identifier = "applicablePolicies",
        applicableForType = AbstractRoleType.class,
        display = @PanelDisplay(label = "pageAdminFocus.applicablePolicies", icon = GuiStyleConstants.CLASS_APPLICABLE_POLICY_ICON, order = 60))
public class FocusApplicablePoliciesPanel<AR extends AbstractRoleType> extends AbstractObjectMainPanel<AR, FocusDetailsModels<AR>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_APPLICABLE_POLICIES_CONTAINER = "applicablePoliciesContainer";
    private static final String ID_APPLICABLE_POLICIES_PANEL = "applicablePolicyPanel";

    public FocusApplicablePoliciesPanel(String id, FocusDetailsModels<AR> focusWrapperModel, ContainerPanelConfigurationType config) {
        super(id, focusWrapperModel, config);
    }

    protected void initLayout() {
        WebMarkupContainer applicablePoliciesContainer = new WebMarkupContainer(ID_APPLICABLE_POLICIES_CONTAINER);
        applicablePoliciesContainer.setOutputMarkupId(true);
        add(applicablePoliciesContainer);

        ApplicablePolicyConfigPanel<AR> applicablePolicyPanel = new ApplicablePolicyConfigPanel<>(ID_APPLICABLE_POLICIES_PANEL,
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), FocusType.F_ASSIGNMENT),
                getObjectWrapperModel());

        applicablePoliciesContainer.add(applicablePolicyPanel);
    }
}
