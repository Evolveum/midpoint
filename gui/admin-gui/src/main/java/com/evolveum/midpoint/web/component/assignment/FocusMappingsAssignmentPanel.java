/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.query.ObjectFilter;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class FocusMappingsAssignmentPanel extends AssignmentPanel {
    private static final long serialVersionUID = 1L;

    public FocusMappingsAssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel){
        super(id, assignmentContainerWrapperModel);
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
    protected ObjectQuery customizeContentQuery(ObjectQuery query) {
        if (query == null) {
            query = getPrismContext().queryFor(AssignmentType.class).build();
        }
        ObjectFilter filter = getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_FOCUS_MAPPINGS).buildFilter();
        query.addFilter(filter);
        return query;
    }

    @Override
    protected ItemVisibility getTypedContainerVisibility(ItemWrapper<?, ?> wrapper) {
        if (QNameUtil.match(AssignmentType.F_TARGET_REF, wrapper.getItemName())) {
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(AssignmentType.F_TENANT_REF, wrapper.getItemName())) {
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(AssignmentType.F_ORG_REF, wrapper.getItemName())) {
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(PolicyRuleType.COMPLEX_TYPE, wrapper.getTypeName())){
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(PersonaConstructionType.COMPLEX_TYPE, wrapper.getTypeName())){
            return ItemVisibility.HIDDEN;
        }

        if (QNameUtil.match(ConstructionType.COMPLEX_TYPE, wrapper.getTypeName())){
            return ItemVisibility.HIDDEN;
        }

        return ItemVisibility.AUTO;
    }

}
