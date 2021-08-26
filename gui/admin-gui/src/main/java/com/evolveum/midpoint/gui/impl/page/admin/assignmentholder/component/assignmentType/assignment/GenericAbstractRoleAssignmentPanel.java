/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.assignment.AbstractRoleAssignmentPanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "dataProtectionAssignments", experimental = true)
@PanelInstance(identifier = "dataProtectionAssignments",
        applicableFor = FocusType.class,
        childOf = AssignmentHolderAssignmentPanel.class)
@PanelDisplay(label = "Data protection")
public class GenericAbstractRoleAssignmentPanel<F extends FocusType> extends AbstractRoleAssignmentPanel<F> {

    private static final long serialVersionUID = 1L;

    public GenericAbstractRoleAssignmentPanel(String id, LoadableModel<PrismObjectWrapper<F>> assignmentContainerWrapperModel, ContainerPanelConfigurationType config) {
        super(id, PrismContainerWrapperModel.fromContainerWrapper(assignmentContainerWrapperModel, AssignmentHolderType.F_ASSIGNMENT), config);
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
        return getPageBase().getPrismContext().queryFor(OrgType.class)
                .block()
                .item(OrgType.F_SUBTYPE)
                .contains("access")
                .or()
                .item(OrgType.F_ORG_TYPE)
                .contains("access")
                .endBlock()
                .buildFilter();
    }

    @Override
    protected QName getAssignmentType() {
        return OrgType.COMPLEX_TYPE;
    }

}
