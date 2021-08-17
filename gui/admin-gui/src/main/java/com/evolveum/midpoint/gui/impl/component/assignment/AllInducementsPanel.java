/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.assignment;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractRoleInducementPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;

import javax.xml.namespace.QName;
import java.util.List;

@PanelType(name = "allInducements")
@PanelInstance(identifier = "allInducements",
        applicableFor = AbstractRoleType.class,
        childOf = AbstractRoleInducementPanel.class)
@PanelDisplay(label = "All", icon = GuiStyleConstants.EVO_ASSIGNMENT_ICON, order = 10)
public class AllInducementsPanel<AR extends AbstractRoleType> extends AbstractInducementPanel<AR> {

    public AllInducementsPanel(String id, LoadableModel<PrismObjectWrapper<AR>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        return null;
    }


}
