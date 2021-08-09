/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.assignment.TabbedAssignmentTypePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.web.application.PanelDescription;
import com.evolveum.midpoint.web.component.assignment.SwitchAssignmentTypePanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PanelDescription(identifier = "assignments",
        panelIdentifier = "assignments",
        applicableFor = AssignmentHolderType.class,
        path = "assignment")
@PanelDisplay(label = "Assignments", icon = GuiStyleConstants.EVO_ASSIGNMENT_ICON)
public class AssignmentHolderAssignmentPanel<AH extends AssignmentHolderType> extends AbstractObjectMainPanel<AH> {

    private static final String ID_ASSIGNMENTS = "assignmentsContainer";
    private static final String ID_ASSIGNMENTS_PANEL = "assignmentsPanel";

    public AssignmentHolderAssignmentPanel(String id, LoadableModel<PrismObjectWrapper<AH>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
        assignments.setOutputMarkupId(true);
        add(assignments);
        PrismContainerWrapperModel<AH, AssignmentType> assignmentModel = PrismContainerWrapperModel.fromContainerWrapper(getModel(), AssignmentHolderType.F_ASSIGNMENT);
        List<ITab> tabs = createAssignmentTabs(assignmentModel);
        TabbedAssignmentTypePanel panel = new TabbedAssignmentTypePanel(ID_ASSIGNMENTS_PANEL, tabs, assignmentModel, getPanelConfiguration());

//        SwitchAssignmentTypePanel panel = createPanel(ID_ASSIGNMENTS_PANEL, assignmentModel);

        assignments.add(panel);
    }

    private List<ITab> createAssignmentTabs(PrismContainerWrapperModel<AH, AssignmentType> assignmentModel) {
        List<ITab> tabs = new ArrayList<>();
        for (ContainerPanelConfigurationType panelConfig : getAssignmentPanels()) {
            tabs.add(new AbstractTab(createStringResource(getLabel(panelConfig))) {
                @Override
                public WebMarkupContainer getPanel(String s) {

                    String panelIdentifier =  panelConfig.getPanelIdentifier();
                    Panel panel = WebComponentUtil.createPanel(panelIdentifier, s, assignmentModel, panelConfig);
                    return panel;
                }
            });
        }
        return tabs;
    }

    private String getLabel(ContainerPanelConfigurationType panelConfig) {
        if (panelConfig == null) {
            return "N/A";
        }
        if (panelConfig.getDisplay() == null) {
            return "N/A";
        }
        return WebComponentUtil.getOrigStringFromPoly(panelConfig.getDisplay().getLabel());
    }

    private List<ContainerPanelConfigurationType> getAssignmentPanels() {
//        List<ContainerPanelConfigurationType> panels = PanelLoader.getAssignmentPanelsFor(UserType.class);
        List<ContainerPanelConfigurationType> subPanels = getPanelConfiguration().getPanel();
        Map<String, ContainerPanelConfigurationType> panelsMap = new HashMap<>();
        for (ContainerPanelConfigurationType subPanel : subPanels) {
            panelsMap.put(subPanel.getIdentifier(), subPanel);
        }
        return subPanels;
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
