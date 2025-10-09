/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

@PanelType(name = "policyAssignments")
@PanelInstance(identifier = "policyAssignments",
        applicableForType = FocusType.class,
        childOf = AssignmentHolderAssignmentPanel.class,
        display = @PanelDisplay(label = "ObjectType.PolicyType", icon = GuiStyleConstants.CLASS_OBJECT_POLICY_ICON, order = 45))
public class PolicyAssignmentsPanel<AH extends AssignmentHolderType> extends AbstractRoleAssignmentPanel<AH> {

    public PolicyAssignmentsPanel(String id, IModel<PrismObjectWrapper<AH>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected QName getAssignmentType() {
        return PolicyType.COMPLEX_TYPE;
    }
}
