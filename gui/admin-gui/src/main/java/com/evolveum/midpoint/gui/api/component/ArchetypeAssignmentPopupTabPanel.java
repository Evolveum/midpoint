/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.commons.collections.map.HashedMap;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by honchar
 */
public class ArchetypeAssignmentPopupTabPanel extends AbstractAssignmentPopupTabPanel<ArchetypeType>{

    private static final long serialVersionUID = 1L;

    private static final String ID_RELATION_CONTAINER = "relationContainer";
    private static final String ID_RELATION = "relation";

    private static final String DOT_CLASS = FocusTypeAssignmentPopupTabPanel.class.getName();
    private static final String OPERATION_LOAD_ASSIGNABLE_ROLES = DOT_CLASS + "loadAssignableRoles";

    public ArchetypeAssignmentPopupTabPanel(String id){
        super(id, ObjectTypes.ARCHETYPE);
    }

    protected QName getSupportedRelation() {
        return SchemaConstants.ORG_DEFAULT;
    }

    @Override
    protected ObjectQuery addFilterToContentQuery(ObjectQuery query){

        return query;
    }

    protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
        return null;
    }

    @Override
    protected void initParametersPanel(Fragment parametersPanel) {
        WebMarkupContainer relationContainer = new WebMarkupContainer(ID_RELATION_CONTAINER);
        relationContainer.setOutputMarkupId(true);
        parametersPanel.add(relationContainer);
    }

    @Override
    protected ObjectTypes getObjectType(){
        return ObjectTypes.ARCHETYPE;
    }

    @Override
    protected Map<String, AssignmentType> getSelectedAssignmentsMap(){
        Map<String, AssignmentType> assignmentsMap = new HashedMap();

        List<ArchetypeType> selectedObjects = getSelectedObjectsList();
        selectedObjects.forEach(selectedObject -> {
            assignmentsMap.put(selectedObject.getOid(), ObjectTypeUtil.createAssignmentTo(selectedObject, getSupportedRelation()));
        });
        return assignmentsMap;
    }
}
