/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.application.PanelDisplay;

@PanelType(name = "orgAssignments")
@PanelInstance(identifier = "orgAssignments",
        applicableFor = FocusType.class,
        childOf = AssignmentHolderAssignmentPanel.class)
@PanelDisplay(label = "Org", icon = GuiStyleConstants.CLASS_OBJECT_ORG_ICON, order = 30)
public class OrgAssignmentPanel<AH extends AssignmentHolderType> extends AbstractRoleAssignmentPanel {

    private static final long serialVersionUID = 1L;

    protected static final String DOT_CLASS = OrgAssignmentPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_TARGET_REF_OBJECT = DOT_CLASS + "loadAssignmentTargetRefObject";

    private ContainerPanelConfigurationType containerPanelConfigurationType;

    public OrgAssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
        super(id, assignmentContainerWrapperModel);
    }

    public OrgAssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel, ContainerPanelConfigurationType config) {
        super(id, assignmentContainerWrapperModel, config);
    }

    public OrgAssignmentPanel(String id, LoadableModel<PrismObjectWrapper<AH>> assignmentContainerWrapperModel, ContainerPanelConfigurationType config) {
        super(id, PrismContainerWrapperModel.fromContainerWrapper(assignmentContainerWrapperModel, AssignmentHolderType.F_ASSIGNMENT), config);
    }


    protected QName getAssignmentType() {
        return OrgType.COMPLEX_TYPE;
    }

}
