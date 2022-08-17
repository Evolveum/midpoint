/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

@PanelType(name = "orgAssignments")
@PanelInstance(identifier = "orgAssignments",
        applicableForType = FocusType.class,
        childOf = AssignmentHolderAssignmentPanel.class,
        display = @PanelDisplay(label = "ObjectType.OrgType", icon = GuiStyleConstants.CLASS_OBJECT_ORG_ICON, order = 30))
public class OrgAssignmentsPanel<AH extends AssignmentHolderType> extends AbstractRoleAssignmentPanel<AH> {

    public OrgAssignmentsPanel(String id, IModel<PrismObjectWrapper<AH>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected QName getAssignmentType() {
        return OrgType.COMPLEX_TYPE;
    }

    @Override
    protected void addSpecificSearchableItems(PrismContainerDefinition<AssignmentType> containerDef,
            List<SearchItemDefinition> defs) {
        super.addSpecificSearchableItems(containerDef, defs);
        if (isRepositorySearchEnabled()) {
            SearchFactory.addSearchPropertyDef(containerDef, TARGET_REF_OBJ.append(OrgType.F_COST_CENTER), defs);
            //SearchFactory.addSearchRefDef(containerDef, TARGET_REF_OBJ.append(OrgType.F_PARENT_ORG_REF), defs, getPageBase());
            SearchFactory.addSearchPropertyDef(containerDef, TARGET_REF_OBJ.append(OrgType.F_LOCALITY), defs);
        }
    }
}
