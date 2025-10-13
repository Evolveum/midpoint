/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.WebMarkupContainer;

//@PanelType(name = "assignments", defaultContainerPath = "assignment")
@PanelInstance(identifier = "assignments",
        applicableForType = FocusType.class, // change later to assignmentHolder type, probably we will want org assignments later
        display = @PanelDisplay(label = "pageAdminFocus.assignments", icon = GuiStyleConstants.EVO_ASSIGNMENT_ICON, order = 30))
@Counter(provider = AssignmentCounter.class)
public class AssignmentHolderAssignmentPanel<AH extends AssignmentHolderType> extends AbstractObjectMainPanel<AH, ObjectDetailsModels<AH>> {

    private static final String ID_ASSIGNMENTS = "assignmentsContainer";

    public AssignmentHolderAssignmentPanel(String id, ObjectDetailsModels<AH> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
        assignments.setOutputMarkupId(true);
        add(assignments);
    }

    protected boolean isReadonly(){
        return false;
    }
}
