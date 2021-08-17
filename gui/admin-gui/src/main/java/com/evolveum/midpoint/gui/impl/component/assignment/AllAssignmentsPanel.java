/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.assignment;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.AssignmentPopupDto;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.List;

@PanelType(name = "allAssignments")
@PanelInstance(identifier = "allAssignments",
        applicableFor = AssignmentHolderType.class,
        childOf = AssignmentHolderAssignmentPanel.class)
@PanelDisplay(label = "All", icon = GuiStyleConstants.EVO_ASSIGNMENT_ICON, order = 10)
public class AllAssignmentsPanel<AH extends AssignmentHolderType> extends AbstractAssignmentPanel<AH> {

    public AllAssignmentsPanel(String id, LoadableModel<PrismObjectWrapper<AH>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        return null;
    }

    @Override
    protected QName getAssignmentType() {
        return null;
    }

}
