/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment;

import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

@PanelType(name = "allAssignments")
@PanelInstance(identifier = "allAssignments",
        applicableForType = AssignmentHolderType.class,
        childOf = AssignmentHolderAssignmentPanel.class,
        display = @PanelDisplay(label = "AssignmentPanel.allLabel", icon = GuiStyleConstants.EVO_ASSIGNMENT_ICON, order = 10))
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

    @Override
    protected void addSpecificSearchableItems(PrismContainerDefinition<AssignmentType> containerDef,
            List<SearchItemDefinition> defs) {
        super.addSpecificSearchableItems(containerDef, defs);
        if (isRepositorySearchEnabled()) {
            SearchFactory.addSearchPropertyDef(containerDef, TARGET_REF_OBJ.append(AbstractRoleType.F_NAME), defs);
        }
    }
}
