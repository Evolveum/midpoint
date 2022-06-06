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

import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.assignment.AbstractRoleAssignmentPanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

@PanelType(name = "dataProtectionAssignments", experimental = true)
@PanelInstance(identifier = "dataProtectionAssignments",
        applicableForType = FocusType.class,
        childOf = AssignmentHolderAssignmentPanel.class,
        display = @PanelDisplay(label = "pageAdminFocus.dataProtection"))
public class GenericAbstractRoleAssignmentPanel<F extends FocusType> extends AbstractAssignmentPanel<F> {

    private static final long serialVersionUID = 1L;

    public GenericAbstractRoleAssignmentPanel(String id, IModel<PrismObjectWrapper<F>> assignmentContainerWrapperModel, ContainerPanelConfigurationType config) {
        super(id, assignmentContainerWrapperModel, config);
    }

    @Override
    protected ObjectQuery getCustomizeQuery() {
        // This should do customPostSearch on repository level.
        return QueryBuilder.queryFor(AssignmentType.class, getPrismContext())
            .ref(AssignmentType.F_TARGET_REF, OrgType.COMPLEX_TYPE, null)
            .item(ObjectType.F_SUBTYPE).contains("access")
            .build();
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
                .endBlock()
                .buildFilter();
    }

    @Override
    protected QName getAssignmentType() {
        return OrgType.COMPLEX_TYPE;
    }

}
