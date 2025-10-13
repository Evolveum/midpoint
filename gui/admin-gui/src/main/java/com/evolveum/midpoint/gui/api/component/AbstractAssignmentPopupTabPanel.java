/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.List;
import java.util.Map;

/**
 * Created by honchar.
 */
public abstract class AbstractAssignmentPopupTabPanel<O extends ObjectType> extends AbstractPopupTabPanel<O> {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = AbstractAssignmentPopupTabPanel.class.getName();

    private final AssignmentObjectRelation assignmentObjectRelation;
    private final ObjectTypes type;

    public AbstractAssignmentPopupTabPanel(String id, ObjectTypes type, AssignmentObjectRelation relationSpec){
        super(id);
        this.type = type;
        this.assignmentObjectRelation = relationSpec;
    }

    protected abstract Map<String, AssignmentType> getSelectedAssignmentsMap();

    public AssignmentObjectRelation getAssignmentObjectRelation() {
        return assignmentObjectRelation;
    }

    @Override
    protected List<ObjectReferenceType> getArchetypeRefList() {
        if (assignmentObjectRelation == null) {
            return null;
        }
        return assignmentObjectRelation.getArchetypeRefs();
    }

    @Override
    protected ObjectTypes getObjectType() {
        return type;
    }
}
