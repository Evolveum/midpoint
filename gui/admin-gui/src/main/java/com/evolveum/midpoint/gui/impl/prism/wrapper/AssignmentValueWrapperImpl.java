/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.AssignmentValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;

/**
 * @author skublik
 *
 */
public class AssignmentValueWrapperImpl extends PrismContainerValueWrapperImpl<AssignmentType> implements AssignmentValueWrapper, Comparable<AssignmentValueWrapper> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentValueWrapperImpl.class);

    boolean direct = false;
    private ObjectType assignmentParent;

    public AssignmentValueWrapperImpl(PrismContainerWrapper<AssignmentType> parent, PrismContainerValue<AssignmentType> pcv, ValueStatus status) {
        super(parent, pcv, status);
    }

    @Override
    public boolean isDirectAssignment() {
        return direct;
    }

    @Override
    public void setDirectAssignment(boolean isDirect) {
        this.direct = isDirect;
    }

    @Override
    public ObjectType getAssignmentParent() {
        return assignmentParent;
    }

    @Override
    public void setAssignmentParent(AssignmentPath assignmentPath) {
        if (assignmentPath != null && assignmentPath.size() > 1 ) {
            assignmentParent = assignmentPath.last().getSource();
        }
    }

    private PrismReferenceWrapper<Referencable> getTargetRef(AssignmentValueWrapper object){
        PrismReferenceWrapper<Referencable> targetRef = null;
        try {
            targetRef = object.findReference(AssignmentType.F_TARGET_REF);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find reference with path " + AssignmentType.F_TARGET_REF.getLocalPart());
        }
        return targetRef;
    }

    @Override
    public int compareTo(AssignmentValueWrapper other) {
        if (this.equals(other)) {
            return 0;
        }

        // firstly sorting by type: orgs -> roles -> resources -> all the other (in the future)
        PrismReferenceWrapper<Referencable> targetRef1 = getTargetRef(this);
        PrismReferenceWrapper<Referencable> targetRef2 = getTargetRef(other);

        if (targetRef1 != null && targetRef2 != null) {
            Referencable targetRefValue1 = targetRef1.getItem().getRealValue();
            Referencable targetRefValue2 = targetRef2.getItem().getRealValue();
            int co1 = getClassOrder(targetRefValue1.getType());
            int co2 = getClassOrder(targetRefValue2.getType());
            if (co1 != co2) {
                return co1 - co2;
            }

            // then by name
            PolyStringType targetName1 = targetRefValue1.getTargetName();
            PolyStringType targetName2 = targetRefValue2.getTargetName();
            if (targetName1 != null && targetName2 != null) {
                int order = targetName1.getOrig().compareToIgnoreCase(targetName2.getOrig());
                if (order != 0) {
                    return order;
                }
            } else if (targetName1 != null && targetName2 == null) {
                return -1;      // named are before unnamed
            } else if (targetName1 == null && targetName2 != null) {
                return 1;       // unnamed are after named
            }
        }

        // if class and names are equal, the order can be arbitrary

        if (this.hashCode() <= other.hashCode()) {
            return -1;
        } else {
            return 1;
        }
    }

    private int getClassOrder(QName targetType) {
        if (QNameUtil.match(OrgType.COMPLEX_TYPE, targetType)) {
            return 0;
        } else if (QNameUtil.match(RoleType.COMPLEX_TYPE, targetType)) {
            return 1;
        } else if (QNameUtil.match(ResourceType.COMPLEX_TYPE, targetType)) {
            return 2;
        } else {
            return 3;
        }
    }
}
