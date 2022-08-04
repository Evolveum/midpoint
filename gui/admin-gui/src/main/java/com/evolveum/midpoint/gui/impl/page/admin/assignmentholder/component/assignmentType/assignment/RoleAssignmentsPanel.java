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
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismReferenceWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "roleAssignments")
@PanelInstance(identifier = "roleAssignments",
        applicableForType = FocusType.class,
        childOf = AssignmentHolderAssignmentPanel.class,
        display = @PanelDisplay(label = "ObjectType.RoleType", icon = GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, order = 20))
public class RoleAssignmentsPanel<AH extends AssignmentHolderType> extends AbstractRoleAssignmentPanel<AH> {

    public RoleAssignmentsPanel(String id, IModel<PrismObjectWrapper<AH>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

//    @Override
//    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
//        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = super.initColumns();
//
//        columns.add(new PrismReferenceWrapperColumn<AssignmentType, ObjectReferenceType>(getContainerModel(), AssignmentType.F_TENANT_REF, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
//        columns.add(new PrismReferenceWrapperColumn<AssignmentType, ObjectReferenceType>(getContainerModel(), AssignmentType.F_ORG_REF, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
//        return columns;
//    }

    @Override
    protected QName getAssignmentType() {
        return RoleType.COMPLEX_TYPE;
    }
}
