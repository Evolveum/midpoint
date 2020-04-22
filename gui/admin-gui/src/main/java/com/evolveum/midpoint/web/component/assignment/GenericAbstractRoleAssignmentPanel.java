/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

public class GenericAbstractRoleAssignmentPanel extends AbstractRoleAssignmentPanel {

    private static final long serialVersionUID = 1L;

    public GenericAbstractRoleAssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
        super(id, assignmentContainerWrapperModel);
    }

    @Override
    protected List<PrismContainerValueWrapper<AssignmentType>> customPostSearch(List<PrismContainerValueWrapper<AssignmentType>> assignments) {

        if(assignments == null) {
            return null;
        }

        List<PrismContainerValueWrapper<AssignmentType>> resultList = new ArrayList<>();
        Task task = getPageBase().createSimpleTask("load assignment targets");
        Iterator<PrismContainerValueWrapper<AssignmentType>> assignmentIterator = assignments.iterator();
        while (assignmentIterator.hasNext()) {
            PrismContainerValueWrapper<AssignmentType> ass = assignmentIterator.next();
            AssignmentType assignment = ass.getRealValue();
            if (assignment == null || assignment.getTargetRef() == null) {
                continue;
            }
            if (QNameUtil.match(assignment.getTargetRef().getType(), OrgType.COMPLEX_TYPE)) {
                PrismObject<OrgType> org = WebModelServiceUtils.loadObject(assignment.getTargetRef(), getPageBase(), task, task.getResult());
                if (org != null) {
                    if (FocusTypeUtil.determineSubTypes(org).contains("access")) {
                        resultList.add(ass);
                    }
                }
            }

        }

        return resultList;
    }

    protected ObjectFilter getSubtypeFilter(){
        ObjectFilter filter = getPageBase().getPrismContext().queryFor(OrgType.class)
                .block()
                .item(OrgType.F_SUBTYPE)
                .contains("access")
                .or()
                .item(OrgType.F_ORG_TYPE)
                .contains("access")
                .endBlock()
                .buildFilter();
        return filter;
    }

    @Override
    protected QName getAssignmentType() {
        return OrgType.COMPLEX_TYPE;
    }

}
