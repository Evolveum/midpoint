package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by honchar.
 */
public class ConstructionAssignmentPanel extends AssignmentPanel {
    private static final long serialVersionUID = 1L;

    public ConstructionAssignmentPanel(String id, IModel<ContainerWrapper<AssignmentType>> assignmentContainerWrapperModel){
        super(id, assignmentContainerWrapperModel);
    }

    @Override
    protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
        List<SearchItemDefinition> defs = new ArrayList<>();

        SearchFactory.addSearchRefDef(containerDef, new ItemPath(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
        SearchFactory.addSearchPropertyDef(containerDef, new ItemPath(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), defs);
        SearchFactory.addSearchPropertyDef(containerDef, new ItemPath(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), defs);

        defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));

        return defs;
    }

    @Override
    protected QName getAssignmentType(){
        return ResourceType.COMPLEX_TYPE;
    }

    @Override
    protected List<IColumn<ContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<ContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(
                createStringResource("ConstructionType.kind")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId, IModel<ContainerValueWrapper<AssignmentType>> assignmentModel) {
                AssignmentType assignment = assignmentModel.getObject().getContainerValue().asContainerable();
                String kindValue = "";
                if (assignment.getConstruction() != null){
                    ConstructionType construction = assignment.getConstruction();
                    kindValue = construction.getKind() != null && !StringUtils.isEmpty(construction.getKind().value()) ?
                            construction.getKind().value() : createStringResource("AssignmentEditorPanel.undefined").getString();
                }
                item.add(new Label(componentId, kindValue));
            }
        });
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(
                createStringResource("ConstructionType.intent")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId, IModel<ContainerValueWrapper<AssignmentType>> assignmentModel) {
                AssignmentType assignment = assignmentModel.getObject().getContainerValue().asContainerable();
                String intentValue = "";
                if (assignment.getConstruction() != null){
                    ConstructionType construction = assignment.getConstruction();
                    intentValue = !StringUtils.isEmpty(construction.getIntent()) ? construction.getIntent()
                            : createStringResource("AssignmentEditorPanel.undefined").getString();
                }
                item.add(new Label(componentId, intentValue));
            }
        });

        return columns;
    }

    @Override
    protected ObjectQuery createObjectQuery(){
        return QueryBuilder.queryFor(AssignmentType.class, getParentPage().getPrismContext())
                .exists(AssignmentType.F_CONSTRUCTION)
                .build();
    }

    @Override
    protected IModel<ContainerWrapper> getSpecificContainerModel(ContainerValueWrapper<AssignmentType> modelObject) {
        if (ConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(modelObject.getContainerValue().getValue()))) {
            ContainerWrapper<ConstructionType> constructionWrapper = modelObject.findContainerWrapper(new ItemPath(modelObject.getPath(),
                    AssignmentType.F_CONSTRUCTION));

            return Model.of(constructionWrapper);
        }

        if (PersonaConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(modelObject.getContainerValue().getValue()))) {
            //TODO is it correct? findContainerWrapper by path F_PERSONA_CONSTRUCTION will return PersonaConstructionType
            //but not PolicyRuleType
            ContainerWrapper<PolicyRuleType> personasWrapper = modelObject.findContainerWrapper(new ItemPath(modelObject.getPath(),
                    AssignmentType.F_PERSONA_CONSTRUCTION));

            return Model.of(personasWrapper);
        }
        return Model.of();
    }

    @Override
    protected ItemVisibility getAssignmentBasicTabVisibity(ItemWrapper itemWrapper, ItemPath parentAssignmentPath, ItemPath assignmentPath, PrismContainerValue<AssignmentType> prismContainerValue) {
        if (itemWrapper.getPath().containsName(AssignmentType.F_CONSTRUCTION)){
            return ItemVisibility.AUTO;
        } else {
            return super.getAssignmentBasicTabVisibity(itemWrapper, parentAssignmentPath, assignmentPath, prismContainerValue);
        }

    }
}
