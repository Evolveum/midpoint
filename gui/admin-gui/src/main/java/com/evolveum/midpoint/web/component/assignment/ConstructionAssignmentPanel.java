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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

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
//        columns.add(new PrismPropertyColumn<AssignmentType>(
//                new ContainerWrapperOnlyForHeaderModel(getModel(), AssignmentType.F_CONSTRUCTION, getPageBase()),
//                ConstructionType.F_KIND, getPageBase()) {
//                    private static final long serialVersionUID = 1L;
//
//                    @Override
//                    public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId, IModel<ContainerValueWrapper<AssignmentType>> assignmentModel) {
//                        AssignmentType assignment = assignmentModel.getObject().getContainerValue().asContainerable();
//                        String kindValue = "";
//                        if (assignment.getConstruction() != null){
//                            ConstructionType construction = assignment.getConstruction();
//                            kindValue = construction.getKind() != null && !StringUtils.isEmpty(construction.getKind().value()) ?
//                                    construction.getKind().value() : createStringResource("AssignmentEditorPanel.undefined").getString();
//                        }
//                        item.add(new Label(componentId, kindValue));
//                    }
//        });
        columns.add(new PrismPropertyWrapperColumn<>(getModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_INTENT), ColumnType.STRING, getPageBase()));
//        columns.add(new PrismPropertyColumn<AssignmentType>(
//                new ContainerWrapperOnlyForHeaderModel(getModel(), AssignmentType.F_CONSTRUCTION, getPageBase()),
//                ConstructionType.F_INTENT, getPageBase()) {
//                    private static final long serialVersionUID = 1L;
//
//                    @Override
//                    public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId, IModel<ContainerValueWrapper<AssignmentType>> assignmentModel) {
//                        AssignmentType assignment = assignmentModel.getObject().getContainerValue().asContainerable();
//                        String intentValue = "";
//                        if (assignment.getConstruction() != null){
//                            ConstructionType construction = assignment.getConstruction();
//                            intentValue = !StringUtils.isEmpty(construction.getIntent()) ? construction.getIntent()
//                                    : createStringResource("AssignmentEditorPanel.undefined").getString();
//                        }
//                        item.add(new Label(componentId, intentValue));
//                    }
//        });

        return columns;
    }

    @Override
    protected ObjectQuery createObjectQuery(){
        return getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_CONSTRUCTION)
                .build();
    }

    @Override
    protected IModel<PrismContainerWrapper> getSpecificContainerModel(IModel<PrismContainerValueWrapper<AssignmentType>> modelObject) {
        AssignmentType assignment = modelObject.getObject().getRealValue();
        if (ConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(assignment))) {
            return (IModel) PrismContainerWrapperModel.fromContainerValueWrapper(modelObject, AssignmentType.F_CONSTRUCTION);
//            PrismContainerWrapper<ConstructionType> constructionWrapper = modelObject.findContainer(ItemPath.create(AssignmentType.F_CONSTRUCTION));
//
//            return Model.of(constructionWrapper);
        }

        if (PersonaConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(assignment))) {
            return (IModel) PrismContainerWrapperModel.fromContainerValueWrapper(modelObject, AssignmentType.F_CONSTRUCTION);
            //TODO is it correct? findContainerWrapper by path F_PERSONA_CONSTRUCTION will return PersonaConstructionType
            //but not PolicyRuleType
//            PrismContainerWrapper<PolicyRuleType> personasWrapper = modelObject.findContainer(ItemPath.create(AssignmentType.F_PERSONA_CONSTRUCTION));
//
//            return Model.of(personasWrapper);
        }
        return Model.of();
    }

}
