/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

public abstract class AbstractRoleAssignmentPanel<AH extends AssignmentHolderType> extends AbstractAssignmentPanel<AH> {

    public AbstractRoleAssignmentPanel(String id, IModel<PrismObjectWrapper<AH>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }
    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<>(
                createStringResource("AbstractRoleAssignmentPanel.relationLabel")) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> item, String componentId, IModel<PrismContainerValueWrapper<AssignmentType>> assignmentModel) {
                item.add(new Label(componentId, WebComponentUtil.getRelationLabelValue(assignmentModel.getObject(), getPageBase())));
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("AbstractRoleAssignmentPanel.identifierLabel")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> item, String componentId,
                    final IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                item.add(new Label(componentId, getIdentifierLabelModel(rowModel.getObject())));
            }
        });

        return columns;
    }

    private <AR extends AbstractRoleType> IModel<String> getIdentifierLabelModel(PrismContainerValueWrapper<AssignmentType> assignmentContainer) {
        if (assignmentContainer == null || assignmentContainer.getRealValue() == null) {
            return Model.of("");
        }
        PrismObject<AR> targetObject = loadTargetObject(assignmentContainer.getRealValue());
        if (targetObject != null) {
            AR targetRefObject = targetObject.asObjectable();
            if (StringUtils.isNotEmpty(targetRefObject.getIdentifier())) {
                return Model.of(targetRefObject.getIdentifier());
            }
            if (targetRefObject.getDisplayName() != null && !targetRefObject.getName().getOrig().equals(targetRefObject.getDisplayName().getOrig())) {
                return Model.of(targetRefObject.getName().getOrig());
            }
        }
        return Model.of("");
    }

    @Override
    protected void addSpecificSearchableItems(PrismContainerDefinition<AssignmentType> containerDef,
            List<SearchItemDefinition> defs) {
        super.addSpecificSearchableItems(containerDef, defs);
        if (isRepositorySearchEnabled()) {
            SearchFactory.addSearchPropertyDef(containerDef, TARGET_REF_OBJ.append(AbstractRoleType.F_NAME), defs);
            SearchFactory.addSearchPropertyDef(containerDef, TARGET_REF_OBJ.append(AbstractRoleType.F_DISPLAY_NAME), defs);
        }
    }
}
