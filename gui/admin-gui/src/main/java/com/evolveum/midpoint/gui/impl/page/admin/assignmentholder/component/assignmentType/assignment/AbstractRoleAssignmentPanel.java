/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

public abstract class AbstractRoleAssignmentPanel<AH extends AssignmentHolderType> extends AbstractAssignmentPanel<AH> {

    public AbstractRoleAssignmentPanel(String id, IModel<PrismObjectWrapper<AH>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<>(
                createStringResource("AbstractRoleAssignmentPanel.relationLabel")) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> item, String componentId, IModel<PrismContainerValueWrapper<AssignmentType>> assignmentModel) {
                item.add(new Label(componentId, RelationUtil.getRelationLabelValue(assignmentModel.getObject(), getPageBase())));
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("AbstractRoleAssignmentPanel.identifierLabel")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> item, String componentId,
                    final IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                item.add(new Label(componentId, AssignmentsUtil.getIdentifierLabelModel(rowModel.getObject().getRealValue(), getPageBase())));
            }
        });

        return columns;
    }

}
