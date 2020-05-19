/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.schema.constants.ObjectTypes;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import javax.xml.namespace.QName;

public class GdprAssignmentPanel extends AbstractRoleAssignmentPanel {

    private static final long serialVersionUID = 1L;

    public GdprAssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
        super(id, assignmentContainerWrapperModel);
    }


    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<AssignmentType, String>(getModel(), AssignmentType.F_LIFECYCLE_STATE, ColumnType.STRING, getPageBase()));

        columns.add(new CheckBoxColumn<PrismContainerValueWrapper<AssignmentType>>(createStringResource("AssignmentType.accepted")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<Boolean> getEnabled(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                return Model.of(Boolean.FALSE);
            }

            @Override
            protected IModel<Boolean> getCheckBoxValueModel(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                return new IModel<Boolean>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean getObject() {
                        AssignmentType assignmentType = rowModel.getObject().getRealValue();
                        if (assignmentType.getLifecycleState() == null) {
                            return Boolean.FALSE;
                        }

                        if (assignmentType.getLifecycleState().equals(SchemaConstants.LIFECYCLE_ACTIVE)) {
                            return Boolean.TRUE;
                        }

                        return Boolean.FALSE;
                    }
                };
            }

        });

        return columns;
    }

//    @Override
//    protected <T extends ObjectType> void addSelectedAssignmentsPerformed(AjaxRequestTarget target, List<T> assignmentsList,
//            QName relation, ShadowKindType kind, String intent) {
//        super.addSelectedAssignmentsPerformed(target, assignmentsList, SchemaConstants.ORG_CONSENT, kind, intent);
//    }

    @Override
    protected QName getPredefinedRelation() {
        return SchemaConstants.ORG_CONSENT;
    }

    protected ObjectQuery createObjectQuery() {
        return getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .refRelation(SchemaConstants.ORG_CONSENT)
                .endBlock()
                .build();
    }

    @Override
    protected List<ObjectTypes> getObjectTypesList() {
        return Arrays.asList(ObjectTypes.ROLE, ObjectTypes.ORG, ObjectTypes.SERVICE);
    }
}
