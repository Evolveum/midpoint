/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.web.application.PanelDescription;

import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;

@PanelDescription(panelIdentifier = "focusMappingsAssignments",
        identifier = "focusMappingsAssignments",
        applicableFor = AbstractRoleType.class,
        childOf = AssignmentHolderAssignmentPanel.class)
@PanelDisplay(label = "Focus mappings")
public class FocusMappingsAssignmentPanel<AR extends AbstractRoleType> extends AssignmentPanel<AR> {
    private static final long serialVersionUID = 1L;

    public FocusMappingsAssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel){
        super(id, assignmentContainerWrapperModel);
    }

    public FocusMappingsAssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel, ContainerPanelConfigurationType config){
        super(id, assignmentContainerWrapperModel, config);
    }

    public FocusMappingsAssignmentPanel(String id, LoadableModel<PrismObjectWrapper<AR>> assignmentContainerWrapperModel, ContainerPanelConfigurationType config) {
        super(id, PrismContainerWrapperModel.fromContainerWrapper(assignmentContainerWrapperModel, AssignmentHolderType.F_ASSIGNMENT), config);
    }

    @Override
    protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
        List<SearchItemDefinition> defs = new ArrayList<>();

        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_FOCUS_MAPPINGS, MappingsType.F_DESCRIPTION), defs);
        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_FOCUS_MAPPINGS, MappingsType.F_MAPPING, MappingType.F_NAME), defs);
        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_FOCUS_MAPPINGS, MappingsType.F_MAPPING, MappingType.F_STRENGTH), defs);

        defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));

        return defs;
    }

    @Override
    protected ObjectQuery getCustomizeQuery() {
        return getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_FOCUS_MAPPINGS).build();
    }
}
