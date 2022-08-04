/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import com.evolveum.midpoint.gui.impl.component.search.AbstractSearchItemWrapper;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "constructionAssignments")
@PanelInstance(identifier = "constructionAssignments",
        applicableForType = FocusType.class,
        childOf = AssignmentHolderAssignmentPanel.class,
        display = @PanelDisplay(label = "ObjectType.ResourceType", icon = GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON, order = 50))
public class ConstructionAssignmentsPanel<AH extends AssignmentHolderType> extends AbstractAssignmentPanel<AH> {

    public ConstructionAssignmentsPanel(String id, IModel<PrismObjectWrapper<AH>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
        List<SearchItemDefinition> defs = super.createSearchableItems(containerDef);
        SearchFactory.addSearchRefDef(containerDef, ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
        return defs;
    }

    @Override
    protected List<? super AbstractSearchItemWrapper> createSearchableItemWrappers(PrismContainerDefinition<AssignmentType> containerDef) {
        List<? super AbstractSearchItemWrapper> defs = super.createSearchableItemWrappers(containerDef);
        SearchFactory.addSearchRefWrapper(containerDef, ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
        return defs;
    }

    @Override
    protected QName getAssignmentType(){
        return ResourceType.COMPLEX_TYPE;
    }

//    @Override
//    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
//        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();
//
//        columns.add(new PrismPropertyWrapperColumn<AssignmentType, String>(getContainerModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_KIND), AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
//        columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_INTENT), AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
//        return columns;
//    }

    @Override
    protected ObjectQuery getCustomizeQuery() {
        return getPageBase().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_CONSTRUCTION).build();
    }
}
