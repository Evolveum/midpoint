/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

/**
 * Created by honchar.
 */
public class ConstructionAssignmentPanel extends AssignmentPanel {
    private static final long serialVersionUID = 1L;

    public ConstructionAssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel){
        super(id, assignmentContainerWrapperModel);
    }

    @Override
    protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
        List<SearchItemDefinition> defs = new ArrayList<>();

        SearchFactory.addSearchRefDef(containerDef, ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), defs);
        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), defs);

        defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));

        return defs;
    }

    @Override
    protected QName getAssignmentType(){
        return ResourceType.COMPLEX_TYPE;
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<AssignmentType, String>(getModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_KIND), ColumnType.STRING, getPageBase()));
        columns.add(new PrismPropertyWrapperColumn<>(getModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_INTENT), ColumnType.STRING, getPageBase()));
        return columns;
    }

    @Override
    protected ObjectQuery createObjectQuery(){
        return getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_CONSTRUCTION)
                .build();
    }
//
//    @Override
//    protected IModel<PrismContainerWrapper> getSpecificContainerModel(IModel<PrismContainerValueWrapper<AssignmentType>> modelObject) {
//        AssignmentType assignment = modelObject.getObject().getRealValue();
//        if (ConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(assignment))) {
//            return (IModel) PrismContainerWrapperModel.fromContainerValueWrapper(modelObject, AssignmentType.F_CONSTRUCTION);
//        }
//
//        if (PersonaConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(assignment))) {
//            return (IModel) PrismContainerWrapperModel.fromContainerValueWrapper(modelObject, AssignmentType.F_CONSTRUCTION);
//            //TODO is it correct? findContainerWrapper by path F_PERSONA_CONSTRUCTION will return PersonaConstructionType
//            //but not PolicyRuleType
//        }
//        return Model.of();
//    }

    @Override
    protected ItemVisibility getTypedContainerVisibility(ItemWrapper<?, ?, ?, ?> wrapper) {
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

        return ItemVisibility.AUTO;
    }

    @Override
    protected boolean getContainerReadability(ItemWrapper<?, ?, ?, ?> wrapper) {
        if (QNameUtil.match(ConstructionType.F_KIND, wrapper.getItemName())) {
            return false;
        }

        if (QNameUtil.match(ConstructionType.F_INTENT, wrapper.getItemName())) {
            return false;
        }

        return true;
    }
}
