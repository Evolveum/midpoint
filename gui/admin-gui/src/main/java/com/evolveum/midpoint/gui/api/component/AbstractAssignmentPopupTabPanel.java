/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Map;

/**
 * Created by honchar.
 */
public abstract class AbstractAssignmentPopupTabPanel<O extends ObjectType> extends AbstractPopupTabPanel<O> {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = AbstractAssignmentPopupTabPanel.class.getName();

    public AbstractAssignmentPopupTabPanel(String id, ObjectTypes type){
        super(id);
    }

    protected abstract Map<String, AssignmentType> getSelectedAssignmentsMap();
}
