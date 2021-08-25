/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.web.component.assignment.SwitchAssignmentTypePanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

//@PanelType(name = "assignments", defaultContainerPath = "assignment")
@PanelInstance(identifier = "assignments", applicableFor = AssignmentHolderType.class)
@PanelDisplay(label = "Assignments", icon = GuiStyleConstants.EVO_ASSIGNMENT_ICON, order = 30)
@Counter(provider = AssignmentCounter.class)
public class AssignmentHolderAssignmentPanel<AH extends AssignmentHolderType> extends AbstractObjectMainPanel<AH, ObjectDetailsModels<AH>> {

    private static final String ID_ASSIGNMENTS = "assignmentsContainer";
    private static final String ID_ASSIGNMENTS_PANEL = "assignmentsPanel";

    public AssignmentHolderAssignmentPanel(String id, ObjectDetailsModels<AH> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
        assignments.setOutputMarkupId(true);
        add(assignments);
    }

    protected SwitchAssignmentTypePanel createPanel(String panelId, PrismContainerWrapperModel<AH, AssignmentType> model) {
        return new SwitchAssignmentTypePanel(panelId, model != null ? model : Model.of(), getPanelConfiguration()){
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean isReadonly(){
                return AssignmentHolderAssignmentPanel.this.isReadonly();
            }
        };
    }

    protected boolean isReadonly(){
        return false;
    }
}
