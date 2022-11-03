/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@PanelType(name = "focusMappingsAssignments")
@PanelInstance(identifier = "focusMappingsAssignments",
        applicableForType = AbstractRoleType.class,
        childOf = AssignmentHolderAssignmentPanel.class,
        display = @PanelDisplay(label = "AssignmentType.focusMappings", order = 70))
public class FocusMappingsAssignmentsPanel<AH extends AssignmentHolderType> extends AbstractAssignmentPanel<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusMappingsAssignmentsPanel.class);

    public FocusMappingsAssignmentsPanel(String id, IModel<PrismObjectWrapper<AH>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected QName getAssignmentType() {
        return null;
    }

//    @Override
//    protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
//        List<SearchItemDefinition> defs = new ArrayList<>();
//
//        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_FOCUS_MAPPINGS, MappingsType.F_DESCRIPTION), defs);
//        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_FOCUS_MAPPINGS, MappingsType.F_MAPPING, MappingType.F_NAME), defs);
//        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_FOCUS_MAPPINGS, MappingsType.F_MAPPING, MappingType.F_STRENGTH), defs);
//
//        defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));
//
//        return defs;
//    }

    protected ObjectQuery createCustomizeQuery() {
        return getPageBase().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_FOCUS_MAPPINGS).build();
    }

    @Override
    protected ObjectQuery getCustomizeQuery() {
        // CustomizeQuery is not repo indexed
        if (isRepositorySearchEnabled()) {
            return null;
        }
        return createCustomizeQuery();
    }

//    @Override
//    protected void addSpecificSearchableItems(PrismContainerDefinition<AssignmentType> containerDef, List<SearchItemDefinition> defs) {
//
//    }

    @Override
    protected List<PrismContainerValueWrapper<AssignmentType>> customPostSearch(
            List<PrismContainerValueWrapper<AssignmentType>> list) {
        // customizeQuery is not repository supported, so we need to prefilter list using in-memory search
        if (isRepositorySearchEnabled()) {
            return prefilterUsingQuery(list, createCustomizeQuery());
        }
        return super.customPostSearch(list);
    }
}
